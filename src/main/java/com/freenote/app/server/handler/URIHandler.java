package com.freenote.app.server.handler;

import java.io.InputStream;
import java.io.OutputStream;

public interface URIHandler {
    void handle(InputStream inputStream, OutputStream outputStream);
}
