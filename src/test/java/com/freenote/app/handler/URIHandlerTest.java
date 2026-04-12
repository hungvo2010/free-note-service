package com.freenote.app.handler;

import com.freenote.app.server.messages.WebSocketFrame;
import com.freenote.app.server.frames.factory.ClientFrameFactory;
import com.freenote.app.server.handler.URIEndpointHandler;
import com.freenote.app.server.handler.impl.NewEchoEndpointHandlerImpl;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.model.OutputWrapper;
import com.freenote.app.server.util.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class URIHandlerTest {
    static ClientFrameFactory clientFrameFactory = null;

    @BeforeAll
    static void setup() {
        clientFrameFactory = new ClientFrameFactory();
    }

    private final URIEndpointHandler mockURIHandler = new NewEchoEndpointHandlerImpl();

    @Test
    void givenWebSocketFrameInputStream_whenHandled_thenEchoesToOutputStream() throws IOException {
        WebSocketFrame textFrame = clientFrameFactory.createTextFrame("Hello World");
        var byteArrayOutputStream = new ByteArrayOutputStream();
        IOUtils.writeOutPut(byteArrayOutputStream, textFrame);
        var bytes = byteArrayOutputStream.toByteArray();
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mockURIHandler.handle(new InputWrapper(), new OutputWrapper(out));
        String result = new String(Arrays.copyOfRange(out.toByteArray(), 2, 2 + "Hello World".length())); // Skip the first two bytes which are the frame type and length
        assertEquals("Hello World", result);
    }

    @Test
    void givenEndOfInputStream_whenHandled_thenReturnsFalse() throws IOException {
        InputStream in = mock(InputStream.class);
        when(in.read(any(byte[].class))).thenReturn(-1);
        when(in.read()).thenReturn(-1);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        var result = mockURIHandler.handle(new InputWrapper(), new OutputWrapper(out));
        assertFalse(result, "Expected handle to return false on end of input stream");
    }
}
