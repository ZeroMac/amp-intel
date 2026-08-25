package com.hl.platform.gateway.controller;

import com.hl.platform.gateway.security.AuthSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthSessionService authSessionService;

    public AuthController(AuthSessionService authSessionService) {
        this.authSessionService = authSessionService;
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        String sid = jwt.getClaimAsString("sid");
        return authSessionService.invalidate(userId, sid)
                .thenReturn(ResponseEntity.noContent().<Void>build())
                .onErrorMap(exception -> new ResponseStatusException(
                        SERVICE_UNAVAILABLE, "Authentication session store unavailable"));
    }
}
