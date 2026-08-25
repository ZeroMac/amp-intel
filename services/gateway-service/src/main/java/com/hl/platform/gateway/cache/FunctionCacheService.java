package com.hl.platform.gateway.cache;

import java.time.Duration;
import java.util.List;

import com.hl.platform.gateway.cache.model.FunctionCacheItem;
import reactor.core.publisher.Mono;

public interface FunctionCacheService {

    Mono<List<FunctionCacheItem>> getFunctions(String userId);

    Mono<Void> saveFunctions(String userId, List<FunctionCacheItem> functions, Duration ttl);

    Mono<Boolean> deleteFunctions(String userId);
}
