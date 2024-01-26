package com.si.googleads.token;

import com.si.googleads.auth.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Service
@EnableScheduling
@RequiredArgsConstructor
public class TokenService {
    private final TokenRepository tokenRepository;
    private final AuthService authService;

//    @Scheduled(fixedDelayString = "PT5S")
    @Scheduled(fixedDelayString = "PT10S")
    public void keepOauthTokensAlive() {
        log.info("Running job...");

        tokenRepository.findAll()
                .filter(this::shouldRefreshToken)
                .flatMap(authService::refreshToken)
                .subscribe();
    }

    private boolean shouldRefreshToken(Token token) {
        Duration expiresInDuration = token.getExpiresIn();
        Instant expirationTime = token.getUpdatedAt().plus(expiresInDuration);

        Duration timeUntilExpiration = Duration.between(Instant.now(), expirationTime);

//    Todo: Update time
        int MIN_TIME_TO_UPDATE = 5;
        return timeUntilExpiration.toSeconds() >= MIN_TIME_TO_UPDATE;
    }
}
