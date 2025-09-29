package com.freenote.app.exceptions;

import com.freenote.app.server.exceptions.http.UpgradeParserException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UpgradeParserExceptionTest {

    @Test
    void testConstructorWithMessage() {
        var exception = new UpgradeParserException("Parsing failed");
        assertEquals("Parsing failed", exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        var cause = new RuntimeException("Root cause");
        var exception = new UpgradeParserException("Parsing failed", cause);
        assertEquals("Parsing failed", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void testConstructorWithCauseOnly() {
        var cause = new RuntimeException("Root cause");
        var exception = new UpgradeParserException(cause);
        assertEquals(cause, exception.getCause());
        assertEquals("java.lang.RuntimeException: Root cause", exception.getMessage());
    }
}
