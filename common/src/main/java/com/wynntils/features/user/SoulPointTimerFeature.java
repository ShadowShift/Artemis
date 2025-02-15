/*
 * Copyright © Wynntils 2021-2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.features.user;

import com.wynntils.core.components.Model;
import com.wynntils.core.components.Models;
import com.wynntils.core.features.UserFeature;
import com.wynntils.core.features.properties.FeatureInfo;
import com.wynntils.core.features.properties.FeatureInfo.Stability;
import java.util.List;

@FeatureInfo(stability = Stability.STABLE)
public class SoulPointTimerFeature extends UserFeature {

    @Override
    public List<Model> getModelDependencies() {
        return List.of(Models.SoulPointItemStack);
    }

    public static SoulPointTimerFeature INSTANCE;
}
