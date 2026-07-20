package com.freenote.app.server.core.nio;

import com.freenote.app.server.core.connection.IncomingConnectionHandler;
import com.freenote.app.server.core.context.ConnectionContext;
import com.freenote.app.server.core.context.ReadableContext;
import com.freenote.app.server.core.nio.state.ConnectionState;
import com.freenote.app.server.core.nio.state.HandShakeState;
import com.freenote.app.server.core.nio.state.MessageState;
import com.freenote.app.server.model.ws.NetworkRequestData;
import io.opentelemetry.api.trace.Span;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import otel.sdk.context.TracingContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static otel.SampleGlobalOpenTelemetry.getSampleGlobalTelemetry;

public class ConnectionPipeline {

    private static final Logger log = LogManager.getLogger(ConnectionPipeline.class);

    private final Map<NetworkRequestData, ConnectionState> connectionStates = new ConcurrentHashMap<>();
    private final IncomingConnectionHandler connectionHandler;

    public ConnectionPipeline(IncomingConnectionHandler handler) {
        this.connectionHandler = handler;
    }

    public boolean process(NetworkRequestData networkData) {
        ConnectionState state = connectionStates.computeIfAbsent(networkData, k -> new HandShakeState());
        try {
            var connectionContext = buildConnectionContext(networkData, state);
            connectionHandler.handle(connectionContext);

            ConnectionState nextState = state.transition(connectionContext);
            if (nextState == null) {
                connectionStates.remove(networkData);
                networkData.close();
                return false;
            }
            if (nextState != state) {
                connectionStates.put(networkData, nextState);
            }
            return true;
        } catch (Exception e) {
            log.error("Error processing connection: {}", e.getCause(), e);
            connectionStates.remove(networkData);
            try {
                networkData.close();
            } catch (Exception ignored) {
            }
            return false;
        }
    }

    private ConnectionContext buildConnectionContext(NetworkRequestData networkData, ConnectionState state) {
        Span span = buildSpan(state);
        TracingContext tracingContext = TracingContext.builder()
                .span(span)
                .build();
        ReadableContext readableContext = ReadableContext.builder()
                .tracingContext(tracingContext)
                .httpUpgradeRequest(state instanceof MessageState ps ? ps.getRequest() : null)
                .build();

        return ConnectionContext.builder()
                .networkRequestData(networkData)
                .readableContext(readableContext)
                .build();
    }

    public void disconnect(NetworkRequestData networkData) {
        connectionStates.remove(networkData);
    }

    private Span buildSpan(ConnectionState state) {
        String spanName = state instanceof HandShakeState ? "websocket.handshake" : "websocket.message";
        return getSampleGlobalTelemetry().getTracer().spanBuilder(spanName)
                .setAttribute("server.address", "localhost")
                .setAttribute("server.port", -1)
                .setAttribute("network.transport", "tcp")
                .setAttribute("app.websocket.state", state.getClass().getSimpleName())
                .startSpan();
    }
}
