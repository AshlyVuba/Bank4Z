package com.bank4z.backend.common.security;

import com.bank4z.backend.authservice.CustomUserDetailsService;
import com.bank4z.backend.authservice.UserPrincipal;
import com.bank4z.backend.sessionservice.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final SessionService sessionService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   CustomUserDetailsService userDetailsService,
                                   SessionService sessionService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.sessionService = sessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // Explicitly require "access" type here: a leaked refresh token
        // must never be usable to call authenticated endpoints directly.
        if (jwtService.isTokenValid(token, "access")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(token, request);
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        UUID sessionId;
        try {
            sessionId = jwtService.extractSessionId(token);
        } catch (IllegalArgumentException e) {
            return; // malformed session id: treat as not logged in
        }

        // A valid signature is not enough any more. The session behind the token
        // must still be active, so revoking a device takes effect immediately.
        if (sessionId == null || !sessionService.isSessionActive(sessionId)) {
            return; // stays unauthenticated, so the caller gets a 401
        }

        try {
            UserPrincipal loaded = (UserPrincipal) userDetailsService
                    .loadUserByUsername(jwtService.extractEmail(token));

            // Same user, but now carrying the session id so controllers know "which device is this".
            UserPrincipal principal = new UserPrincipal(loaded.getUser(), sessionId);

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        } catch (UsernameNotFoundException e) {
            // The user no longer exists: leave the request unauthenticated.
        }
    }
}