package com.eyreland.conflux.hud.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.gui.screens.Screen;

final class HudAccess {
    static BossHealthOverlay bosses() { return Minecraft.getInstance().gui.hud.getBossOverlay(); }
    static PlayerTabOverlay tab() { return Minecraft.getInstance().gui.hud.getTabList(); }
    static Screen screen() { return Minecraft.getInstance().gui.screen(); }
    static boolean hidden() { return Minecraft.getInstance().gui.hud.isHidden(); }
}
