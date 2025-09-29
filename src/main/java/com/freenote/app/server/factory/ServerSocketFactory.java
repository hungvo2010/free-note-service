package com.freenote.app.server.factory;

import java.net.ServerSocket;

public interface ServerSocketFactory {
    ServerSocket createServerSocket(int port) throws Exception;
}
