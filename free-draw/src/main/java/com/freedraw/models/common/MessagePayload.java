package com.freedraw.models.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessagePayload {
    private String messageId;
    private MessageType type;
    private long timestamp;
    @JsonProperty("payload")
    private Object body;

    public MessagePayload(Object payload) {
        this.body = payload;
        this.messageId = UUID.randomUUID().toString();
    }
}
