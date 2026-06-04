package com.freenote.app.server.io.socket;

import com.freenote.app.server.core.config.ServerSocketConfig;

import java.net.ServerSocket;

public interface ServerSocketFactory {
    ServerSocket createServerSocket(ServerSocketConfig config) throws Exception;
}
