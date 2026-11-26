package br.com.sabios.api.shared.exception;

/** Requisição semanticamente inválida (HTTP 400). */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
