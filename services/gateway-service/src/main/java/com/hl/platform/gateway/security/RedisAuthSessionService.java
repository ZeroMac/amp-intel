package com.hl.platform.gateway.security;

import com.hl.platform.base.security.AuthCacheKeys;
import com.hl.platform.gateway.cache.AuthCacheJsonCodec;
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

}
