package com.freenote.app.handler;

import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.handler.impl.FragmentedURIHandlerImpl;
import com.freenote.app.server.model.InputWrapper;
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

        boolean result = handler.handle(new InputWrapper(input), output);

        assertFalse(result);
        assertEquals("", output.toString());
    }

    @Test
    void shouldThrowWhenStreamsAreNull() {
        URIHandler handler = new FragmentedURIHandlerImpl();

        assertThrows(NullPointerException.class,
                () -> handler.handle(null, new ByteArrayOutputStream()));
        assertThrows(NullPointerException.class,
                () -> handler.handle(new InputWrapper(new ByteArrayInputStream("hi".getBytes())), null));
    }
}
