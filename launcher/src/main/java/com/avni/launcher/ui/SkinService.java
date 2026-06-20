package com.avni.launcher.ui;

import com.avni.launcher.core.LauncherPaths;
import com.avni.launcher.util.Http;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves the 8×8 face (head front + hat overlay) from a premium player's
 * Minecraft skin, via Mojang's session server. Faces are cached in memory for
 * the session and the raw skin texture is cached on disk under
 * {@code ~/.avni-client/cache/skins}. On any failure callers fall back to the
 * coloured-initial avatar.
 */
public final class SkinService {
    private static final String PROFILE =
            "https://sessionserver.mojang.com/session/minecraft/profile/";

    private static final Map<String, Image> FACE_CACHE = new ConcurrentHashMap<>();

    private SkinService() {
    }

    /**
     * Returns the composed face image for {@code uuidNoDashes}. Blocking — call
     * off the JavaFX thread. Throws if the profile/skin can't be resolved.
     */
    public static Image faceFor(String uuidNoDashes) throws Exception {
        Image cached = FACE_CACHE.get(uuidNoDashes);
        if (cached != null) {
            return cached;
        }

        String skinUrl = skinUrl(uuidNoDashes);
        String texHash = skinUrl.substring(skinUrl.lastIndexOf('/') + 1);
        Path skinFile = LauncherPaths.ROOT.resolve("cache").resolve("skins").resolve(texHash + ".png");
        Http.download(skinUrl, skinFile, null); // skips if already present

        Image face = cropFace(new Image(skinFile.toUri().toString()));
        FACE_CACHE.put(uuidNoDashes, face);
        return face;
    }

    /** Queries the session server and decodes the base64 textures property. */
    private static String skinUrl(String uuid) throws Exception {
        JsonObject profile = JsonParser.parseString(Http.getString(PROFILE + uuid)).getAsJsonObject();
        JsonArray props = profile.getAsJsonArray("properties");
        for (var el : props) {
            JsonObject prop = el.getAsJsonObject();
            if ("textures".equals(prop.get("name").getAsString())) {
                String json = new String(Base64.getDecoder().decode(prop.get("value").getAsString()),
                        StandardCharsets.UTF_8);
                return JsonParser.parseString(json).getAsJsonObject()
                        .getAsJsonObject("textures")
                        .getAsJsonObject("SKIN")
                        .get("url").getAsString();
            }
        }
        throw new IllegalStateException("no textures property for " + uuid);
    }

    /** Composites the head (8,8) base and hat (40,8) overlay into an 8×8 image. */
    private static Image cropFace(Image skin) {
        PixelReader src = skin.getPixelReader();
        WritableImage face = new WritableImage(8, 8);
        PixelWriter out = face.getPixelWriter();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int base = src.getArgb(8 + x, 8 + y);
                int hat = src.getArgb(40 + x, 8 + y);
                out.setArgb(x, y, over(hat, base));
            }
        }
        return face;
    }

    /** Alpha-composites {@code top} over {@code bottom} (both 0xAARRGGBB). */
    private static int over(int top, int bottom) {
        int ta = (top >>> 24) & 0xff;
        if (ta == 0) {
            return bottom;
        }
        if (ta == 0xff) {
            return top;
        }
        int ba = (bottom >>> 24) & 0xff;
        float af = ta / 255f;
        float bf = ba / 255f * (1 - af);
        float outA = af + bf;
        int r = blend(top, bottom, 16, af, bf, outA);
        int g = blend(top, bottom, 8, af, bf, outA);
        int b = blend(top, bottom, 0, af, bf, outA);
        return ((int) (outA * 255) << 24) | (r << 16) | (g << 8) | b;
    }

    private static int blend(int top, int bottom, int shift, float af, float bf, float outA) {
        int t = (top >> shift) & 0xff;
        int b = (bottom >> shift) & 0xff;
        return outA <= 0 ? 0 : Math.min(255, Math.round((t * af + b * bf) / outA));
    }
}
