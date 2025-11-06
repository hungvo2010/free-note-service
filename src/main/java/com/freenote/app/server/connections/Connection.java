package com.freenote.app.server.connections;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
public class Connection implements Closeable {
    private OutputStream outputStream;
    private boolean open = true;

    public Connection(OutputStream outputStream) {
        this.outputStream = outputStream;
        this.open = true;
    }

    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() throws IOException {
        this.open = false;
    }
}
