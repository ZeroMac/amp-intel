package com.hl.platform.gateway.security;

import java.time.Duration;

import com.hl.platform.gateway.cache.AuthCacheJsonCodec;
import com.hl.platform.gateway.cache.AuthCacheKeys;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class RedisAuthSessionService implements AuthSessionService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final AuthCacheJsonCodec jsonCodec;

    public RedisAuthSessionService(ReactiveStringRedisTemplate redisTemplate, AuthCacheJsonCodec jsonCodec) {
        this.redisTemplate = redisTemplate;
        this.jsonCodec = jsonCodec;
    }

    @Override
    public Mono<AuthSession> get(String userId, String sid) {
        return redisTemplate.opsForValue().get(AuthCacheKeys.session(userId, sid))
                .flatMap(json -> Mono.fromCallable(() -> jsonCodec.read(json, AuthSession.class)));
    }

    @Override
    public Mono<Void> save(String userId, String sid, AuthSession session, Duration ttl) {
        if (!userId.equals(session.userId())) {
            return Mono.error(new IllegalArgumentException("Session userId must match the Redis key userId"));
        }
        String key = AuthCacheKeys.session(userId, sid);
        Duration sessionTtl = AuthCacheKeys.positiveTtl(ttl);
        return Mono.fromCallable(() -> jsonCodec.write(session))
                .flatMap(json -> redisTemplate.opsForValue().set(key, json, sessionTtl))
                .then();
    }

    @Override
    public Mono<Boolean> delete(String userId, String sid) {
        return redisTemplate.delete(AuthCacheKeys.session(userId, sid))
                .map(deleted -> deleted > 0);
    }

    @Override
    public Mono<Boolean> invalidate(String userId, String sid) {
        return delete(userId, sid);
    }

    @Override
    public Mono<Boolean> updateTokenVersion(String userId, String sid, long newVersion) {
        String key = AuthCacheKeys.session(userId, sid);
        return redisTemplate.opsForValue().get(key)
                .flatMap(json -> Mono.fromCallable(() -> jsonCodec.read(json, AuthSession.class)))
                .flatMap(session -> redisTemplate.getExpire(key)
                        .filter(ttl -> !ttl.isZero() && !ttl.isNegative())
                        .flatMap(ttl -> Mono.fromCallable(() -> jsonCodec.write(session.withTokenVersion(newVersion)))
                                .flatMap(updated -> redisTemplate.opsForValue().set(key, updated, ttl))))
                .defaultIfEmpty(false);
    }
}
