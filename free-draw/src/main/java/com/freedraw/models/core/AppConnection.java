package com.freedraw.models.core;

import com.freenote.app.server.model.connection.WebSocketConnection;
import com.freenote.app.server.frames.ws.WebSocketFrame;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

@AllArgsConstructor
@Getter
public class AppConnection implements Closeable {
    private WebSocketConnection socketConnection;
    private OutputStream outputStream;
    private boolean open = true;
    private String senderId;

    public AppConnection(WebSocketConnection webSocketConnection, String senderId) {
        this.socketConnection = webSocketConnection;
        this.senderId = senderId;
    }

    public AppConnection(WebSocketConnection webSocketConnection) {
        this.socketConnection = webSocketConnection;
    }

    @Override
    public void close() {
        this.open = false;
    }

    public void writeData(WebSocketFrame data) throws IOException {
        this.socketConnection.setResponseFrame(data);
        this.socketConnection.sendCurrentResponse();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AppConnection other)) {
            return false;
        }
        return this.socketConnection.getNetworkRequestData()
                == other.getSocketConnection().getNetworkRequestData();
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this.socketConnection.getNetworkRequestData());
    }
}
