package com.freenote.app.handler;

import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.handler.impl.MockHandler;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class URIHandlerTest {
    private final URIHandler httpParser = new MockHandler();

    @Test
    public void givenInputStreamThenWriteToOutputStream() throws IOException {
        InputStream in = new ByteArrayInputStream("hello\nworld\n".getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        httpParser.handle(in, out);
        String result = out.toString();
        assertEquals("hello\nworld\n", result);
    }
}
