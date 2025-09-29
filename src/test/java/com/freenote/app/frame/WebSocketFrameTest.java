package com.freenote.app.frame;

import com.freenote.app.server.exceptions.InvalidFrameException;
import com.freenote.app.server.frames.factory.ServerFrameFactory;
import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.frames.control.PongFrame;
import com.freenote.app.server.util.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

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
    void givenInvalidBytes_whenParseToWebSocketFrame_thenMustThrowException() {
        var bytes = new byte[]{(byte) 0x81};
        assertThrows(InvalidFrameException.class, () -> DataFrame.fromRawFrameBytes(bytes));
    }

    @Test
    void testLargeTextFrame() throws IOException {
        var largeText = "a".repeat(126);
        var largeFrame = serverFrameFactory.createTextFrame(largeText);
        var byteArrayOutputStream = new ByteArrayOutputStream();
        IOUtils.writeOutPut(byteArrayOutputStream, largeFrame);
        var bytes = byteArrayOutputStream.toByteArray();
        assertEquals(FrameType.TEXT.getHexValue(), bytes[0]);
        assertEquals(126, bytes[1]);
    }

    @Test
    void testSuperLargeTextFrame() throws IOException {
        var largeText = "a".repeat(647136);
        var largeFrame = serverFrameFactory.createTextFrame(largeText);
        var byteArrayOutputStream = new ByteArrayOutputStream();
        IOUtils.writeOutPut(byteArrayOutputStream, largeFrame);
        var bytes = byteArrayOutputStream.toByteArray();
        assertEquals(FrameType.TEXT.getHexValue(), bytes[0]);
        assertEquals(127, bytes[1]);
    }
}
