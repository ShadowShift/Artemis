/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wynntils.core.components.Managers;
import com.wynntils.gui.render.FontRenderer;
import com.wynntils.gui.render.RenderUtils;
import com.wynntils.gui.render.Texture;
import com.wynntils.gui.screens.CharacterSelectorScreen;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class ClassSelectionDeleteButton extends WynntilsButton {
    private static final List<Component> TOOLTIP = List.of(
            Component.translatable("screens.wynntils.characterSelection.delete.name")
                    .withStyle(ChatFormatting.RED),
            Component.translatable("screens.wynntils.characterSelection.delete.discussion")
                    .withStyle(ChatFormatting.GRAY));
    private final CharacterSelectorScreen characterSelectorScreen;

    public ClassSelectionDeleteButton(
            int x, int y, int width, int height, CharacterSelectorScreen characterSelectorScreen) {
        super(x, y, width, height, Component.literal("Class Selection Delete Button"));
        this.characterSelectorScreen = characterSelectorScreen;
    }

    @Override
    public void onPress() {
        if (characterSelectorScreen.getSelected() == null) return;

        Managers.CharacterSelection.deleteCharacter(
                characterSelectorScreen.getSelected().getClassInfo().slot());
    }

    @Override
    public void renderButton(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        RenderUtils.drawTexturedRect(
                poseStack,
                Texture.REMOVE_BUTTON.resource(),
                this.getX(),
                this.getY(),
                0,
                this.width,
                this.height,
                0,
                characterSelectorScreen.getSelected() == null ? Texture.REMOVE_BUTTON.height() / 2 : 0,
                Texture.REMOVE_BUTTON.width(),
                Texture.REMOVE_BUTTON.height() / 2,
                Texture.REMOVE_BUTTON.width(),
                Texture.REMOVE_BUTTON.height());

        if (isHovered && characterSelectorScreen.getSelected() != null) {
            RenderUtils.drawTooltipAt(
                    poseStack,
                    mouseX,
                    mouseY,
                    100,
                    TOOLTIP,
                    FontRenderer.getInstance().getFont(),
                    true);
        }
    }
}
