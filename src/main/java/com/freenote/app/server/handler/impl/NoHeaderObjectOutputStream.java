package com.freenote.app.server.handler.impl;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class NoHeaderObjectOutputStream extends ObjectOutputStream {
    private final OutputStream out;

    public NoHeaderObjectOutputStream(OutputStream out) throws IOException {
        super();
        this.out = out;
    }

    @Override
    protected void writeStreamHeader() {
        // Do nothing to skip the header
    }

    @Override
    protected void writeObjectOverride(Object obj) throws IOException {
        if (obj instanceof Externalizable externalizable) {
            externalizable.writeExternal(this);
        }
    }

    @Override
    public void write(byte[] buf) throws IOException {
        this.out.write(buf);
    }

    @Override
    public void flush() throws IOException {
        this.out.flush();
    }
}

