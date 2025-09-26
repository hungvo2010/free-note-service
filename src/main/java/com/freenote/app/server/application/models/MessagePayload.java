package com.freenote.app.server.application.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.util.UUID;

@Data
public class MessagePayload {
    private UUID messageId;
    private Object payload;

    public byte[] toBytes() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsBytes(this);
        } catch (Exception e) {
            return new byte[0];
        }
    }
}
