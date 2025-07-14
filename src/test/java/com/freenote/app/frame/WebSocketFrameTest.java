package com.freenote.app.frame;

import com.freenote.app.server.frames.*;
import com.freenote.app.server.frames.base.WebSocketFrame;
import io.NoHeaderObjectOutputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WebSocketFrameTest {
    @Test
    void testPingPongFrame() {
        var pingFrame = new PingFrame();
        assertEquals(FrameType.PING.getOpCode(), pingFrame.getOpcode());
        assertEquals(0, pingFrame.getPayloadLength());
        var pongFrame = new PongFrame();
        assertEquals(FrameType.PONG.getOpCode(), pongFrame.getOpcode());
    }

    @Test
    void testServerFrame() {
        var serverFrame = new ServerFrame();
        assertFalse(serverFrame.isMasked());
    }

    @Test
    void testFragmentedFrame() {
        var fragmentedFrame = new WebSocketFrame(false, FrameType.CONTINUATION.getOpCode(), "abc".getBytes());
        assertEquals(FrameType.CONTINUATION.getOpCode(), fragmentedFrame.getOpcode());
        assertEquals(3, fragmentedFrame.getPayloadLength());

    }

    @Test
    void testLargeTextFrame() throws IOException {
        var largeText = "a".repeat(126);
        var largeFrame = TextFrame.createServerFrame(largeText.getBytes());
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
        var largeFrame = TextFrame.createServerFrame(largeText.getBytes());
        var byteArrayOutputStream = new ByteArrayOutputStream();
        var outputStream = new NoHeaderObjectOutputStream(byteArrayOutputStream);
        largeFrame.writeExternal(outputStream);
        var bytes = byteArrayOutputStream.toByteArray();
        assertEquals(FrameType.TEXT.getHexValue(), bytes[0]);
        assertEquals(127, bytes[1]);
    }

    @Test
    void testEmptyFrame() throws IOException {
        var emptyFrame = new ServerFrame();
        var byteArrayOutputStream = new ByteArrayOutputStream();
        var outputStream = new NoHeaderObjectOutputStream(byteArrayOutputStream);
        emptyFrame.writeExternal(outputStream);
        var bytes = byteArrayOutputStream.toByteArray();
        assertEquals(FrameType.CONTINUATION.getHexValue(), bytes[0]);
        assertEquals(0, bytes[1]);
    }
}
