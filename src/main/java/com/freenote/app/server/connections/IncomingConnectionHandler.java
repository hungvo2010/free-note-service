package com.freenote.app.server.connections;


import java.io.IOException;
import java.net.Socket;

public interface IncomingConnectionHandler {
    void handle(Socket socket) throws IOException;
}

