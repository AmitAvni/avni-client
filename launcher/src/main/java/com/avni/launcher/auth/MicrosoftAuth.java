package com.avni.launcher.auth;

import com.avni.launcher.model.Account;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Microsoft sign-in via the OAuth2 <b>device-code</b> flow, then the full
 * Xbox Live → XSTS → Minecraft token chain. Device code needs only an Azure
 * "public client" application id — no redirect URI or secret.
 */
public final class MicrosoftAuth {
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30)).build();

    private static final String DEVICE_CODE_URL =
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode";
    private static final String TOKEN_URL =
            "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String SCOPE = "XboxLive.signin offline_access";
    // Xbox/Minecraft endpoints (Cloudflare/Akamai) 403 the default Java user agent.
    private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AvniClient/1.0";

    /**
     * The Azure "public client" application id, registered ONCE by whoever ships
     * Avni Client. Every player then just signs in — they never touch Azure.
     * Register a free app at portal.azure.com (Personal Microsoft accounts, no
     * redirect URI, "Allow public client flows" = Yes) and paste its
     * Application (client) ID here.
     */
    public static final String CLIENT_ID = "4c39c548-749a-4604-8d1b-6f62eaa8d0b6";

    private MicrosoftAuth() {
    }

    /** True once a real client id is present (baked-in constant or a config override). */
    public static boolean isConfigured(String clientId) {
        return clientId != null && !clientId.isBlank() && !clientId.equals("PASTE_YOUR_AZURE_CLIENT_ID_HERE");
    }

    /** Resolves the effective client id: a config override wins, else the baked-in one. */
    public static String clientId(String override) {
        return override != null && !override.isBlank() ? override : CLIENT_ID;
    }

    public record DeviceCode(String deviceCode, String userCode, String verificationUri,
                             int interval, int expiresIn) {
    }

    public static DeviceCode requestDeviceCode(String clientId) throws Exception {
        JsonObject j = postForm(DEVICE_CODE_URL, "client_id=" + enc(clientId) + "&scope=" + enc(SCOPE));
        return new DeviceCode(
                j.get("device_code").getAsString(),
                j.get("user_code").getAsString(),
                j.get("verification_uri").getAsString(),
                j.has("interval") ? j.get("interval").getAsInt() : 5,
                j.has("expires_in") ? j.get("expires_in").getAsInt() : 900);
    }

    /** Blocks (polling) until the user approves in the browser; returns the MS token JSON. */
    public static JsonObject pollForToken(String clientId, DeviceCode dc) throws Exception {
        String body = "grant_type=urn:ietf:params:oauth:grant-type:device_code"
                + "&client_id=" + enc(clientId) + "&device_code=" + enc(dc.deviceCode());
        long deadline = System.currentTimeMillis() + dc.expiresIn() * 1000L;
        int interval = Math.max(1, dc.interval());
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(interval * 1000L);
            HttpResponse<String> resp = postFormRaw(TOKEN_URL, body);
            JsonObject j = JsonParser.parseString(resp.body()).getAsJsonObject();
            if (resp.statusCode() / 100 == 2) {
                return j;
            }
            String err = j.has("error") ? j.get("error").getAsString() : "";
            switch (err) {
                case "authorization_pending" -> {
                }
                case "slow_down" -> interval += 2;
                case "authorization_declined" -> throw new RuntimeException("Sign-in was declined");
                case "expired_token" -> throw new RuntimeException("The code expired — try again");
                default -> throw new RuntimeException("Sign-in failed: " + err);
            }
        }
        throw new RuntimeException("Sign-in timed out");
    }

    public static JsonObject refreshToken(String clientId, String refreshToken) throws Exception {
        return postForm(TOKEN_URL, "grant_type=refresh_token&client_id=" + enc(clientId)
                + "&refresh_token=" + enc(refreshToken) + "&scope=" + enc(SCOPE));
    }

    /** MS token → Xbox → XSTS → Minecraft → profile → a ready-to-launch Account. */
    public static Account toMinecraftAccount(JsonObject msToken) throws Exception {
        String msAccess = msToken.get("access_token").getAsString();
        String refresh = msToken.has("refresh_token") ? msToken.get("refresh_token").getAsString() : null;

        JsonObject xbl = postJson("https://user.auth.xboxlive.com/user/authenticate",
                "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\","
                        + "\"RpsTicket\":\"d=" + msAccess + "\"},"
                        + "\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}");
        String xblToken = xbl.get("Token").getAsString();
        String uhs = xbl.getAsJsonObject("DisplayClaims").getAsJsonArray("xui")
                .get(0).getAsJsonObject().get("uhs").getAsString();

        JsonObject xsts = postJson("https://xsts.auth.xboxlive.com/xsts/authorize",
                "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\"" + xblToken + "\"]},"
                        + "\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}");
        String xstsToken = xsts.get("Token").getAsString();

        JsonObject mc = postJson("https://api.minecraftservices.com/authentication/login_with_xbox",
                "{\"identityToken\":\"XBL3.0 x=" + uhs + ";" + xstsToken + "\"}");
        String mcToken = mc.get("access_token").getAsString();

        HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                .header("Authorization", "Bearer " + mcToken)
                .header("User-Agent", USER_AGENT).GET().build();
        HttpResponse<String> presp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (presp.statusCode() == 404) {
            throw new RuntimeException("This account doesn't own Minecraft");
        }
        if (presp.statusCode() / 100 != 2) {
            throw new RuntimeException("Couldn't fetch Minecraft profile (HTTP " + presp.statusCode() + ")");
        }
        JsonObject profile = JsonParser.parseString(presp.body()).getAsJsonObject();
        return new Account(profile.get("name").getAsString(),
                dashify(profile.get("id").getAsString()), "microsoft", mcToken, refresh);
    }

    // ---- HTTP helpers ----

    private static JsonObject postForm(String url, String body) throws Exception {
        HttpResponse<String> r = postFormRaw(url, body);
        if (r.statusCode() / 100 != 2) {
            throw new RuntimeException("HTTP " + r.statusCode() + ": " + r.body());
        }
        return JsonParser.parseString(r.body()).getAsJsonObject();
    }

    private static HttpResponse<String> postFormRaw(String url, String body) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", USER_AGENT)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return HTTP.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private static JsonObject postJson(String url, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> r = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() / 100 != 2) {
            if (r.statusCode() == 401 && url.contains("xsts")) {
                try {
                    long xerr = JsonParser.parseString(r.body()).getAsJsonObject().get("XErr").getAsLong();
                    if (xerr == 2148916233L) {
                        throw new RuntimeException("This Microsoft account has no Xbox profile — make one at xbox.com first");
                    }
                    if (xerr == 2148916238L) {
                        throw new RuntimeException("Child account — must be added to a Family by an adult");
                    }
                } catch (NumberFormatException | NullPointerException | IllegalStateException ignored) {
                    // fall through
                }
            }
            if (url.contains("minecraftservices") && r.body().contains("Invalid app registration")) {
                throw new RuntimeException("This launcher's app ID isn't approved by Mojang yet. "
                        + "New app IDs must be allow-listed at aka.ms/mce-reviewappid. "
                        + "Use an offline account until it's approved.");
            }
            throw new RuntimeException("Auth step failed (HTTP " + r.statusCode() + ")");
        }
        return JsonParser.parseString(r.body()).getAsJsonObject();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String dashify(String id) {
        if (id.length() != 32) {
            return id;
        }
        return id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16)
                + "-" + id.substring(16, 20) + "-" + id.substring(20);
    }
}
