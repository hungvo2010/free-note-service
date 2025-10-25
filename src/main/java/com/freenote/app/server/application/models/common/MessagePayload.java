package com.freenote.app.server.application.models.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

@Data
@NoArgsConstructor
public class MessagePayload {
    private static final Logger log = LogManager.getLogger(MessagePayload.class);
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
            log.info("error writing to");
            return new byte[0];
        }
    }
}
