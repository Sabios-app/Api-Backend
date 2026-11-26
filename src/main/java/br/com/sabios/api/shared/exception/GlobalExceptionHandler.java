package br.com.sabios.api.shared.exception;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<Map<String, String>> fieldErrors = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.add(Map.of(
                    "field", error.getField(),
                    "message", error.getDefaultMessage() != null ? error.getDefaultMessage() : "Dado inválido."
            ));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "code", "VALIDATION_ERROR",
                "message", "Os dados enviados são inválidos.",
                "errors", fieldErrors
        ));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "code", "BAD_REQUEST",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "code", "MALFORMED_REQUEST",
                "message", "O corpo da requisição é inválido ou contém valores não reconhecidos."
        ));
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", HttpStatus.BAD_REQUEST.value(),
                "code", "INVALID_PARAMETER",
                "message", "Valor inválido para o parâmetro '" + ex.getName() + "'."
        ));
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<Map<String, Object>> handleTooManyRequests(TooManyRequestsException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(Map.of(
                        "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                        "code", "TOO_MANY_REQUESTS",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "status", HttpStatus.UNAUTHORIZED.value(),
                "code", "UNAUTHORIZED",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "status", HttpStatus.CONFLICT.value(),
                "code", "CONFLICT",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(java.util.NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "status", HttpStatus.NOT_FOUND.value(),
                "code", "NOT_FOUND",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "status", HttpStatus.FORBIDDEN.value(),
                "code", "FORBIDDEN",
                "message", ex.getMessage()
        ));
    }
}
