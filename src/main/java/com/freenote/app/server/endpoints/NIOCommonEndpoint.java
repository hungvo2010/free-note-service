package com.freenote.app.server.endpoints;

import com.freenote.app.server.model.connection.WebSocketConnection;
import com.freenote.app.server.parser.impl.ByteBufferFrameParserImpl;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

@Log4j2
public class NIOCommonEndpoint extends AbstractEndpointHandler {

    public NIOCommonEndpoint() {
        super(new ByteBufferFrameParserImpl());
    }

//    @Override
//    protected void sendResponse(WebSocketConnection webSocketConnection) throws IOException {
//        var networkRequestData = webSocketConnection.getNetworkRequestData();
//        byte[] dataToWrite = getDataToWrite(webSocketConnection);
//        networkRequestData.write(dataToWrite);
//
//    }

    private byte[] getDataToWrite(WebSocketConnection webSocketConnection) throws IOException {
        return webSocketConnection.getPayloadBytes();
    }
}
