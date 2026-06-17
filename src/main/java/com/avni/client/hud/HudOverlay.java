package com.avni.client.hud;

import net.minecraft.client.gui.DrawContext;

/** Entry point called from {@code InGameHudMixin}; delegates to {@link Hud}. */
public final class HudOverlay {
    private HudOverlay() {
    }

    public static void render(DrawContext ctx) {
        Hud.renderAll(ctx);
    }
}
