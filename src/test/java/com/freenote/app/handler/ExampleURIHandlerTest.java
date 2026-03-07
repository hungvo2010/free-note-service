package com.freenote.app.handler;

import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.handler.impl.FragmentedURIHandlerImpl;
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

        URIHandler handler = new FragmentedURIHandlerImpl();

        boolean result = handler.handle(new InputWrapper(), new OutputWrapper(output));

        assertFalse(result);
        assertEquals("", output.toString());
    }

    @Test
    void shouldThrowWhenStreamsAreNull() {
        URIHandler handler = new FragmentedURIHandlerImpl();

        assertThrows(NullPointerException.class,
                () -> handler.handle(null, new OutputWrapper(new ByteArrayOutputStream())));
        assertThrows(NullPointerException.class,
                () -> handler.handle(new InputWrapper(), null));
    }
}
