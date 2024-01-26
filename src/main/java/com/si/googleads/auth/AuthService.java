package com.si.googleads.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.si.googleads.configuration.JwtService;
import com.si.googleads.response.MessageResponse;
import com.si.googleads.token.Token;
import com.si.googleads.token.TokenRepository;
import com.si.googleads.user.Role;
import com.si.googleads.user.User;
import com.si.googleads.user.UserRepository;
import com.si.googleads.user.UserResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Environment env;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final TokenRepository tokenRepository;

    public Mono<AuthResponse> register(RegisterRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .flatMap(user -> Mono.error(new RuntimeException("Email is already registered. Please login instead")))
                .switchIfEmpty(Mono.defer(() -> {
                    Instant now = Instant.now();

                    var user = User.builder()
                            .email(request.getEmail())
                            .password(passwordEncoder.encode(request.getPassword()))
                            .role(Role.USER)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();

                    return userRepository.save(user)
                            .flatMap(savedUser -> {
                                UserResponseDto userResponseDto = UserResponseDto.builder()
                                        .id(savedUser.getId())
                                        .email(savedUser.getEmail())
                                        .build();
                                var jwtToken = jwtService.generateToken(user);
                                var authResponse = AuthResponse.builder()
                                        .token(jwtToken)
                                        .user(userResponseDto)
                                        .message("User registered successfully")
                                        .build();
                                return Mono.just(authResponse);
                            });
                }))
                .cast(AuthResponse.class);
    }

    public Mono<AuthResponse> login(LoginRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .flatMap(user -> {
                    if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        var jwtToken = jwtService.generateToken(user);

                        UserResponseDto userResponse = UserResponseDto.builder()
                                .id(user.getId())
                                .email(user.getEmail())
                                .build();

                        var res = AuthResponse.builder()
                                .token(jwtToken)
                                .user(userResponse)
                                .message("Login successful")
                                .build();

                        return Mono.just(res);
                    } else {
                        return Mono.error(new RuntimeException("Invalid credentials"));
                    }
                })
                .switchIfEmpty(Mono.defer(() -> Mono.error(new RuntimeException("We did not find any user with email " + request.getEmail()))));
    }

    public String getAuthorizationUrl() {
        return UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/auth")
                .queryParam("response_type", "code")
                .queryParam("client_id", env.getProperty("google_oauth_client_id"))
                .queryParam("scope", env.getProperty("google_oauth_scope"))
                .queryParam("redirect_uri", env.getProperty("google_oauth_redirect_uri"))
                .queryParam("prompt", "consent")
                .queryParam("access_type", "offline")
                .toUriString();
    }

    private String generateTokenRequestUrl() {
        return UriComponentsBuilder.fromUriString(Objects.requireNonNull(env.getProperty("google_oauth_access_token_uri")))
                .queryParam("client_id", env.getProperty("google_oauth_client_id"))
                .queryParam("client_secret", env.getProperty("google_oauth_client_secret"))
                .toUriString();
    }

    public Mono<MessageResponse> getAccessToken(String authorizationCode, String authUserEmail) {
        // Todo: Change to value in env
        Map<String, String> tokenRequestBody = Map.of(
                "grant_type", "authorization_code",
                "code", authorizationCode,
                "redirect_uri", Objects.requireNonNull(env.getProperty("google_oauth_redirect_uri"))
        );

        try {
            WebClient client = WebClient.builder().build();
            return client.post()
                    .uri(generateTokenRequestUrl())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(tokenRequestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMap(googleResponseString -> handleTokenResponse(googleResponseString, authUserEmail))
                    .onErrorResume(error -> {
//                        error.printStackTrace();
                        return Mono.error(new RuntimeException("Error getting credentials from Google. Please try again."));
                    });
        } catch (Exception e) {
            return Mono.error(new RuntimeException("Error in processing the request", e));
        }
    }

    private Mono<MessageResponse> handleTokenResponse(String googleResponseString, String authUserEmail) {
        try {
            AccessTokenResponse tokenResponse = objectMapper.readValue(googleResponseString, AccessTokenResponse.class);

            Instant now = Instant.now();

            var googleAuthToken = Token.builder()
                    .email(authUserEmail)
                    .accessToken(tokenResponse.getAccessToken())
                    .tokenType(tokenResponse.getTokenType())
                    .refreshToken(tokenResponse.getRefreshToken())
                    .scope(tokenResponse.getScope())
                    .expiresIn(tokenResponse.getExpiresIn())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            return tokenRepository.findByEmail(authUserEmail)
                    .flatMap(existingToken -> {
                        existingToken.setAccessToken(tokenResponse.getAccessToken());
                        existingToken.setUpdatedAt(now);
                        return tokenRepository.save(existingToken)
                                .map(savedToken -> MessageResponse.builder().message("Connected successfully.").build());
                    })
                    .switchIfEmpty(Mono.defer(() -> tokenRepository.save(googleAuthToken)
                            .map(savedToken -> MessageResponse.builder().message("Connected successfully.").build())));
        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException("Failed to deserialize JSON response body", e));
        }
    }

    public Mono<Token> refreshToken(Token token) {
        Map<String, String> refreshTokenRequestBody = Map.of(
                "grant_type", "refresh_token",
                "refresh_token", token.getRefreshToken()
        );

        String jsonBody;

        try {
            jsonBody = objectMapper.writeValueAsString(refreshTokenRequestBody);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize JSON request body");
            return Mono.empty();
        }

        return webClient.post()
            .uri(generateTokenRequestUrl())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(jsonBody)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(googleResponseString -> handleRefreshTokenResponse(googleResponseString, token))
                .onErrorResume(error -> {
//                    error.printStackTrace();
                    log.error("Error getting credentials from Google for token " + token.getId() + " with user " + token.getEmail());
                    return Mono.empty();
                });
    }

    private Mono<Token> handleRefreshTokenResponse(String googleResponseString, Token token) {
        try {
            AccessTokenResponse newTokenResponse = objectMapper.readValue(googleResponseString, AccessTokenResponse.class);

            token.setAccessToken(newTokenResponse.getAccessToken());
            token.setExpiresIn(newTokenResponse.getExpiresIn());
            token.setUpdatedAt(Instant.now());

            return tokenRepository.save(token);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON response body " + token.getId() + " with user " + token.getEmail());
            return Mono.empty();
        }
    }

    public Mono<String> getUserToken(String userEmail) {
        return tokenRepository.findByEmail(userEmail)
                .map(Token::getAccessToken)
                .onErrorResume(error -> Mono.error(new RuntimeException("Token not found. User account is not connected to google.")));
    }
}
