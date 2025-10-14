package com.freenote.app.server.application.models.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class MessagePayload {
    private UUID messageId;
    private Object payload;

    public MessagePayload(Object payload) {
        this.payload = payload;
        this.messageId = UUID.randomUUID();
    }

    public byte[] toBytes() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsBytes(this);
        } catch (Exception e) {
            return new byte[0];
        }
    }
}
