package com.freenote.app.server.handler.impl;

import com.freenote.annotations.URIHandlerImplementation;
import com.freenote.app.server.handler.URIHandler;

import java.io.InputStream;
import java.io.OutputStream;

@URIHandlerImplementation("/general")
public class GeneralHandler implements URIHandler {
    @Override
    public boolean handle(InputStream inputStream, OutputStream outputStream) {
        return false;
    }
}
