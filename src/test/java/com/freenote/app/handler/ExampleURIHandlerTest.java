package com.freenote.app.handler;

import com.freenote.app.server.routes.URIEndpointHandler;
import com.freenote.app.server.routes.endpoint.FragmentedEndpoint;
import com.freenote.app.server.model.OutputWrapper;
import com.freenote.app.server.model.ws.NetworkRequestData;
import com.freenote.app.test.StubNetworkRequestData;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExampleURIHandlerTest {
    @Test
    void shouldReturnFalseWhenInputIsEmpty() throws IOException {
        InputStream input = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        URIEndpointHandler handler = new FragmentedEndpoint();

        boolean result = handler.handle(new StubNetworkRequestData(input), new OutputWrapper(output));

        assertFalse(result);
        assertEquals("", output.toString());
    }

    @Test
    void shouldThrowWhenStreamsAreNull() throws IOException {
        URIEndpointHandler handler = new FragmentedEndpoint();

        // null NetworkRequestData → NPE when calling read()
        assertThrows(NullPointerException.class,
                () -> handler.handle(null, new OutputWrapper(new ByteArrayOutputStream())));

        // null OutputWrapper → record allows null, NPE happens when outputStream() is called
        // Give the mock valid frame bytes via read(byte[]) so execution reaches the write path
        var networkData = mock(NetworkRequestData.class);
        byte[] frameBytes = new byte[]{(byte) 0x81, 0x05, 'H', 'e', 'l', 'l', 'o'};
        when(networkData.read(any(byte[].class))).thenAnswer(invocation -> {
            byte[] buf = invocation.getArgument(0);
            System.arraycopy(frameBytes, 0, buf, 0, frameBytes.length);
            return frameBytes.length;
        });
        assertThrows(NullPointerException.class,
                () -> handler.handle(networkData, null));
    }
}
