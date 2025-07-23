package com.freenote.app.handler;

import com.freenote.app.server.frames.TextFrame;
import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.handler.impl.EchoHandler;
import io.NoHeaderObjectOutputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class URIHandlerTest {
    private final URIHandler mockURIHandler = new EchoHandler();

    @Test
    void givenInputStreamThenWriteToOutputStream() throws IOException {
        TextFrame textFrame = TextFrame.createClientFrame("Hello World".getBytes());
        var byteArrayOutputStream = new ByteArrayOutputStream();
        textFrame.writeExternal(new NoHeaderObjectOutputStream(byteArrayOutputStream));
        byteArrayOutputStream.flush();
        var bytes = byteArrayOutputStream.toByteArray();
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mockURIHandler.handle(in, out);
        String result = new String(Arrays.copyOfRange(out.toByteArray(), 2, 2 + "Hello World".length())); // Ski    p the first two bytes which are the frame type and length
        assertEquals("Hello World", result);
    }

    @Test
    void givenEndOfInputStream_ThenReturn() throws IOException {
        InputStream in = mock(InputStream.class);
        when(in.read()).thenReturn(-1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        var result = mockURIHandler.handle(in, out);
        assertTrue(result, "Expected handle to return false on end of input stream");
    }
}
