package com.hl.platform.gateway.cache;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import reactor.core.publisher.Mono;

@Service
public class RedisAuthorityCacheService implements AuthorityCacheService {

    private static final TypeReference<List<String>> AUTHORITY_LIST_TYPE = new TypeReference<>() {
    };

    private final ReactiveStringRedisTemplate redisTemplate;
    private final AuthCacheJsonCodec jsonCodec;

    public RedisAuthorityCacheService(ReactiveStringRedisTemplate redisTemplate, AuthCacheJsonCodec jsonCodec) {
        this.redisTemplate = redisTemplate;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public Mono<List<String>> getAuthorities(String userId) {
        String key = AuthCacheKeys.authority(userId);
        return redisTemplate.opsForValue().get(key)
                .flatMap(json -> Mono.fromCallable(() -> List.copyOf(jsonCodec.read(json, AUTHORITY_LIST_TYPE))))
                .defaultIfEmpty(List.of());
    }

    @Override
    public Mono<Void> saveAuthorities(String userId, List<String> authorities, Duration ttl) {
        String key = AuthCacheKeys.authority(userId);
        Duration cacheTtl = AuthCacheKeys.positiveTtl(ttl);
        List<String> value = List.copyOf(authorities);
        return Mono.fromCallable(() -> jsonCodec.write(value))
                .flatMap(json -> redisTemplate.opsForValue().set(key, json, cacheTtl))
                .then();
    }

    @Override
    public Mono<Boolean> deleteAuthorities(String userId) {
        return redisTemplate.delete(AuthCacheKeys.authority(userId))
                .map(deleted -> deleted > 0);
    }
}
