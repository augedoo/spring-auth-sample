package com.si.googleads.auth;

//import com.si.googleads.account.AccountService;
//import com.si.googleads.account.AssociatedPropertiesResponse;
import com.si.googleads.response.MessageResponse;
import com.si.googleads.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.security.Principal;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Mono<AuthResponse>> register(
            @RequestBody @Valid RegisterRequest request
    ) {
        return ResponseEntity.ok().body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<Mono<AuthResponse>> login(
            @RequestBody @Valid LoginRequest request
    ) {
        return ResponseEntity.ok().body(authService.login(request));
    }

    @GetMapping("/google/connect")
    @ResponseStatus(HttpStatus.MOVED_PERMANENTLY)
    public Mono<Void> authorize (ServerHttpResponse response) {
        String redirectUrl = authService.getAuthorizationUrl();
        response.setStatusCode(HttpStatus.PERMANENT_REDIRECT);
        response.getHeaders().setLocation(URI.create(redirectUrl));
        return response.setComplete();
    }

    @GetMapping("/google/callback")
    public Mono<ResponseEntity<Mono<MessageResponse>>>  redirectCallback (@RequestParam("code") String authorizationCode,  Mono<Principal> principal) {
        return principal
                .map(Principal::getName)
                .map(userEmail -> {
                    // Todo: Ensure multiple signups update existing token in db
                    Mono<MessageResponse> message = authService.getAccessToken(authorizationCode, userEmail);
                    return ResponseEntity.status(HttpStatus.OK).body(message);
                });
    }
}
