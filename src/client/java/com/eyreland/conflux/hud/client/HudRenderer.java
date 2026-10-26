package com.eyreland.conflux.hud.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class HudRenderer {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREEN = 0xFF55FF55, RED = 0xFFFF5555, GOLD = 0xFFFFAA00, BLUE = 0xFF55FFFF;
    // The button's top border stays above the screen, so the notch hangs from the top edge.
    private static final int HIDDEN = 2, HEIGHT = 15, LINE = 11, TOP = 2, BEVEL = 3, PADDING = 8;
    // Each bottom row steps in by one pixel at most, keeping the outline unbroken around the corners.
    private static final int[] CORNER = {1, 2, 3};
    // Time constant of the easing, in nanoseconds.
    private static final double EASING = 4e7;
    private static final Pattern UNSAFE_TEXT = Pattern.compile("§[0-9a-fk-orA-FK-OR]|[\\p{Cntrl}]");
    private static List<Row> shown = List.of();
    // Size of the notch, and how far it is raised while it slides in or out.
    private static float width, height, offset;
    private static long updated;

    static void render(HudCanvas canvas) {
        update(canvas.width());
        if (shown.isEmpty()) return;
        int w = Math.round(width), h = Math.round(height), x = (canvas.width() - w) / 2, top = Math.round(offset);
        background(canvas, x, top, w, h);
        if (top + h - BEVEL <= 0) return;
        // Content follows the left edge, so entries added or removed at the end slide instead of jumping.
        int content = shown.stream().mapToInt(Row::width).max().orElseThrow(), y = top + TOP;
        canvas.clip(x + 2, Math.max(0, top), x + w - 2, top + h - BEVEL);
        for (var row : shown) {
            row(canvas, row.entries(), x + (content - row.width()) / 2, y, canvas.width() - 16);
            y += LINE;
        }
        canvas.unclip();
    }

    public static int height(int screenWidth) {
        update(screenWidth);
        return Math.round(height + offset);
    }

    // The notch slides out of the top edge when rows appear and back when they are gone, showing the last rows meanwhile.
    private static void update(int screenWidth) {
        var rows = rows(screenWidth);
        long now = System.nanoTime();
        float progress = 1 - (float) Math.exp(Math.min(0, updated - now) / EASING);
        updated = now;
        if (!rows.isEmpty()) {
            boolean hidden = offset == -height;
            int targetWidth = rows.stream().mapToInt(Row::width).max().orElseThrow(), targetHeight = HEIGHT + (rows.size() - 1) * LINE;
            width = hidden ? targetWidth : ease(width, targetWidth, progress);
            height = hidden ? targetHeight : ease(height, targetHeight, progress);
            if (hidden) offset = -height;
            shown = rows;
        }
        offset = ease(offset, rows.isEmpty() ? -height : 0, progress);
        if (offset == -height) shown = List.of();
    }

    private static float ease(float value, float target, float progress) {
        value += (target - value) * progress;
        return Math.abs(target - value) < .5f ? target : value;
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

    private static void background(HudCanvas canvas, int x, int top, int width, int height) {
        int first = Math.max(0, -top), rounded = height - Math.min(CORNER.length, height);
        if (first < rounded) canvas.buttonSlice(x, top - HIDDEN, width, height + HIDDEN, HIDDEN + first, rounded - first);
        for (int row = Math.max(first, rounded); row < height; row++) {
            int inset = CORNER[CORNER.length - height + row];
            canvas.buttonSlice(x + inset, top - HIDDEN, width - 2 * inset, height + HIDDEN, HIDDEN + row, 1);
        }
    }

    private static void row(HudCanvas canvas, List<Entry> entries, int x, int y, int maxWidth) {
        for (var entry : entries) {
            int cell = entry.width(maxWidth);
            int left = x + PADDING, right = x + cell - PADDING;
            String detail = clip(entry.detail(), right - left - measure("…") - 4);
            int titleWidth = right - left - (detail.isEmpty() ? 0 : measure(detail) + 4);
            String label = clip(entry.label(), titleWidth);
            canvas.text(label, left, y, entry.accent());
            left += measure(label) + 3;
            String name = clip(entry.name(), titleWidth - measure(label) - 3);
            if (!name.isEmpty()) canvas.text(name, left, y, WHITE);
            if (!detail.isEmpty()) canvas.text(detail, right - measure(detail), y, WHITE);
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

    private static int measure(String value) { return Minecraft.getInstance().font.width(value); }
    private static String signed(int value) { return value > 0 ? "+" + value : Integer.toString(value); }
    private static String clip(String value, int width) {
        if (width <= 0) return "";
        if (measure(value) <= width) return value;
        if (width < measure("…")) return "";
        return Minecraft.getInstance().font.plainSubstrByWidth(value, width - measure("…")) + "…";
    }
    private static String clean(String value) { return UNSAFE_TEXT.matcher(value).replaceAll(""); }
    private static String tr(String key, Object... arguments) { return Component.translatable("conflux_hud." + key, arguments).getString(); }
}
