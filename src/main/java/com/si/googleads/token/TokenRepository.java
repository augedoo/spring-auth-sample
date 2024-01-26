package com.si.googleads.token;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface TokenRepository extends ReactiveMongoRepository<Token,String> {
    Mono<Token> findByEmail(String email);
}
