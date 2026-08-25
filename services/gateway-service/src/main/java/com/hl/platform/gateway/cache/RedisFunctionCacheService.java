package com.hl.platform.gateway.cache;

import java.time.Duration;
import java.util.List;

import com.hl.platform.gateway.cache.model.FunctionCacheItem;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import reactor.core.publisher.Mono;

@Service
public class RedisFunctionCacheService implements FunctionCacheService {

    private static final TypeReference<List<FunctionCacheItem>> FUNCTION_LIST_TYPE = new TypeReference<>() {
    };

    private final ReactiveStringRedisTemplate redisTemplate;
    private final AuthCacheJsonCodec jsonCodec;

    public RedisFunctionCacheService(ReactiveStringRedisTemplate redisTemplate, AuthCacheJsonCodec jsonCodec) {
        this.redisTemplate = redisTemplate;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public Mono<List<FunctionCacheItem>> getFunctions(String userId) {
        String key = AuthCacheKeys.function(userId);
        return redisTemplate.opsForValue().get(key)
                .flatMap(json -> Mono.fromCallable(() -> List.copyOf(jsonCodec.read(json, FUNCTION_LIST_TYPE))))
                .defaultIfEmpty(List.of());
    }

    @Override
    public Mono<Void> saveFunctions(String userId, List<FunctionCacheItem> functions, Duration ttl) {
        String key = AuthCacheKeys.function(userId);
        Duration cacheTtl = AuthCacheKeys.positiveTtl(ttl);
        List<FunctionCacheItem> value = List.copyOf(functions);
        return Mono.fromCallable(() -> jsonCodec.write(value))
                .flatMap(json -> redisTemplate.opsForValue().set(key, json, cacheTtl))
                .then();
    }

    @Override
    public Mono<Boolean> deleteFunctions(String userId) {
        return redisTemplate.delete(AuthCacheKeys.function(userId))
                .map(deleted -> deleted > 0);
    }
}
