package com.freenote.app.server.application.models.common;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class MessagePayload {
    private String messageId;
    private Object body;

    public MessagePayload(Object payload) {
        this.body = payload;
        this.messageId = UUID.randomUUID().toString();
    }
}
