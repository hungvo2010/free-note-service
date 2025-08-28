package com.freenote.app.handler;

import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.handler.impl.ExampleURIHandler;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class ExampleURIHandlerTest {

    @Test
    void shouldReturnTrueWhenInputIsValid() {
        InputStream input = new ByteArrayInputStream("hello".getBytes());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        URIHandler handler = new ExampleURIHandler(); // class you’ll implement

        boolean result = handler.handle(input, output);

        assertTrue(result);
        assertEquals("HELLO", output.toString()); // assume transformation
    }

    @Test
    void shouldReturnFalseWhenInputIsEmpty() {
        InputStream input = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        URIHandler handler = new ExampleURIHandler();

        boolean result = handler.handle(input, output);

        assertFalse(result);
        assertEquals("", output.toString());
    }

    @Test
    void shouldHandleInvalidDataGracefully() {
        InputStream input = new ByteArrayInputStream("$$$".getBytes());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        URIHandler handler = new ExampleURIHandler();

        boolean result = handler.handle(input, output);

        assertFalse(result);
        assertEquals("", output.toString()); // nothing written
    }

    @Test
    void shouldThrowWhenStreamsAreNull() {
        URIHandler handler = new ExampleURIHandler();

        assertThrows(NullPointerException.class,
                () -> handler.handle(null, new ByteArrayOutputStream()));
        assertThrows(NullPointerException.class,
                () -> handler.handle(new ByteArrayInputStream("hi".getBytes()), null));
    }
}
