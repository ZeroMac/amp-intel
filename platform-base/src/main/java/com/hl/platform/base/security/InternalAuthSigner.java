package com.hl.platform.base.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Shared gateway-to-service identity signature protocol. Never expose the secret to clients. */
public final class InternalAuthSigner {
    private final SecretKeySpec key;
    private final Clock clock;

    public InternalAuthSigner(String secret) {
        this(secret, Clock.systemUTC());
    }

    public InternalAuthSigner(String secret, Clock clock) {
        if (secret == null || secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("security.internal.secret must contain at least 32 bytes");
        }
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.clock = clock;
    }

    public String timestamp() {
        return Long.toString(clock.instant().getEpochSecond());
    }

    public String sign(String userId, String sid, String version, String timestamp) {
        validate(userId);
        validate(sid);
        Long.parseLong(version);
        Long.parseLong(timestamp);
        String payload = "v1\n" + userId + "\n" + sid + "\n" + version + "\n" + timestamp;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Internal authentication signing unavailable", exception);
        }
    }

    public boolean verify(String userId, String sid, String version, String timestamp, String signature) {
        try {
            long issuedAt = Long.parseLong(timestamp);
            long now = clock.instant().getEpochSecond();
            if (issuedAt < now - 60 || issuedAt > now + 5 || signature == null) {
                return false;
            }
            return MessageDigest.isEqual(sign(userId, sid, version, timestamp).getBytes(StandardCharsets.US_ASCII),
                    signature.getBytes(StandardCharsets.US_ASCII));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static void validate(String value) {
        if (value == null || value.isBlank() || value.indexOf(':') >= 0
                || value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid internal identity");
        }
    }
}
