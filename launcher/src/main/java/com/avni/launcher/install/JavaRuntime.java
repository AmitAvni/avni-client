package com.avni.launcher.install;

import com.avni.launcher.core.LauncherPaths;
import com.avni.launcher.util.Http;
import com.avni.launcher.util.Platform;
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
        // The launcher runs on JDK 21 — reuse it when that's what the game wants,
        // but only if it exposes a real `java` launcher. A jpackage-bundled
        // runtime is stripped (boots via libjli, has no bin/java), so in the
        // packaged app we fall through and download Mojang's runtime instead.
        if (major == ours) {
            Path here = Path.of(System.getProperty("java.home"), "bin", Platform.javaExe());
            if (Files.exists(here)) {
                return here;
            }
        }

        Path dir = LauncherPaths.ROOT.resolve("runtimes").resolve(component);
        Path javaBin = dir.resolve("bin").resolve(Platform.javaExe());
        if (Files.exists(javaBin)) {
            return javaBin;
        }

        progress.update(-1, "Downloading Java " + major + " runtime…");
        String platformKey = Platform.runtimePlatform();
        JsonObject all = JsonParser.parseString(Http.getString(ALL)).getAsJsonObject();
        JsonObject platform = all.getAsJsonObject(platformKey);
        if (platform == null || !platform.has(component) || platform.getAsJsonArray(component).isEmpty()) {
            throw new IOException("No Java runtime '" + component + "' available for " + platformKey);
        }
        String manifestUrl = platform.getAsJsonArray(component).get(0).getAsJsonObject()
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
