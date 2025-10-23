package com.freenote.app.server.application.models.common;

public class WebSocketAPIResponse extends MessagePayload {
    public WebSocketAPIResponse(String message) {
        super(message);
    }

    public static WebSocketAPIResponse UNEXPECTED_ERROR = new WebSocketAPIResponse("An unexpected error occurred.");
}
