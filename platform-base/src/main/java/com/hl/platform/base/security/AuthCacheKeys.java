package com.hl.platform.base.security;

import java.time.Duration;

public final class AuthCacheKeys {

    private static final String PREFIX = "auth:";

    private AuthCacheKeys() {
    }

    public static String session(String userId, String sid) {
        return PREFIX + component(userId, "userId") + ":session:" + component(sid, "sid");
    }

    public static String authority(String userId) {
        return PREFIX + component(userId, "userId") + ":authority";
    }

    public static String function(String userId) {
        return PREFIX + component(userId, "userId") + ":function";
    }

    public static Duration positiveTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Cache TTL must be positive");
        }
        return ttl;
    }

    private static String component(String value, String name) {
        if (value == null || value.isBlank() || value.indexOf(':') >= 0) {
            throw new IllegalArgumentException(name + " must be non-blank and must not contain ':'");
        }
        return value;
    }
}
