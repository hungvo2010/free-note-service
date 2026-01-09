package com.freedraw.models.core;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Closeable;
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

    @Override
    public void close() {
        this.open = false;
    }
}
