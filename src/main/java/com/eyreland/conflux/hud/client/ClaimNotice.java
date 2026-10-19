package com.eyreland.conflux.hud.client;

public final class ClaimNotice {
    private String holder;
    private int ticks;

    public boolean accept(String message) {
        holder = HudText.claim(message);
        ticks = holder == null ? 0 : 60;
        return holder != null;
    }

    public String holder() { return ticks > 0 ? holder : null; }
    public void tick() { if (ticks > 0 && --ticks == 0) holder = null; }
    public void clear() { holder = null; ticks = 0; }
}
