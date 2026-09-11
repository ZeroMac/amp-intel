package com.hl.platform.gateway.security;

import reactor.core.publisher.Mono;

public interface AuthSessionService {

    Mono<AuthSession> get(String userId, String sid);

}
