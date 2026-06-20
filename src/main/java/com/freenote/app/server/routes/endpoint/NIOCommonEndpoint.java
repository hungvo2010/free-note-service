package com.freenote.app.server.routes.endpoint;

import com.freenote.app.server.core.connection.WebSocketConnection;
import com.freenote.app.server.parser.ByteBufferFrameParserImpl;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

@Log4j2
public class NIOCommonEndpoint extends AbstractEndpointHandler {

    public NIOCommonEndpoint() {
        super(new ByteBufferFrameParserImpl());
    }

    @Override
    protected void sendResponse(WebSocketConnection webSocketConnection) throws IOException {
        var networkRequestData = webSocketConnection.getSession().getNetworkRequestData();
        byte[] dataToWrite = getDataToWrite(webSocketConnection);
        networkRequestData.write(dataToWrite);
    }

    private byte[] getDataToWrite(WebSocketConnection webSocketConnection) throws IOException {
        return webSocketConnection.getPayloadBytes();
    }
}
