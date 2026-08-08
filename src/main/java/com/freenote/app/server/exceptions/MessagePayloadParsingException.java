package com.freenote.app.server.exceptions;

public class MessagePayloadParsingException extends RuntimeException {
    public MessagePayloadParsingException(String description, Throwable cause) {
        super(description, cause);
    }
}
