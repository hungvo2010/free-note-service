package com.freenote.app.server.exceptions;

public class DiskReadException extends RuntimeException {
    public DiskReadException(String description, Throwable cause) {
        super(description, cause);
    }
}
