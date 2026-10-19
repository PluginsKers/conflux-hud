package com.eyreland.conflux.hud.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

final class HudPlatform {
    private static final Identifier BUTTON = Identifier.withDefaultNamespace("widget/button");
    static void register() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.parse("conflux-hud:territory"),
            (graphics, delta) -> {
                if (!ConfluxHudClient.trading()) HudRenderer.render(new Canvas(graphics));
            });
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!ConfluxHudClient.isTradeScreen(screen)) return;
            ScreenEvents.afterExtract(screen).register((current, graphics, mouseX, mouseY, delta) -> {
                if (ConfluxHudClient.trading() && !HudAccess.hidden()) HudRenderer.render(new Canvas(graphics));
            });
            ScreenEvents.remove(screen).register(current -> ConfluxHudClient.clearTrade());
        });
        for (var id : new Identifier[]{VanillaHudElements.BOSS_BAR, VanillaHudElements.PLAYER_LIST}) {
            HudElementRegistry.replaceElement(id, element -> (graphics, delta) -> {
                graphics.pose().pushMatrix();
                try {
                    graphics.pose().translate(0, HudRenderer.height(graphics.guiWidth()));
                    element.extractRenderState(graphics, delta);
                } finally {
                    graphics.pose().popMatrix();
                }
            });
        }
    }

    private record Canvas(GuiGraphicsExtractor graphics) implements HudCanvas {
        public int width() { return graphics.guiWidth(); }
        public void buttonSlice(int x, int y, int width, int height, int sliceTop, int sliceHeight) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BUTTON, width, height, 0, sliceTop, x, y + sliceTop, width, sliceHeight);
        }
        public void text(String text, int x, int y, int color) {
            graphics.pose().pushMatrix();
            try {
                graphics.pose().translate(x, y);
                graphics.pose().scale(HudRenderer.TEXT_SCALE);
                graphics.text(Minecraft.getInstance().font, text, 0, 0, color);
            } finally {
                graphics.pose().popMatrix();
            }
        }
    }
}
