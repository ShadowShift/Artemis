/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.model;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Model;
import com.wynntils.mc.event.SubtitleSetTextEvent;
import com.wynntils.mc.utils.ComponentUtils;
import com.wynntils.wynn.event.ShamanMaskTitlePacketEvent;
import com.wynntils.wynn.event.WorldStateEvent;
import com.wynntils.wynn.objects.ShamanMaskType;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ShamanMaskModel extends Model {
    private static final Pattern MASK_PATTERN = Pattern.compile("§cMask of the (Coward|Lunatic|Fanatic)");

    private ShamanMaskType currentMaskType = ShamanMaskType.NONE;

    @SubscribeEvent
    public void onTitle(SubtitleSetTextEvent event) {
        String title = ComponentUtils.getCoded(event.getComponent());

        if (title.contains("Mask of the ") || title.contains("➤")) {
            parseMask(title);
            ShamanMaskTitlePacketEvent maskEvent = new ShamanMaskTitlePacketEvent();
            WynntilsMod.postEvent(maskEvent);

            if (maskEvent.isCanceled()) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onWorldStateChange(WorldStateEvent event) {
        currentMaskType = ShamanMaskType.NONE;
    }

    private void parseMask(String title) {
        Matcher matcher = MASK_PATTERN.matcher(title);

        ShamanMaskType parsedMask = ShamanMaskType.NONE;

        if (matcher.matches()) {
            parsedMask = ShamanMaskType.find(matcher.group(1));
        } else {
            for (ShamanMaskType type : ShamanMaskType.values()) {
                if (type.getParseString() == null) continue;

                if (title.contains(type.getParseString())) {
                    parsedMask = type;
                    break;
                }
            }
        }

        currentMaskType = parsedMask;
    }

    public ShamanMaskType getCurrentMaskType() {
        return currentMaskType;
    }
}
