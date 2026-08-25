package com.hl.platform.gateway.cache;

import java.time.Duration;
import java.util.List;

import reactor.core.publisher.Mono;

public interface AuthorityCacheService {

    Mono<List<String>> getAuthorities(String userId);

    Mono<Void> saveAuthorities(String userId, List<String> authorities, Duration ttl);

    Mono<Boolean> deleteAuthorities(String userId);
}
