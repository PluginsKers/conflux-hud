package com.eyreland.conflux.hud.client;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public final class AnvilClientGameTest implements FabricClientGameTest {
    private static final Set<String> LABELS = new HashSet<>();

    public static void capture(FormattedText text) {
        if (HudAccess.screen() instanceof AnvilScreen) LABELS.add(text.getString());
    }

    @Override public void runTest(ClientGameTestContext context) {
        context.getInput().resizeWindow(1280, 720);
        context.runOnClient(client -> client.options.guiScale().set(2));
        try (var world = context.worldBuilder().create()) {
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.openMenu(new SimpleMenuProvider((id, inventory, user) -> new AnvilMenu(id, inventory),
                    Component.translatable("container.repair")));
            });
            context.waitForScreen(AnvilScreen.class);
            for (String language : new String[]{"en_us", "zh_cn"}) {
                var reload = context.computeOnClient(client -> {
                    client.options.languageCode = language;
                    client.getLanguageManager().setSelected(language);
                    return client.reloadResourcePacks();
                });
                context.waitFor(client -> reload.isDone());
                reload.join();
                // cost, experience, creative, result, expected label (0 = hidden, 1 = cost, 2 = too expensive)
                int[][] cases = {{39, 39, 0, 1, 1}, {40, 40, 0, 1, 1}, {65, 65, 0, 1, 1},
                    {65, 64, 0, 1, 1}, {65, 0, 1, 1, 1}, {32767, 32767, 0, 1, 1},
                    {65, 65, 0, 0, 2}, {39, 39, 0, 0, 0}, {0, 0, 0, 0, 0}};
                for (int index = 0; index < cases.length; index++) {
                    int[] scenario = cases[index];
                    int cost = scenario[0], experience = scenario[1], expected = scenario[4];
                    boolean creative = scenario[2] == 1, result = scenario[3] == 1;
                    world.getServer().runOnServer(server -> {
                        var player = server.getPlayerList().getPlayers().getFirst();
                        player.setGameMode(creative ? GameType.CREATIVE : GameType.SURVIVAL);
                        player.setExperienceLevels(experience);
                        var menu = (AnvilMenu) player.containerMenu;
                        menu.getSlot(2).set(result ? new ItemStack(Items.DIAMOND_SWORD) : ItemStack.EMPTY);
                        menu.setData(0, cost);
                        menu.broadcastChanges();
                    });
                    context.waitFor(client -> {
                        var menu = ((AnvilScreen) HudAccess.screen()).getMenu();
                        return menu.getCost() == cost && menu.getSlot(2).hasItem() == result
                            && client.player.experienceLevel == experience && client.player.hasInfiniteMaterials() == creative;
                    });
                    context.runOnClient(client -> LABELS.clear());
                    Path screenshot = context.takeScreenshot("anvil-" + language + "-" + index);
                    context.runOnClient(client -> {
                        String numeric = Component.translatable("container.repair.cost", cost).getString();
                        String expensive = Component.translatable("container.repair.expensive").getString();
                        check(LABELS.contains(numeric) == (expected == 1), "Incorrect numeric label: " + LABELS);
                        check(LABELS.contains(expensive) == (expected == 2), "Incorrect expensive label: " + LABELS);
                        check(client.player.hasInfiniteMaterials() == creative, "Rendering changed player abilities");
                        var menu = ((AnvilScreen) HudAccess.screen()).getMenu();
                        check(menu.getCost() == cost, "Rendering changed the cost");
                        if (result) check(menu.getSlot(2).mayPickup(client.player) == (creative || experience >= cost),
                            "Rendering changed experience requirements");
                    });
                    if (expected != 0) {
                        int[] bounds = context.computeOnClient(client -> new int[]{HudAccess.screen().width, HudAccess.screen().height});
                        checkColor(screenshot, bounds, expected == 2 || (!creative && experience < cost) ? 0xFF6060 : 0x80FF20);
                    }
                }
            }
        }
    }

    private static void checkColor(Path screenshot, int[] bounds, int color) {
        try {
            var image = ImageIO.read(screenshot.toFile());
            int scale = image.getWidth() / bounds[0];
            int left = (bounds[0] - 176) / 2, top = (bounds[1] - 166) / 2;
            for (int y = (top + 67) * scale; y < (top + 79) * scale; y++) {
                for (int x = left * scale; x < (left + 168) * scale; x++) {
                    int pixel = image.getRGB(x, y);
                    if (Math.abs((pixel >> 16 & 255) - (color >> 16 & 255)) <= 4
                        && Math.abs((pixel >> 8 & 255) - (color >> 8 & 255)) <= 4
                        && Math.abs((pixel & 255) - (color & 255)) <= 4) return;
                }
            }
            throw new AssertionError("Missing anvil label color " + Integer.toHexString(color));
        } catch (IOException exception) {
            throw new AssertionError("Could not read anvil screenshot", exception);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
