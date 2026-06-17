package com.avni.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple JSON-backed settings store. One singleton, persisted to
 * {@code config/avni-client.json}.
 */
public class AvniConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH =
            FabricLoader.getInstance().getConfigDir().resolve("avni-client.json");

    private static AvniConfig instance;

    // Master HUD switch + individual modules.
    public boolean hudEnabled = true;
    public boolean fps = true;
    public boolean coords = true;
    public boolean cps = true;
    public boolean keystrokes = true;
    public boolean watermark = true;
    public boolean direction = true;
    public boolean memory = true;
    public boolean day = true;
    public boolean clock = true;
    public boolean session = true;
    public boolean compass = true;
    public boolean waypoints = false;

    public boolean fullbright = false;

    // Zoom FOV is the saved FOV divided by this.
    public int zoomDivisor = 4;

    // Per-HUD-element layout: id -> [normalizedX, normalizedY, scale].
    public Map<String, float[]> hudLayout = new HashMap<>();

    /** Returns the stored [x, y, scale] for an element, seeding a default if absent. */
    public float[] layout(String id, float defaultX, float defaultY) {
        float[] v = hudLayout.get(id);
        if (v == null || v.length < 3) {
            v = new float[]{defaultX, defaultY, 1.0f};
            hudLayout.put(id, v);
        }
        return v;
    }

    public static AvniConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                try (Reader reader = Files.newBufferedReader(PATH)) {
                    instance = GSON.fromJson(reader, AvniConfig.class);
                }
            }
        } catch (Exception e) {
            AvniClient.LOGGER.warn("Failed to read config, using defaults", e);
        }
        if (instance == null) {
            instance = new AvniConfig();
        }
        if (instance.hudLayout == null) {
            instance.hudLayout = new HashMap<>();
        }
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(PATH)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            AvniClient.LOGGER.warn("Failed to write config", e);
        }
    }
}
