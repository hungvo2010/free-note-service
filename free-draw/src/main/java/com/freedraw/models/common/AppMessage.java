package com.freedraw.models.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.freedraw.models.enums.MessageType;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppMessage {
    private String msgId;
    private MessageType type;
    private long timestamp;
    @JsonProperty("payload")
    private Object body;

    public AppMessage(Object payload) {
        this.body = payload;
        this.msgId = UUID.randomUUID().toString();
    }

    public AppMessage() {
        timestamp = System.currentTimeMillis();
        this.msgId = UUID.randomUUID().toString();
    }
}
