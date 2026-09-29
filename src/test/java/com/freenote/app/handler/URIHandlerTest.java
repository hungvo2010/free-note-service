package com.freenote.app.handler;

import com.freenote.app.server.frames.ws.WebSocketFrame;
import com.freenote.app.server.frames.factory.ClientFrameFactory;
import com.freenote.app.server.endpoints.URIEndpointHandler;
import com.freenote.app.server.endpoints.NewEchoEndpoint;
import com.freenote.app.server.model.NetworkResponseData;
import com.freenote.app.server.model.ws.NetworkRequestData;
import com.freenote.app.server.util.IOUtils;
import com.freenote.app.test.StubNetworkRequestData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    private final URIEndpointHandler mockURIHandler = new NewEchoEndpoint();

    @Test
    void givenWebSocketFrameInputStream_whenHandled_thenEchoesToOutputStream() throws IOException {
        WebSocketFrame textFrame = clientFrameFactory.createTextFrame("Hello World");
        var byteArrayOutputStream = new ByteArrayOutputStream();
        IOUtils.writeOutPut(byteArrayOutputStream, textFrame);
        var bytes = byteArrayOutputStream.toByteArray();
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mockURIHandler.handle(new StubNetworkRequestData(in), new NetworkResponseData(out));
        String result = new String(Arrays.copyOfRange(out.toByteArray(), 2, 2 + "Hello World".length())); // Skip the first two bytes which are the frame type and length
        assertEquals("Hello World", result);
    }

    @Test
    void givenEndOfInputStream_whenHandled_thenReturnsFalse() throws IOException {
        NetworkRequestData networkData = mock(NetworkRequestData.class);
        when(networkData.readFrameBytes()).thenReturn(new byte[0]);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        var result = mockURIHandler.handle(networkData, new NetworkResponseData(out));
        assertFalse(result, "Expected handle to return false on end of input stream");
    }
}
