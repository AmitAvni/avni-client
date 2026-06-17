package com.avni.launcher.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * A player account. For now only {@code offline} accounts are produced; the
 * Microsoft device-code flow will add {@code microsoft} accounts later with a
 * real access token.
 */
public record Account(String name, String uuid, String type, String accessToken, String refreshToken) {

    public static Account offline(String name) {
        return new Account(name, offlineUuid(name), "offline", "0", null);
    }

    public boolean isMicrosoft() {
        return "microsoft".equals(type);
    }

    /** Mirrors vanilla's offline UUID: a v3 UUID over "OfflinePlayer:&lt;name&gt;". */
    public static String offlineUuid(String name) {
        try {
            byte[] hash = MessageDigest.getInstance("MD5")
                    .digest(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
            hash[6] = (byte) ((hash[6] & 0x0f) | 0x30); // version 3
            hash[8] = (byte) ((hash[8] & 0x3f) | 0x80); // IETF variant
            long msb = 0;
            long lsb = 0;
            for (int i = 0; i < 8; i++) {
                msb = (msb << 8) | (hash[i] & 0xff);
            }
            for (int i = 8; i < 16; i++) {
                lsb = (lsb << 8) | (hash[i] & 0xff);
            }
            return new UUID(msb, lsb).toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String uuidNoDashes() {
        return uuid.replace("-", "");
    }
}
