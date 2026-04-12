package com.freenote.app.frame;

import com.freenote.app.server.frames.factory.ServerFrameFactory;
import com.freenote.app.server.frames.base.ControlFrame;
import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.messages.WebSocketFrame;
import com.freenote.app.server.util.IOUtils;
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
        // Test Close frame creation (opcode 8)
        WebSocketFrame result = serverFrameFactory.createCloseFrame(1000, "Normal closure");

        assertNotNull(result);
        assertTrue(result instanceof ControlFrame);
        assertEquals(8, result.getOpcode());
    }

    @Test
    void testCreateControlFrame_WithDifferentOpcodes() {
        // Test various control frame types with valid opcodes
        WebSocketFrame closeFrame = serverFrameFactory.createCloseFrame(1000, "Normal closure"); // opcode 8
        WebSocketFrame pingFrame = serverFrameFactory.createPingFrame(); // opcode 9
        WebSocketFrame pongFrame = serverFrameFactory.createPongFrame(); // opcode 10

        assertNotNull(closeFrame);
        assertNotNull(pingFrame);
        assertNotNull(pongFrame);

        assertTrue(closeFrame instanceof ControlFrame);
        assertTrue(pingFrame instanceof ControlFrame);
        assertTrue(pongFrame instanceof ControlFrame);

        assertEquals(8, closeFrame.getOpcode());
        assertEquals(9, pingFrame.getOpcode());
        assertEquals(10, pongFrame.getOpcode());
    }

    @Test
    void testCreatePingFrame() {
        WebSocketFrame result = serverFrameFactory.createPingFrame(); // opcode 9

        assertNotNull(result);
        assertTrue(result instanceof ControlFrame);
        assertEquals(9, result.getOpcode());
    }

    @Test
    void testCreatePongFrame() {
        WebSocketFrame result = serverFrameFactory.createPongFrame(); // opcode 10

        assertNotNull(result);
        assertTrue(result instanceof ControlFrame);
        assertEquals(10, result.getOpcode());
    }

    @Test
    void testCreateDataFrame_ReturnsDataFrameInstance() {
        // Text frame (opcode 1)
        WebSocketFrame result = serverFrameFactory.createTextFrame("Hello World");

        assertNotNull(result);
        assertTrue(result instanceof DataFrame);
        assertEquals(1, result.getOpcode());
    }

    @Test
    void testCreateDataFrame_BinaryFrame() {
        // Binary frame (opcode 2)
        byte[] testData = {0x01, 0x02, 0x03, 0x04};
        WebSocketFrame result = serverFrameFactory.createBinaryFrame(testData);

        assertNotNull(result);
        assertTrue(result instanceof DataFrame);
        assertEquals(2, result.getOpcode());
    }

    @Test
    void testCreateDataFrame_WithDifferentOpcodes() {
        // Test valid data frame types
        WebSocketFrame textFrame = serverFrameFactory.createTextFrame("Hello"); // opcode 1
        WebSocketFrame binaryFrame = serverFrameFactory.createBinaryFrame(new byte[]{1, 2, 3}); // opcode 2
        WebSocketFrame continuationFrame = serverFrameFactory.createContinuationFrame(new byte[]{4, 5, 6}); // opcode 0

        assertNotNull(textFrame);
        assertNotNull(binaryFrame);
        assertNotNull(continuationFrame);

        assertTrue(textFrame instanceof DataFrame);
        assertTrue(binaryFrame instanceof DataFrame);
        assertTrue(continuationFrame instanceof DataFrame);

        assertEquals(1, textFrame.getOpcode());
        assertEquals(2, binaryFrame.getOpcode());
        assertEquals(0, continuationFrame.getOpcode());
    }

    @Test
    void testCreateContinuationFrame() {
        // Continuation frame (opcode 0)
        byte[] testData = {0x05, 0x06, 0x07};
        WebSocketFrame result = serverFrameFactory.createContinuationFrame(testData);

        assertNotNull(result);
        assertTrue(result instanceof DataFrame);
        assertEquals(0, result.getOpcode());
    }

    @Test
    void testCreateTextFrame() {
        // Text frame (opcode 1)
        WebSocketFrame result = serverFrameFactory.createTextFrame("Test message");

        assertNotNull(result);
        assertTrue(result instanceof DataFrame);
        assertEquals(1, result.getOpcode());
    }

    @Test
    void testCreateBinaryFrame() {
        // Binary frame (opcode 2)
        byte[] testData = {0x0A, 0x0B, 0x0C};
        WebSocketFrame result = serverFrameFactory.createBinaryFrame(testData);

        assertNotNull(result);
        assertTrue(result instanceof DataFrame);
        assertEquals(2, result.getOpcode());
    }

    @Test
    void testFactoryMethods_ReturnDifferentInstances() {
        WebSocketFrame controlFrame1 = serverFrameFactory.createPingFrame();
        WebSocketFrame controlFrame2 = serverFrameFactory.createPingFrame();
        WebSocketFrame dataFrame1 = serverFrameFactory.createTextFrame("Test1");
        WebSocketFrame dataFrame2 = serverFrameFactory.createTextFrame("Test2");

        // Each call should return a new instance
        assertNotSame(controlFrame1, controlFrame2);
        assertNotSame(dataFrame1, dataFrame2);
        assertNotSame(controlFrame1, dataFrame1);
    }

    @Test
    void testFactoryMethods_ReturnCorrectTypes() {
        WebSocketFrame controlFrame = serverFrameFactory.createPingFrame();
        WebSocketFrame dataFrame = serverFrameFactory.createTextFrame("Test");

        // Verify the correct types are returned
        assertTrue(controlFrame instanceof ControlFrame);
        assertTrue(dataFrame instanceof DataFrame);
    }

    @Test
    void testControlFrameProperties() {
        // Close frame (opcode 8)
        ControlFrame controlFrame = (ControlFrame) serverFrameFactory.createCloseFrame(1000, "Normal closure");

        assertNotNull(controlFrame);
        assertEquals(8, controlFrame.getOpcode());
        assertFalse(controlFrame.isMasked()); // Control frames are not masked by default
        assertEquals(0, controlFrame.getPayloadLength()); // Control frames have no payload
    }

    @Test
    void testDataFrameProperties() {
        // Text frame (opcode 1)
        DataFrame dataFrame = (DataFrame) serverFrameFactory.createTextFrame("Test message");

        assertNotNull(dataFrame);
        assertEquals(1, dataFrame.getOpcode());
        assertFalse(dataFrame.isMasked()); // Server frames are not masked
    }

    @Test
    void testControlFrameWriting() throws IOException {
        var pongFrame = serverFrameFactory.createPongFrame();
        var byteArrayOutputStream = new ByteArrayOutputStream();
        IOUtils.writeOutPut(byteArrayOutputStream, pongFrame);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        assertEquals(0x0A, bytes[0]); // Pong frame opcode
    }

    @Test
    void testMaskedControlFrameWriting() throws IOException {
        var pongFrame = serverFrameFactory.createPongFrame();
        pongFrame.setMasked(true); // Set masked to true for testing
        pongFrame.setMaskingKey(new byte[]{0x1, 0x2, 0x3, 0x4}); // Example masking key
        var byteArrayOutputStream = new ByteArrayOutputStream();
        IOUtils.writeOutPut(byteArrayOutputStream, pongFrame);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        assertEquals(0x0A, bytes[0]); // Pong frame opcode
    }
}
