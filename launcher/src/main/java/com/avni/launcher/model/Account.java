package com.avni.launcher.model;

/**
 * A player account, produced by the Microsoft device-code sign-in flow with a
 * real access token.
 */
public record Account(String name, String uuid, String type, String accessToken, String refreshToken) {

    public boolean isMicrosoft() {
        return "microsoft".equals(type);
    }

    public String uuidNoDashes() {
        return uuid.replace("-", "");
    }
}
