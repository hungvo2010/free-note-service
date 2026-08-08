package com.freenote.app.server.exceptions;

public class SelectorInterruptException extends RuntimeException {
    public SelectorInterruptException(String message, Exception cause) {
        super(message, cause);
    }

    public SelectorInterruptException(String message) {
        super(message);
    }
}
