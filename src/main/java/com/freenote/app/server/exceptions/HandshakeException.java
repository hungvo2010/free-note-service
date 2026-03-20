package com.freenote.app.server.exceptions;

public class HandshakeException extends RuntimeException {
    public HandshakeException(String message) {
        super(message);
    }

    public HandshakeException(String message, Throwable cause) {
        super(message, cause);
    }
}
