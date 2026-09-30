package com.freenote.app.server.core.nio;

import com.freenote.app.server.core.connection.PerClientConnectionHandler;
import com.freenote.app.server.core.event.ConnectionEvent;
import com.freenote.app.server.core.nio.state.ConnectionState;
import com.freenote.app.server.core.nio.state.HandShakeState;
import com.freenote.app.server.model.ws.NetworkRequestData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NIOConnectionPipes {

    private static final Logger log = LogManager.getLogger(NIOConnectionPipes.class);

    private final Map<NetworkRequestData, ConnectionState> connectionStates = new ConcurrentHashMap<>();
    private final PerClientConnectionHandler perClientHandler;

    public NIOConnectionPipes(PerClientConnectionHandler handler) {
        this.perClientHandler = handler;
    }

    public boolean process(NetworkRequestData networkData) {
        ConnectionState state = connectionStates.computeIfAbsent(networkData, k -> new HandShakeState());
        try {
            var connectionEvent = buildConnectionEvent(networkData, state);
            var upgradeRequest = perClientHandler.handle(connectionEvent);

            ConnectionState nextState = state.transition(connectionEvent, upgradeRequest);
            if (nextState == null) {
                connectionStates.remove(networkData);
                closeQuietly(networkData);
                return false;
            }
            if (nextState != state) {
                connectionStates.put(networkData, nextState);
            }
            return true;
        } catch (Exception e) {
            log.error("Error processing connection: {}", e.getCause(), e);
            connectionStates.remove(networkData);
            closeQuietly(networkData);
            return false;
        }
    }

    private ConnectionEvent buildConnectionEvent(NetworkRequestData networkData, ConnectionState state) {
        return ConnectionEvent.builder()
                .networkRequestData(networkData)
                .state(state)
                .build();
    }

    private void closeQuietly(NetworkRequestData networkData) {
        try {
            networkData.close();
        } catch (IOException ex) {
            log.error("Error closing connection", ex);
        }
    }

    public void disconnect(NetworkRequestData networkData) {
        connectionStates.remove(networkData);
    }
}
