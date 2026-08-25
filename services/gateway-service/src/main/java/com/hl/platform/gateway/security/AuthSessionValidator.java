package com.hl.platform.gateway.security;

import com.hl.platform.gateway.cache.AuthCacheSerializationException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AuthSessionValidator {

    private final AuthSessionService authSessionService;

    public AuthSessionValidator(AuthSessionService authSessionService) {
        this.authSessionService = authSessionService;
    }

    public Mono<Void> validate(Jwt jwt) {
        return Mono.defer(() -> {
            SessionClaims claims = sessionClaims(jwt);
            return authSessionService.get(claims.userId(), claims.sid())
                    .switchIfEmpty(invalidSession())
                    .flatMap(session -> isValid(session, claims)
                            ? Mono.<Void>empty()
                            : invalidSession());
        })
                .onErrorMap(AuthCacheSerializationException.class,
                        exception -> new BadCredentialsException("Invalid authentication session"))
                .onErrorMap(exception -> !(exception instanceof AuthenticationException),
                        exception -> new AuthenticationServiceException("Authentication session store unavailable", exception));
    }

    private static SessionClaims sessionClaims(Jwt jwt) {
        String userId = jwt.getSubject();
        Object sidClaim = jwt.getClaim("sid");
        Object versionClaim = jwt.getClaim("ver");

        if (userId == null || userId.isBlank() || userId.indexOf(':') >= 0
                || !(sidClaim instanceof String sid) || sid.isBlank() || sid.indexOf(':') >= 0
                || !(versionClaim instanceof Number version)
                || version.doubleValue() != version.longValue()) {
            throw new BadCredentialsException("JWT session claims are invalid");
        }
        return new SessionClaims(userId, sid, version.longValue());
    }

    private static boolean isValid(AuthSession session, SessionClaims claims) {
        return claims.userId().equals(session.userId())
                && claims.tokenVersion() == session.tokenVersion()
                && AuthSession.ACTIVE_STATUS.equals(session.status());
    }

    private static <T> Mono<T> invalidSession() {
        return Mono.error(new BadCredentialsException("Invalid authentication session"));
    }

    private record SessionClaims(String userId, String sid, long tokenVersion) {
    }
}
