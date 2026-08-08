package com.freenote.app.server.core.legacy.socket;

import com.freenote.app.server.core.config.ServerSocketConfig;
import com.freenote.app.server.exceptions.SocketCreationException;

import java.net.ServerSocket;

public interface ServerSocketProvider {
    ServerSocket createServerSocket(ServerSocketConfig config) throws SocketCreationException;
}
