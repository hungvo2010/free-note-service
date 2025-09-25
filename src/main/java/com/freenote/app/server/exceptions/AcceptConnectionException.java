package com.freenote.app.server.exceptions;

public class AcceptConnectionException extends RuntimeException {
    public AcceptConnectionException(Exception e) {
        super(e);
    }
}
