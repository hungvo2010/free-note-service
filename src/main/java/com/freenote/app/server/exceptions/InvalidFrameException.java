package com.freenote.app.server.exceptions;

public class InvalidFrameException extends RuntimeException {
    public InvalidFrameException(String message) {
        super(message);
    }

    public InvalidFrameException(String message, Throwable cause) {
        super(message, cause);
    }
}
