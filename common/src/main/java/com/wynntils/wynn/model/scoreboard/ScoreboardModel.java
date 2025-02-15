/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model.scoreboard;

import com.wynntils.core.components.Handlers;
import com.wynntils.core.components.Managers;
import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import java.util.Set;
import java.util.regex.Pattern;

public final class ScoreboardModel extends Model {
    public static final Pattern GUILD_ATTACK_UPCOMING_PATTERN = Pattern.compile("Upcoming Attacks:");
    public static final Pattern QUEST_TRACK_PATTERN = Pattern.compile("Tracked Quest:");
    public static final Pattern OBJECTIVE_HEADER_PATTERN = Pattern.compile("([★⭑] )?(Daily )?Objectives?:");
    public static final Pattern GUILD_OBJECTIVE_HEADER_PATTERN = Pattern.compile("([★⭑] )?Guild Obj: (.+)");
    public static final Pattern PARTY_PATTERN = Pattern.compile("Party:\\s\\[Lv. (\\d+)]");

    @Override
    public void init() {
        Handlers.Scoreboard.registerListener(
                Managers.Objectives.SCOREBOARD_LISTENER, Set.of(SegmentType.Objective, SegmentType.GuildObjective));
        Handlers.Scoreboard.registerListener(Managers.Quest.SCOREBOARD_LISTENER, SegmentType.Quest);
        Handlers.Scoreboard.registerListener(Models.GuildAttackTimer.SCOREBOARD_LISTENER, SegmentType.GuildAttackTimer);

        Handlers.Scoreboard.init();
    }

    @Override
    public void disable() {
        Handlers.Scoreboard.disable();
    }

    public enum SegmentType {
        Quest(QUEST_TRACK_PATTERN),
        Party(PARTY_PATTERN),
        Objective(OBJECTIVE_HEADER_PATTERN),
        GuildObjective(GUILD_OBJECTIVE_HEADER_PATTERN),
        GuildAttackTimer(GUILD_ATTACK_UPCOMING_PATTERN);

        private final Pattern headerPattern;

        SegmentType(Pattern headerPattern) {
            this.headerPattern = headerPattern;
        }

        public Pattern getHeaderPattern() {
            return headerPattern;
        }
    }
}
