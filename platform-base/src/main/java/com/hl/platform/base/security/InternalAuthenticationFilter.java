package com.hl.platform.base.security;

import java.io.IOException;
import java.util.Collections;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class InternalAuthenticationFilter extends OncePerRequestFilter {
    private final AuthorityCacheReader authorityCacheReader;
    private final InternalAuthSigner signer;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public InternalAuthenticationFilter(AuthorityCacheReader authorityCacheReader, InternalAuthSigner signer,
            AuthenticationEntryPoint authenticationEntryPoint) {
        this.authorityCacheReader = authorityCacheReader;
        this.signer = signer;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        SecurityContextHolder.clearContext();
        String userId = singleHeader(request, AuthHeaders.USER_ID);
        String sid = singleHeader(request, AuthHeaders.SID);
        String version = singleHeader(request, AuthHeaders.TOKEN_VERSION);
        if (!signer.verify(userId, sid, version, singleHeader(request, AuthHeaders.TIMESTAMP),
                singleHeader(request, AuthHeaders.SIGNATURE))) {
            authenticationEntryPoint.commence(request, response,
                    new BadCredentialsException("Invalid internal authentication headers"));
            return;
        }
        try {
            var authorities = authorityCacheReader.getAuthorities(userId).stream()
                    .map(SimpleGrantedAuthority::new).toList();
            var authentication = UsernamePasswordAuthenticationToken.authenticated(userId, null, authorities);
            authentication.setDetails(new SessionIdentity(sid, Long.parseLong(version)));
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
            writeError(response, 503, "Service Unavailable");
            return;
        }
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static String singleHeader(HttpServletRequest request, String name) {
        var values = Collections.list(request.getHeaders(name));
        return values.size() == 1 ? values.getFirst() : null;
    }

    static void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"message\":\"" + message + "\"}");
    }

}
