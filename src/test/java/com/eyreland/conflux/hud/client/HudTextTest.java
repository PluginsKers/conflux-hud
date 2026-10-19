package com.eyreland.conflux.hud.client;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class HudTextTest {
    @Test void readsTheReleasedPluginBossBars() {
        var natural = HudText.boss("§c敌对领地 §f[8]", "RED", "NOTCHED_6", .4f);
        assertTrue(natural.natural());
        assertEquals(8, natural.units());
        var hostile = HudText.boss("§c红杉 [队伍] §c[24]", "RED", "NOTCHED_6", .7f);
        assertEquals("红杉 [队伍]", hostile.name());
        assertEquals("hostile", hostile.relation());
        assertFalse(hostile.natural());
        assertEquals("neutral", HudText.boss("§b苍松 §b[0]", "BLUE", "NOTCHED_6", 0).relation());
    }

    @Test void preservesOtherBossBarsAndMalformedTitles() {
        assertNull(HudText.boss("Wither", "PURPLE", "PROGRESS", 1));
        assertNull(HudText.boss("Team [5]", "RED", "PROGRESS", 1));
        assertNull(HudText.boss("Team [5]", "GREEN", "NOTCHED_6", 1));
        for (String text : new String[]{"[5]", "Team [-1]", "Team [9999999999]", "Team [5]\nOops", "x".repeat(257) + " [5]"}) {
            assertNull(HudText.boss(text, "RED", "NOTCHED_6", .5f), text);
        }
        assertNull(HudText.boss("Team [1]", "RED", "NOTCHED_6", Float.NaN));
    }

    @Test void readsOwnTerritoryFromTheUnopenedTabFooter() {
        var own = HudText.own("§7公告\n§c你的领地正在遭受袭击，请尽快驰援\n§7所在领地: §f苍松领地 §7单位: §f24\n网站");
        assertEquals("苍松领地", own.name());
        assertEquals(24, own.units());
        assertTrue(own.underAttack());
        assertEquals(-1, own.proximity());
        assertNull(HudText.own("公告"));
        assertNull(HudText.own("所在领地: test 单位: 9999999999"));
    }

    @Test void claimsExpireAndClearOnOtherMessagesOrDisconnect() {
        var claim = new ClaimNotice();
        assertTrue(claim.accept("署名地块 旅人"));
        for (int i = 0; i < 59; i++) claim.tick();
        assertEquals("旅人", claim.holder());
        claim.tick();
        assertNull(claim.holder());
        claim.accept("署名地块 玩家");
        assertFalse(claim.accept("收到 5 枚金币"));
        assertNull(claim.holder());
        claim.accept("署名地块 玩家");
        claim.clear();
        assertNull(claim.holder());
        assertFalse(claim.accept("署名地块 \n"));
    }

    @Test void readsDeedPointCountWithoutInventingServerLimits() {
        assertEquals(4, HudText.points("§7已选 §e4 §7点"));
        assertEquals(-1, HudText.points("1. 200, 300"));
        assertEquals(-1, HudText.points("已选 9999999999 点"));
    }

    @Test void bothLanguagesProvideTheSameKeysAndArguments() throws Exception {
        var resources = getClass().getClassLoader();
        try (var english = new InputStreamReader(resources.getResourceAsStream("assets/conflux-hud/lang/en_us.json"), StandardCharsets.UTF_8);
             var chinese = new InputStreamReader(resources.getResourceAsStream("assets/conflux-hud/lang/zh_cn.json"), StandardCharsets.UTF_8)) {
            var en = JsonParser.parseReader(english).getAsJsonObject();
            var zh = JsonParser.parseReader(chinese).getAsJsonObject();
            assertEquals(en.keySet(), zh.keySet());
            for (String key : en.keySet()) assertEquals(en.get(key).getAsString().split("%s", -1).length, zh.get(key).getAsString().split("%s", -1).length, key);
        }
    }
}
