package com.hl.platform.gateway.cache;

import java.time.Duration;
import java.util.List;

import com.hl.platform.gateway.cache.model.FunctionCacheItem;
import com.hl.platform.gateway.security.AuthSession;
import com.hl.platform.gateway.security.RedisAuthSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisAuthCacheServiceTest {

    private static final Duration TTL = Duration.ofMinutes(30);

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    private AuthCacheJsonCodec jsonCodec;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        jsonCodec = new AuthCacheJsonCodec(new ObjectMapper());
    }

    @Test
    void sessionUsesExpectedKeyAndJsonAndAlwaysSetsTtl() {
        RedisAuthSessionService service = new RedisAuthSessionService(redisTemplate, jsonCodec);
        AuthSession session = new AuthSession("100", "admin", 1, "ACTIVE");
        when(valueOperations.set(eq("auth:100:session:session-a"), anyString(), eq(TTL)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(service.save("100", "session-a", session, TTL)).verifyComplete();

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("auth:100:session:session-a"), json.capture(), eq(TTL));
        assertThat(jsonCodec.read(json.getValue(), AuthSession.class)).isEqualTo(session);
    }

    @Test
    void updateTokenVersionPreservesRemainingSessionTtl() {
        RedisAuthSessionService service = new RedisAuthSessionService(redisTemplate, jsonCodec);
        AuthSession session = new AuthSession("100", "admin", 1, "ACTIVE");
        String key = "auth:100:session:session-a";
        when(valueOperations.get(key)).thenReturn(Mono.just(jsonCodec.write(session)));
        when(redisTemplate.getExpire(key)).thenReturn(Mono.just(TTL));
        when(valueOperations.set(eq(key), anyString(), eq(TTL))).thenReturn(Mono.just(true));

        StepVerifier.create(service.updateTokenVersion("100", "session-a", 2))
                .expectNext(true)
                .verifyComplete();

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(key), json.capture(), eq(TTL));
        assertThat(jsonCodec.read(json.getValue(), AuthSession.class).tokenVersion()).isEqualTo(2);
    }

    @Test
    void invalidateDeletesOnlyCurrentSessionKeyAndIsIdempotent() {
        RedisAuthSessionService service = new RedisAuthSessionService(redisTemplate, jsonCodec);
        when(redisTemplate.delete("auth:100:session:session-a"))
                .thenReturn(Mono.just(1L), Mono.just(0L));

        StepVerifier.create(service.invalidate("100", "session-a"))
                .expectNext(true)
                .verifyComplete();
        StepVerifier.create(service.invalidate("100", "session-a"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void authorityCacheRoundTripsStringArrayWithTtl() {
        RedisAuthorityCacheService service = new RedisAuthorityCacheService(redisTemplate, jsonCodec);
        List<String> authorities = List.of("sys:func:read", "sys:func:add");
        when(valueOperations.set(eq("auth:100:authority"), anyString(), eq(TTL)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(service.saveAuthorities("100", authorities, TTL)).verifyComplete();

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("auth:100:authority"), json.capture(), eq(TTL));
        when(valueOperations.get("auth:100:authority")).thenReturn(Mono.just(json.getValue()));
        StepVerifier.create(service.getAuthorities("100"))
                .expectNext(authorities)
                .verifyComplete();
    }

    @Test
    void functionCacheRoundTripsSystemFunctionShapeWithTtl() {
        RedisFunctionCacheService service = new RedisFunctionCacheService(redisTemplate, jsonCodec);
        List<FunctionCacheItem> functions = List.of(new FunctionCacheItem(
                1001L, 1000L, "func-read", "功能查询", "/system/func", 1, "10"));
        when(valueOperations.set(eq("auth:100:function"), anyString(), eq(TTL)))
                .thenReturn(Mono.just(true));

        StepVerifier.create(service.saveFunctions("100", functions, TTL)).verifyComplete();

        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq("auth:100:function"), json.capture(), eq(TTL));
        when(valueOperations.get("auth:100:function")).thenReturn(Mono.just(json.getValue()));
        StepVerifier.create(service.getFunctions("100"))
                .expectNext(functions)
                .verifyComplete();
    }
}
