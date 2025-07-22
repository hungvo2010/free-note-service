package com.freenote.app.server.frames;

import com.freenote.app.server.frames.base.DataFrame;

public class ClientFrame extends DataFrame {
    public ClientFrame(byte[] payload) {
        super(payload);
    }
}
