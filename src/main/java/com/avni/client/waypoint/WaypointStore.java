package com.avni.client.waypoint;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Persists waypoints to {@code config/avni-client-waypoints.json}. */
public final class WaypointStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<Waypoint>>() {
    }.getType();
    private static final Path PATH =
            FabricLoader.getInstance().getConfigDir().resolve("avni-client-waypoints.json");

    /** A palette to cycle through when creating waypoints. */
    public static final int[] PALETTE = {
            0xFFE2483B, 0xFFFF9E3D, 0xFFFFD23D, 0xFF2FE0A6,
            0xFF38C7FF, 0xFF6C8CFF, 0xFF8B7BFF, 0xFFFFFFFF};

    private static List<Waypoint> waypoints;

    private WaypointStore() {
    }

    public static List<Waypoint> all() {
        if (waypoints == null) {
            load();
        }
        return waypoints;
    }

    public static void load() {
        waypoints = null;
        try {
            if (Files.exists(PATH)) {
                try (Reader r = Files.newBufferedReader(PATH)) {
                    waypoints = GSON.fromJson(r, LIST_TYPE);
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        if (waypoints == null) {
            waypoints = new ArrayList<>();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer w = Files.newBufferedWriter(PATH)) {
                GSON.toJson(all(), LIST_TYPE, w);
            }
        } catch (Exception ignored) {
            // best effort
        }
    }

    public static void add(Waypoint wp) {
        all().add(wp);
        save();
    }

    public static void remove(Waypoint wp) {
        all().remove(wp);
        save();
    }
}
