package com.freenote.app.server.exceptions;

public class AcceptConnectionException extends RuntimeException {
    public AcceptConnectionException(Exception e) {
        super(e);
    }

    public AcceptConnectionException(String err) {
        super(err);
    }
}
