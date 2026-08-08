package com.freenote.app.server.exceptions;

public class NIOReadException extends Throwable {
    public NIOReadException(Exception e) {
        super(e);
    }
}
