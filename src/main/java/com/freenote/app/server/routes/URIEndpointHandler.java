package com.freenote.app.server.routes;

import com.freenote.app.server.frames.ws.WebSocketFrame;
import com.freenote.app.server.model.OutputWrapper;
import com.freenote.app.server.model.ws.NetworkRequestData;

import java.io.IOException;
import java.util.List;

public interface URIEndpointHandler {
    boolean handle(NetworkRequestData networkRequestData, OutputWrapper outputWrapper) throws IOException; // TODO: check why boolean return type

    boolean continuationHandler(List<WebSocketFrame> clientFrame, NetworkRequestData networkRequestData, OutputWrapper outputWrapper) throws IOException;
}