package it.polito.ai.pedibusbackend.security.jwt;

import org.springframework.security.core.AuthenticationException;

public class InvalidJwtAuthenticationException extends AuthenticationException {
    public InvalidJwtAuthenticationException(String e) {
        super(e);
    }
}
