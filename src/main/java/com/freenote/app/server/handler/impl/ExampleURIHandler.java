package com.freenote.app.server.handler.impl;

import com.freenote.annotations.URIHandlerImplementation;
import com.freenote.app.server.handler.URIHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@URIHandlerImplementation("/update")
public class ExampleURIHandler implements URIHandler {
    @Override
    public boolean handle(InputStream inputStream, OutputStream outputStream) {
        if (inputStream == null || outputStream == null) throw new NullPointerException();

        try {
            byte[] data = inputStream.readAllBytes();
            if (data.length == 0) return false;

            String input = new String(data);
            if (!input.matches("[a-zA-Z]+")) return false;

            outputStream.write(input.toUpperCase().getBytes());
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
