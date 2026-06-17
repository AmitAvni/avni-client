package com.avni.launcher.model;

import com.avni.launcher.core.LauncherPaths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads/writes the in-game Avni mod config (~/.avni-client/config/avni-client.json).
 * Field names mirror the mod's {@code AvniConfig} so the toggles here take effect
 * in-game.
 */
public class ModSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean hudEnabled = true;
    public boolean fps = true;
    public boolean coords = true;
    public boolean cps = true;
    public boolean keystrokes = true;
    public boolean watermark = true;
    public boolean fullbright = false;
    public int zoomDivisor = 4;

    private static Path file() {
        return LauncherPaths.gameDir().resolve("config").resolve("avni-client.json");
    }

    public static ModSettings load() {
        try {
            if (Files.exists(file())) {
                try (Reader r = Files.newBufferedReader(file())) {
                    ModSettings m = GSON.fromJson(r, ModSettings.class);
                    if (m != null) {
                        return m;
                    }
                }
            }
        } catch (Exception ignored) {
            // defaults
        }
        return new ModSettings();
    }

    public void save() {
        try {
            Files.createDirectories(file().getParent());
            try (Writer w = Files.newBufferedWriter(file())) {
                GSON.toJson(this, w);
            }
        } catch (Exception ignored) {
            // best effort
        }
    }
}
