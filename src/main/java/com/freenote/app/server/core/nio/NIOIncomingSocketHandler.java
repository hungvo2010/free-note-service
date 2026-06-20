package com.freenote.app.server.core.nio;

import com.freenote.app.server.auth.AcceptHandshakeHandler;
import com.freenote.app.server.auth.impl.AcceptHandshakeImpl;
import com.freenote.app.server.core.connection.IncomingConnectionHandler;
import com.freenote.app.server.core.context.ConnectionContext;
import com.freenote.app.server.core.context.ReadableContext;
import com.freenote.app.server.exceptions.AcceptConnectionException;
import com.freenote.app.server.exceptions.ConnectionException;
import com.freenote.app.server.model.OutputWrapper;
import com.freenote.app.server.model.http.HttpUpgradeRequest;
import com.freenote.app.server.model.ws.NIONetworkRequestData;
import com.freenote.app.server.model.ws.NetworkRequestData;
import com.freenote.app.server.parser.HttpParser;
import com.freenote.app.server.parser.impl.HttpParserImpl;
import com.freenote.app.server.routes.URIEndpointHandler;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import otel.metrics.MetricUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import static generated.URIHandlerRegistry.getInstanceByURI;
import static otel.SampleGlobalOpenTelemetry.getSampleGlobalTelemetry;

// TODO: confusing name
public class NIOIncomingSocketHandler implements ModernIncomingConnectionHandler, IncomingConnectionHandler {
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
    public void handleInComingMessage(ReadableContext context, HttpUpgradeRequest upgradeRequest) throws IOException {
        log.info("Subsequent read from {}", context.getRemoteAddress());
        var newMessageSpan = buildMessageSpan(context, upgradeRequest);
        if (emptyReadFromChannel(context.getChannel(), context.getByteBuffer())) return;
        routeToHandler(context.getChannel(), context.getByteBuffer(), upgradeRequest);
        newMessageSpan.end();
    }

    private Span buildMessageSpan(ReadableContext context, HttpUpgradeRequest upgradeRequest) {
        var parentSpan = this.getParentSpan(context);
        Context parentContext = Context.current().with(parentSpan);
        return getSampleGlobalTelemetry().getTracer()
                .spanBuilder("ws.message")
                .setParent(parentContext)
                .setAttribute("origin", upgradeRequest.getOrigin())
                .setAttribute("path", upgradeRequest.getPath())
                .setAttribute("uri", upgradeRequest.getUri())
                .startSpan();
    }

    private Span getParentSpan(ReadableContext context) {
        return context.getTracingContext().getSpan();
    }

    @Override
    public HttpUpgradeRequest handShake(ReadableContext context) throws IOException {
        if (emptyReadFromChannel(context.getChannel(), context.getByteBuffer())) return null;

        var upgradeRequest = parseUpgradeRequest(context.getByteBuffer());
        performHandshake(context.getChannel(), upgradeRequest);

        return upgradeRequest;
    }

    private boolean emptyReadFromChannel(SocketChannel channel, ByteBuffer byteBuffer) throws IOException {
        byteBuffer.clear();
        if (channel.read(byteBuffer) == -1) {
            if (channel.isOpen()) {
                channel.close();
                MetricUtils.decrementConcurrentUsers();
            }
            return true;
        }
        return false;
    }

    private HttpUpgradeRequest parseUpgradeRequest(ByteBuffer byteBuffer) {
        return this.httpParser.parse(byteBuffer);
    }

    private void performHandshake(SocketChannel channel, HttpUpgradeRequest request) throws IOException {
        log.info("Performing handshake for: {}", request);
        var handShakeResp = this.handshakeHandler.process(request);
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
        var inputWrapper = builtNetworkRequest(channel, byteBuffer);
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

    private NetworkRequestData builtNetworkRequest(SocketChannel channel, ByteBuffer byteBuffer) {
        return new NIONetworkRequestData(channel, byteBuffer);
    }

    @Override
    public void handle(ConnectionContext context) throws ConnectionException {

    }
}
