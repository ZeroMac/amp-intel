package com.hl.platform.system.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import com.hl.platform.base.security.AuthHeaders;
import com.hl.platform.base.security.AuthorityCacheReader;
import com.hl.platform.base.security.InternalAuthSigner;
import com.hl.platform.base.security.RedisAuthorityCacheReader;
import com.hl.platform.system.controller.FunctionController;
import com.hl.platform.system.service.FunctionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(PermissionChainTest.TestConfig.class)
@TestPropertySource(properties = "security.internal.secret=" + PermissionChainTest.SECRET)
class PermissionChainTest {
    static final String SECRET = "test-only-internal-secret-32-bytes-long";
    private final InternalAuthSigner signer = new InternalAuthSigner(SECRET);
    @Autowired WebApplicationContext context;
    @Autowired ValueOperations<String, String> values;
    @Autowired FunctionService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(values, service);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void redisAuthorityAllowsMethodAndContextContainsIdentity() throws Exception {
        when(values.get("auth:100:authority")).thenReturn("[\"sys:func:read\"]");
        when(service.listChildrenByParentId(0L)).thenAnswer(invocation -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication.getName()).isEqualTo("100");
            assertThat(authentication.getDetails())
                    .isEqualTo(new InternalAuthenticationFilter.SessionIdentity("session-a", 2));
            return List.of();
        });
        mvc.perform(signed(signer)).andExpect(status().isOk());
        verify(service).listChildrenByParentId(0L);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        mvc.perform(get("/sys/functions/0/children")).andExpect(status().isUnauthorized());
    }

    @Test
    void missingEmptyOrDifferentAuthorityCannotInvokeMethod() throws Exception {
        for (String json : new String[]{null, "[]", "[\"sys:func:add\"]"}) {
            when(values.get("auth:100:authority")).thenReturn(json);
            mvc.perform(signed(signer).header("X-Auth-Authorities", "sys:func:read"))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }

    @Test
    void unsignedForgedDuplicateAndTamperedHeadersAreRejectedBeforeRedis() throws Exception {
        mvc.perform(get("/sys/functions/0/children")
                .header(AuthHeaders.USER_ID, "100").header(AuthHeaders.SID, "session-a")
                .header(AuthHeaders.TOKEN_VERSION, "2")).andExpect(status().isUnauthorized());
        mvc.perform(signed(new InternalAuthSigner("another-test-secret-32-bytes-long!!")))
                .andExpect(status().isUnauthorized());
        mvc.perform(signed(signer).header(AuthHeaders.USER_ID, "200"))
                .andExpect(status().isUnauthorized());
        mvc.perform(signed(signer).with(request -> {
            request.removeHeader(AuthHeaders.TOKEN_VERSION);
            request.addHeader(AuthHeaders.TOKEN_VERSION, "3");
            return request;
        })).andExpect(status().isUnauthorized());
        verifyNoInteractions(values, service);
    }

    @Test
    void expiredAndFutureSignaturesAreRejected() throws Exception {
        for (long offset : new long[]{-61, 30}) {
            var timedSigner = new InternalAuthSigner(SECRET,
                    Clock.fixed(Instant.now().plusSeconds(offset), ZoneOffset.UTC));
            mvc.perform(signed(timedSigner)).andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(values, service);
    }

    @Test
    void redisFailureAndMalformedCacheFailClosed() throws Exception {
        when(values.get("auth:100:authority")).thenThrow(new IllegalStateException("Redis unavailable"));
        mvc.perform(signed(signer)).andExpect(status().isServiceUnavailable());
        reset(values);
        when(values.get("auth:100:authority")).thenReturn("broken-json");
        mvc.perform(signed(signer)).andExpect(status().isServiceUnavailable());
        verifyNoInteractions(service);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private MockHttpServletRequestBuilder signed(InternalAuthSigner signing) {
        String timestamp = signing.timestamp();
        return get("/sys/functions/0/children")
                .header(AuthHeaders.USER_ID, "100").header(AuthHeaders.SID, "session-a")
                .header(AuthHeaders.TOKEN_VERSION, "2").header(AuthHeaders.TIMESTAMP, timestamp)
                .header(AuthHeaders.SIGNATURE, signing.sign("100", "session-a", "2", timestamp));
    }

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, FunctionController.class})
    static class TestConfig {
        @Bean FunctionService functionService() { return mock(FunctionService.class); }
        @Bean @SuppressWarnings("unchecked")
        ValueOperations<String, String> values() { return mock(ValueOperations.class); }
        @Bean AuthorityCacheReader reader(ValueOperations<String, String> values) {
            var redis = mock(StringRedisTemplate.class);
            when(redis.opsForValue()).thenReturn(values);
            return new RedisAuthorityCacheReader(redis, new ObjectMapper());
        }
    }
}
