package com.freenote.app.server.model.ws;

import java.io.IOException;
import java.io.OutputStream;

public record NetworkResponseData(OutputStream outputStream) {
    public static NetworkResponseData from(NetworkRequestData networkData) {
        return new NetworkResponseData(new OutputStream() {
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
