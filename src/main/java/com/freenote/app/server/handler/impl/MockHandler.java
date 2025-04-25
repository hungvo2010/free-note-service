package com.freenote.app.server.handler.impl;

import com.freenote.app.server.auth.AcceptHandshakeHandler;
import com.freenote.app.server.auth.impl.AcceptHandshakeImpl;
import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.parser.impl.HttpParserImpl;

import java.io.InputStream;
import java.io.OutputStream;

public class MockHandler implements URIHandler {
    private final HttpParserImpl parser = new HttpParserImpl();
    private final AcceptHandshakeHandler handshakeHandler = new AcceptHandshakeImpl();

    @Override
    public void handle(InputStream inputStream, OutputStream outputStream) {

    }
}
