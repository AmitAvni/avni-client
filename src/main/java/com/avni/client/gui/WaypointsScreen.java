package com.avni.client.gui;

import com.avni.client.waypoint.Waypoint;
import com.avni.client.waypoint.WaypointStore;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * Manage waypoints: type a name and add one at your current position, recolor,
 * toggle visibility, or delete. Themed to match the Avni config panel.
 */
public class WaypointsScreen extends Screen {
    private static final int PANEL_W = 340;
    private static final int HEADER = 44;
    private static final int ADD_ROW = 28;
    private static final int ROW_H = 22;
    private static final int FOOTER = 34;
    private static final int PAD = 16;

    private static final int MINT = 0xFF2FE0A6;
    private static final int TEXT = 0xFFEDF0F6;
    private static final int MUTED = 0xFFAAB2C0;

    private final Screen parent;
    private TextFieldWidget nameField;

    public WaypointsScreen(Screen parent) {
        super(Text.literal("Waypoints"));
        this.parent = parent;
    }

    private int rowsCount() {
        return Math.max(1, WaypointStore.all().size());
    }

    private int panelH() {
        return HEADER + ADD_ROW + rowsCount() * ROW_H + FOOTER;
    }

    private int panelX() {
        return (width - PANEL_W) / 2;
    }

    private int panelY() {
        return (height - panelH()) / 2;
    }

    private int listTop() {
        return panelY() + HEADER + ADD_ROW;
    }

    @Override
    protected void init() {
        nameField = new TextFieldWidget(textRenderer, panelX() + PAD, panelY() + HEADER, PANEL_W - PAD * 2 - 70, 18,
                Text.literal("Name"));
        nameField.setMaxLength(24);
        nameField.setPlaceholder(Text.literal("Waypoint name"));
        addDrawableChild(nameField);
    }

    private int[] addBtn() {
        return new int[]{panelX() + PANEL_W - PAD - 62, panelY() + HEADER, 62, 18};
    }

    private int[] doneBtn() {
        return new int[]{panelX() + PAD, panelY() + panelH() - FOOTER + 8, PANEL_W - PAD * 2, 20};
    }

    private int[] rowEye(int i) {
        return new int[]{panelX() + PANEL_W - PAD - 40, listTop() + i * ROW_H + 3, 16, 16};
    }

    private int[] rowDelete(int i) {
        return new int[]{panelX() + PANEL_W - PAD - 18, listTop() + i * ROW_H + 3, 16, 16};
    }

    private int[] rowSwatch(int i) {
        return new int[]{panelX() + PAD, listTop() + i * ROW_H + 5, 12, 12};
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x99000000);
        int px = panelX();
        int py = panelY();
        roundRect(ctx, px - 1, py - 1, PANEL_W + 2, panelH() + 2, 0xFF222A38);
        roundRect(ctx, px, py, PANEL_W, panelH(), 0xF013171F);
        ctx.fill(px, py, px + PANEL_W, py + 2, MINT);

        ctx.drawText(textRenderer, Text.literal("Waypoints"), px + PAD, py + 16, TEXT, false);

        super.render(ctx, mouseX, mouseY, delta); // draws the name field
        drawButton(ctx, addBtn(), "Add here", contains(addBtn(), mouseX, mouseY));

        // list
        var list = WaypointStore.all();
        if (list.isEmpty()) {
            ctx.drawText(textRenderer, Text.literal("No waypoints yet — type a name and Add here."),
                    px + PAD, listTop() + 7, MUTED, false);
        }
        for (int i = 0; i < list.size(); i++) {
            Waypoint wp = list.get(i);
            int ry = listTop() + i * ROW_H;
            int[] sw = rowSwatch(i);
            roundRect(ctx, sw[0], sw[1], sw[2], sw[3], wp.color | 0xFF000000);
            ctx.drawText(textRenderer, wp.name, px + PAD + 18, ry + 7, wp.visible ? TEXT : MUTED, false);
            String coords = wp.x + ", " + wp.y + ", " + wp.z;
            ctx.drawText(textRenderer, coords, px + PAD + 18 + 96, ry + 7, MUTED, false);
            drawIcon(ctx, rowEye(i), wp.visible ? "On" : "Off", contains(rowEye(i), mouseX, mouseY), wp.visible);
            drawIcon(ctx, rowDelete(i), "X", contains(rowDelete(i), mouseX, mouseY), false);
        }

        drawButton(ctx, doneBtn(), "Done", contains(doneBtn(), mouseX, mouseY));
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        double mx = click.x();
        double my = click.y();

        if (contains(addBtn(), mx, my)) {
            addHere();
            return true;
        }
        var list = WaypointStore.all();
        for (int i = 0; i < list.size(); i++) {
            Waypoint wp = list.get(i);
            if (contains(rowSwatch(i), mx, my)) {
                wp.color = nextColor(wp.color);
                WaypointStore.save();
                return true;
            }
            if (contains(rowEye(i), mx, my)) {
                wp.visible = !wp.visible;
                WaypointStore.save();
                return true;
            }
            if (contains(rowDelete(i), mx, my)) {
                WaypointStore.remove(wp);
                clearAndInit();
                return true;
            }
        }
        if (contains(doneBtn(), mx, my)) {
            close();
            return true;
        }
        return false;
    }

    private void addHere() {
        var list = WaypointStore.all();
        String name = nameField.getText().isBlank() ? "Waypoint " + (list.size() + 1) : nameField.getText().trim();
        BlockPos p = client.player != null ? client.player.getBlockPos() : BlockPos.ORIGIN;
        int color = WaypointStore.PALETTE[list.size() % WaypointStore.PALETTE.length];
        WaypointStore.add(new Waypoint(name, p.getX(), p.getY(), p.getZ(), color));
        nameField.setText("");
        clearAndInit();
    }

    private static int nextColor(int current) {
        int[] p = WaypointStore.PALETTE;
        for (int i = 0; i < p.length; i++) {
            if ((p[i] | 0xFF000000) == (current | 0xFF000000)) {
                return p[(i + 1) % p.length];
            }
        }
        return p[0];
    }

    @Override
    public void close() {
        WaypointStore.save();
        client.setScreen(parent);
    }

    // ---- drawing helpers ----

    private void drawButton(DrawContext ctx, int[] r, String label, boolean hover) {
        roundRect(ctx, r[0], r[1], r[2], r[3], hover ? 0xFF45E8B4 : MINT);
        int tw = textRenderer.getWidth(label);
        ctx.drawText(textRenderer, label, r[0] + (r[2] - tw) / 2, r[1] + (r[3] - 8) / 2, 0xFF042018, false);
    }

    private void drawIcon(DrawContext ctx, int[] r, String label, boolean hover, boolean active) {
        roundRect(ctx, r[0], r[1], r[2], r[3], hover ? 0x33FFFFFF : 0x22FFFFFF);
        int tw = textRenderer.getWidth(label);
        ctx.drawText(textRenderer, label, r[0] + (r[2] - tw) / 2, r[1] + (r[3] - 8) / 2,
                active ? MINT : MUTED, false);
    }

    private static void roundRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x + 1, y, x + w - 1, y + h, color);
        ctx.fill(x, y + 1, x + w, y + h - 1, color);
    }

    private static boolean contains(int[] r, double mx, double my) {
        return mx >= r[0] && mx <= r[0] + r[2] && my >= r[1] && my <= r[1] + r[3];
    }
}
