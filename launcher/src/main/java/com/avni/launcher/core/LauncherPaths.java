package com.avni.launcher.core;

import java.nio.file.Path;

/** Standard on-disk layout for the launcher, rooted at {@code ~/.avni-client}. */
public final class LauncherPaths {
    public static final Path ROOT = Path.of(System.getProperty("user.home"), ".avni-client");

    private LauncherPaths() {
    }

    public static Path versions() {
        return ROOT.resolve("versions");
    }

    public static Path libraries() {
        return ROOT.resolve("libraries");
    }

    public static Path assets() {
        return ROOT.resolve("assets");
    }

    public static Path natives() {
        return ROOT.resolve("natives");
    }

    public static Path mods() {
        return ROOT.resolve("mods");
    }

    /** Where the game stores saves, options, logs. */
    public static Path gameDir() {
        return ROOT;
    }

    public static Path configFile() {
        return ROOT.resolve("launcher.json");
    }
}
