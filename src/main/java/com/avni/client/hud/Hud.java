package com.avni.client.hud;

import com.avni.client.AvniConfig;
import com.avni.client.waypoint.Waypoint;
import com.avni.client.waypoint.WaypointStore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix3x2fStack;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Single source of truth for the movable/scalable HUD elements. Both the live
 * in-game HUD ({@link HudOverlay}) and the HUD editor render through here, so
 * what you arrange in the editor is exactly what you see while playing. Every
 * element is drawn inside a dark bordered panel (Lunar-style).
 */
public final class Hud {
    private static final int ACCENT = 0xFF2FE0A6;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int PANEL_BG = 0xA00B0E14;
    private static final int PANEL_BORDER = 0x40FFFFFF;

    /** Padding between an element's content and its panel border. */
    public static final int PAD = 3;

    private static final int KEY = 20;
    private static final int GAP = 2;
    public static final int KEYS_W = KEY * 3 + GAP * 2;
    public static final int KEYS_H = KEY * 4 + GAP * 3;

    private static final int COMPASS_W = 140;
    private static final int COMPASS_H = 16;
    private static final String[] CARD8 = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    private static final DateTimeFormatter CLOCK_FMT = DateTimeFormatter.ofPattern("h:mm a");
    private static final long SESSION_START = System.currentTimeMillis();

    public record Element(String id, String label, float defX, float defY) {
    }

    public static final List<Element> ELEMENTS = List.of(
            new Element("watermark", "Watermark", 0.006f, 0.010f),
            new Element("fps", "FPS counter", 0.006f, 0.050f),
            new Element("coords", "Coordinates", 0.006f, 0.090f),
            new Element("cps", "CPS counter", 0.006f, 0.280f),
            new Element("memory", "Memory", 0.006f, 0.330f),
            new Element("day", "Day counter", 0.006f, 0.380f),
            new Element("direction", "Direction", 0.006f, 0.430f),
            new Element("clock", "Clock", 0.900f, 0.020f),
            new Element("session", "Session timer", 0.900f, 0.060f),
            new Element("compass", "Compass", 0.420f, 0.020f),
            new Element("waypoints", "Waypoint list", 0.820f, 0.120f),
            new Element("keystrokes", "Keystrokes", 0.905f, 0.700f));

    private Hud() {
    }

    public static boolean enabled(AvniConfig c, String id) {
        return switch (id) {
            case "watermark" -> c.watermark;
            case "fps" -> c.fps;
            case "coords" -> c.coords;
            case "cps" -> c.cps;
            case "memory" -> c.memory;
            case "day" -> c.day;
            case "direction" -> c.direction;
            case "clock" -> c.clock;
            case "session" -> c.session;
            case "compass" -> c.compass;
            case "waypoints" -> c.waypoints;
            case "keystrokes" -> c.keystrokes;
            default -> false;
        };
    }

    public static int width(MinecraftClient mc, String id) {
        return switch (id) {
            case "keystrokes" -> KEYS_W;
            case "compass" -> COMPASS_W;
            case "coords" -> {
                int max = 0;
                for (String line : coordsLines(mc)) {
                    max = Math.max(max, mc.textRenderer.getWidth(line));
                }
                String f = facingLetter(mc);
                yield max + (f.isEmpty() ? 0 : mc.textRenderer.getWidth(f) + 8);
            }
            case "waypoints" -> {
                int max = 0;
                for (String line : waypointLines(mc)) {
                    max = Math.max(max, mc.textRenderer.getWidth(line));
                }
                yield max;
            }
            default -> mc.textRenderer.getWidth(text(mc, id));
        };
    }

    public static int height(MinecraftClient mc, String id) {
        return switch (id) {
            case "keystrokes" -> KEYS_H;
            case "compass" -> COMPASS_H;
            case "coords" -> coordsLines(mc).length * (mc.textRenderer.fontHeight + 1) - 1;
            case "waypoints" -> waypointLines(mc).length * (mc.textRenderer.fontHeight + 1) - 1;
            default -> mc.textRenderer.fontHeight;
        };
    }

    public static void renderAll(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        AvniConfig cfg = AvniConfig.get();
        if (!cfg.hudEnabled || mc.options.hudHidden) {
            return;
        }
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        for (Element e : ELEMENTS) {
            if (!enabled(cfg, e.id())) {
                continue;
            }
            float[] l = cfg.layout(e.id(), e.defX(), e.defY());
            renderAt(ctx, mc, e.id(), Math.round(l[0] * sw), Math.round(l[1] * sh), l[2]);
        }
    }

    public static void renderAt(DrawContext ctx, MinecraftClient mc, String id, int x, int y, float scale) {
        int w = width(mc, id);
        int h = height(mc, id);
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(scale, scale);

        // Lunar-style panel: dark background + subtle border
        ctx.fill(-PAD, -PAD, w + PAD, h + PAD, PANEL_BG);
        border(ctx, -PAD, -PAD, w + 2 * PAD, h + 2 * PAD, PANEL_BORDER);

        switch (id) {
            case "keystrokes" -> renderKeystrokes(ctx, mc);
            case "compass" -> renderCompass(ctx, mc);
            case "coords" -> renderCoords(ctx, mc, w);
            case "waypoints" -> renderWaypoints(ctx, mc);
            default -> ctx.drawText(mc.textRenderer, text(mc, id), 0, 0,
                    id.equals("watermark") ? ACCENT : WHITE, true);
        }
        m.popMatrix();
    }

    private static void renderCoords(DrawContext ctx, MinecraftClient mc, int w) {
        TextRenderer tr = mc.textRenderer;
        String[] lines = coordsLines(mc);
        int lh = tr.fontHeight + 1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith("Biome: ")) {
                ctx.drawText(tr, "Biome: ", 0, i * lh, WHITE, true);
                ctx.drawText(tr, lines[i].substring("Biome: ".length()),
                        tr.getWidth("Biome: "), i * lh, ACCENT, true);
            } else {
                ctx.drawText(tr, lines[i], 0, i * lh, WHITE, true);
            }
        }
        String f = facingLetter(mc);
        if (!f.isEmpty()) {
            int fy = (lines.length * lh - tr.fontHeight) / 2;
            ctx.drawText(tr, f, w - tr.getWidth(f), fy, ACCENT, true);
        }
    }

    private static String[] coordsLines(MinecraftClient mc) {
        if (mc.player == null) {
            return new String[]{"X: 0", "Y: 64", "Z: 0", "C: 0, 0", "Biome: --"};
        }
        BlockPos p = mc.player.getBlockPos();
        String biome = mc.world == null ? "--" : mc.world.getBiome(p).getKey()
                .map(k -> capitalize(k.getValue().getPath().replace('_', ' '))).orElse("--");
        return new String[]{
                "X: " + p.getX(),
                "Y: " + p.getY(),
                "Z: " + p.getZ(),
                "C: " + (p.getX() & 15) + ", " + (p.getZ() & 15),
                "Biome: " + biome};
    }

    private static void renderWaypoints(DrawContext ctx, MinecraftClient mc) {
        TextRenderer tr = mc.textRenderer;
        int lh = tr.fontHeight + 1;
        List<Waypoint> wps = WaypointStore.all();
        int i = 0;
        boolean any = false;
        for (Waypoint wp : wps) {
            if (!wp.visible) {
                continue;
            }
            any = true;
            ctx.drawText(tr, waypointLine(mc, wp), 0, i * lh, wp.color | 0xFF000000, true);
            i++;
        }
        if (!any) {
            ctx.drawText(tr, "No waypoints", 0, 0, 0xFF8A93A6, true);
        }
    }

    private static String[] waypointLines(MinecraftClient mc) {
        List<String> lines = new ArrayList<>();
        for (Waypoint wp : WaypointStore.all()) {
            if (wp.visible) {
                lines.add(waypointLine(mc, wp));
            }
        }
        if (lines.isEmpty()) {
            lines.add("No waypoints");
        }
        return lines.toArray(new String[0]);
    }

    private static String waypointLine(MinecraftClient mc, Waypoint wp) {
        if (mc.player == null) {
            return wp.name + "  --";
        }
        double dx = wp.x - mc.player.getX();
        double dz = wp.z - mc.player.getZ();
        int dist = (int) Math.sqrt(dx * dx + dz * dz);
        double bearing = ((Math.toDegrees(Math.atan2(dx, -dz))) % 360 + 360) % 360;
        String card = CARD8[(int) Math.round(bearing / 45.0) % 8];
        return wp.name + "  " + dist + "m  " + card;
    }

    private static String text(MinecraftClient mc, String id) {
        return switch (id) {
            case "watermark" -> "Avni Client";
            case "fps" -> mc.getCurrentFps() + " FPS";
            case "direction" -> mc.player != null ? "Facing: " + facing(mc.player.getYaw()) : "Facing: --";
            case "cps" -> "CPS: " + CpsTracker.LEFT.cps() + " L / " + CpsTracker.RIGHT.cps() + " R";
            case "memory" -> {
                Runtime rt = Runtime.getRuntime();
                long used = rt.totalMemory() - rt.freeMemory();
                yield "Mem: " + (used * 100 / rt.maxMemory()) + "%";
            }
            case "day" -> mc.world != null ? "Day " + (mc.world.getTimeOfDay() / 24000L) : "Day 0";
            case "clock" -> LocalTime.now().format(CLOCK_FMT);
            case "session" -> {
                long s = (System.currentTimeMillis() - SESSION_START) / 1000L;
                yield String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
            }
            default -> "";
        };
    }

    private static String facingLetter(MinecraftClient mc) {
        if (mc.player == null) {
            return "";
        }
        double y = ((mc.player.getYaw() % 360) + 360) % 360;
        return CARD8[(int) Math.round(y / 45.0) % 8];
    }

    private static String facing(float yaw) {
        double y = ((yaw % 360) + 360) % 360;
        String card = CARD8[(int) Math.round(y / 45.0) % 8];
        return switch (card) {
            case "N" -> "North (-Z)";
            case "E" -> "East (+X)";
            case "S" -> "South (+Z)";
            case "W" -> "West (-X)";
            case "NE" -> "North East";
            case "NW" -> "North West";
            case "SE" -> "South East";
            default -> "South West";
        };
    }

    private static void renderCompass(DrawContext ctx, MinecraftClient mc) {
        int center = COMPASS_W / 2;
        double bearing = mc.player == null ? 0
                : (((mc.player.getYaw() + 180.0) % 360) + 360) % 360;
        double pxPerDeg = COMPASS_W / 100.0;
        TextRenderer tr = mc.textRenderer;
        for (int deg = 0; deg < 360; deg += 15) {
            double off = wrap(deg - bearing);
            if (Math.abs(off) > 50) {
                continue;
            }
            int x = (int) Math.round(center + off * pxPerDeg);
            boolean major = deg % 45 == 0;
            ctx.fill(x, major ? 9 : 11, x + 1, COMPASS_H, 0xAAFFFFFF);
            if (major) {
                ctx.drawCenteredTextWithShadow(tr, CARD8[deg / 45], x, 0, deg == 0 ? ACCENT : WHITE);
            }
        }
        // waypoint markers on the strip
        if (mc.player != null) {
            for (Waypoint wp : WaypointStore.all()) {
                if (!wp.visible) {
                    continue;
                }
                double wb = ((Math.toDegrees(Math.atan2(wp.x - mc.player.getX(),
                        -(wp.z - mc.player.getZ())))) % 360 + 360) % 360;
                double off = wrap(wb - bearing);
                if (Math.abs(off) > 50) {
                    continue;
                }
                int x = (int) Math.round(center + off * pxPerDeg);
                ctx.fill(x - 1, 8, x + 2, COMPASS_H, wp.color | 0xFF000000);
            }
        }
        ctx.fill(center, 0, center + 1, COMPASS_H, ACCENT);
    }

    private static double wrap(double deg) {
        return (((deg + 180) % 360) + 360) % 360 - 180;
    }

    private static void renderKeystrokes(DrawContext ctx, MinecraftClient mc) {
        GameOptions o = mc.options;
        int half = (KEYS_W - GAP) / 2;
        key(ctx, mc, KEY + GAP, 0, KEY, KEY, "W", o.forwardKey.isPressed());
        int row1 = KEY + GAP;
        key(ctx, mc, 0, row1, KEY, KEY, "A", o.leftKey.isPressed());
        key(ctx, mc, KEY + GAP, row1, KEY, KEY, "S", o.backKey.isPressed());
        key(ctx, mc, (KEY + GAP) * 2, row1, KEY, KEY, "D", o.rightKey.isPressed());
        int row2 = row1 + KEY + GAP;
        key(ctx, mc, 0, row2, half, KEY, "LMB", o.attackKey.isPressed());
        key(ctx, mc, half + GAP, row2, KEYS_W - half - GAP, KEY, "RMB", o.useKey.isPressed());
        int row3 = row2 + KEY + GAP;
        key(ctx, mc, 0, row3, KEYS_W, KEY, "___", o.jumpKey.isPressed());
    }

    private static void key(DrawContext ctx, MinecraftClient mc, int x, int y, int w, int h,
                            String label, boolean pressed) {
        ctx.fill(x, y, x + w, y + h, pressed ? 0xC82FE0A6 : 0x80000000);
        TextRenderer tr = mc.textRenderer;
        int tw = tr.getWidth(label);
        ctx.drawText(tr, label, x + (w - tw) / 2, y + (h - tr.fontHeight) / 2 + 1,
                pressed ? 0xFF06210F : WHITE, true);
    }

    private static void border(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y + 1, x + 1, y + h - 1, color);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
