package com.eyreland.conflux.hud.client;

interface HudCanvas {
    int width();
    void buttonSlice(int x, int y, int width, int height, int sliceTop, int sliceHeight);
    void text(String text, int x, int y, int color);
}
