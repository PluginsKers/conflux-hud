package com.eyreland.conflux.hud.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TradeNoticeTest {
    private static final String EMPTY = "取出买入，放入卖出，关闭统一结算";

    @Test void readsServerEstimatesInEmeralds() {
        assertEquals(new HudText.Trade(0, 0, 0), HudText.trade(EMPTY));
        assertEquals(new HudText.Trade(16, 120, 40), HudText.trade("§e预估买入 16 件（120 绿宝石），卖出 40 绿宝石"));
        assertEquals(new HudText.Trade(1, Integer.MAX_VALUE, 0), HudText.trade(quote(1, Integer.MAX_VALUE, 0)));
        for (String text : new String[]{"预估买入 -1 件（120 绿宝石），卖出 40 绿宝石",
            "预估买入 1 件（9999999999 绿宝石），卖出 40 绿宝石", "预估买入 1 件（1.5 绿宝石），卖出 40 绿宝石",
            "预估买入 1 件（1 绿宝石），卖出 -40 绿宝石", quote(1, 1, 0) + "\n公告", "署名地块 玩家"}) {
            assertNull(HudText.trade(text), text);
        }
    }

    @Test void showsTheLastPurchaseSaleOrUndoWithoutChangingTotals() {
        var notice = new TradeNotice();
        assertTrue(notice.accept(EMPTY));
        assertNull(notice.change());
        notice.accept(quote(16, 120, 0));
        assertEquals(new HudText.Trade(16, 120, 0), notice.change());
        notice.accept(quote(16, 120, 40));
        assertEquals(new HudText.Trade(16, 120, 40), notice.quote());
        assertEquals(new HudText.Trade(0, 0, 40), notice.change());
        notice.accept(quote(16, 120, 40));
        assertEquals(new HudText.Trade(0, 0, 40), notice.change(), "Repeated server estimates must not erase the last change");
        notice.accept(quote(8, 60, 40));
        assertEquals(new HudText.Trade(-8, -60, 0), notice.change());
        notice.accept(quote(8, 60, 0));
        assertEquals(new HudText.Trade(0, 0, -40), notice.change());
        notice.accept(EMPTY);
        assertEquals(new HudText.Trade(0, 0, 0), notice.quote());
        assertEquals(new HudText.Trade(-8, -60, 0), notice.change());
    }

    @Test void unknownQuotesAndNewSessionsNeverReuseOldMoney() {
        var notice = new TradeNotice();
        assertFalse(notice.accept("普通提示"));
        assertNull(notice.message(), "Unrelated messages must not activate a shopping HUD");
        notice.accept(EMPTY);
        notice.accept(quote(16, 120, 0));
        String warning = "这些物品无法估价，关闭后本次买卖将取消";
        assertFalse(notice.accept(warning));
        assertNull(notice.quote());
        assertNull(notice.change());
        assertEquals(warning, notice.message());
        notice.accept(quote(8, 60, 0));
        assertNull(notice.change(), "A missing baseline must not be treated as a zero-price operation");
        notice.clear();
        assertNull(notice.message());
        assertNull(notice.quote());
        assertNull(notice.change());
        notice.accept(EMPTY);
        assertNull(notice.change());
    }

    private static String quote(int items, int buy, int sell) {
        return "预估买入 " + items + " 件（" + buy + " 绿宝石），卖出 " + sell + " 绿宝石";
    }
}
