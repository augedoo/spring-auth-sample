package com.si.googleads.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthenticationManager implements ReactiveAuthenticationManager {
    private final JwtService jwtService;
    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String username;
        String authToken = authentication.getCredentials().toString();

        try {
            username = jwtService.getUsernameFromToken(authToken);
        } catch (Exception e) {
            username = null;
        }

        if (username != null && !jwtService.isTokenValid(authToken)) {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(username, username, null);
            SecurityContextHolder.getContext().setAuthentication(new AppUser(username, null));

            return Mono.just(auth);
        } else {
            return Mono.empty();
        }
    }
}

