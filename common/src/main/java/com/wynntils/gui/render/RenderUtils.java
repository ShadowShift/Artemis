/*
 * Copyright © Wynntils 2022.
 * This file is released under AGPLv3. See LICENSE for full license details.
 */
package com.wynntils.gui.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.wynntils.mc.objects.CustomColor;
import com.wynntils.mc.utils.McUtils;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

public final class RenderUtils {
    // tooltip colors for item screenshot creation. somewhat hacky solution to get around transparency issues -
    // these colors were chosen to best match how tooltips are displayed in-game
    private static final CustomColor BACKGROUND = CustomColor.fromInt(0xFF100010);
    private static final CustomColor BORDER_START = CustomColor.fromInt(0xFF25005B);
    private static final CustomColor BORDER_END = CustomColor.fromInt(0xFF180033);

    // number of possible segments for arc drawing
    private static final float MAX_CIRCLE_STEPS = 16f;

    // See https://github.com/MinecraftForge/MinecraftForge/issues/8083 as to why this uses TRIANGLE_STRIPS.
    // TLDR: New OpenGL only supports TRIANGLES and Minecraft patched QUADS to be usable ATM, but LINES patch is broken
    // and you can't use it.
    // (This also means that using QUADS is probably not the best idea)
    public static void drawLine(
            PoseStack poseStack, CustomColor color, float x1, float y1, float x2, float y2, float z, float width) {
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();

        float halfWidth = width / 2;

        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        if (x1 == x2) {
            if (y2 < y1) {
                float tmp = y1;
                y1 = y2;
                y2 = tmp;
            }
            bufferBuilder
                    .vertex(matrix, x1 - halfWidth, y1, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x2 - halfWidth, y2, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x1 + halfWidth, y1, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x2 + halfWidth, y2, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
        } else if (y1 == y2) {
            if (x2 < x1) {
                float tmp = x1;
                x1 = x2;
                x2 = tmp;
            }

            bufferBuilder
                    .vertex(matrix, x1, y1 - halfWidth, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x1, y1 + halfWidth, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x2, y2 - halfWidth, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x2, y2 + halfWidth, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
        } else if ((x1 < x2 && y1 < y2) || (x2 < x1 && y2 < y1)) { // Top Left to Bottom Right line
            if (x2 < x1) {
                float tmp = x1;
                x1 = x2;
                x2 = tmp;

                tmp = y1;
                y1 = y2;
                y2 = tmp;
            }

            bufferBuilder
                    .vertex(matrix, x1 + halfWidth, y1 - halfWidth, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x1 - halfWidth, y1 + halfWidth, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x2 + halfWidth, y2 - halfWidth, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x2 - halfWidth, y2 + halfWidth, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
        } else { // Top Right to Bottom Left Line
            if (x1 < x2) {
                float tmp = x1;
                x1 = x2;
                x2 = tmp;

                tmp = y1;
                y1 = y2;
                y2 = tmp;
            }

            bufferBuilder
                    .vertex(matrix, x1 + halfWidth, y1 + halfWidth, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x1 - halfWidth, y1 - halfWidth, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x2 + halfWidth, y2 + halfWidth, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, x2 - halfWidth, y2 - halfWidth, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
        }

        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public static void drawRectBorders(
            PoseStack poseStack, CustomColor color, float x1, float y1, float x2, float y2, float z, float lineWidth) {
        drawLine(poseStack, color, x1, y1, x2, y1, z, lineWidth);
        drawLine(poseStack, color, x2, y1, x2, y2, z, lineWidth);
        drawLine(poseStack, color, x2, y2, x1, y2, z, lineWidth);
        drawLine(poseStack, color, x1, y2, x1, y1, z, lineWidth);
    }

    public static void drawRect(CustomColor color, float x, float y, float z, float width, float height) {
        drawRect(new PoseStack(), color, x, y, z, width, height);
    }

    public static void drawRect(
            PoseStack poseStack, CustomColor color, float x, float y, float z, float width, float height) {
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder
                .vertex(matrix, x, y + height, z)
                .color(color.r, color.g, color.b, color.a)
                .endVertex();
        bufferBuilder
                .vertex(matrix, x + width, y + height, z)
                .color(color.r, color.g, color.b, color.a)
                .endVertex();
        bufferBuilder
                .vertex(matrix, x + width, y, z)
                .color(color.r, color.g, color.b, color.a)
                .endVertex();
        bufferBuilder
                .vertex(matrix, x, y, z)
                .color(color.r, color.g, color.b, color.a)
                .endVertex();

        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public static void drawHoverableTexturedRect(
            PoseStack poseStack, Texture texture, float x, float y, boolean hovered) {
        drawTexturedRect(
                poseStack,
                texture.resource(),
                x,
                y,
                0,
                texture.width(),
                texture.height() / 2f,
                0,
                hovered ? texture.height() / 2 : 0,
                texture.width(),
                texture.height() / 2,
                texture.width(),
                texture.height());
    }

    public static void drawTexturedRect(PoseStack poseStack, Texture texture, float x, float y) {
        drawTexturedRect(
                poseStack,
                texture.resource(),
                x,
                y,
                texture.width(),
                texture.height(),
                texture.width(),
                texture.height());
    }

    public static void drawTexturedRect(
            PoseStack poseStack,
            ResourceLocation tex,
            float x,
            float y,
            float width,
            float height,
            int textureWidth,
            int textureHeight) {
        drawTexturedRect(
                poseStack, tex, x, y, 0, width, height, 0, 0, (int) width, (int) height, textureWidth, textureHeight);
    }

    public static void drawTexturedRect(
            PoseStack poseStack,
            ResourceLocation tex,
            float x,
            float y,
            float z,
            float width,
            float height,
            int textureWidth,
            int textureHeight) {
        drawTexturedRect(
                poseStack, tex, x, y, z, width, height, 0, 0, (int) width, (int) height, textureWidth, textureHeight);
    }

    public static void drawTexturedRect(
            PoseStack poseStack,
            ResourceLocation tex,
            float x,
            float y,
            float z,
            float width,
            float height,
            int uOffset,
            int vOffset,
            int u,
            int v,
            int textureWidth,
            int textureHeight) {
        float uScale = 1f / textureWidth;
        float vScale = 1f / textureHeight;

        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, tex);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder
                .vertex(matrix, x, y + height, z)
                .uv(uOffset * uScale, (vOffset + v) * vScale)
                .endVertex();
        bufferBuilder
                .vertex(matrix, x + width, y + height, z)
                .uv((uOffset + u) * uScale, (vOffset + v) * vScale)
                .endVertex();
        bufferBuilder
                .vertex(matrix, x + width, y, z)
                .uv((uOffset + u) * uScale, vOffset * vScale)
                .endVertex();
        bufferBuilder
                .vertex(matrix, x, y, z)
                .uv(uOffset * uScale, vOffset * vScale)
                .endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    public static void drawScalingTexturedRect(
            PoseStack poseStack,
            ResourceLocation tex,
            float x,
            float y,
            float z,
            float width,
            float height,
            int textureWidth,
            int textureHeight) {
        drawTexturedRect(
                poseStack, tex, x, y, z, width, height, 0, 0, textureWidth, textureHeight, textureWidth, textureHeight);
    }

    public static void drawTexturedRectWithColor(
            ResourceLocation tex,
            CustomColor color,
            float x,
            float y,
            float z,
            float width,
            float height,
            int textureWidth,
            int textureHeight) {
        drawTexturedRectWithColor(
                new PoseStack(),
                tex,
                color,
                x,
                y,
                z,
                width,
                height,
                0,
                0,
                (int) width,
                (int) height,
                textureWidth,
                textureHeight);
    }

    public static void drawTexturedRectWithColor(
            PoseStack poseStack,
            ResourceLocation tex,
            CustomColor color,
            float x,
            float y,
            float z,
            float width,
            float height,
            int uOffset,
            int vOffset,
            int u,
            int v,
            int textureWidth,
            int textureHeight) {
        float uScale = 1f / textureWidth;
        float vScale = 1f / textureHeight;

        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, tex);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferBuilder
                .vertex(matrix, x, y + height, z)
                .uv(uOffset * uScale, (vOffset + v) * vScale)
                .color(color.r, color.g, color.b, color.a)
                .endVertex();
        bufferBuilder
                .vertex(matrix, x + width, y + height, z)
                .uv((uOffset + u) * uScale, (vOffset + v) * vScale)
                .color(color.r, color.g, color.b, color.a)
                .endVertex();
        bufferBuilder
                .vertex(matrix, x + width, y, z)
                .uv((uOffset + u) * uScale, vOffset * vScale)
                .color(color.r, color.g, color.b, color.a)
                .endVertex();
        bufferBuilder
                .vertex(matrix, x, y, z)
                .uv(uOffset * uScale, vOffset * vScale)
                .color(color.r, color.g, color.b, color.a)
                .endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public static void drawArc(
            CustomColor color, float x, float y, float z, float fill, int innerRadius, int outerRadius) {
        drawArc(new PoseStack(), color, x, y, z, fill, innerRadius, outerRadius, 0);
    }

    public static void drawArc(
            PoseStack poseStack,
            CustomColor color,
            float x,
            float y,
            float z,
            float fill,
            int innerRadius,
            int outerRadius,
            float angleOffset) {
        // keeps arc from overlapping itself
        int segments = (int) Math.min(fill * MAX_CIRCLE_STEPS, MAX_CIRCLE_STEPS - 1);
        float midX = x + outerRadius;
        float midY = y + outerRadius;
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();

        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        float angle;
        float sinAngle;
        float cosAngle;
        for (int i = 0; i <= segments; i++) {
            angle = Mth.TWO_PI * i / (MAX_CIRCLE_STEPS - 1f) + angleOffset;
            sinAngle = Mth.sin(angle);
            cosAngle = Mth.cos(angle);

            bufferBuilder
                    .vertex(matrix, midX + sinAngle * outerRadius, midY - cosAngle * outerRadius, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
            bufferBuilder
                    .vertex(matrix, midX + sinAngle * innerRadius, midY - cosAngle * innerRadius, z)
                    .color(color.r, color.g, color.b, color.a)
                    .endVertex();
        }

        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    public static void drawRoundedRectWithBorder(
            PoseStack poseStack,
            CustomColor borderColor,
            CustomColor fillColor,
            float x,
            float y,
            float z,
            float width,
            float height,
            float lineWidth,
            int innerRadius,
            int outerRadius) {

        float x2 = x + width;
        float y2 = y + height;

        // Fill the rect
        final int fillOffset = (int) lineWidth;
        drawRect(
                poseStack,
                fillColor,
                x + fillOffset,
                y + fillOffset,
                z,
                width - fillOffset * 2,
                height - fillOffset * 2);
        drawLine(poseStack, fillColor, x + fillOffset, y + fillOffset, x2 - fillOffset, y + fillOffset, z, lineWidth);
        drawLine(poseStack, fillColor, x2 - fillOffset, y + fillOffset, x2 - fillOffset, y2 - fillOffset, z, lineWidth);
        drawLine(poseStack, fillColor, x + fillOffset, y2 - fillOffset, x2 - fillOffset, y2 - fillOffset, z, lineWidth);
        drawLine(poseStack, fillColor, x + fillOffset, y + fillOffset, x + fillOffset, y2 - fillOffset, z, lineWidth);

        float offset = outerRadius - 1;

        // Edges
        drawLine(poseStack, borderColor, x + offset, y, x2 - offset, y, z, lineWidth);
        drawLine(poseStack, borderColor, x2, y + offset, x2, y2 - offset, z, lineWidth);
        drawLine(poseStack, borderColor, x + offset, y2, x2 - offset, y2, z, lineWidth);
        drawLine(poseStack, borderColor, x, y + offset, x, y2 - offset, z, lineWidth);

        // Corners
        poseStack.pushPose();
        poseStack.translate(-1, -1, 0);
        drawRoundedCorner(poseStack, borderColor, x, y, z, innerRadius, outerRadius, Mth.HALF_PI * 3);
        drawRoundedCorner(poseStack, borderColor, x, y2 - offset * 2, z, innerRadius, outerRadius, Mth.HALF_PI * 2);
        drawRoundedCorner(
                poseStack, borderColor, x2 - offset * 2, y2 - offset * 2, z, innerRadius, outerRadius, Mth.HALF_PI * 1);
        drawRoundedCorner(poseStack, borderColor, x2 - offset * 2, y, z, innerRadius, outerRadius, 0);
        poseStack.popPose();
    }

    private static void drawRoundedCorner(
            PoseStack poseStack,
            CustomColor color,
            float x,
            float y,
            float z,
            int innerRadius,
            int outerRadius,
            float angleOffset) {
        drawArc(poseStack, color, x, y, z, 0.25f, innerRadius, outerRadius, angleOffset);
    }

    public static void drawTooltip(
            PoseStack poseStack, List<Component> componentLines, Font font, boolean firstLineHasPlusHeight) {
        List<ClientTooltipComponent> lines = componentToClientTooltipComponent(componentLines);

        int tooltipWidth = getToolTipWidth(lines, font);
        int tooltipHeight = getToolTipHeight(lines);

        // background box
        poseStack.pushPose();
        int tooltipX = 4;
        int tooltipY = 4;
        int zLevel = 400;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix4f = poseStack.last().pose();
        fillGradient(
                matrix4f,
                bufferBuilder,
                tooltipX - 3,
                tooltipY - 4,
                tooltipX + tooltipWidth + 3,
                tooltipY - 3,
                zLevel,
                BACKGROUND,
                BACKGROUND);
        fillGradient(
                matrix4f,
                bufferBuilder,
                tooltipX - 3,
                tooltipY + tooltipHeight + 3,
                tooltipX + tooltipWidth + 3,
                tooltipY + tooltipHeight + 4,
                zLevel,
                BACKGROUND,
                BACKGROUND);
        fillGradient(
                matrix4f,
                bufferBuilder,
                tooltipX - 3,
                tooltipY - 3,
                tooltipX + tooltipWidth + 3,
                tooltipY + tooltipHeight + 3,
                zLevel,
                BACKGROUND,
                BACKGROUND);
        fillGradient(
                matrix4f,
                bufferBuilder,
                tooltipX - 4,
                tooltipY - 3,
                tooltipX - 3,
                tooltipY + tooltipHeight + 3,
                zLevel,
                BACKGROUND,
                BACKGROUND);
        fillGradient(
                matrix4f,
                bufferBuilder,
                tooltipX + tooltipWidth + 3,
                tooltipY - 3,
                tooltipX + tooltipWidth + 4,
                tooltipY + tooltipHeight + 3,
                zLevel,
                BACKGROUND,
                BACKGROUND);
        fillGradient(
                matrix4f,
                bufferBuilder,
                tooltipX - 3,
                tooltipY - 3 + 1,
                tooltipX - 3 + 1,
                tooltipY + tooltipHeight + 3 - 1,
                zLevel,
                BORDER_START,
                BORDER_END);
        fillGradient(
                matrix4f,
                bufferBuilder,
                tooltipX + tooltipWidth + 2,
                tooltipY - 3 + 1,
                tooltipX + tooltipWidth + 3,
                tooltipY + tooltipHeight + 3 - 1,
                zLevel,
                BORDER_START,
                BORDER_END);
        fillGradient(
                matrix4f,
                bufferBuilder,
                tooltipX - 3,
                tooltipY - 3,
                tooltipX + tooltipWidth + 3,
                tooltipY - 3 + 1,
                zLevel,
                BORDER_START,
                BORDER_START);
        fillGradient(
                matrix4f,
                bufferBuilder,
                tooltipX - 3,
                tooltipY + tooltipHeight + 2,
                tooltipX + tooltipWidth + 3,
                tooltipY + tooltipHeight + 3,
                zLevel,
                BORDER_END,
                BORDER_END);
        RenderSystem.enableDepthTest();
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();

        // text
        MultiBufferSource.BufferSource bufferSource =
                MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        poseStack.translate(0.0, 0.0, 400.0);
        int s = tooltipY;
        boolean first = firstLineHasPlusHeight;
        for (ClientTooltipComponent line : lines) {
            line.renderText(font, tooltipX, s, matrix4f, bufferSource);
            s += line.getHeight() + (first ? 2 : 0);
            first = false;
        }
        bufferSource.endBatch();
        poseStack.popPose();
    }

    public static void drawTooltipAt(
            PoseStack poseStack,
            double renderX,
            double renderY,
            double renderZ,
            List<Component> componentLines,
            Font font,
            boolean firstLineHasPlusHeight) {
        poseStack.pushPose();

        poseStack.translate(renderX, renderY, renderZ);
        drawTooltip(poseStack, componentLines, font, firstLineHasPlusHeight);

        poseStack.popPose();
    }

    public static int getToolTipWidth(List<ClientTooltipComponent> lines, Font font) {
        return lines.stream()
                .map(clientTooltipComponent -> clientTooltipComponent.getWidth(font))
                .max(Integer::compareTo)
                .orElse(0);
    }

    public static int getToolTipHeight(List<ClientTooltipComponent> lines) {
        return (lines.size() == 1 ? -2 : 0)
                + lines.stream()
                        .map(ClientTooltipComponent::getHeight)
                        .mapToInt(Integer::intValue)
                        .sum();
    }

    public static List<ClientTooltipComponent> componentToClientTooltipComponent(List<Component> components) {
        return components.stream()
                .map(Component::getVisualOrderText)
                .map(ClientTooltipComponent::create)
                .toList();
    }

    /**
     * drawProgressBar
     * Draws a progress bar (textureY1 and textureY2 now specify both textures with background being on top of the bar)
     *
     * @param poseStack poseStack to use
     * @param texture   the texture to use
     * @param customColor the color for the bar
     * @param x1        left x on screen
     * @param y1        top y on screen
     * @param x2        right x on screen
     * @param y2        bottom right on screen
     * @param textureX1 texture left x for the part
     * @param textureY1 texture top y for the part (top of background)
     * @param textureX2 texture right x for the part
     * @param textureY2 texture bottom y for the part (bottom of bar)
     * @param progress  progress of the bar, 0.0f to 1.0f is left to right and 0.0f to -1.0f is right to left
     */
    public static void drawColoredProgressBar(
            PoseStack poseStack,
            Texture texture,
            CustomColor customColor,
            float x1,
            float y1,
            float x2,
            float y2,
            int textureX1,
            int textureY1,
            int textureX2,
            int textureY2,
            float progress) {

        int half = (textureY1 + textureY2) / 2 + (textureY2 - textureY1) % 2;
        drawProgressBarBackground(poseStack, texture, x1, y1, x2, y2, textureX1, textureY1, textureX2, half);
        drawProgressBarForegroundWithColor(
                poseStack,
                texture,
                customColor,
                x1,
                y1,
                x2,
                y2,
                textureX1,
                half,
                textureX2,
                textureY2 + (textureY2 - textureY1) % 2,
                progress);
    }

    /**
     * drawProgressBar
     * Draws a progress bar (textureY1 and textureY2 now specify both textures with background being on top of the bar)
     *
     * @param poseStack poseStack to use
     * @param texture   the texture to use
     * @param x1        left x on screen
     * @param y1        top y on screen
     * @param x2        right x on screen
     * @param y2        bottom right on screen
     * @param textureX1 texture left x for the part
     * @param textureY1 texture top y for the part (top of background)
     * @param textureX2 texture right x for the part
     * @param textureY2 texture bottom y for the part (bottom of bar)
     * @param progress  progress of the bar, 0.0f to 1.0f is left to right and 0.0f to -1.0f is right to left
     */
    public static void drawProgressBar(
            PoseStack poseStack,
            Texture texture,
            float x1,
            float y1,
            float x2,
            float y2,
            int textureX1,
            int textureY1,
            int textureX2,
            int textureY2,
            float progress) {

        int half = (textureY1 + textureY2) / 2 + (textureY2 - textureY1) % 2;
        drawProgressBarBackground(poseStack, texture, x1, y1, x2, y2, textureX1, textureY1, textureX2, half);
        drawProgressBarForeground(
                poseStack,
                texture,
                x1,
                y1,
                x2,
                y2,
                textureX1,
                half,
                textureX2,
                textureY2 + (textureY2 - textureY1) % 2,
                progress);
    }

    public static void drawProgressBarForeground(
            PoseStack poseStack,
            Texture texture,
            float x1,
            float y1,
            float x2,
            float y2,
            int textureX1,
            int textureY1,
            int textureX2,
            int textureY2,
            float progress) {
        if (progress == 0f) {
            return;
        }

        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableTexture();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture.resource());
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        float xMin = Math.min(x1, x2),
                xMax = Math.max(x1, x2),
                yMin = Math.min(y1, y2),
                yMax = Math.max(y1, y2),
                txMin = (float) Math.min(textureX1, textureX2) / texture.width(),
                txMax = (float) Math.max(textureX1, textureX2) / texture.width(),
                tyMin = (float) Math.min(textureY1, textureY2) / texture.height(),
                tyMax = (float) Math.max(textureY1, textureY2) / texture.height();

        if (progress < 1.0f && progress > -1.0f) {
            if (progress < 0.0f) {
                xMin += (1.0f + progress) * (xMax - xMin);
                txMin += (1.0f + progress) * (txMax - txMin);
            } else {
                xMax -= (1.0f - progress) * (xMax - xMin);
                txMax -= (1.0f - progress) * (txMax - txMin);
            }
        }

        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix, xMin, yMin, 0).uv(txMin, tyMin).endVertex();
        bufferBuilder.vertex(matrix, xMin, yMax, 0).uv(txMin, tyMax).endVertex();
        bufferBuilder.vertex(matrix, xMax, yMax, 0).uv(txMax, tyMax).endVertex();
        bufferBuilder.vertex(matrix, xMax, yMin, 0).uv(txMax, tyMin).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    private static void drawProgressBarForegroundWithColor(
            PoseStack poseStack,
            Texture texture,
            CustomColor customColor,
            float x1,
            float y1,
            float x2,
            float y2,
            int textureX1,
            int textureY1,
            int textureX2,
            int textureY2,
            float progress) {
        if (progress == 0f) {
            return;
        }

        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture.resource());
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        float xMin = Math.min(x1, x2),
                xMax = Math.max(x1, x2),
                yMin = Math.min(y1, y2),
                yMax = Math.max(y1, y2),
                txMin = (float) Math.min(textureX1, textureX2) / texture.width(),
                txMax = (float) Math.max(textureX1, textureX2) / texture.width(),
                tyMin = (float) Math.min(textureY1, textureY2) / texture.height(),
                tyMax = (float) Math.max(textureY1, textureY2) / texture.height();

        if (progress < 1.0f && progress > -1.0f) {
            if (progress < 0.0f) {
                xMin += (1.0f + progress) * (xMax - xMin);
                txMin += (1.0f + progress) * (txMax - txMin);
            } else {
                xMax -= (1.0f - progress) * (xMax - xMin);
                txMax -= (1.0f - progress) * (txMax - txMin);
            }
        }

        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferBuilder
                .vertex(matrix, xMin, yMin, 0)
                .uv(txMin, tyMin)
                .color(customColor.asInt())
                .endVertex();
        bufferBuilder
                .vertex(matrix, xMin, yMax, 0)
                .uv(txMin, tyMax)
                .color(customColor.asInt())
                .endVertex();
        bufferBuilder
                .vertex(matrix, xMax, yMax, 0)
                .uv(txMax, tyMax)
                .color(customColor.asInt())
                .endVertex();
        bufferBuilder
                .vertex(matrix, xMax, yMin, 0)
                .uv(txMax, tyMin)
                .color(customColor.asInt())
                .endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    public static void drawProgressBarBackground(
            PoseStack poseStack,
            Texture texture,
            float x1,
            float y1,
            float x2,
            float y2,
            int textureX1,
            int textureY1,
            int textureX2,
            int textureY2) {
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableTexture();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture.resource());
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        float xMin = Math.min(x1, x2),
                xMax = Math.max(x1, x2),
                yMin = Math.min(y1, y2),
                yMax = Math.max(y1, y2),
                txMin = (float) Math.min(textureX1, textureX2) / texture.width(),
                txMax = (float) Math.max(textureX1, textureX2) / texture.width(),
                tyMin = (float) Math.min(textureY1, textureY2) / texture.height(),
                tyMax = (float) Math.max(textureY1, textureY2) / texture.height();

        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix, xMin, yMin, 0).uv(txMin, tyMin).endVertex();
        bufferBuilder.vertex(matrix, xMin, yMax, 0).uv(txMin, tyMax).endVertex();
        bufferBuilder.vertex(matrix, xMax, yMax, 0).uv(txMax, tyMax).endVertex();
        bufferBuilder.vertex(matrix, xMax, yMin, 0).uv(txMax, tyMin).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    public static void fillGradient(
            Matrix4f matrix,
            BufferBuilder builder,
            float x1,
            float y1,
            float x2,
            float y2,
            int blitOffset,
            CustomColor colorA,
            CustomColor colorB) {
        builder.vertex(matrix, x2, y1, blitOffset)
                .color(colorA.r, colorA.g, colorA.b, colorA.a)
                .endVertex();
        builder.vertex(matrix, x1, y1, blitOffset)
                .color(colorA.r, colorA.g, colorA.b, colorA.a)
                .endVertex();
        builder.vertex(matrix, x1, y2, blitOffset)
                .color(colorB.r, colorB.g, colorB.b, colorB.a)
                .endVertex();
        builder.vertex(matrix, x2, y2, blitOffset)
                .color(colorB.r, colorB.g, colorB.b, colorB.a)
                .endVertex();
    }

    public static void copyImageToClipboard(BufferedImage bi) {
        ClipboardImage ci = new ClipboardImage(bi);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ci, null);
    }

    public static BufferedImage createScreenshot(RenderTarget fb) {
        BufferedImage bufferedimage = new BufferedImage(fb.width, fb.height, BufferedImage.TYPE_INT_ARGB);
        try (NativeImage image = new NativeImage(fb.width, fb.height, false)) {
            RenderSystem.bindTexture(fb.getColorTextureId());
            image.downloadTexture(0, false);
            image.flipY();

            int[] pixelValues = image.makePixelArray();

            bufferedimage.setRGB(0, 0, fb.width, fb.height, pixelValues, 0, fb.width);
        }
        return bufferedimage;
    }

    /*
       Normal GL implementation is in screen coordinates and thus y is inverted,
       this changes it so it doesn't do that
    */
    public static void enableScissor(int x, int y, int width, int height) {
        Window window = McUtils.window();
        double scale = window.getGuiScale();
        RenderSystem.enableScissor(
                (int) (x * scale), (int) (window.getHeight() - ((y + height) * scale)), (int) (width * scale), (int)
                        (height * scale));
    }

    public static void rotatePose(PoseStack poseStack, float centerX, float centerZ, float angle) {
        poseStack.translate(centerX, centerZ, 0);
        // See Quaternion#fromXYZ
        poseStack.mulPose(new Quaternionf(0F, 0, (float) StrictMath.sin(Math.toRadians(angle) / 2), (float)
                StrictMath.cos(-Math.toRadians(angle) / 2)));
        poseStack.translate(-centerX, -centerZ, 0);
    }

    // Basically this is ItemRenderer#renderGuiItem, but we can modify the poseStack
    public static void renderGuiItem(ItemStack itemStack, int x, int y, float scale) {
        BakedModel bakedModel = McUtils.mc().getItemRenderer().getModel(itemStack, null, null, 0);

        McUtils.mc()
                .getItemRenderer()
                .textureManager
                .getTexture(InventoryMenu.BLOCK_ATLAS)
                .setFilter(false, false);
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        PoseStack poseStack = RenderSystem.getModelViewStack();
        poseStack.pushPose();
        poseStack.translate(x, y, (100.0F + McUtils.mc().getItemRenderer().blitOffset));
        poseStack.translate(8.0, 8.0, 0.0);
        poseStack.scale(1.0F, -1.0F, 1.0F);
        poseStack.scale(16.0F, 16.0F, 16.0F);
        poseStack.scale(scale, scale, 0);

        RenderSystem.applyModelViewMatrix();
        PoseStack poseStack2 = new PoseStack();
        MultiBufferSource.BufferSource bufferSource =
                Minecraft.getInstance().renderBuffers().bufferSource();
        boolean modelUsesBlockLighting = bakedModel.usesBlockLight();

        if (!modelUsesBlockLighting) {
            Lighting.setupForFlatItems();
        }

        McUtils.mc()
                .getItemRenderer()
                .render(
                        itemStack,
                        ItemTransforms.TransformType.GUI,
                        false,
                        poseStack2,
                        bufferSource,
                        15728880,
                        OverlayTexture.NO_OVERLAY,
                        bakedModel);

        bufferSource.endBatch();
        RenderSystem.enableDepthTest();

        if (!modelUsesBlockLighting) {
            Lighting.setupFor3DItems();
        }

        poseStack.popPose();
        RenderSystem.applyModelViewMatrix();
    }

    public static void renderVignetteOverlay(PoseStack poseStack, CustomColor color, float alpha) {
        float[] colorArray = color.asFloatArray();
        RenderSystem.setShaderColor(colorArray[0], colorArray[1], colorArray[2], alpha);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        Window window = McUtils.window();

        RenderUtils.drawTexturedRect(
                poseStack,
                Texture.VIGNETTE.resource(),
                0,
                0,
                0,
                window.getGuiScaledWidth(),
                window.getGuiScaledHeight(),
                0,
                0,
                Texture.VIGNETTE.width(),
                Texture.VIGNETTE.height(),
                Texture.VIGNETTE.width(),
                Texture.VIGNETTE.height());

        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
    }

    public static void createMask(PoseStack poseStack, Texture texture, int x1, int y1, int x2, int y2) {
        createMask(poseStack, texture, x1, y1, x2, y2, 0, 0, texture.width(), texture.height());
    }

    /**
     * Creates a mask that will remove anything drawn after
     * this and before the next {clearMask()}(or {endGL()})
     * and is not inside the mask.
     * A mask, is a clear and white texture where anything
     * white will allow drawing.
     *
     * @param texture mask texture(please use Textures.Masks)
     * @param x1 bottom-left x(on screen)
     * @param y1 bottom-left y(on screen)
     * @param x2 top-right x(on screen)
     * @param y2 top-right y(on screen)
     */
    public static void createMask(
            PoseStack poseStack,
            Texture texture,
            float x1,
            float y1,
            float x2,
            float y2,
            int tx1,
            int ty1,
            int tx2,
            int ty2) {
        // See https://gist.github.com/burgerguy/8233170683ad93eea6aa27ee02a5c4d1

        GL11.glEnable(GL11.GL_STENCIL_TEST);

        // Enable writing to stencil
        RenderSystem.stencilMask(0xff);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, true);
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);

        // Disable writing to color or depth
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(false);

        // Draw textured image
        int width = texture.width();
        int height = texture.height();
        drawTexturedRect(
                poseStack,
                texture.resource(),
                x1,
                y1,
                0f,
                x2 - x1,
                y2 - y1,
                tx1,
                ty1,
                tx2 - tx1,
                ty2 - ty1,
                width,
                height);

        // Reenable color and depth
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(true);

        // Only write to stencil area
        RenderSystem.stencilMask(0x00);
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        RenderSystem.stencilFunc(GL11.GL_EQUAL, 1, 0xff);
    }

    /**
     * Clears the active rendering mask from the screen.
     * Based on Figura <a href="https://github.com/Kingdom-of-The-Moon/FiguraRewriteRewrite"> code</a>.
     */
    public static void clearMask() {
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, true);

        // Turn off writing to stencil buffer.
        RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        RenderSystem.stencilMask(0x00);

        // Always succeed in the stencil test, no matter what.
        RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xFF);
    }

    private static final class ClipboardImage implements Transferable {
        private final Image image;

        private ClipboardImage(Image image) {
            this.image = image;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] {DataFlavor.imageFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!DataFlavor.imageFlavor.equals(flavor)) throw new UnsupportedFlavorException(flavor);
            return this.image;
        }
    }
}
