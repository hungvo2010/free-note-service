package com.freenote.app.frame;

import com.freenote.app.server.factory.ServerFrameFactory;
import com.freenote.app.server.frames.ServerFrame;
import com.freenote.app.server.frames.base.ControlFrame;
import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.frames.base.WebSocketFrame;
import io.NoHeaderObjectOutputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ServerFrameTest {
    private static ServerFrameFactory serverFrameFactory = null;

    @BeforeAll
    static void setup() {
        serverFrameFactory = new ServerFrameFactory();
    }

    @Test
    void testCreateControlFrame_ReturnsControlFrameInstance() {
        short opcode = 0x08; // Close frame opcode

        WebSocketFrame result = ServerFrame.createControlFrame(opcode);

        assertNotNull(result);
        assertTrue(result instanceof ControlFrame);
        assertEquals(opcode, result.getOpcode());
    }

    @Test
    void testCreateControlFrame_WithDifferentOpcodes() {
        // Test various control frame opcodes
        short[] opcodes = {0x08, 0x09, 0x0A}; // Close, Ping, Pong

        for (short opcode : opcodes) {
            WebSocketFrame result = ServerFrame.createControlFrame(opcode);

            assertNotNull(result);
            assertTrue(result instanceof ControlFrame);
            assertEquals(opcode, result.getOpcode());
        }
    }

    @Test
    void testCreateControlFrame_WithZeroOpcode() {
        short opcode = 0x00;

        WebSocketFrame result = ServerFrame.createControlFrame(opcode);

        assertNotNull(result);
        assertTrue(result instanceof ControlFrame);
        assertEquals(opcode, result.getOpcode());
    }

    @Test
    void testCreateControlFrame_WithMaxOpcode() {
        short opcode = Short.MAX_VALUE;

        WebSocketFrame result = ServerFrame.createControlFrame(opcode);

        assertNotNull(result);
        assertTrue(result instanceof ControlFrame);
        assertEquals(opcode, result.getOpcode());
    }

    @Test
    void testCreateControlFrame_WithNegativeOpcode() {
        short opcode = -1;

        WebSocketFrame result = ServerFrame.createControlFrame(opcode);

        assertNotNull(result);
        assertTrue(result instanceof ControlFrame);
        assertEquals(opcode, result.getOpcode());
    }

    @Test
    void testCreateDataFrame_ReturnsDataFrameInstance() {
        short opcode = 0x01; // Text frame opcode

        WebSocketFrame result = ServerFrame.createDataFrame(opcode);

        assertNotNull(result);
        assertTrue(result instanceof DataFrame);
    }

    @Test
    void testCreateDataFrame_IgnoresOpcodeParameter() {
        // The current implementation ignores the opcode parameter
        // and always creates a DataFrame with default TEXT opcode
        short inputOpcode = 0x02; // Binary frame opcode

        WebSocketFrame result = ServerFrame.createDataFrame(inputOpcode);

        assertNotNull(result);
        assertTrue(result instanceof DataFrame);
        // Note: The implementation currently ignores the opcode parameter
        // This might be a bug that should be addressed
    }

    @Test
    void testCreateDataFrame_WithDifferentOpcodes() {
        short[] opcodes = {0x01, 0x02, 0x00, 0x0F}; // Text, Binary, Continuation, Reserved

        for (short opcode : opcodes) {
            WebSocketFrame result = ServerFrame.createDataFrame(opcode);

            assertNotNull(result);
            assertTrue(result instanceof DataFrame);
            // All should return DataFrame instances regardless of opcode
        }
    }

    @Test
    void testCreateDataFrame_WithZeroOpcode() {
        short opcode = 0x00;

        WebSocketFrame result = ServerFrame.createDataFrame(opcode);

        assertNotNull(result);
        assertTrue(result instanceof DataFrame);
    }

    @Test
    void testCreateDataFrame_WithMaxOpcode() {
        short opcode = Short.MAX_VALUE;

        WebSocketFrame result = ServerFrame.createDataFrame(opcode);

        assertNotNull(result);
        assertTrue(result instanceof DataFrame);
    }

    @Test
    void testCreateDataFrame_WithNegativeOpcode() {
        short opcode = -1;

        WebSocketFrame result = ServerFrame.createDataFrame(opcode);

        assertNotNull(result);
        assertTrue(result instanceof DataFrame);
    }

    @Test
    void testFactoryMethods_ReturnDifferentInstances() {
        short opcode = 0x01;

        WebSocketFrame controlFrame1 = ServerFrame.createControlFrame(opcode);
        WebSocketFrame controlFrame2 = ServerFrame.createControlFrame(opcode);
        WebSocketFrame dataFrame1 = ServerFrame.createDataFrame(opcode);
        WebSocketFrame dataFrame2 = ServerFrame.createDataFrame(opcode);

        // Each call should return a new instance
        assertNotSame(controlFrame1, controlFrame2);
        assertNotSame(dataFrame1, dataFrame2);
        assertNotSame(controlFrame1, dataFrame1);
    }

    @Test
    void testFactoryMethods_ReturnCorrectTypes() {
        short opcode = 0x01;

        WebSocketFrame controlFrame = ServerFrame.createControlFrame(opcode);
        WebSocketFrame dataFrame = ServerFrame.createDataFrame(opcode);

        // Verify the correct types are returned
        assertTrue(controlFrame instanceof ControlFrame);
        assertTrue(dataFrame instanceof DataFrame);
    }

    @Test
    void testControlFrameProperties() {
        short opcode = 0x08; // Close frame

        ControlFrame controlFrame = (ControlFrame) ServerFrame.createControlFrame(opcode);

        assertNotNull(controlFrame);
        assertEquals(opcode, controlFrame.getOpcode());
        assertFalse(controlFrame.isMasked()); // Control frames are not masked by default
        assertEquals(0, controlFrame.getPayloadLength()); // Control frames have no payload
    }

    @Test
    void testDataFrameProperties() {
        short opcode = 0x01;

        DataFrame dataFrame = (DataFrame) ServerFrame.createDataFrame(opcode);

        assertNotNull(dataFrame);
        // Note: Current implementation ignores opcode parameter
        // This test documents the current behavior
        assertFalse(dataFrame.isMasked()); // Server frames are not masked
    }

    @Test
    void testControlFrameWriting() throws IOException {
        var pongFrame = serverFrameFactory.createPongFrame();
        var byteArrayOutputStream = new ByteArrayOutputStream();
        var outputStream = new NoHeaderObjectOutputStream(byteArrayOutputStream);
        pongFrame.writeExternal(outputStream);
        outputStream.flush();
        byte[] bytes = byteArrayOutputStream.toByteArray();
        assertEquals(0x0A, bytes[0]); // Pong frame opcode
    }

    @Test
    void testMaskedControlFrameWriting() throws IOException {
        var pongFrame = serverFrameFactory.createPongFrame();
        pongFrame.setMasked(true); // Set masked to true for testing
        pongFrame.setMaskingKey(new byte[]{0x1, 0x2, 0x3, 0x4}); // Example masking key
        var byteArrayOutputStream = new ByteArrayOutputStream();
        var outputStream = new NoHeaderObjectOutputStream(byteArrayOutputStream);
        pongFrame.writeExternal(outputStream);
        outputStream.flush();
        byte[] bytes = byteArrayOutputStream.toByteArray();
        assertEquals(0x0A, bytes[0]); // Pong frame opcode
    }
}
