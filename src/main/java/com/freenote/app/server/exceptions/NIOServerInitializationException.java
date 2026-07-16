package com.freenote.app.server.exceptions;

public class NIOServerInitializationException extends RuntimeException {
    public NIOServerInitializationException(String s, Throwable cause) {
        super(s, cause);
    }
}
