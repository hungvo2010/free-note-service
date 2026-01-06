package com.freenote.app.server.core;


import java.io.IOException;
import java.net.Socket;

public interface IncomingConnectionHandler {
    void handle(Socket socket) throws IOException;
}

