package com.freenote.app.handler;

import com.freenote.app.server.handler.URIEndpointHandler;
import com.freenote.app.server.handler.impl.FragmentedURIEndpointHandlerImpl;
import com.freenote.app.server.model.InputWrapper;
import com.freenote.app.server.model.OutputWrapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class ExampleURIHandlerTest {
    @Test
    void shouldReturnFalseWhenInputIsEmpty() throws IOException {
        InputStream input = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        URIEndpointHandler handler = new FragmentedURIEndpointHandlerImpl();

        boolean result = handler.handle(new InputWrapper(), new OutputWrapper(output));

        assertFalse(result);
        assertEquals("", output.toString());
    }

    @Test
    void shouldThrowWhenStreamsAreNull() {
        URIEndpointHandler handler = new FragmentedURIEndpointHandlerImpl();

        assertThrows(NullPointerException.class,
                () -> handler.handle(null, new OutputWrapper(new ByteArrayOutputStream())));
        assertThrows(NullPointerException.class,
                () -> handler.handle(new InputWrapper(), null));
    }
}
