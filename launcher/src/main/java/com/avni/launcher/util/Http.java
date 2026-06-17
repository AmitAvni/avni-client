package com.avni.launcher.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

/** Tiny HTTP helper: fetch strings and download files with sha1 verification. */
public final class Http {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private Http() {
    }

    private static final String USER_AGENT = "AvniClientLauncher/1.0";

    public static String getString(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT).GET().build();
        HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + resp.statusCode() + " for " + url);
        }
        return resp.body();
    }

    /** Downloads {@code url} to {@code dest}. Skips if the file already matches {@code sha1}. */
    public static void download(String url, Path dest, String sha1) throws IOException, InterruptedException {
        if (Files.exists(dest)) {
            if (sha1 == null || sha1.equalsIgnoreCase(sha1(dest))) {
                return;
            }
        }
        Files.createDirectories(dest.getParent());
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT).GET().build();
        HttpResponse<InputStream> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() / 100 != 2) {
            throw new IOException("HTTP " + resp.statusCode() + " for " + url);
        }
        Path tmp = dest.resolveSibling(dest.getFileName() + ".part");
        try (InputStream in = resp.body()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    public static String sha1(Path file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    md.update(buf, 0, n);
                }
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            throw new IOException("sha1 failed for " + file, e);
        }
    }
}
