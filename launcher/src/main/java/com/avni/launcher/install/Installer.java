package com.avni.launcher.install;

import com.avni.launcher.core.LauncherPaths;
import com.avni.launcher.util.Http;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Downloads a vanilla Minecraft version (client jar, libraries, natives, assets)
 * from Mojang, installs the Fabric loader from meta.fabricmc.net, and drops the
 * bundled Avni mod into the mods folder. Produces a {@link LaunchSpec}.
 */
public class Installer {

    public interface Progress {
        /** @param fraction 0..1, or negative for indeterminate. */
        void update(double fraction, String message);
    }

    private static final String VERSION_MANIFEST =
            "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";
    private static final String RESOURCES = "https://resources.download.minecraft.net/";
    private static final String FABRIC_META = "https://meta.fabricmc.net/v2/versions/loader/";
    private static final String MODRINTH_FABRIC_API =
            "https://api.modrinth.com/v2/project/fabric-api/version";

    /** The Minecraft version the bundled Avni mod is built for. */
    public static final String AVNI_MC_VERSION = "1.21.11";

    private final String mcVersion;
    private final Progress progress;

    public Installer(String mcVersion, Progress progress) {
        this.mcVersion = mcVersion;
        this.progress = progress;
    }

    public LaunchSpec install() throws Exception {
        progress.update(-1, "Fetching version manifest…");
        JsonObject manifest = json(Http.getString(VERSION_MANIFEST));
        String versionUrl = null;
        String versionSha = null;
        for (JsonElement e : manifest.getAsJsonArray("versions")) {
            JsonObject v = e.getAsJsonObject();
            if (v.get("id").getAsString().equals(mcVersion)) {
                versionUrl = v.get("url").getAsString();
                versionSha = v.has("sha1") ? v.get("sha1").getAsString() : null;
                break;
            }
        }
        if (versionUrl == null) {
            throw new IOException("Minecraft version not found in manifest: " + mcVersion);
        }

        Path verDir = LauncherPaths.versions().resolve(mcVersion);
        Path verJson = verDir.resolve(mcVersion + ".json");
        Http.download(versionUrl, verJson, versionSha);
        JsonObject version = json(Files.readString(verJson));

        // ---- java runtime (download an older JRE if this version needs one) ----
        Path javaBin = JavaRuntime.resolve(version, progress);

        // ---- client jar ----
        progress.update(0.05, "Downloading client…");
        JsonObject clientDl = version.getAsJsonObject("downloads").getAsJsonObject("client");
        Path clientJar = verDir.resolve(mcVersion + ".jar");
        Http.download(clientDl.get("url").getAsString(), clientJar, clientDl.get("sha1").getAsString());

        // ---- libraries + natives ----
        List<Path> classpath = new ArrayList<>();
        Path nativesDir = LauncherPaths.natives().resolve(mcVersion);
        Files.createDirectories(nativesDir);

        List<JsonObject> libs = new ArrayList<>();
        version.getAsJsonArray("libraries").forEach(l -> libs.add(l.getAsJsonObject()));
        for (int i = 0; i < libs.size(); i++) {
            JsonObject lib = libs.get(i);
            progress.update(0.1 + 0.15 * (i + 1) / libs.size(),
                    "Libraries " + (i + 1) + "/" + libs.size());
            if (!rulesAllow(lib.has("rules") ? lib.getAsJsonArray("rules") : null)) {
                continue;
            }
            JsonObject downloads = lib.has("downloads") ? lib.getAsJsonObject("downloads") : null;
            if (downloads != null && downloads.has("artifact")) {
                JsonObject art = downloads.getAsJsonObject("artifact");
                Path dest = LauncherPaths.libraries().resolve(art.get("path").getAsString());
                Http.download(art.get("url").getAsString(), dest, art.get("sha1").getAsString());
                classpath.add(dest);
                if (lib.get("name").getAsString().contains(":natives-")) {
                    extractNatives(dest, nativesDir);
                }
            }
            // legacy-style natives (classifiers + "natives" map)
            if (downloads != null && downloads.has("classifiers") && lib.has("natives")) {
                JsonObject natives = lib.getAsJsonObject("natives");
                if (natives.has("linux")) {
                    String key = natives.get("linux").getAsString();
                    JsonObject classifiers = downloads.getAsJsonObject("classifiers");
                    if (classifiers.has(key)) {
                        JsonObject art = classifiers.getAsJsonObject(key);
                        Path dest = LauncherPaths.libraries().resolve(art.get("path").getAsString());
                        Http.download(art.get("url").getAsString(), dest, art.get("sha1").getAsString());
                        extractNatives(dest, nativesDir);
                    }
                }
            }
        }
        classpath.add(clientJar);

        // ---- assets ----
        JsonObject assetIndex = version.getAsJsonObject("assetIndex");
        String assetIndexId = assetIndex.get("id").getAsString();
        Path indexFile = LauncherPaths.assets().resolve("indexes").resolve(assetIndexId + ".json");
        Http.download(assetIndex.get("url").getAsString(), indexFile, assetIndex.get("sha1").getAsString());
        downloadAssets(json(Files.readString(indexFile)));

        // ---- fabric loader ----
        progress.update(0.9, "Installing Fabric loader…");
        String fabricMain = installFabric(classpath);

        // ---- mods: start clean, add Fabric API, and the Avni mod where supported ----
        cleanMods();
        progress.update(0.95, "Downloading Fabric API…");
        installFabricApi();
        if (mcVersion.equals(AVNI_MC_VERSION)) {
            progress.update(0.97, "Adding Avni mod…");
            installMod();
        }

        progress.update(1.0, "Install complete");
        return new LaunchSpec(mcVersion, classpath, fabricMain, nativesDir,
                LauncherPaths.assets(), assetIndexId, LauncherPaths.gameDir(), javaBin);
    }

    private void cleanMods() throws IOException {
        Path mods = LauncherPaths.mods();
        if (!Files.isDirectory(mods)) {
            Files.createDirectories(mods);
            return;
        }
        try (var stream = Files.list(mods)) {
            for (Path p : stream.filter(x -> x.toString().endsWith(".jar")).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }

    private void downloadAssets(JsonObject index) throws Exception {
        JsonObject objects = index.getAsJsonObject("objects");
        int total = objects.size();
        ExecutorService pool = Executors.newFixedThreadPool(16);
        AtomicInteger done = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (Map.Entry<String, JsonElement> e : objects.entrySet()) {
                String hash = e.getValue().getAsJsonObject().get("hash").getAsString();
                String sub = hash.substring(0, 2);
                Path dest = LauncherPaths.assets().resolve("objects").resolve(sub).resolve(hash);
                futures.add(pool.submit(() -> {
                    Http.download(RESOURCES + sub + "/" + hash, dest, hash);
                    int d = done.incrementAndGet();
                    if (d % 40 == 0 || d == total) {
                        progress.update(0.4 + 0.45 * d / total, "Assets " + d + "/" + total);
                    }
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                f.get(); // propagate any download failure
            }
        } finally {
            pool.shutdown();
        }
    }

    /** Installs Fabric libraries into the classpath and returns its main class. */
    private String installFabric(List<Path> classpath) throws Exception {
        JsonArray loaders = JsonParser.parseString(Http.getString(FABRIC_META + mcVersion)).getAsJsonArray();
        if (loaders.isEmpty()) {
            throw new IOException("No Fabric loader available for " + mcVersion);
        }
        String loaderVersion = null;
        for (JsonElement e : loaders) {
            JsonObject loader = e.getAsJsonObject().getAsJsonObject("loader");
            if (loader.get("stable").getAsBoolean()) {
                loaderVersion = loader.get("version").getAsString();
                break;
            }
        }
        if (loaderVersion == null) {
            loaderVersion = loaders.get(0).getAsJsonObject().getAsJsonObject("loader").get("version").getAsString();
        }

        String profileUrl = FABRIC_META + mcVersion + "/" + loaderVersion + "/profile/json";
        JsonObject profile = json(Http.getString(profileUrl));
        for (JsonElement e : profile.getAsJsonArray("libraries")) {
            JsonObject lib = e.getAsJsonObject();
            String name = lib.get("name").getAsString();
            String base = lib.has("url") ? lib.get("url").getAsString() : "https://maven.fabricmc.net/";
            String path = mavenToPath(name);
            Path dest = LauncherPaths.libraries().resolve(path);
            String sha1 = lib.has("sha1") ? lib.get("sha1").getAsString() : null;
            Http.download(stripSlash(base) + "/" + path, dest, sha1);
            classpath.add(dest);
        }
        return profile.get("mainClass").getAsString();
    }

    /** Resolves the latest Fabric API build for this Minecraft version via Modrinth. */
    private void installFabricApi() throws IOException, InterruptedException {
        String gameVersions = URLEncoder.encode("[\"" + mcVersion + "\"]", StandardCharsets.UTF_8);
        String loaders = URLEncoder.encode("[\"fabric\"]", StandardCharsets.UTF_8);
        String url = MODRINTH_FABRIC_API + "?game_versions=" + gameVersions + "&loaders=" + loaders;

        JsonArray versions = JsonParser.parseString(Http.getString(url)).getAsJsonArray();
        if (versions.isEmpty()) {
            return; // no Fabric API for this version — launch without it
        }
        JsonArray files = versions.get(0).getAsJsonObject().getAsJsonArray("files");
        String fileUrl = null;
        for (JsonElement fe : files) {
            JsonObject f = fe.getAsJsonObject();
            if (f.get("primary").getAsBoolean()) {
                fileUrl = f.get("url").getAsString();
                break;
            }
        }
        if (fileUrl == null && !files.isEmpty()) {
            fileUrl = files.get(0).getAsJsonObject().get("url").getAsString();
        }
        if (fileUrl != null) {
            Files.createDirectories(LauncherPaths.mods());
            Http.download(fileUrl, LauncherPaths.mods().resolve("fabric-api.jar"), null);
        }
    }

    private void installMod() throws IOException {
        Files.createDirectories(LauncherPaths.mods());
        Path dest = LauncherPaths.mods().resolve("avni-client.jar");

        // Preferred: bundled on the classpath (works for packaged app and `gradle run`).
        try (InputStream in = getClass().getResourceAsStream("/avni-client.jar")) {
            if (in != null) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                return;
            }
        }
        // Fallback: look in known build outputs next to the project.
        Path cwd = Path.of("").toAbsolutePath();
        for (Path candidate : List.of(
                cwd.resolve("build/bundled-mods/avni-client.jar"),
                cwd.resolve("launcher/build/bundled-mods/avni-client.jar"),
                cwd.resolve("build/libs/avni-client-1.0.0.jar"),
                cwd.getParent() != null ? cwd.getParent().resolve("build/libs/avni-client-1.0.0.jar") : cwd)) {
            if (Files.exists(candidate)) {
                Files.copy(candidate, dest, StandardCopyOption.REPLACE_EXISTING);
                return;
            }
        }
        throw new IOException("Could not locate the bundled Avni mod jar");
    }

    // ---- helpers ----

    private static void extractNatives(Path jar, Path dir) throws IOException {
        try (ZipFile zf = new ZipFile(jar.toFile())) {
            var entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry ze = entries.nextElement();
                if (ze.isDirectory() || ze.getName().startsWith("META-INF")) {
                    continue;
                }
                String n = ze.getName();
                if (n.endsWith(".so") || n.endsWith(".dll") || n.endsWith(".dylib")) {
                    Path out = dir.resolve(Path.of(n).getFileName().toString());
                    try (InputStream in = zf.getInputStream(ze)) {
                        Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    private static boolean rulesAllow(JsonArray rules) {
        if (rules == null) {
            return true;
        }
        boolean allowed = false;
        for (JsonElement re : rules) {
            JsonObject r = re.getAsJsonObject();
            boolean matches = true;
            if (r.has("os") && r.getAsJsonObject("os").has("name")) {
                matches = r.getAsJsonObject("os").get("name").getAsString().equals("linux");
            }
            if (matches) {
                allowed = r.get("action").getAsString().equals("allow");
            }
        }
        return allowed;
    }

    static String mavenToPath(String name) {
        String[] p = name.split(":");
        String group = p[0].replace('.', '/');
        String artifact = p[1];
        String version = p[2];
        String classifier = p.length > 3 ? "-" + p[3] : "";
        return group + "/" + artifact + "/" + version + "/" + artifact + "-" + version + classifier + ".jar";
    }

    private static String stripSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    private static JsonObject json(String s) {
        return JsonParser.parseString(s).getAsJsonObject();
    }
}
