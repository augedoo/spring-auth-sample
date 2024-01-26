package com.si.googleads.advice;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoSocketException;
import com.si.googleads.exceptions.ApiRequestException;
import com.si.googleads.exceptions.DatabaseResourceException;
import com.si.googleads.response.ErrorResponse;
import org.springframework.core.codec.DecodingException;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ApplicationExceptionHandler {

    // General Exception Handler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Throwable.class)
    public Mono<ErrorResponse<?>> handleAnyUnhandledException(Throwable ex) {
        if (ex instanceof RuntimeException runtimeException) {
            Throwable cause = runtimeException.getCause();

            if (cause instanceof DecodingException) {
                return handleDecodingException((DecodingException) cause);
            }
        }

        // If not handled specifically, provide a generic error response
        ErrorResponse<?> errorResponse = ErrorResponse.builder().message(ex.getMessage()).build();
        return Mono.just(errorResponse);
    }

    // Handle DecodingException
    private Mono<ErrorResponse<?>> handleDecodingException(DecodingException ex) {
        String exceptionMessage = ex.getMessage();

//        if (exceptionMessage.contains("MetricType")) {
//            Map<String, String> errors = new HashMap<>();
//            errors.put("metrics", "Invalid value for metricTypes field. Allowed values are: " + Arrays.toString(MetricType.getAllValues()));
//
//            ErrorResponse<?> errorResponse = ErrorResponse.builder().message("Validation Error").error(errors).build();
//
//            return Mono.just(errorResponse);
//        }

        // Handle other DecodingException scenarios or return a generic error response
        ErrorResponse<?> errorResponse = ErrorResponse.builder().message("JSON decoding error: " + exceptionMessage).build();
        return Mono.just(errorResponse);
    }


    // Form Validation and Message Read Exceptions
    @ExceptionHandler({WebExchangeBindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ErrorResponse<?>> handleValidationAndMessageReadExceptions(Exception ex) {
        Map<String, String> errors = new HashMap<>();

        if (ex instanceof WebExchangeBindException bindException) {
            bindException.getBindingResult().getAllErrors().forEach((error) -> {
                String fieldName = ((FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage);
            });
        }

        ErrorResponse<?> errorResponse = ErrorResponse.builder().message("Validation Error").error(errors).build();

        return Mono.just(errorResponse);
    }


    // Business Logic Errors
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(ApiRequestException.class)
    public ErrorResponse<?> handleBusinessLogicException(ApiRequestException ex) {
        return new ErrorResponse<>(ex.getMessage());
    }

    // Database Exceptions
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = {MongoSocketException.class, MongoBulkWriteException.class, UncategorizedMongoDbException.class, MongoSocketException.class})
    public ErrorResponse<?> handleMongoDbConnectionExceptions(Exception ex) {
        return new ErrorResponse<>(ex.getMessage());
    }

    // Database Resource Exceptions
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(DatabaseResourceException.class)
    public ErrorResponse<?> handleDatabaseResourceException(DatabaseResourceException ex) {
        return new ErrorResponse<>(ex.getMessage());
    }

    // Form Validation Exceptions
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return errors;
    }

    // Custom Authentication Error Handling
    public Mono<Void> handleAuthenticationError(ServerWebExchange exchange, AuthenticationException exception) {
        exchange.getResponse

                ().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // Customize your error response here
        String errorResponse = "{\"error\":\"Authentication failed\",\"message\":\"" + exception.getMessage() + "\"}";
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(errorResponse.getBytes())));
    }

    // Custom Access Denied Error Handling
    public Mono<Void> handleAccessDeniedError(ServerWebExchange exchange, AccessDeniedException exception) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        // Customize your error response here
        String errorResponse = "{\"error\":\"Access denied\",\"message\":\"" + exception.getMessage() + "\"}";
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(errorResponse.getBytes())));
    }
}
