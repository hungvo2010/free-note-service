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

    @Override
    public void writeByte(int val) throws IOException {
        this.out.write(val);
    }

    @Override
    public void writeShort(int val) throws IOException {
        this.out.write((val >>> 8) & 0xFF);
        this.out.write(val & 0xFF);
    }

    @Override
    public void writeLong(long val) throws IOException {
        this.out.write((int) (val >>> 56) & 0xFF);
        this.out.write((int) (val >>> 48) & 0xFF);
        this.out.write((int) (val >>> 40) & 0xFF);
        this.out.write((int) (val >>> 32) & 0xFF);
        this.out.write((int) (val >>> 24) & 0xFF);
        this.out.write((int) (val >>> 16) & 0xFF);
        this.out.write((int) (val >>> 8) & 0xFF);
        this.out.write((int) val & 0xFF);
    }
}

