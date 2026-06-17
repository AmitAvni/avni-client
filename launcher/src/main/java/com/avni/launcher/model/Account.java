package com.avni.launcher.model;

/**
 * A player account, authenticated with Microsoft.
 */
public record Account(String name, String uuid, String type, String accessToken, String refreshToken) {

    public boolean isMicrosoft() {
        return "microsoft".equals(type);
    }

    public String uuidNoDashes() {
        return uuid.replace("-", "");
    }
}
