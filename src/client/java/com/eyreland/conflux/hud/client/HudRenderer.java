package com.eyreland.conflux.hud.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class HudRenderer {
    static final float TEXT_SCALE = .95f;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREEN = 0xFF55FF55, RED = 0xFFFF5555, GOLD = 0xFFFFAA00, BLUE = 0xFF55FFFF;
    private static final int HEIGHT = 14, GAP = 2, PADDING = 8;
    private static final Pattern UNSAFE_TEXT = Pattern.compile("§[0-9a-fk-orA-FK-OR]|[\\p{Cntrl}]");

    static void render(HudCanvas canvas) {
        int y = 0;
        for (var row : rows(canvas.width())) {
            row(canvas, row.entries(), (canvas.width() - row.width()) / 2, y, row.width(), canvas.width() - 16);
            y += HEIGHT + GAP;
        }
    }

    public static int height(int screenWidth) {
        int count = rows(screenWidth).size();
        return count == 0 ? 0 : count * (HEIGHT + GAP) - GAP;
    }

    private static List<Row> rows(int screenWidth) {
        var client = Minecraft.getInstance();
        int maxWidth = screenWidth - 16;
        if (client.level == null || client.player == null || maxWidth < 32) return List.of();
        var entries = new ArrayList<Entry>();
        if (ConfluxHudClient.trading()) {
            var quote = ConfluxHudClient.TRADE.quote();
            var change = ConfluxHudClient.TRADE.change();
            if (quote == null) {
                String message = ConfluxHudClient.TRADE.message();
                entries.add(new Entry(tr("trade_title"), "", message == null || message.isBlank() ? tr("trade_waiting") : clean(message), GOLD));
            } else {
                entries.add(new Entry(tr("trade_total"), "", tr("trade_prices", quote.buy(), quote.sell()), GOLD));
                if (change != null) entries.add(new Entry(tr("trade_change"), "", tr("trade_prices", signed(change.buy()), signed(change.sell())), BLUE));
            }
        } else {
            for (var value : ConfluxHudClient.territories()) {
                boolean own = value.relation().equals("own"), hostile = value.relation().equals("hostile");
                int accent = value.underAttack() || hostile ? RED : own ? GREEN : BLUE;
                String label = tr(value.underAttack() ? "under_attack" : value.natural() ? "natural" : value.relation());
                String detail = tr(value.natural() ? "guards" : "units", value.units());
                if (value.proximity() >= 0) detail += "  " + tr("proximity", Math.round(value.proximity() * 100));
                entries.add(new Entry(label,
                    value.natural() ? "" : clean(value.name()), detail, accent));
            }
            String holder = ConfluxHudClient.CLAIM.holder();
            if (holder != null) entries.add(new Entry(tr("claim"), clean(holder), "", GOLD));
            var deed = ConfluxHudClient.deed();
            if (!deed.isEmpty()) {
                int points = deed.stream().mapToInt(HudText::points).filter(count -> count >= 0).findFirst().orElse(-1);
                entries.add(new Entry(tr("deed_title"), "", points < 0 ? "" : tr("points", points), GOLD));
            }
        }

        var rows = new ArrayList<Row>();
        for (int start = 0; start < entries.size();) {
            int end = start, width = 0;
            do {
                width += entries.get(end++).width(maxWidth);
            } while (end < entries.size() && width + entries.get(end).width(maxWidth) <= maxWidth);
            rows.add(new Row(entries.subList(start, end), width));
            start = end;
        }
        return rows;
    }

    private static void row(HudCanvas canvas, List<Entry> entries, int x, int y, int width, int maxWidth) {
        canvas.buttonSlice(x, y, width, HEIGHT, 0, HEIGHT - 3);
        canvas.buttonSlice(x + 1, y, width - 2, HEIGHT, HEIGHT - 3, 1);
        canvas.buttonSlice(x + 2, y, width - 4, HEIGHT, HEIGHT - 2, 1);
        canvas.buttonSlice(x + 4, y, width - 8, HEIGHT, HEIGHT - 1, 1);
        for (var entry : entries) {
            int cell = entry.width(maxWidth);
            int left = x + PADDING, right = x + cell - PADDING;
            String detail = clip(entry.detail(), right - left - measure("…") - 4);
            int titleWidth = right - left - (detail.isEmpty() ? 0 : measure(detail) + 4);
            String label = clip(entry.label(), titleWidth);
            canvas.text(label, left, y + 3, entry.accent());
            left += measure(label) + 3;
            String name = clip(entry.name(), titleWidth - measure(label) - 3);
            if (!name.isEmpty()) canvas.text(name, left, y + 3, WHITE);
            if (!detail.isEmpty()) canvas.text(detail, right - measure(detail), y + 3, WHITE);
            x += cell;
        }
    }

    private record Row(List<Entry> entries, int width) {}
    private record Entry(String label, String name, String detail, int accent) {
        int width(int maxWidth) {
            return Math.min(maxWidth, 2 * PADDING + measure(label) + (name.isEmpty() ? 0 : 3 + measure(name))
                + (detail.isEmpty() ? 0 : 4 + measure(detail)));
        }
    }

    private static int measure(String value) { return (int) Math.ceil(Minecraft.getInstance().font.width(value) * TEXT_SCALE); }
    private static String signed(int value) { return value > 0 ? "+" + value : Integer.toString(value); }
    private static String clip(String value, int width) {
        if (width <= 0) return "";
        if (measure(value) <= width) return value;
        if (width < measure("…")) return "";
        return Minecraft.getInstance().font.plainSubstrByWidth(value, (int) ((width - measure("…")) / TEXT_SCALE)) + "…";
    }
    private static String clean(String value) { return UNSAFE_TEXT.matcher(value).replaceAll(""); }
    private static String tr(String key, Object... arguments) { return Component.translatable("conflux_hud." + key, arguments).getString(); }
}
