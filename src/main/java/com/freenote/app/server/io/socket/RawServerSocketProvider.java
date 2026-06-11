package com.freenote.app.server.io.socket;

import com.freenote.app.server.core.config.ServerSocketConfig;
import com.freenote.app.server.exceptions.SocketCreationException;

import java.io.IOException;
import java.net.ServerSocket;

public class RawServerSocketProvider implements ServerSocketProvider {
    @Override
    public ServerSocket createServerSocket(ServerSocketConfig config) throws SocketCreationException {
        try {
            return new ServerSocket(config.port());
        } catch (IOException e) {
            throw new SocketCreationException("Failed to create server socket", e);
        }
    }
}