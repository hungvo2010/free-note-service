package com.freenote.app.server.handler.impl;

import com.freenote.annotations.URIHandlerImplementation;
import com.freenote.app.server.application.models.request.core.ResponseData;
import com.freenote.app.server.application.models.request.core.ResponseObject;
import com.freenote.app.server.connections.WebSocketConnection;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

@URIHandlerImplementation("/echo")
public class NewEchoHandlerImpl extends CommonHandlerImpl {
    private static final Logger log = LogManager.getLogger(NewEchoHandlerImpl.class);

    @Override
    public void onMessage(WebSocketConnection webSocketConnection, String message) {
        log.info("Writing to output stream with message: {}", message);
        log.info("===========================================================================");
        webSocketConnection.setResponse(new ResponseObject<>(1, new EchoResponseData(message)));
    }

    @Setter
    @Getter
    public static class EchoResponseData extends ResponseData {
        private String echoMessage;

        public EchoResponseData(String echoMessage) {
            this.echoMessage = echoMessage;
        }

    }
}
