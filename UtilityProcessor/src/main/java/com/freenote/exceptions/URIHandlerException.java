package com.freenote.exceptions;

public class URIHandlerException extends Exception {
    private final String path;

    public URIHandlerException(String path, String message) {
        super(message);
        this.path = path;
    }

    public URIHandlerException(String path, String message, Throwable cause) {
        super(message, cause);
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
