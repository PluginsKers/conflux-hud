package com.eyreland.conflux.hud.client;

public final class TradeNotice {
    private HudText.Trade quote, change;
    private String message;

    public boolean accept(String text) {
        var next = HudText.trade(text);
        if (next == null) {
            if (quote != null || message != null) {
                clear();
                message = HudText.clean(text);
            }
            return false;
        }
        if (!next.equals(quote)) {
            // shortcut: The server can combine rapid actions; per-action prices need server-issued operation IDs.
            change = quote == null ? null : new HudText.Trade(next.items() - quote.items(), next.buy() - quote.buy(), next.sell() - quote.sell());
            quote = next;
        }
        message = null;
        return true;
    }

    public HudText.Trade quote() { return quote; }
    public HudText.Trade change() { return change; }
    public String message() { return message; }
    public void clear() { quote = null; change = null; message = null; }
}
