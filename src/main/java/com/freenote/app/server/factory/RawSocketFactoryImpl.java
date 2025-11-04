package com.freenote.app.server.factory;

import java.net.ServerSocket;

public class RawSocketFactoryImpl implements ServerSocketFactory {
    @Override
    public ServerSocket createServerSocket(int port) throws Exception {
        return new ServerSocket(port);
    }
}
