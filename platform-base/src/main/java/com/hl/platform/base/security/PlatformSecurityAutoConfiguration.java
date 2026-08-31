package com.hl.platform.base.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
@ConditionalOnClass({StringRedisTemplate.class, ObjectMapper.class})
public class PlatformSecurityAutoConfiguration {

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    @ConditionalOnMissingBean(AuthorityCacheReader.class)
    public AuthorityCacheReader authorityCacheReader(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        return new RedisAuthorityCacheReader(redisTemplate, objectMapper);
    }
}
