package com.freenote.app.server.core.v2;

import com.freenote.app.server.auth.AcceptHandshakeHandler;
import com.freenote.app.server.auth.impl.AcceptHandshakeImpl;
import com.freenote.app.server.exceptions.AcceptConnectionException;
import com.freenote.app.server.handler.URIEndpointHandler;
import com.freenote.app.server.model.http.HttpUpgradeRequest;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.model.OutputWrapper;
import com.freenote.app.server.model.ws.CommonRequestObject;
import com.freenote.app.server.parser.HttpParser;
import com.freenote.app.server.parser.impl.HttpParserImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import static generated.URIHandlerRegistry.getInstanceByURI;

public class NIOIncomingSocketHandler implements IncomingConnectionHandlerV2 {
    private static final Logger log = LogManager.getLogger(NIOIncomingSocketHandler.class);
    private final AcceptHandshakeHandler handshakeHandler;
    private final HttpParser httpParser;

    public NIOIncomingSocketHandler(AcceptHandshakeHandler handshakeHandler, HttpParser httpParser) {
        this.handshakeHandler = handshakeHandler;
        this.httpParser = httpParser;
    }

    public NIOIncomingSocketHandler() {
        this(new AcceptHandshakeImpl(), new HttpParserImpl());
    }

    @Override
    public void handleInComingMessage(SocketChannel channel, ByteBuffer byteBuffer, HttpUpgradeRequest upgradeRequest) throws IOException {
        if (!readFromChannel(channel, byteBuffer)) return;
        
        routeToHandler(channel, byteBuffer, upgradeRequest);
    }

    @Override
    public HttpUpgradeRequest handShake(SocketChannel channel, ByteBuffer byteBuffer) throws IOException {
        if (!readFromChannel(channel, byteBuffer)) return null;

        var upgradeRequest = parseUpgradeRequest(byteBuffer);
        performHandshake(channel, upgradeRequest);
        
        return upgradeRequest;
    }

    private boolean readFromChannel(SocketChannel channel, ByteBuffer byteBuffer) throws IOException {
        byteBuffer.clear();
        if (channel.read(byteBuffer) == -1) {
            channel.close();
            return false;
        }
        return true;
    }

    private HttpUpgradeRequest parseUpgradeRequest(ByteBuffer byteBuffer) {
        return this.httpParser.parse(byteBuffer);
    }

    private void performHandshake(SocketChannel channel, HttpUpgradeRequest request) throws IOException {
        log.info("Performing handshake for: {}", request);
        var handShakeResp = this.handshakeHandler.handle(request);
        var outputBytes = handShakeResp.toString().getBytes(StandardCharsets.UTF_8);
        
        writeResponse(channel, outputBytes);
    }

    private void writeResponse(SocketChannel channel, byte[] data) throws IOException {
        ByteBuffer respBuffer = ByteBuffer.wrap(data);
        while (respBuffer.hasRemaining()) {
            channel.write(respBuffer);
        }
    }

    private void routeToHandler(SocketChannel channel, ByteBuffer byteBuffer, HttpUpgradeRequest upgradeRequest) throws IOException {
        var pathHandler = getPathHandler(upgradeRequest);
        var inputWrapper = buildInputWrapper(channel, upgradeRequest, byteBuffer);
        var outputWrapper = new OutputWrapper(channel.socket().getOutputStream());
        pathHandler.handle(inputWrapper, outputWrapper);
    }

    private URIEndpointHandler getPathHandler(HttpUpgradeRequest upgradeRequest) {
        var pathHandler = (URIEndpointHandler) (getInstanceByURI(upgradeRequest.getPath()));
        if (pathHandler == null) {
            log.warn("No handler found for URI: {}", upgradeRequest.getPath());
            throw new AcceptConnectionException("No handler for URI: " + upgradeRequest.getPath());
        }
        return pathHandler;
    }

    private InputWrapper buildInputWrapper(SocketChannel channel, HttpUpgradeRequest upgradeRequest, ByteBuffer byteBuffer) {
        var request = CommonRequestObject.builder()
                .origin(upgradeRequest.getOrigin())
                .socket(channel.socket())
                .build();
        return InputWrapper.builder()
                .socketChannel(channel)
                .channelBuffer(byteBuffer)
                .requestObject(request)
                .build();
    }
}
