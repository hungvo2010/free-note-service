package com.freenote.app.server.core.v2;

import com.freenote.app.server.auth.AcceptHandshakeHandler;
import com.freenote.app.server.auth.impl.AcceptHandshakeImpl;
import com.freenote.app.server.exceptions.AcceptConnectionException;
import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.http.HttpUpgradeRequest;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.model.OutputWrapper;
import com.freenote.app.server.model.ws.CommonRequestObject;
import com.freenote.app.server.parser.HttpParser;
import com.freenote.app.server.parser.impl.HttpParserImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class NIOIncomingConnectionHandlerImpl implements IncomingConnectionHandlerV2 {
    private static final Logger log = LogManager.getLogger(NIOIncomingConnectionHandlerImpl.class);
    private final AcceptHandshakeHandler handshakeHandler;
    private final HttpParser httpParser;

    public NIOIncomingConnectionHandlerImpl() {
        this.handshakeHandler = new AcceptHandshakeImpl();
        this.httpParser = new HttpParserImpl();
    }

    @Override
    public void handle(SocketChannel channel) throws IOException {
        channel.configureBlocking(false);
        var byteBuffer = ByteBuffer.allocateDirect(2048);

        var upgradeRequest = this.httpParser.parse(byteBuffer);

        log.info("Received request: {}\n", upgradeRequest);
        writeHandshakeResponse(upgradeRequest, channel.socket().getOutputStream());

        var inputWrapper = buildInputWrapper(channel, upgradeRequest, byteBuffer);
        var pathHandler = getPathHandler(upgradeRequest);

        var outputWrapper = new OutputWrapper(channel.socket().getOutputStream());
        while (channel.isConnected()) {
            pathHandler.handle(inputWrapper, outputWrapper);
        }
    }

    private URIHandler getPathHandler(HttpUpgradeRequest upgradeRequest) {
        return null;
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

    private void writeHandshakeResponse(HttpUpgradeRequest request, OutputStream output) throws IOException {
        var handShakeResp = this.handshakeHandler.handle(request);
        output.write(handShakeResp.toString().getBytes(StandardCharsets.UTF_8));
        output.flush();
        if (!handShakeResp.getStatusCode().equals("101")) {
            throw new AcceptConnectionException("Handshake failed, connection not accepted");
        }
    }
}
