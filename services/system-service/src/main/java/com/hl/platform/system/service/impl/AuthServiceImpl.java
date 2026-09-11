package com.hl.platform.system.service.impl;

import com.hl.platform.base.security.AuthCacheKeys;
import com.hl.platform.base.security.SessionIdentity;
import com.hl.platform.system.service.AuthService;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthServiceImpl implements AuthService {
    private final StringRedisTemplate redisTemplate;

    public AuthServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void logout() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getDetails() instanceof SessionIdentity identity)) {
            throw new BadCredentialsException("Current session identity is required");
        }
        String key = AuthCacheKeys.session(authentication.getName(), identity.sid());
        try {
            redisTemplate.delete(key);
        } catch (DataAccessException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Authentication session store unavailable", exception);
        }
    }
}
