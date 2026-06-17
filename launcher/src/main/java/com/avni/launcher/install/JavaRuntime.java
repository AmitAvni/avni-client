package com.avni.launcher.install;

import com.avni.launcher.core.LauncherPaths;
import com.avni.launcher.util.Http;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Resolves the correct Java runtime for a given Minecraft version. Modern
 * versions (Java 21) reuse the JDK the launcher itself runs on; older versions
 * (Java 8/16/17) download Mojang's bundled runtime for linux x64.
 */
public final class JavaRuntime {
    private static final String ALL =
            "https://piston-meta.mojang.com/v1/products/java-runtime/"
            + "2ec0cc96c44e5a76b9c8b7c39df7210883d12871/all.json";

    private JavaRuntime() {
    }

    static Path resolve(JsonObject versionJson, Installer.Progress progress) throws Exception {
        int ours = Runtime.version().feature();
        String component = "java-runtime-delta";
        int major = ours;
        if (versionJson.has("javaVersion")) {
            JsonObject jv = versionJson.getAsJsonObject("javaVersion");
            component = jv.get("component").getAsString();
            major = jv.get("majorVersion").getAsInt();
        }
        // The launcher runs on JDK 21 — reuse it when that's what the game wants.
        if (major == ours) {
            return Path.of(System.getProperty("java.home"), "bin", "java");
        }

        Path dir = LauncherPaths.ROOT.resolve("runtimes").resolve(component);
        Path javaBin = dir.resolve("bin").resolve("java");
        if (Files.exists(javaBin)) {
            return javaBin;
        }

        progress.update(-1, "Downloading Java " + major + " runtime…");
        JsonObject all = JsonParser.parseString(Http.getString(ALL)).getAsJsonObject();
        JsonObject linux = all.getAsJsonObject("linux");
        if (linux == null || !linux.has(component) || linux.getAsJsonArray(component).isEmpty()) {
            throw new IOException("No Java runtime '" + component + "' available for linux");
        }
        String manifestUrl = linux.getAsJsonArray(component).get(0).getAsJsonObject()
                .getAsJsonObject("manifest").get("url").getAsString();
        JsonObject files = JsonParser.parseString(Http.getString(manifestUrl)).getAsJsonObject()
                .getAsJsonObject("files");

        for (Map.Entry<String, JsonElement> e : files.entrySet()) {
            Path out = dir.resolve(e.getKey());
            JsonObject f = e.getValue().getAsJsonObject();
            switch (f.get("type").getAsString()) {
                case "directory" -> Files.createDirectories(out);
                case "file" -> {
                    JsonObject raw = f.getAsJsonObject("downloads").getAsJsonObject("raw");
                    Http.download(raw.get("url").getAsString(), out, raw.get("sha1").getAsString());
                    if (f.has("executable") && f.get("executable").getAsBoolean()) {
                        out.toFile().setExecutable(true);
                    }
                }
                case "link" -> {
                    Files.createDirectories(out.getParent());
                    Files.deleteIfExists(out);
                    try {
                        Files.createSymbolicLink(out, Path.of(f.get("target").getAsString()));
                    } catch (Exception ignored) {
                        // some filesystems disallow symlinks; non-fatal
                    }
                }
                default -> {
                }
            }
        }
        javaBin.toFile().setExecutable(true);
        return javaBin;
    }
}
