/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Model;
import com.wynntils.core.net.hades.event.HadesEvent;
import com.wynntils.handlers.chat.MessageType;
import com.wynntils.handlers.chat.event.ChatMessageReceivedEvent;
import com.wynntils.mc.utils.ComponentUtils;
import com.wynntils.mc.utils.McUtils;
import com.wynntils.wynn.event.RelationsUpdateEvent;
import com.wynntils.wynn.event.WorldStateEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * This model handles the player's in-game relations, like friends, party info, guild info.
 */
public final class PlayerRelationsModel extends Model {
    private static final Pattern FRIEND_LIST_MESSAGE_PATTERN = Pattern.compile(".+'s friends \\(.+\\): (.*)");
    private static final Pattern FRIEND_NO_LIST_MESSAGE_PATTERN_1 = Pattern.compile("§eWe couldn't find any friends.");
    private static final Pattern FRIEND_NO_LIST_MESSAGE_PATTERN_2 =
            Pattern.compile("§eTry typing §r§6/friend add Username§r§e!");
    private static final Pattern FRIEND_REMOVE_MESSAGE_PATTERN =
            Pattern.compile("§e(.+) has been removed from your friends!");
    private static final Pattern FRIEND_ADD_MESSAGE_PATTERN = Pattern.compile("§e(.+) has been added to your friends!");

    private static final Pattern PARTY_LIST_MESSAGE_PATTERN = Pattern.compile("Party members: (.*)");
    private static final Pattern PARTY_NO_LIST_MESSAGE_PATTERN = Pattern.compile("§eYou must be in a party to list.");
    private static final Pattern PARTY_OTHER_LEAVE_MESSAGE_PATTERN = Pattern.compile("§e(.+) has left the party.");
    private static final Pattern PARTY_OTHER_JOIN_MESSAGE_PATTERN = Pattern.compile("§e(.+) has joined the party.");
    private static final Pattern PARTY_OTHER_JOIN_SWITCH_MESSAGE_PATTERN =
            Pattern.compile("§eSay hello to (.+) which just joined your party!");
    private static final Pattern PARTY_SELF_LEAVE_MESSAGE_PATTERN =
            Pattern.compile("§eYou have been removed from the party.");
    private static final Pattern PARTY_SELF_JOIN_MESSAGE_PATTERN =
            Pattern.compile("§eYou have successfully joined the party.");
    private static final Pattern PARTY_DISBAND = Pattern.compile("§eYour party has been disbanded.");

    private boolean expectingFriendMessage = false;
    private boolean expectingPartyMessage = false;

    private Set<String> friends;
    private Set<String> partyMembers;

    @Override
    public void init() {
        resetRelations();
    }

    @Override
    public void disable() {
        resetRelations();
    }

    @SubscribeEvent
    public void onAuth(HadesEvent.Authenticated event) {
        if (!Managers.WorldState.onWorld()) return;

        requestFriendListUpdate();
        requestPartyListUpdate();
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        if (event.getNewState() == WorldStateManager.State.WORLD) {
            requestFriendListUpdate();
            requestPartyListUpdate();
        } else {
            resetRelations();
        }
    }

    @SubscribeEvent
    public void onChatReceived(ChatMessageReceivedEvent event) {
        if (event.getMessageType() != MessageType.FOREGROUND) return;

        String coded = event.getOriginalCodedMessage();
        String unformatted = ComponentUtils.stripFormatting(coded);

        if (tryParseFriendMessages(coded)) {
            return;
        }

        if (tryParsePartyMessages(coded)) {
            return;
        }

        if (expectingFriendMessage) {
            if (tryParseFriendList(unformatted) || tryParseNoFriendList(coded)) {
                event.setCanceled(true);
                expectingFriendMessage = false;
                return;
            }

            // Skip first message of two, but still expect more messages
            if (FRIEND_NO_LIST_MESSAGE_PATTERN_1.matcher(coded).matches()) {
                event.setCanceled(true);
                return;
            }
        }

        if (expectingPartyMessage) {
            if (tryParseNoPartyMessage(coded) || tryParsePartyList(unformatted)) {
                event.setCanceled(true);
                expectingPartyMessage = false;
                return;
            }
        }
    }

    // region Party List Parsing

    private boolean tryParsePartyMessages(String coded) {
        if (PARTY_DISBAND.matcher(coded).matches()
                || PARTY_SELF_LEAVE_MESSAGE_PATTERN.matcher(coded).matches()) {
            WynntilsMod.info("Player left the party.");

            partyMembers = Set.of();
            WynntilsMod.postEvent(
                    new RelationsUpdateEvent.PartyList(partyMembers, RelationsUpdateEvent.ChangeType.RELOAD));
            return true;
        }

        if (PARTY_SELF_JOIN_MESSAGE_PATTERN.matcher(coded).matches()) {
            WynntilsMod.info("Player joined a party.");

            requestPartyListUpdate();
            return true;
        }

        Matcher matcher = PARTY_OTHER_JOIN_MESSAGE_PATTERN.matcher(coded);
        if (matcher.matches()) {
            String player = matcher.group(1);

            WynntilsMod.info("Player's party has a new member: " + player);

            partyMembers.add(player);
            WynntilsMod.postEvent(
                    new RelationsUpdateEvent.PartyList(Set.of(player), RelationsUpdateEvent.ChangeType.ADD));
            return true;
        }

        matcher = PARTY_OTHER_JOIN_SWITCH_MESSAGE_PATTERN.matcher(coded);
        if (matcher.matches()) {
            String player = matcher.group(1);

            WynntilsMod.info("Player's party has a new member #2: " + player);

            partyMembers.add(player);
            WynntilsMod.postEvent(
                    new RelationsUpdateEvent.PartyList(Set.of(player), RelationsUpdateEvent.ChangeType.ADD));
            return true;
        }

        matcher = PARTY_OTHER_LEAVE_MESSAGE_PATTERN.matcher(coded);
        if (matcher.matches()) {
            String player = matcher.group(1);

            WynntilsMod.info("Player's party has been left by an other player: " + player);

            partyMembers.remove(player);
            WynntilsMod.postEvent(
                    new RelationsUpdateEvent.PartyList(Set.of(player), RelationsUpdateEvent.ChangeType.REMOVE));
            return true;
        }

        return false;
    }

    private boolean tryParseNoPartyMessage(String coded) {
        if (PARTY_NO_LIST_MESSAGE_PATTERN.matcher(coded).matches()) {
            WynntilsMod.info("Player is not in a party.");
            return true;
        }

        return false;
    }

    private boolean tryParsePartyList(String unformatted) {
        Matcher matcher = PARTY_LIST_MESSAGE_PATTERN.matcher(unformatted);
        if (!matcher.matches()) return false;

        String[] partyList = matcher.group(1).split(", ");

        partyMembers = Arrays.stream(partyList).collect(Collectors.toSet());
        WynntilsMod.postEvent(new RelationsUpdateEvent.PartyList(partyMembers, RelationsUpdateEvent.ChangeType.RELOAD));

        WynntilsMod.info("Successfully updated party list, user has " + partyList.length + " friends.");
        return true;
    }

    // endregion

    // region Friend List Parsing

    private boolean tryParseNoFriendList(String coded) {
        if (FRIEND_NO_LIST_MESSAGE_PATTERN_2.matcher(coded).matches()) {
            WynntilsMod.info("Player has no friends!");
            return true;
        }

        return false;
    }

    private boolean tryParseFriendMessages(String coded) {
        Matcher matcher = FRIEND_REMOVE_MESSAGE_PATTERN.matcher(coded);
        if (matcher.matches()) {
            String player = matcher.group(1);

            WynntilsMod.info("Player has removed friend: " + player);

            friends.remove(player);
            WynntilsMod.postEvent(
                    new RelationsUpdateEvent.FriendList(Set.of(player), RelationsUpdateEvent.ChangeType.REMOVE));
            return true;
        }

        matcher = FRIEND_ADD_MESSAGE_PATTERN.matcher(coded);
        if (matcher.matches()) {
            String player = matcher.group(1);

            WynntilsMod.info("Player has added friend: " + player);

            friends.add(player);
            WynntilsMod.postEvent(
                    new RelationsUpdateEvent.FriendList(Set.of(player), RelationsUpdateEvent.ChangeType.ADD));
            return true;
        }

        return false;
    }

    private boolean tryParseFriendList(String unformatted) {
        Matcher matcher = FRIEND_LIST_MESSAGE_PATTERN.matcher(unformatted);
        if (!matcher.matches()) return false;

        String[] friendList = matcher.group(1).split(", ");

        friends = Arrays.stream(friendList).collect(Collectors.toSet());
        WynntilsMod.postEvent(new RelationsUpdateEvent.FriendList(friends, RelationsUpdateEvent.ChangeType.RELOAD));

        WynntilsMod.info("Successfully updated friend list, user has " + friendList.length + " friends.");
        return true;
    }

    // endregion

    private void resetRelations() {
        friends = new HashSet<>();
        partyMembers = new HashSet<>();

        WynntilsMod.postEvent(new RelationsUpdateEvent.FriendList(friends, RelationsUpdateEvent.ChangeType.RELOAD));
        WynntilsMod.postEvent(new RelationsUpdateEvent.PartyList(partyMembers, RelationsUpdateEvent.ChangeType.RELOAD));
    }

    public void requestFriendListUpdate() {
        if (McUtils.player() == null) return;

        expectingFriendMessage = true;
        McUtils.sendCommand("friend list");
        WynntilsMod.info("Requested friend list from Wynncraft.");
    }

    public void requestPartyListUpdate() {
        if (McUtils.player() == null) return;

        expectingPartyMessage = true;
        McUtils.sendCommand("party list");
        WynntilsMod.info("Requested party list from Wynncraft.");
    }
}
