package com.avni.client.gui;

import com.avni.client.AvniConfig;
import com.avni.client.hud.Hud;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/**
 * In-game HUD editor: drag elements to move them, scroll over one to resize it.
 * Layout is persisted to the mod config and used by the live HUD.
 */
public class HudEditorScreen extends Screen {
    private static final int ACCENT = 0xFF2FE0A6;

    private final Screen parent;
    private final AvniConfig cfg = AvniConfig.get();

    private String dragging = null;
    private double grabOffsetX;
    private double grabOffsetY;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("HUD Editor"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int y = height - 28;
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(width / 2 - 102, y, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset layout"), b -> {
            cfg.hudLayout.clear();
            cfg.save();
        }).dimensions(width / 2 + 2, y, 100, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Light dim (no blur) so the game/HUD stays visible while arranging it.
        ctx.fill(0, 0, width, height, 0x66000000);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Drag to move  •  Scroll to resize  •  Done to save"),
                width / 2, 14, 0xFFD7DEEA);

        for (Hud.Element e : Hud.ELEMENTS) {
            if (!Hud.enabled(cfg, e.id())) {
                continue;
            }
            float[] l = cfg.layout(e.id(), e.defX(), e.defY());
            int px = Math.round(l[0] * width);
            int py = Math.round(l[1] * height);
            int w = Math.round(Hud.width(client, e.id()) * l[2]);
            int h = Math.round(Hud.height(client, e.id()) * l[2]);

            // the element draws its own panel/border; just add a selection outline
            Hud.renderAt(ctx, client, e.id(), px, py, l[2]);
            int ps = Math.round(Hud.PAD * l[2]);
            boolean hover = e.id().equals(dragging) || (mouseX >= px - ps && mouseX <= px + w + ps
                    && mouseY >= py - ps && mouseY <= py + h + ps);
            if (hover) {
                border(ctx, px - ps - 1, py - ps - 1, w + 2 * ps + 2, h + 2 * ps + 2, ACCENT);
                ctx.drawTextWithShadow(textRenderer,
                        Text.literal(e.label() + "  ×" + String.format("%.1f", l[2])),
                        px - ps, py - ps - 11, ACCENT);
            }
        }
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        double mx = click.x();
        double my = click.y();
        // topmost first
        for (int i = Hud.ELEMENTS.size() - 1; i >= 0; i--) {
            Hud.Element e = Hud.ELEMENTS.get(i);
            if (!Hud.enabled(cfg, e.id())) {
                continue;
            }
            float[] l = cfg.layout(e.id(), e.defX(), e.defY());
            int px = Math.round(l[0] * width);
            int py = Math.round(l[1] * height);
            int w = Math.round(Hud.width(client, e.id()) * l[2]);
            int h = Math.round(Hud.height(client, e.id()) * l[2]);
            int ps = Math.round(Hud.PAD * l[2]);
            if (mx >= px - ps && mx <= px + w + ps && my >= py - ps && my <= py + h + ps) {
                dragging = e.id();
                grabOffsetX = mx - px;
                grabOffsetY = my - py;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging != null) {
            float[] l = cfg.hudLayout.get(dragging);
            if (l != null) {
                int w = Math.round(Hud.width(client, dragging) * l[2]);
                int h = Math.round(Hud.height(client, dragging) * l[2]);
                place(l, dragging, click.x() - grabOffsetX, click.y() - grabOffsetY, w, h);
            }
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    private static final int SNAP = 8;

    /** Clamps the element on-screen and snaps to screen edges/center and other elements. */
    private void place(float[] l, String id, double rawX, double rawY, int w, int h) {
        double maxX = Math.max(Hud.PAD, width - w - Hud.PAD);
        double maxY = Math.max(Hud.PAD, height - h - Hud.PAD);
        rawX = MathHelper.clamp(rawX, Hud.PAD, maxX);
        rawY = MathHelper.clamp(rawY, Hud.PAD, maxY);
        l[0] = (float) (snap(rawX, w, id, true) / width);
        l[1] = (float) (snap(rawY, h, id, false) / height);
    }

    /** Finds the nearest snap target (screen + other elements) within SNAP, else returns raw. */
    private double snap(double raw, int size, String id, boolean horizontal) {
        double screen = horizontal ? width : height;
        double max = Math.max(Hud.PAD, screen - size - Hud.PAD);
        double best = raw;
        double bestDist = SNAP + 0.5;

        double[] base = {Hud.PAD, max, (screen - size) / 2.0};
        for (double c : base) {
            double d = Math.abs(raw - c);
            if (d < bestDist) {
                bestDist = d;
                best = c;
            }
        }
        for (Hud.Element e : Hud.ELEMENTS) {
            if (e.id().equals(id) || !Hud.enabled(cfg, e.id())) {
                continue;
            }
            float[] ol = cfg.layout(e.id(), e.defX(), e.defY());
            double op = (horizontal ? ol[0] * width : ol[1] * height);
            double osize = horizontal
                    ? Math.round(Hud.width(client, e.id()) * ol[2])
                    : Math.round(Hud.height(client, e.id()) * ol[2]);
            for (double c : new double[]{op, op + osize - size, op + osize + 2 * Hud.PAD, op - size - 2 * Hud.PAD}) {
                double d = Math.abs(raw - c);
                if (d < bestDist) {
                    bestDist = d;
                    best = c;
                }
            }
        }
        return MathHelper.clamp(best, Hud.PAD, max);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragging != null) {
            dragging = null;
            cfg.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        for (int i = Hud.ELEMENTS.size() - 1; i >= 0; i--) {
            Hud.Element e = Hud.ELEMENTS.get(i);
            if (!Hud.enabled(cfg, e.id())) {
                continue;
            }
            float[] l = cfg.layout(e.id(), e.defX(), e.defY());
            int px = Math.round(l[0] * width);
            int py = Math.round(l[1] * height);
            int w = Math.round(Hud.width(client, e.id()) * l[2]);
            int h = Math.round(Hud.height(client, e.id()) * l[2]);
            int ps = Math.round(Hud.PAD * l[2]);
            if (mouseX >= px - ps && mouseX <= px + w + ps && mouseY >= py - ps && mouseY <= py + h + ps) {
                l[2] = MathHelper.clamp(l[2] + (float) vertical * 0.1f, 0.5f, 3.0f);
                // keep it on-screen after resizing
                int nw = Math.round(Hud.width(client, e.id()) * l[2]);
                int nh = Math.round(Hud.height(client, e.id()) * l[2]);
                place(l, e.id(), l[0] * width, l[1] * height, nw, nh);
                cfg.save();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public void close() {
        cfg.save();
        client.setScreen(parent);
    }

    private static void border(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }
}
