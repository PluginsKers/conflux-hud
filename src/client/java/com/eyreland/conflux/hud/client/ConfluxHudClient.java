package com.eyreland.conflux.hud.client;

import com.eyreland.conflux.hud.client.mixin.BossOverlayAccessor;
import com.eyreland.conflux.hud.client.mixin.TabOverlayAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.inventory.ChestMenu;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

public final class ConfluxHudClient implements ClientModInitializer {
    static final ClaimNotice CLAIM = new ClaimNotice();
    static final TradeNotice TRADE = new TradeNotice();
    private static ClientLevel level;
    private static ChestMenu tradeMenu;

    @Override public void onInitializeClient() {
        HudPlatform.register();
        ClientReceiveMessageEvents.MODIFY_GAME.register((message, overlay) -> overlay ? actionBar(message) : message);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            CLAIM.clear();
            clearTrade();
            level = null;
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            syncLevel(client.level);
            syncTrade();
            CLAIM.tick();
        });
        LoggerFactory.getLogger("conflux-hud").info("Conflux HUD {} loaded (vanilla BossBar, TAB, deed and shopping data)",
            FabricLoader.getInstance().getModContainer("conflux-hud").orElseThrow().getMetadata().getVersion());
    }

    public static Component actionBar(Component message) {
        syncLevel(Minecraft.getInstance().level);
        syncTrade();
        boolean claim = CLAIM.accept(message.getString());
        boolean trade = tradeMenu != null && TRADE.accept(message.getString());
        return claim || trade ? Component.empty() : message;
    }

    static boolean isTradeScreen(Screen screen) {
        var player = Minecraft.getInstance().player;
        return player != null && screen instanceof ContainerScreen container
            && container.getMenu() == player.containerMenu && container.getMenu().getRowCount() == 6
            && screen.getTitle().getString().equals("货物木桶");
    }

    static boolean trading() { return isTradeScreen(HudAccess.screen()) && (TRADE.quote() != null || TRADE.message() != null); }

    static void clearTrade() { TRADE.clear(); tradeMenu = null; }

    private static void syncTrade() {
        var screen = HudAccess.screen();
        ChestMenu current = isTradeScreen(screen) ? ((ContainerScreen) screen).getMenu() : null;
        if (tradeMenu != current) {
            TRADE.clear();
            tradeMenu = current;
        }
    }

    public static HudText.Territory territory(BossEvent bar) {
        return HudText.boss(bar.getName().getString(), bar.getColor().name(), bar.getOverlay().name(), bar.getProgress());
    }

    static List<HudText.Territory> territories() {
        var result = new ArrayList<HudText.Territory>();
        Component footer = ((TabOverlayAccessor) HudAccess.tab()).conflux$getFooter();
        var own = HudText.own(footer == null ? "" : footer.getString());
        if (own != null) result.add(own);
        for (var bar : ((BossOverlayAccessor) HudAccess.bosses()).conflux$getEvents().values()) {
            var territory = territory(bar);
            if (territory != null) result.add(territory);
        }
        return result;
    }

    static List<String> deed() {
        var player = Minecraft.getInstance().player;
        if (player == null) return List.of();
        var item = player.getMainHandItem();
        if (!item.is(Items.PAPER)) return List.of();
        var data = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompoundOrEmpty("PublicBukkitValues");
        if (!data.getBooleanOr("conflux:is_land_deed", false)) return List.of();
        return item.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines().stream().map(line -> HudText.clean(line.getString())).toList();
    }

    private static void syncLevel(ClientLevel current) {
        if (level != current) {
            CLAIM.clear();
            clearTrade();
            level = current;
        }
    }
}
