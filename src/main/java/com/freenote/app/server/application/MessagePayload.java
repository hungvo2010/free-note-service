package com.freenote.app.server.application;

import lombok.Data;

import java.util.UUID;

@Data
public class MessagePayload {
    private UUID messageId;
    private Object payload;
}
