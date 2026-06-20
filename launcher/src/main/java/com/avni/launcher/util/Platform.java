package com.avni.launcher.util;

/** Operating-system detection shared by the installer and the launch pipeline. */
public final class Platform {
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static final String ARCH = System.getProperty("os.arch").toLowerCase();

    public static final boolean WINDOWS = OS.contains("win");
    public static final boolean MAC = OS.contains("mac") || OS.contains("darwin");

    private Platform() {
    }

    /** Mojang library/rule {@code os.name}: {@code windows} / {@code osx} / {@code linux}. */
    public static String mojangOsName() {
        if (WINDOWS) {
            return "windows";
        }
        if (MAC) {
            return "osx";
        }
        return "linux";
    }

    /** Mojang java-runtime platform key: {@code windows-x64} / {@code mac-os[-arm64]} / {@code linux}. */
    public static String runtimePlatform() {
        if (WINDOWS) {
            return "windows-x64";
        }
        if (MAC) {
            return ARCH.contains("aarch64") || ARCH.contains("arm") ? "mac-os-arm64" : "mac-os";
        }
        return "linux";
    }

    /** The {@code java} launcher executable name for this OS. */
    public static String javaExe() {
        return WINDOWS ? "java.exe" : "java";
    }
}
