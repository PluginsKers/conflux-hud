package com.eyreland.conflux.hud.client;

import com.eyreland.conflux.hud.client.mixin.BossOverlayAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.world.BossEvent;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

public final class HudClientGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        var languageReload = context.computeOnClient(client -> {
            client.options.languageCode = "zh_cn";
            client.options.guiScale().set(2);
            client.getLanguageManager().setSelected("zh_cn");
            return client.reloadResourcePacks();
        });
        context.waitFor(client -> languageReload.isDone());
        languageReload.join();
        context.getInput().resizeWindow(1280, 720);
        UUID naturalId = UUID.randomUUID();
        UUID hostileId = UUID.randomUUID();
        UUID witherId = UUID.randomUUID();
        try (var world = context.worldBuilder().create()) {
            context.runOnClient(client -> checkEmpty());
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                var scoreboard = server.getScoreboard();
                scoreboard.setDisplayObjective(DisplaySlot.LIST, scoreboard.addObjective("hud_test", ObjectiveCriteria.DUMMY,
                    Component.literal("在线玩家"), ObjectiveCriteria.RenderType.INTEGER, false, null));
                player.connection.send(new ClientboundTabListPacket(Component.literal("在线玩家"),
                    Component.literal("§7公告\n§7所在领地: §f苍松领地 §7单位: §f24")));
                player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal("署名地块 旅人")));
                var deed = new ItemStack(Items.PAPER);
                var values = new CompoundTag();
                values.putBoolean("conflux:is_land_deed", true);
                values.putString("conflux:land_deed_points", "0,0;14,0;14,14;0,14");
                var tag = new CompoundTag();
                tag.put("PublicBukkitValues", values);
                deed.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                deed.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal("§7右键地面增加选点"), Component.literal("§7潜行右键清空选点"),
                    Component.literal("§7左键发起提案并消耗一份地契"), Component.literal("§7已选 §e4 §7点"),
                    Component.literal("§81. 0, 0"), Component.literal("§82. 14, 0"),
                    Component.literal("§83. 14, 14"), Component.literal("§84. 0, 14"))));
                player.getInventory().setItem(0, deed);
                player.inventoryMenu.broadcastChanges();
            });
            context.waitFor(client -> ConfluxHudClient.territories().size() == 1 && ConfluxHudClient.CLAIM.holder() != null && !ConfluxHudClient.deed().isEmpty());
            context.runOnClient(client -> {
                var canvas = new LayoutCheck(640, 360);
                HudRenderer.render(canvas);
                canvas.verify(0, 1);
                check(canvas.rows.getFirst()[2] - canvas.rows.getFirst()[0] <= 240, "Common HUD must stay compact");
                check(canvas.text.stream().anyMatch(text -> text.contains("24")), "Unit count must remain visible");
                check(canvas.text.stream().anyMatch(text -> text.contains("4")), "Deed points must remain visible");
                check(canvas.text.contains("己方") && canvas.text.contains("地块") && canvas.text.contains("已选4点"), "Compact labels must retain ownership and selected-point meaning");
                check(canvas.text.stream().noneMatch(text -> text.matches(".*[A-Za-z].*")), "Chinese HUD must not mix in English labels");
                check(canvas.text.stream().noneMatch(text -> text.contains("右键")), "Item instructions stay in the item tooltip");
            });
            context.waitTick();
            var normal = context.takeScreenshot("conflux-hud-centered");
            context.waitFor(client -> client.level.getScoreboard().getDisplayObjective(DisplaySlot.LIST) != null);
            context.getInput().holdKey(InputConstants.KEY_TAB);
            context.waitTick();
            checkHudUncovered(normal, context.takeScreenshot("conflux-hud-tab-open"), context.computeOnClient(client -> HudRenderer.height(640)));
            context.getInput().releaseKey(InputConstants.KEY_TAB);
            context.waitTick();
            checkHudUncovered(normal, context.takeScreenshot("conflux-hud-tab-closed"), context.computeOnClient(client -> HudRenderer.height(640)));
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.connection.send(ClientboundBossEventPacket.createAddPacket(boss(naturalId, "敌对领地 [8]", BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_6)));
                player.connection.send(ClientboundBossEventPacket.createAddPacket(boss(hostileId, "红杉领地 [12]", BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_6)));
                player.connection.send(ClientboundBossEventPacket.createAddPacket(boss(witherId, "凋灵", BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS)));
                player.connection.send(new ClientboundTabListPacket(Component.literal("Server"),
                    Component.literal("§7公告\n§c你的领地正在遭受袭击，请尽快驰援\n§7所在领地: §f苍松领地 §7单位: §f24")));
                player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal("署名地块 旅人")));
            });
            context.waitFor(client -> ConfluxHudClient.territories().size() == 3);
            context.runOnClient(client -> {
                var bars = ((BossOverlayAccessor) HudAccess.bosses()).conflux$getEvents();
                check(bars.size() == 3, "Rendering must retain vanilla boss state");
                check(ConfluxHudClient.territory(bars.get(witherId)) == null, "Wither must keep its original display");
                check(ConfluxHudClient.territories().stream().anyMatch(t -> t.relation().equals("own") && t.underAttack()), "Own territory must not require opening TAB");
                var canvas = new LayoutCheck(320, 240);
                HudRenderer.render(canvas);
                canvas.verify(0, 3);
                check(canvas.text.containsAll(List.of("遇袭", "敌对营地", "敌对", "地块", "地契")), "All active statuses must render");
            });
            context.waitTick();
            context.takeScreenshot("conflux-hud-vanilla-packets");
            context.getInput().resizeWindow(854, 480);
            context.waitTick();
            context.takeScreenshot("conflux-hud-small-window");
            context.getInput().pressKey(InputConstants.KEY_F1);
            context.takeScreenshot("conflux-hud-hidden");
            context.getInput().pressKey(InputConstants.KEY_F1);

            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.connection.send(ClientboundBossEventPacket.createRemovePacket(naturalId));
                player.connection.send(ClientboundBossEventPacket.createUpdateNamePacket(boss(hostileId, "苍松领地 [3]", BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_6)));
                player.connection.send(ClientboundBossEventPacket.createUpdateStylePacket(boss(hostileId, "苍松领地 [3]", BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_6)));
                player.connection.send(new ClientboundTabListPacket(Component.literal("Server"), Component.literal("公告")));
                player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal("普通提示")));
                player.getInventory().setItem(0, new ItemStack(Items.PAPER));
                player.inventoryMenu.broadcastChanges();
            });
            context.waitFor(client -> ConfluxHudClient.territories().size() == 1 && ConfluxHudClient.territories().getFirst().relation().equals("neutral")
                && ConfluxHudClient.territories().getFirst().units() == 3 && ConfluxHudClient.CLAIM.holder() == null && ConfluxHudClient.deed().isEmpty());
            context.takeScreenshot("conflux-hud-updated");
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().connection.send(
                ClientboundBossEventPacket.createUpdateNamePacket(boss(hostileId, "领地".repeat(100) + " [2147483647]", BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_6))));
            context.waitFor(client -> ConfluxHudClient.territories().getFirst().units() == Integer.MAX_VALUE);
            context.runOnClient(client -> {
                var canvas = new LayoutCheck(320, 240);
                HudRenderer.render(canvas);
                canvas.verify(0, 1);
                check(canvas.text.stream().anyMatch(text -> text.contains("…")), "Long names must be ellipsized");
                check(canvas.text.stream().anyMatch(text -> text.contains("2147483647") && text.contains("65%")), "Long names must not hide counts or progress");
            });
            context.takeScreenshot("conflux-hud-long-name");
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.connection.send(ClientboundBossEventPacket.createRemovePacket(hostileId));
                player.connection.send(new ClientboundSystemChatPacket(Component.literal("署名地块 Another player"), true));
            });
            context.waitFor(client -> "Another player".equals(ConfluxHudClient.CLAIM.holder()));
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().connection.send(new ClientboundSystemChatPacket(Component.empty(), true)));
            context.waitFor(client -> ConfluxHudClient.CLAIM.holder() == null);
            context.runOnClient(client -> checkEmpty());
            context.takeScreenshot("conflux-hud-cleared");
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().connection.send(new ClientboundSetActionBarTextPacket(Component.literal("署名地块 Disconnect"))));
            context.waitFor(client -> ConfluxHudClient.CLAIM.holder() != null);
        }
        try (var world = context.worldBuilder().create()) {
            context.runOnClient(client -> checkEmpty());
        }
        shopping(context);
    }

    private static void shopping(ClientGameTestContext context) {
        context.getInput().resizeWindow(1280, 720);
        try (var world = context.worldBuilder().create()) {
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                var stock = new SimpleContainer(54);
                stock.setItem(0, new ItemStack(Items.OAK_LOG, 64));
                stock.setItem(1, new ItemStack(Items.IRON_INGOT, 16));
                player.openMenu(new SimpleMenuProvider((id, inventory, user) -> ChestMenu.sixRows(id, inventory, stock), Component.literal("货物木桶")));
                player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal("取出买入，放入卖出，关闭统一结算")));
            });
            context.waitFor(client -> ConfluxHudClient.trading() && ConfluxHudClient.TRADE.quote() != null);
            context.runOnClient(client -> {
                check(ConfluxHudClient.TRADE.quote().buy() == 0 && ConfluxHudClient.TRADE.change() == null, "Shopping starts with no previous transaction");
            });
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().connection.send(
                new ClientboundSetActionBarTextPacket(Component.literal("预估买入 16 件（120 绿宝石），卖出 0 绿宝石"))));
            context.waitFor(client -> ConfluxHudClient.TRADE.quote() != null && ConfluxHudClient.TRADE.quote().buy() == 120);
            context.runOnClient(client -> check(ConfluxHudClient.TRADE.change().buy() == 120, "First purchase must show its incremental cost"));
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().connection.send(
                new ClientboundSystemChatPacket(Component.literal("预估买入 16 件（120 绿宝石），卖出 40 绿宝石"), true)));
            context.waitFor(client -> ConfluxHudClient.TRADE.quote() != null && ConfluxHudClient.TRADE.quote().sell() == 40);
            context.runOnClient(client -> {
                check(ConfluxHudClient.TRADE.change().buy() == 0 && ConfluxHudClient.TRADE.change().sell() == 40, "Sale must not include the preceding purchase");
                var canvas = new LayoutCheck(640, 360);
                HudRenderer.render(canvas);
                canvas.verify(0, 1);
                check(canvas.text.containsAll(List.of("预估", "上次变动", "买入 120  卖出 40  绿宝石", "买入 0  卖出 +40  绿宝石")), "Trade HUD must show authoritative totals, signed changes and currency");
            });
            context.waitTick();
            context.takeScreenshot("conflux-hud-shopping");
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().connection.send(
                new ClientboundSetActionBarTextPacket(Component.literal("预估买入 8 件（60 绿宝石），卖出 40 绿宝石"))));
            context.waitFor(client -> ConfluxHudClient.TRADE.quote() != null && ConfluxHudClient.TRADE.quote().buy() == 60);
            context.runOnClient(client -> check(ConfluxHudClient.TRADE.change().buy() == -60 && ConfluxHudClient.TRADE.change().sell() == 0, "Returning goods must show a negative purchase change"));
            context.takeScreenshot("conflux-hud-shopping-undo");
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().connection.send(
                new ClientboundSetActionBarTextPacket(Component.literal("这些物品无法估价，关闭后本次买卖将取消"))));
            context.waitFor(client -> ConfluxHudClient.TRADE.message() != null);
            context.runOnClient(client -> check(ConfluxHudClient.TRADE.quote() == null && ConfluxHudClient.TRADE.change() == null, "Invalid quotes must clear old prices"));
            context.takeScreenshot("conflux-hud-shopping-warning");
            context.getInput().pressKey(InputConstants.KEY_ESCAPE);
            context.waitFor(client -> !ConfluxHudClient.trading() && ConfluxHudClient.TRADE.message() == null);
            context.runOnClient(client -> checkEmpty());
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.openMenu(new SimpleMenuProvider((id, inventory, user) -> ChestMenu.sixRows(id, inventory, new SimpleContainer(54)), Component.literal("普通箱子")));
            });
            context.waitFor(client -> HudAccess.screen() != null);
            context.runOnClient(client -> {
                var message = Component.literal("预估买入 1 件（20 绿宝石），卖出 0 绿宝石");
                check(ConfluxHudClient.actionBar(message) == message, "Other containers must keep their original action bar");
                check(ConfluxHudClient.TRADE.quote() == null, "Other containers must not acquire trade state");
            });
            context.getInput().pressKey(InputConstants.KEY_ESCAPE);
            context.waitFor(client -> HudAccess.screen() == null);
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.openMenu(new SimpleMenuProvider((id, inventory, user) -> ChestMenu.sixRows(id, inventory, new SimpleContainer(54)), Component.literal("货物木桶")));
            });
            context.waitFor(client -> ConfluxHudClient.isTradeScreen(HudAccess.screen()));
            context.runOnClient(client -> {
                check(!ConfluxHudClient.trading(), "A chest name alone must not activate a shopping HUD");
                var message = Component.literal("普通提示");
                check(ConfluxHudClient.actionBar(message) == message && !ConfluxHudClient.trading(), "An unrelated message in a named chest must be preserved");
            });
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().connection.send(
                new ClientboundSetActionBarTextPacket(Component.literal("取出买入，放入卖出，关闭统一结算"))));
            context.waitFor(client -> ConfluxHudClient.TRADE.quote() != null);
            context.runOnClient(client -> check(ConfluxHudClient.TRADE.quote().buy() == 0 && ConfluxHudClient.TRADE.change() == null, "Reopening shopping must not retain previous prices"));
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().connection.send(
                new ClientboundSetActionBarTextPacket(Component.literal("预估买入 16 件（2147483647 绿宝石），卖出 2147483647 绿宝石"))));
            context.waitFor(client -> ConfluxHudClient.TRADE.quote().buy() == Integer.MAX_VALUE);
            context.runOnClient(client -> {
                var canvas = new LayoutCheck(320, 240);
                HudRenderer.render(canvas);
                canvas.verify(0, 2);
                check(canvas.text.stream().filter(value -> value.contains("2147483647")).count() == 2, "Narrow HUD must retain both totals and both price changes");
                check(canvas.text.stream().noneMatch(value -> value.contains("…")), "Maximum prices must not be truncated");
            });
        }
        context.runOnClient(client -> check(ConfluxHudClient.TRADE.quote() == null && ConfluxHudClient.TRADE.change() == null, "Disconnect must clear trade prices"));
    }

    private static BossEvent boss(UUID id, String name, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay) {
        var bar = new BossEvent(id, Component.literal(name), color, overlay) {};
        bar.setProgress(.65f);
        return bar;
    }

    private static void checkHudUncovered(Path normal, Path withTab, int height) {
        try {
            var before = ImageIO.read(normal.toFile());
            var after = ImageIO.read(withTab.toFile());
            int scale = before.getWidth() / 640, center = before.getWidth() / 2;
            for (int y = 0; y < height * scale; y++) {
                for (int x = center - 8 * scale; x < center + 8 * scale; x++) {
                    check(before.getRGB(x, y) == after.getRGB(x, y), "TAB must not cover the HUD");
                }
            }
        } catch (IOException exception) {
            throw new AssertionError("Could not read TAB regression screenshots", exception);
        }
    }

    private static void checkEmpty() {
        check(ConfluxHudClient.territories().isEmpty(), "HUD must not retain old territories");
        check(ConfluxHudClient.deed().isEmpty(), "HUD must not retain old deeds");
        check(ConfluxHudClient.CLAIM.holder() == null, "HUD must not retain old claims");
        check(ConfluxHudClient.TRADE.quote() == null && ConfluxHudClient.TRADE.change() == null, "HUD must not retain old trade prices");
        check(HudRenderer.height(427) == 0, "No HUD must leave the original BossBar position unchanged");
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class LayoutCheck implements HudCanvas {
        private final int width, height;
        private final List<int[]> rows = new ArrayList<>();
        private final List<int[]> bottomEdges = new ArrayList<>();
        private final List<String> text = new ArrayList<>();
        LayoutCheck(int width, int height) { this.width = width; this.height = height; }
        public int width() { return width; }
        public void buttonSlice(int x, int y, int width, int height, int sliceTop, int sliceHeight) {
            check(sliceTop >= 0 && sliceHeight > 0 && sliceTop + sliceHeight <= height, "Button slice outside texture bounds");
            check(x >= 0 && x + width <= this.width && y >= 0 && y + sliceTop + sliceHeight <= this.height, "HUD background outside viewport");
            if (sliceTop == 0) rows.add(new int[]{x, y, x + width, y + height});
            if (sliceTop + sliceHeight == height) bottomEdges.add(new int[]{x, x + width});
        }
        public void text(String value, int x, int y, int color) {
            check(x >= 0 && x + Math.ceil(net.minecraft.client.Minecraft.getInstance().font.width(value) * HudRenderer.TEXT_SCALE) <= width - 8
                && y >= 0 && y + Math.ceil(9 * HudRenderer.TEXT_SCALE) <= height, "HUD text outside viewport");
            text.add(value);
        }
        void verify(int top, int maxRows) {
            check(!rows.isEmpty() && rows.size() <= maxRows, "HUD takes too many rows");
            check(bottomEdges.size() == rows.size(), "Each row needs one bottom edge");
            for (int i = 0; i < rows.size(); i++) {
                int[] row = rows.get(i);
                check(Math.abs(row[0] - (width - row[2])) <= 1, "Every HUD row must be centered");
                check(row[3] - row[1] == 14 && row[1] == top + i * 16, "HUD rows must be compact and separated");
                int[] bottom = bottomEdges.get(i);
                check(bottom[0] > row[0] && bottom[1] < row[2] && bottom[0] - row[0] == row[2] - bottom[1], "Both bottom corners must be rounded symmetrically");
            }
            check(HudRenderer.height(width) == rows.getLast()[3], "BossBars must start below the rendered HUD");
        }
    }
}
