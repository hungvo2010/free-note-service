package com.freenote.app.server.exceptions;

public class TwoSingletonException extends RuntimeException {
    public TwoSingletonException(String message) {
        super(message);
    }

    public TwoSingletonException(String message, Throwable cause) {
        super(message, cause);
    }

    public TwoSingletonException(Throwable cause) {
        super(cause);
    }
}
