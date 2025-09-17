package com.freenote.app.server.example;


import java.io.IOException;
import java.net.Socket;

public interface ConnectionHandler {
    void handle(Socket socket) throws IOException;
}

