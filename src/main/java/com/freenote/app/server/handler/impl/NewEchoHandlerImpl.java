package com.freenote.app.server.handler.impl;

import com.freenote.annotations.URIHandlerImplementation;
import com.freenote.app.server.application.models.request.core.ResponseData;
import com.freenote.app.server.application.models.request.core.ResponseObject;
import com.freenote.app.server.connections.WebSocketConnection;
import com.freenote.app.server.frames.factory.FrameFactory;
import com.freenote.app.server.util.IOUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

@URIHandlerImplementation("/echo")
public class NewEchoHandlerImpl extends CommonHandlerImpl {
    private static final Logger log = LogManager.getLogger(NewEchoHandlerImpl.class);

    @Override
    public void onMessage(WebSocketConnection webSocketConnection, String message) throws IOException {
        log.info("Writing to output stream with message: {}", message);
        log.info("===========================================================================");
        webSocketConnection.setResponse(new ResponseObject<>(1, new EchoResponseData(message)));
    }

    @Override
    public void sendResponse(WebSocketConnection webSocketConnection) throws IOException {
        var message = ((EchoResponseData) (webSocketConnection.getResponse()
                .getResponseData(EchoResponseData.class))).getEchoMessage();
        IOUtils.writeOutPut(webSocketConnection.getOutputStream(), FrameFactory.SERVER.createTextFrame(message));
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
