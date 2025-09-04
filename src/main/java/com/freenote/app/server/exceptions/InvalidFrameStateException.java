package com.freenote.app.server.exceptions;

public class InvalidFrameStateException extends RuntimeException {
    public InvalidFrameStateException(String message) {
        super(message);
    }

    public InvalidFrameStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
