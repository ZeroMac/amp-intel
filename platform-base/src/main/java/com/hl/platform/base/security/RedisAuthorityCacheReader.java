package com.hl.platform.base.security;

import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

public class RedisAuthorityCacheReader implements AuthorityCacheReader {

    private static final TypeReference<List<String>> AUTHORITY_LIST_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisAuthorityCacheReader(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<String> getAuthorities(String userId) {
        String json = redisTemplate.opsForValue().get(AuthCacheKeys.authority(userId));
        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {
            List<String> authorities = objectMapper.readValue(json, AUTHORITY_LIST_TYPE);
            return authorities == null ? List.of() : List.copyOf(authorities);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Unable to deserialize authority cache", exception);
        }
    }
}
