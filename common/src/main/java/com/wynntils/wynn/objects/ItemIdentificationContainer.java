/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.wynn.objects;

import com.wynntils.wynn.objects.profiles.item.IdentificationModifier;
import com.wynntils.wynn.objects.profiles.item.IdentificationProfile;
import com.wynntils.wynn.objects.profiles.item.ItemProfile;
import net.minecraft.network.chat.Component;

public record ItemIdentificationContainer(
        ItemProfile item,
        IdentificationProfile identification,
        IdentificationModifier modifier,
        String shortIdName,
        int value,
        int stars,
        float percent,
        Component rawLoreLine,
        Component percentLoreLine,
        Component rangeLoreLine,
        Component rerollLoreLine) {

    public boolean isNew() {
        return (identification == null || identification.isInvalidValue(value));
    }

    public boolean isFixed() {
        return !isNew() && identification.hasConstantValue();
    }
}
