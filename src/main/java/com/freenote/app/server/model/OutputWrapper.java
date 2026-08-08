package com.freenote.app.server.model;

import com.freenote.app.server.model.ws.NetworkRequestData;
import com.freenote.app.server.model.ws.NIONetworkRequestData;
import com.freenote.app.server.model.ws.BlockingNetworkRequestData;

import java.io.IOException;
import java.io.OutputStream;

public record OutputWrapper(OutputStream outputStream) {

    /**
     * Creates an OutputWrapper backed by a NetworkRequestData.
     * Writes are delegated to {@code networkData.write(byte[])}.
     * Eliminates the need for {@code channel.socket().getOutputStream()}.
     */
    public static OutputWrapper from(NetworkRequestData networkData) {
        if (networkData instanceof NIONetworkRequestData nio) {
            return new OutputWrapper(nio.getOutputStream());
        }
        if (networkData instanceof BlockingNetworkRequestData blocking) {
            return new OutputWrapper(blocking.getOutputStream());
        }
        // Generic fallback for any NetworkRequestData implementation
        return new OutputWrapper(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                networkData.write(new byte[]{(byte) b});
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                byte[] chunk = new byte[len];
                System.arraycopy(b, off, chunk, 0, len);
                networkData.write(chunk);
            }
        });
    }
}
