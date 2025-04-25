package com.freenote.app.server.handler.impl;

import com.freenote.app.server.auth.AcceptHandshakeHandler;
import com.freenote.app.server.auth.impl.AcceptHandshakeImpl;
import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.http.HttpParser;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

public class MockHandler implements URIHandler {
    private HttpParser parser = new HttpParser();
    private AcceptHandshakeHandler handshakeHandler = new AcceptHandshakeImpl();

    @Override
    public void handle(InputStream inputStream, OutputStream outputStream) {
        try (var out = new PrintWriter(outputStream, true);) {
            out.println(handshakeHandler.handle(parser.parse(inputStream)).toString());
        }
    }
}
