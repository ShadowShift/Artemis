/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model.guild.territory;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wynntils.core.components.Manager;
import com.wynntils.core.components.Managers;
import com.wynntils.core.net.Download;
import com.wynntils.core.net.NetManager;
import com.wynntils.core.net.UrlId;
import com.wynntils.mc.event.AdvancementUpdateEvent;
import com.wynntils.mc.utils.ComponentUtils;
import com.wynntils.wynn.model.guild.territory.objects.TerritoryInfo;
import com.wynntils.wynn.model.map.TerritoryDefenseFilterType;
import com.wynntils.wynn.model.map.poi.Poi;
import com.wynntils.wynn.model.map.poi.TerritoryPoi;
import com.wynntils.wynn.objects.profiles.TerritoryProfile;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.FrameType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class TerritoryManager extends Manager {
    private static final int TERRITORY_UPDATE_MS = 15000;
    private static final Gson TERRITORY_PROFILE_GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(TerritoryProfile.class, new TerritoryProfile.TerritoryDeserializer())
            .create();

    // This is territory POIs as returned by the advancement from Wynncraft
    private final Map<String, TerritoryPoi> territoryPoiMap = new ConcurrentHashMap<>();

    // This is the profiles as downloaded from Athena
    private Map<String, TerritoryProfile> territoryProfileMap = new HashMap<>();

    // This is just a cache of TerritoryPois created for all territoryProfileMap values
    private Set<TerritoryPoi> allTerritoryPois = new HashSet<>();

    private final ScheduledExecutorService timerExecutor = new ScheduledThreadPoolExecutor(1);

    public TerritoryManager(NetManager netManager) {
        super(List.of(netManager));
        timerExecutor.scheduleWithFixedDelay(
                this::updateTerritoryProfileMap, 0, TERRITORY_UPDATE_MS, TimeUnit.MILLISECONDS);
    }

    public TerritoryProfile getTerritoryProfile(String name) {
        return territoryProfileMap.get(name);
    }

    public Stream<String> getTerritoryNames() {
        return territoryProfileMap.keySet().stream();
    }

    public Set<TerritoryPoi> getTerritoryPois() {
        return allTerritoryPois;
    }

    public List<Poi> getTerritoryPoisFromAdvancement() {
        return new ArrayList<>(territoryPoiMap.values());
    }

    public List<Poi> getFilteredTerritoryPoisFromAdvancement(int filterLevel, TerritoryDefenseFilterType filterType) {
        return switch (filterType) {
            case HIGHER -> territoryPoiMap.values().stream()
                    .filter(poi -> poi.getTerritoryInfo().getDefences().getLevel() >= filterLevel)
                    .collect(Collectors.toList());
            case LOWER -> territoryPoiMap.values().stream()
                    .filter(poi -> poi.getTerritoryInfo().getDefences().getLevel() <= filterLevel)
                    .collect(Collectors.toList());
            case DEFAULT -> territoryPoiMap.values().stream()
                    .filter(poi -> poi.getTerritoryInfo().getDefences().getLevel() == filterLevel)
                    .collect(Collectors.toList());
        };
    }

    public TerritoryPoi getTerritoryPoiFromAdvancement(String name) {
        return territoryPoiMap.get(name);
    }

    @SubscribeEvent
    public void onAdvancementUpdate(AdvancementUpdateEvent event) {
        Map<String, TerritoryInfo> tempMap = new HashMap<>();

        for (Map.Entry<ResourceLocation, Advancement.Builder> added :
                event.getAdded().entrySet()) {
            added.getValue().parent((ResourceLocation) null);
            Advancement built = added.getValue().build(added.getKey());

            if (built.getDisplay() == null) continue;

            String territoryName = ComponentUtils.getUnformatted(
                            built.getDisplay().getTitle())
                    .replace("[", "")
                    .replace("]", "")
                    .trim();

            // Do not parse same thing twice
            if (tempMap.containsKey(territoryName)) continue;

            // ignore empty display texts they are used to generate the "lines"
            if (territoryName.isEmpty()) continue;

            // headquarters frame is challenge
            boolean headquarters = built.getDisplay().getFrame() == FrameType.CHALLENGE;

            // description is a raw string with \n, so we have to split
            String description = ComponentUtils.getCoded(built.getDisplay().getDescription());
            String[] colored = description.split("\n");
            String[] raw = ComponentUtils.stripFormatting(description).split("\n");

            TerritoryInfo container = new TerritoryInfo(raw, colored, headquarters);
            tempMap.put(territoryName, container);
        }

        for (Map.Entry<String, TerritoryInfo> entry : tempMap.entrySet()) {
            TerritoryProfile territoryProfile = getTerritoryProfile(entry.getKey());

            if (territoryProfile == null) continue;

            territoryPoiMap.put(entry.getKey(), new TerritoryPoi(territoryProfile, entry.getValue()));
        }
    }

    private void updateTerritoryProfileMap() {
        // dataAthenaTerritoryList is based on
        // https://api.wynncraft.com/public_api.php?action=territoryList
        // but guild prefix is injected based on
        // https://api.wynncraft.com/public_api.php?action=guildStats&command=<guildName>
        // and guild color is injected based on values maintained on Athena, and a constant
        // level = 1 is also injected.

        Download dl = Managers.Net.download(UrlId.DATA_ATHENA_TERRITORY_LIST);
        dl.handleJsonObject(json -> {
            if (!json.has("territories")) return;

            Type type = new TypeToken<HashMap<String, TerritoryProfile>>() {}.getType();
            territoryProfileMap = TERRITORY_PROFILE_GSON.fromJson(json.get("territories"), type);
            allTerritoryPois =
                    territoryProfileMap.values().stream().map(TerritoryPoi::new).collect(Collectors.toSet());
            // TODO: Add events if territories changed
        });
    }
}
