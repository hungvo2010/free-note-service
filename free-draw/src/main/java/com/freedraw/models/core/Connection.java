package com.freedraw.models.core;

import com.freenote.app.server.core.connection.WebSocketConnection;
import com.freenote.app.server.messages.ws.WebSocketFrame;
import com.freenote.app.server.util.IOUtils;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

@AllArgsConstructor
@EqualsAndHashCode
@Getter
public class Connection implements Closeable {
    private OutputStream outputStream;
    private boolean open = true;

    public Connection(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public static Connection from(WebSocketConnection webSocketConnection) {
        return new Connection(webSocketConnection.getOutputStream());
    }

    @Override
    public void close() {
        this.open = false;
    }

    public void writeData(WebSocketFrame data) throws IOException {
        IOUtils.writeOutPut(this.outputStream, data);
    }
}
