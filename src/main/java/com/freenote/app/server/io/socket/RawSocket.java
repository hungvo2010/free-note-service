package com.freenote.app.server.io.socket;

import com.freenote.app.server.core.config.ServerSocketConfig;

import java.io.IOException;
import java.net.ServerSocket;

public class RawSocket implements ServerSocketFactory {
    @Override
    public ServerSocket createServerSocket(ServerSocketConfig config) throws IOException {
        return new ServerSocket(config.port());
    }
}