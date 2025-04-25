package com.freenote.app.server.http.exceptions;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class UpgradeParserException extends RuntimeException {
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
