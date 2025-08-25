package com.freenote.app.frame;

import com.freenote.app.server.factory.ServerFrameFactory;
import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.PongFrame;
import io.NoHeaderObjectOutputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WebSocketFrameTest {
    private static ServerFrameFactory serverFrameFactory = null;

    @BeforeAll
    static void setup() {
        serverFrameFactory = new ServerFrameFactory();
    }

    @Test
    void testPingPongFrame() {
        var pingFrame = serverFrameFactory.createPingFrame();
        assertEquals(FrameType.PING.getOpCode(), pingFrame.getOpcode());
        assertEquals(0, pingFrame.getPayloadLength());
        var pongFrame = new PongFrame();
        assertEquals(FrameType.PONG.getOpCode(), pongFrame.getOpcode());
        assertFalse(pingFrame.isMasked());
    }

    @Test
    void testLargeTextFrame() throws IOException {
        var largeText = "a".repeat(126);
        var largeFrame = serverFrameFactory.createTextFrame(largeText);
        var byteArrayOutputStream = new ByteArrayOutputStream();
        var outputStream = new NoHeaderObjectOutputStream(byteArrayOutputStream);
        largeFrame.writeExternal(outputStream);
        var bytes = byteArrayOutputStream.toByteArray();
        assertEquals(FrameType.TEXT.getHexValue(), bytes[0]);
        assertEquals(126, bytes[1]);
    }

    @Test
    void testSuperLargeTextFrame() throws IOException {
        var largeText = "a".repeat(647136);
        var largeFrame = serverFrameFactory.createTextFrame(largeText);
        var byteArrayOutputStream = new ByteArrayOutputStream();
        var outputStream = new NoHeaderObjectOutputStream(byteArrayOutputStream);
        largeFrame.writeExternal(outputStream);
        var bytes = byteArrayOutputStream.toByteArray();
        assertEquals(FrameType.TEXT.getHexValue(), bytes[0]);
        assertEquals(127, bytes[1]);
    }
//
//    @Test
//    void testEmptyFrame() throws IOException {
//        var emptyFrame = new ServerFrame();
//        var byteArrayOutputStream = new ByteArrayOutputStream();
//        var outputStream = new NoHeaderObjectOutputStream(byteArrayOutputStream);
//        emptyFrame.writeExternal(outputStream);
//        var bytes = byteArrayOutputStream.toByteArray();
//        assertEquals(FrameType.CONTINUATION.getHexValue(), bytes[0]);
//        assertEquals(0, bytes[1]);
//    }
}
