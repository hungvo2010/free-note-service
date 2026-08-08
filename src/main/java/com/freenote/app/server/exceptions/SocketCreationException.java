package com.freenote.app.server.exceptions;

public class SocketCreationException extends Exception {
    public SocketCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SocketCreationException(String message) {
        super(message);
    }
}
