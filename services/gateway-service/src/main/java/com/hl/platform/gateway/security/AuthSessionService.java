package com.hl.platform.gateway.security;

import java.time.Duration;

import reactor.core.publisher.Mono;

public interface AuthSessionService {

    Mono<AuthSession> get(String userId, String sid);

    Mono<Void> save(String userId, String sid, AuthSession session, Duration ttl);

    Mono<Boolean> delete(String userId, String sid);

    Mono<Boolean> invalidate(String userId, String sid);

    Mono<Boolean> updateTokenVersion(String userId, String sid, long newVersion);
}
