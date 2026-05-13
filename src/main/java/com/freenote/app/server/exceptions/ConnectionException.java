package com.freenote.app.server.exceptions;

public class ConnectionException extends RuntimeException {
    public ConnectionException(String errorMsg, Throwable cause) {
        super(errorMsg, cause);
    }
}
