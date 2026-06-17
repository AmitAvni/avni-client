package com.avni.client.gui;

import com.avni.client.AvniConfig;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Custom-styled settings panel for the Avni mod: themed surface, real toggle
 * switches (not vanilla buttons) laid out in two compact columns, and an entry
 * to the HUD layout editor.
 */
public class AvniConfigScreen extends Screen {
    private static final Identifier CUBE = Identifier.of("avni-client", "textures/gui/cube.png");
    private static final Identifier FONT = Identifier.of("avni-client", "avni");

    private static final int COLS = 3;
    private static final int PANEL_W = 452;
    private static final int HEADER = 40;
    private static final int ROW_H = 26;
    private static final int FOOTER = 64;
    private static final int PAD = 16;
    private static final int COL_GAP = 14;
    private static final int TOG_W = 34;
    private static final int TOG_H = 17;
    private static final int BTN_H = 20;

    private static final int MINT = 0xFF2FE0A6;
    private static final int TEXT = 0xFFEDF0F6;
    private static final int MUTED = 0xFFAAB2C0;
    private static final int PANEL_BG = 0xF013171F;

    private record Row(String label, BooleanSupplier get, Consumer<Boolean> set) {
    }

    private final Screen parent;
    private final List<Row> rows;

    public AvniConfigScreen(Screen parent) {
        super(Text.literal("Avni Client"));
        this.parent = parent;
        AvniConfig cfg = AvniConfig.get();
        this.rows = List.of(
                new Row("HUD", () -> cfg.hudEnabled, v -> cfg.hudEnabled = v),
                new Row("FPS counter", () -> cfg.fps, v -> cfg.fps = v),
                new Row("Coordinates", () -> cfg.coords, v -> cfg.coords = v),
                new Row("Direction", () -> cfg.direction, v -> cfg.direction = v),
                new Row("CPS counter", () -> cfg.cps, v -> cfg.cps = v),
                new Row("Memory", () -> cfg.memory, v -> cfg.memory = v),
                new Row("Day counter", () -> cfg.day, v -> cfg.day = v),
                new Row("Clock", () -> cfg.clock, v -> cfg.clock = v),
                new Row("Session timer", () -> cfg.session, v -> cfg.session = v),
                new Row("Compass", () -> cfg.compass, v -> cfg.compass = v),
                new Row("Waypoint list", () -> cfg.waypoints, v -> cfg.waypoints = v),
                new Row("Keystrokes", () -> cfg.keystrokes, v -> cfg.keystrokes = v),
                new Row("Watermark", () -> cfg.watermark, v -> cfg.watermark = v),
                new Row("Fullbright", () -> cfg.fullbright, v -> cfg.fullbright = v));
    }

    private int numRows() {
        return (rows.size() + COLS - 1) / COLS;
    }

    private int panelH() {
        return HEADER + numRows() * ROW_H + FOOTER;
    }

    private int panelX() {
        return (width - PANEL_W) / 2;
    }

    private int panelY() {
        return (height - panelH()) / 2;
    }

    private int colW() {
        return (PANEL_W - PAD * 2 - COL_GAP * (COLS - 1)) / COLS;
    }

    /** Bounds of the toggle for row i (grid of COLS columns). */
    private int[] toggleRect(int i) {
        int col = i % COLS;
        int rowIdx = i / COLS;
        int cellX = panelX() + PAD + col * (colW() + COL_GAP);
        int cellY = panelY() + HEADER + rowIdx * ROW_H;
        return new int[]{cellX + colW() - TOG_W, cellY + (ROW_H - TOG_H) / 2, TOG_W, TOG_H};
    }

    private int labelX(int i) {
        int col = i % COLS;
        return panelX() + PAD + col * (colW() + COL_GAP);
    }

    private int btnRowY() {
        return panelY() + HEADER + numRows() * ROW_H + 10;
    }

    private int halfW() {
        return (PANEL_W - PAD * 2 - 8) / 2;
    }

    private int[] waypointsBtn() {
        return new int[]{panelX() + PAD, btnRowY(), halfW(), BTN_H};
    }

    private int[] editRect() {
        return new int[]{panelX() + PAD + halfW() + 8, btnRowY(), halfW(), BTN_H};
    }

    private int[] doneRect() {
        return new int[]{panelX() + PAD, panelY() + HEADER + numRows() * ROW_H + 10 + BTN_H + 8, PANEL_W - PAD * 2, BTN_H};
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x99000000);

        int px = panelX();
        int py = panelY();
        roundRect(ctx, px - 1, py - 1, PANEL_W + 2, panelH() + 2, 0xFF222A38);
        roundRect(ctx, px, py, PANEL_W, panelH(), PANEL_BG);
        ctx.fill(px, py, px + PANEL_W, py + 2, MINT);

        // header
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, CUBE, px + PAD, py + 11, 0F, 0F, 18, 18, 18, 18);
        Text title = Text.literal("Avni Client").setStyle(Style.EMPTY.withFont(new StyleSpriteSource.Font(FONT)));
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(px + PAD + 24, py + 13);
        m.scale(1.4f, 1.4f);
        ctx.drawText(textRenderer, title, 0, 0, TEXT, false);
        m.popMatrix();
        ctx.fill(px + PAD - 2, py + HEADER - 6, px + PANEL_W - PAD + 2, py + HEADER - 5, 0x18FFFFFF);

        // toggle rows (two columns)
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int[] t = toggleRect(i);
            ctx.drawText(textRenderer, row.label(), labelX(i), t[1] + (TOG_H - 8) / 2, MUTED, false);
            drawToggle(ctx, t, row.get().getAsBoolean(), contains(t, mouseX, mouseY));
        }

        drawButton(ctx, waypointsBtn(), "Waypoints", contains(waypointsBtn(), mouseX, mouseY), false);
        drawButton(ctx, editRect(), "Edit HUD Layout", contains(editRect(), mouseX, mouseY), false);
        drawButton(ctx, doneRect(), "Done", contains(doneRect(), mouseX, mouseY), true);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        for (int i = 0; i < rows.size(); i++) {
            if (contains(toggleRect(i), mx, my)) {
                Row row = rows.get(i);
                row.set().accept(!row.get().getAsBoolean());
                AvniConfig.get().save();
                return true;
            }
        }
        if (contains(waypointsBtn(), mx, my)) {
            client.setScreen(new WaypointsScreen(this));
            return true;
        }
        if (contains(editRect(), mx, my)) {
            client.setScreen(new HudEditorScreen(this));
            return true;
        }
        if (contains(doneRect(), mx, my)) {
            close();
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void close() {
        AvniConfig.get().save();
        client.setScreen(parent);
    }

    // ---- drawing helpers ----

    private void drawToggle(DrawContext ctx, int[] r, boolean on, boolean hover) {
        int x = r[0];
        int y = r[1];
        int w = r[2];
        int h = r[3];
        roundRect(ctx, x, y, w, h, on ? MINT : (hover ? 0x66FFFFFF : 0x44FFFFFF));
        int knob = h - 6;
        int kx = on ? x + w - knob - 3 : x + 3;
        roundRect(ctx, kx, y + 3, knob, knob, on ? 0xFF06210F : 0xFFE8ECF3);
    }

    private void drawButton(DrawContext ctx, int[] r, String label, boolean hover, boolean filled) {
        int x = r[0];
        int y = r[1];
        int w = r[2];
        int h = r[3];
        int textColor;
        if (filled) {
            roundRect(ctx, x, y, w, h, hover ? 0xFF45E8B4 : MINT);
            textColor = 0xFF042018;
        } else {
            roundRect(ctx, x, y, w, h, hover ? 0x332FE0A6 : 0x1E2FE0A6);
            border(ctx, x, y, w, h, MINT);
            textColor = MINT;
        }
        int tw = textRenderer.getWidth(label);
        ctx.drawText(textRenderer, label, x + (w - tw) / 2, y + (h - 8) / 2, textColor, false);
    }

    private static void roundRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x + 1, y, x + w - 1, y + h, color);
        ctx.fill(x, y + 1, x + w, y + h - 1, color);
    }

    private static void border(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x + 1, y, x + w - 1, y + 1, color);
        ctx.fill(x + 1, y + h - 1, x + w - 1, y + h, color);
        ctx.fill(x, y + 1, x + 1, y + h - 1, color);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private static boolean contains(int[] r, double mx, double my) {
        return mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3];
    }
}
