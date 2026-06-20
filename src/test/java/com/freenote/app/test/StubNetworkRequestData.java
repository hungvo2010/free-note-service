package com.freenote.app.test;

import com.freenote.app.server.frames.ws.WebSocketFrame;
import com.freenote.app.server.model.ws.NetworkRequestData;
import com.freenote.app.server.parser.FullFrameParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Test stub for NetworkRequestData that reads from an InputStream for readFrameBytes/read
 * and captures writes into a byte array.
 */
public class StubNetworkRequestData implements NetworkRequestData {
    private final InputStream inputStream;
    private final ByteArrayOutputStream writeCapture = new ByteArrayOutputStream();
    private boolean closed;

    public StubNetworkRequestData(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public byte[] readFrameBytes() throws IOException {
        return new FullFrameParser().getRawBytes(inputStream);
    }

    @Override
    public int read(byte[] data) throws IOException {
        return inputStream.read(data);
    }

    @Override
    public byte[] read() throws IOException {
        var bytes = new byte[8192];
        int count = inputStream.read(bytes);
        if (count == -1) return new byte[0];
        return Arrays.copyOf(bytes, count);
    }

    @Override
    public void write(byte[] data) throws IOException {
        if (data != null) {
            writeCapture.write(data);
        }
    }

    public byte[] getWrittenBytes() {
        return writeCapture.toByteArray();
    }

    @Override
    public WebSocketFrame buildRequestFrame() { return null; }

    @Override
    public WebSocketFrame buildResponseFrame() { return null; }

    @Override
    public void write(WebSocketFrame frame) { }

    @Override
    public void close() throws IOException {
        closed = true;
    }

    @Override
    public boolean isClosed() { return closed; }

    @Override
    public Object getRemoteAddress() { return null; }
}
