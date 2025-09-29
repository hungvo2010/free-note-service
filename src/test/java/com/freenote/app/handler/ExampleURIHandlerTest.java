package com.freenote.app.handler;

import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.handler.impl.FragmentedURIHandlerImpl;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class ExampleURIHandlerTest {
    @Test
    void shouldReturnFalseWhenInputIsEmpty() {
        InputStream input = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        URIHandler handler = new FragmentedURIHandlerImpl();

        boolean result = handler.handle(input, output);

        assertFalse(result);
        assertEquals("", output.toString());
    }

    @Test
    void shouldThrowWhenStreamsAreNull() {
        URIHandler handler = new FragmentedURIHandlerImpl();

        assertThrows(NullPointerException.class,
                () -> handler.handle(null, new ByteArrayOutputStream()));
        assertThrows(NullPointerException.class,
                () -> handler.handle(new ByteArrayInputStream("hi".getBytes()), null));
    }
}
