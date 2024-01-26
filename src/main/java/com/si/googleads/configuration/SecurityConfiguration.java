package com.si.googleads.configuration;

import com.si.googleads.advice.ApplicationExceptionHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@RequiredArgsConstructor
@EnableReactiveMethodSecurity
public class SecurityConfiguration {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private ApplicationExceptionHandler applicationExceptionHandler;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .cors(ServerHttpSecurity.CorsSpec::disable)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                                .pathMatchers("/api/v1/auth/register",
                                        "/api/v1/auth/login",
                                        "/api/v1/auth/google/connect" //Todo: Move to protected routes
//                                "/api/v1/auth/google/callback" //Todo: Move to protected routes
                                )
                                .permitAll()
                                .anyExchange()
                                .authenticated()
                )
                .authenticationManager(authenticationManager)
                .securityContextRepository(securityContextRepository)
//                .exceptionHandling(ex ->
//                        {
//                            ex.authenticationEntryPoint((exchange, e) -> {
//                                        return applicationExceptionHandler.handleAuthenticationError(exchange, e);
//                                    })
//                                    .accessDeniedHandler((exchange, e) -> {
//                                        return applicationExceptionHandler.handleAccessDeniedError(exchange, e);
//                                    });
//                        }
//                )
                ;

        return http.build();
    }
}
