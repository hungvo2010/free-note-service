package com.freenote.app.server.model.ws;

import com.freenote.app.server.frames.ws.WebSocketFrame;
import com.freenote.app.server.parser.FullFrameParser;
import com.freenote.app.server.util.IOUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class BlockingNetworkRequestData implements NetworkRequestData {
    private final Socket socket;

    public BlockingNetworkRequestData(Socket socket) {
        this.socket = socket;
    }

    @Override
    public WebSocketFrame buildRequestFrame() {
        return null;
    }

    @Override
    public WebSocketFrame buildResponseFrame() {
        return null;
    }

    @Override
    public byte[] readFrameBytes() throws IOException {
        return new FullFrameParser().getRawBytes(socket.getInputStream());
    }

    @Override
    public int read(byte[] data) throws IOException {
        return this.socket.getInputStream().read(data);
    }

    @Override
    public void write(byte[] data) throws IOException {
        IOUtils.writeOutPut(socket.getOutputStream(), data);
    }

    @Override
    public byte[] read() throws IOException {
        var bytes = new byte[8192];
        int count = this.read(bytes);
        if (count == -1) {
            return new byte[0];
        }
        return java.util.Arrays.copyOf(bytes, count);
    }

    @Override
    public void close() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    @Override
    public boolean isClosed() {
        return socket != null && socket.isClosed();
    }

    @Override
    public Object getRemoteAddress() {
        if (socket != null) {
            return socket.getRemoteSocketAddress();
        }
        return null;
    }

    // TODO: break encapsulation
    public OutputStream getOutputStream() {
        try {
            return socket.getOutputStream();
        } catch (IOException e) {
            throw new RuntimeException("Failed to get output stream from socket", e);
        }
    }
}
