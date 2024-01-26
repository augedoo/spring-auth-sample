package com.si.googleads.user;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveMongoRepository<User, String> {
    Mono<User> findByEmail(String email);

    // Custom query to eagerly fetch Token data
    @Query(value = "{ 'email' : ?0 }", fields = "{ 'token' : 1 }")
    Mono<User> findByEmailWithToken(String email);
}
