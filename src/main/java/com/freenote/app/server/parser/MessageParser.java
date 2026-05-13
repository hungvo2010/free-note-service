package com.freenote.app.server.parser;

import com.freenote.app.server.dto.HeartbeatMsg;
import com.freenote.app.server.exceptions.MessageParsingException;
import com.freenote.app.server.model.enums.MsgType;
import com.freenote.app.server.messages.DataIncomingMessage;
import com.freenote.app.server.messages.HeartbeatIncomingMessage;
import com.freenote.app.server.messages.IncomingMessage;
import com.freenote.app.server.util.JSONUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MessageParser {
    private static final Logger log = LogManager.getLogger(MessageParser.class);

    public IncomingMessage parse(String message) throws MessageParsingException {
        try {
            var requestMessage = JSONUtils.fromJSON(message, HeartbeatMsg.class);
            if (requestMessage != null && requestMessage.getMsgType() == MsgType.PING) {
                return new HeartbeatIncomingMessage(requestMessage);
            }
            return new DataIncomingMessage(message);
        } catch (Exception e) {
            log.error("Error parsing message: {}", message, e);
            throw new MessageParsingException("Failed to parse message", e);
        }
    }
}
