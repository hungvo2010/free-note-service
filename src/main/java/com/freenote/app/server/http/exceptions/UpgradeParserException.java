package com.freenote.app.server.http.exceptions;

public class UpgradeParserException extends RuntimeException {

    public UpgradeParserException() {
    }

    public UpgradeParserException(String message) {
        super(message);
    }

    public UpgradeParserException(String message, Throwable cause) {
        super(message, cause);
    }

    public UpgradeParserException(Throwable cause) {
        super(cause);
    }
}
