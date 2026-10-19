package com.eyreland.conflux.hud.client;

import java.util.regex.Pattern;

public final class HudText {
    private static final Pattern FORMATTING = Pattern.compile("§[0-9a-fk-orA-FK-OR]");
    private static final Pattern BOSS = Pattern.compile("([^\\r\\n]{1,256}) \\[(\\d{1,10})]");
    private static final Pattern OWN = Pattern.compile("所在领地: ([^\\r\\n]{1,256}) 单位: (\\d{1,10})");
    private static final Pattern POINTS = Pattern.compile("已选 (\\d{1,10}) 点");
    private static final Pattern TRADE = Pattern.compile("预估买入 (\\d{1,10}) 件（(\\d{1,10}) 绿宝石），卖出 (\\d{1,10}) 绿宝石");

    public record Territory(String name, int units, String relation, float proximity, boolean natural, boolean underAttack) {}
    public record Trade(int items, int buy, int sell) {}

    private HudText() {}

    public static Territory boss(String title, String color, String overlay, float progress) {
        if (!overlay.equals("NOTCHED_6") || !(color.equals("RED") || color.equals("BLUE"))) return null;
        if (!Float.isFinite(progress) || progress < 0 || progress > 1) return null;
        var match = BOSS.matcher(clean(title));
        if (!match.matches()) return null;
        int units = count(match.group(2));
        if (units < 0) return null;
        boolean natural = color.equals("RED") && match.group(1).equals("敌对领地");
        return new Territory(match.group(1), units, color.equals("RED") ? "hostile" : "neutral", progress, natural, false);
    }

    public static Territory own(String footer) {
        String text = clean(footer);
        boolean underAttack = text.lines().anyMatch("你的领地正在遭受袭击，请尽快驰援"::equals);
        for (String line : text.split("\\n")) {
            var match = OWN.matcher(line);
            if (match.matches()) {
                int units = count(match.group(2));
                if (units >= 0) return new Territory(match.group(1), units, "own", -1, false, underAttack);
            }
        }
        return null;
    }

    public static String claim(String text) {
        String clean = clean(text);
        if (!clean.startsWith("署名地块 ")) return null;
        String holder = clean.substring("署名地块 ".length());
        return !holder.isBlank() && holder.length() <= 256 && !holder.contains("\n") ? holder : null;
    }

    public static int points(String line) {
        var match = POINTS.matcher(clean(line));
        return match.matches() ? count(match.group(1)) : -1;
    }

    public static Trade trade(String message) {
        String text = clean(message);
        if (text.equals("取出买入，放入卖出，关闭统一结算")) return new Trade(0, 0, 0);
        var match = TRADE.matcher(text);
        if (!match.matches()) return null;
        int items = count(match.group(1)), buy = count(match.group(2)), sell = count(match.group(3));
        return items < 0 || buy < 0 || sell < 0 ? null : new Trade(items, buy, sell);
    }

    public static String clean(String text) { return FORMATTING.matcher(text).replaceAll(""); }

    private static int count(String text) {
        try { return Integer.parseInt(text); } catch (NumberFormatException ignored) { return -1; }
    }
}
