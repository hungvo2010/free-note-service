package com.freenote.app.server.exceptions;

public class ClientDisconnectException extends RuntimeException {
    public ClientDisconnectException(String message, Exception ex) {
        super(message, ex);
    }

    public ClientDisconnectException(String message) {
        super(message);
    }
}
