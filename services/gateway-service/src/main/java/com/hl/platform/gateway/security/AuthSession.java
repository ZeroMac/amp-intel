package com.hl.platform.gateway.security;

public record AuthSession(
        String userId,
        String userName,
        long tokenVersion,
        String status) {

    public static final String ACTIVE_STATUS = "ACTIVE";

    public AuthSession withTokenVersion(long newVersion) {
        return new AuthSession(userId, userName, newVersion, status);
    }
}
