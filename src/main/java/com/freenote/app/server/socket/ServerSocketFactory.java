package com.freenote.app.server.socket;

import java.net.ServerSocket;

public interface ServerSocketFactory {
    ServerSocket createServerSocket(int port) throws Exception;
}
