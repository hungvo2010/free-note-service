package com.freenote.app.server.socket;

import java.net.ServerSocket;

public class RawSocket implements ServerSocketFactory {
    @Override
    public ServerSocket createServerSocket(int port) throws Exception {
        return new ServerSocket(port);
    }
}
