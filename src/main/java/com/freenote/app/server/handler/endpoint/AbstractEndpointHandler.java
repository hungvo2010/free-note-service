package com.freenote.app.server.handler.endpoint;


import com.freenote.app.server.core.connection.WebSocketConnection;
import com.freenote.app.server.exceptions.ClientDisconnectException;
import com.freenote.app.server.exceptions.ConnectionException;
import com.freenote.app.server.exceptions.MessageParsingException;
import com.freenote.app.server.frames.handler.WebSocketFrameHandler;
import com.freenote.app.server.handler.URIEndpointHandler;
import com.freenote.app.server.handler.frames.WebSocketFrameDispatcher;
import com.freenote.app.server.messages.IncomingMessage;
import com.freenote.app.server.messages.ws.WebSocketFrame;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.model.OutputWrapper;
import com.freenote.app.server.model.http.HttpUpgradeRequest;
import com.freenote.app.server.parser.MessageParser;
import com.freenote.app.server.parser.WebSocketFrameParser;
import com.freenote.app.server.parser.impl.InputStreamFrameParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import otel.metrics.MetricUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public abstract class AbstractEndpointHandler implements URIEndpointHandler, WebSocketFrameHandler {
    private static final Logger log = LogManager.getLogger(AbstractEndpointHandler.class);
    private final WebSocketFrameParser frameParser;
    private final MessageParser messageParser = new MessageParser();

    public AbstractEndpointHandler() {
        this.frameParser = new InputStreamFrameParser();
    }

    protected AbstractEndpointHandler(WebSocketFrameParser frameParser) {
        this.frameParser = frameParser;
    }

    @Override
    public boolean handle(InputWrapper inputWrapper, OutputWrapper outputWrapper) {
        MetricUtils.incrementInFlightRequests();
        try {
            MetricUtils.getLatencyMetric().time(() -> this.serveConnection(inputWrapper, outputWrapper));
            return true;
        } catch (ConnectionException e) {
            log.error("Error handling input stream", e);
            return false;
        } finally {
            MetricUtils.decrementInFlightRequests();
        }
    }

    private void serveConnection(InputWrapper inputWrapper, OutputWrapper outputWrapper) {
        try {
            WebSocketFrame wsFrame = parseFrame(inputWrapper);

            log.debug(wsFrame.toString());
            WebSocketConnection webSocketConnection = WebSocketConnection.from(inputWrapper, outputWrapper);

            dispatchAndRespond(webSocketConnection, wsFrame);
        } catch (IOException e) {
            log.error("Error handling frame", e);
            throw new ConnectionException("Error handling frame", e);
        }
    }

    private void dispatchAndRespond(WebSocketConnection webSocketConnection, WebSocketFrame wsFrame) throws IOException {
        WebSocketFrameDispatcher.dispatch(this, webSocketConnection, wsFrame);
        sendResponse(webSocketConnection);
    }

    private WebSocketFrame parseFrame(InputWrapper inputWrapper) throws IOException {
        return frameParser.parseFrame(inputWrapper);
    }

    protected void sendResponse(WebSocketConnection webSocketConnection) throws IOException {
        webSocketConnection.sendCurrentResponse();
    }

    @Override
    public boolean continuationHandler(List<WebSocketFrame> clientFrame, InputWrapper inputWrapper, OutputWrapper outputWrapper) {
        return false;
    }

    @Override
    public void onMessage(WebSocketConnection webSocketConnection, String message) {
        try {
            IncomingMessage request = messageParser.parse(message);
            request.handle(this, webSocketConnection);
        } catch (MessageParsingException e) {
            handleErrorMessage(e);
        }
    }

    private void handleErrorMessage(MessageParsingException e) {
        log.error("Failed to parse message", e);
    }

    public void onData(WebSocketConnection webSocketConnection, String message) {
        webSocketConnection.sendText(message);
    }

    @Override
    public void onMessage(WebSocketConnection webSocketConnection, ByteBuffer message) {
        onBinaryMessage(webSocketConnection, message);
    }

    protected void onBinaryMessage(WebSocketConnection webSocketConnection, ByteBuffer message) {
        log.info("Received binary message of length {}, sender: {}", message.remaining(), webSocketConnection.getRemoteAddress());
    }


    @Override
    public void onOpen(WebSocketConnection webSocketConnection, HttpUpgradeRequest handshake) {

    }

    @Override
    public void onClose(WebSocketConnection webSocketConnection, int code, String reason, boolean remote) {
        log.warn("Received CLOSE frame. No further processing.");
        throw new ClientDisconnectException("Client sent CLOSE frame");
    }

    @Override
    public void onError(WebSocketConnection webSocketConnection, Exception exception) {
        log.error("Received ERROR frame", exception);
    }

    @Override
    public void onPing(WebSocketConnection webSocketConnection, ByteBuffer payload) {
        onControl(webSocketConnection, payload);
    }

    public void onControl(WebSocketConnection webSocketConnection, ByteBuffer payload) {
    }

    @Override
    public void onPong(WebSocketConnection webSocketConnection, ByteBuffer payload) {
        onControl(webSocketConnection, payload);
    }

    @Override
    public void onContinue(WebSocketConnection webSocketConnection, ByteBuffer payload) {
    }
}
