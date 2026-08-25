package com.hl.platform.gateway.cache;

import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class AuthCacheJsonCodec {

    private final ObjectMapper objectMapper;

    public AuthCacheJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (RuntimeException exception) {
            throw new AuthCacheSerializationException("Unable to serialize authentication cache value", exception);
        }
    }

    public <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (RuntimeException exception) {
            throw new AuthCacheSerializationException("Unable to deserialize authentication cache value", exception);
        }
    }

    public <T> T read(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (RuntimeException exception) {
            throw new AuthCacheSerializationException("Unable to deserialize authentication cache value", exception);
        }
    }
}
