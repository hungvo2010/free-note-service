package com.freenote.app.server.model.ws;

import com.freenote.app.server.frames.ws.WebSocketFrame;
import com.freenote.app.server.parser.FullFrameParser;
import com.freenote.app.server.util.IOUtils;

import java.io.IOException;
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
    public byte[] read() throws IOException {
        return new FullFrameParser().getRawBytes(socket.getInputStream());
    }

    @Override
    public int read(byte[] data) {
        return 0;
    }

    @Override
    public void write(byte[] data) throws IOException {
         IOUtils.writeOutPut(socket.getOutputStream(), data);
    }

    @Override
    public void write(WebSocketFrame frame) {

    }
}
