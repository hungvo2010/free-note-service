package com.freenote.app.frame;

import com.freenote.app.server.frames.factory.ClientFrameFactory;
import com.freenote.app.server.frames.FrameType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientFrameTest {
    static ClientFrameFactory clientFrameFactory = null;

    @BeforeAll
    static void setup() {
        clientFrameFactory = new ClientFrameFactory();
    }

    @Test
    void testClientFrameCreation() {
        var clientFrame = clientFrameFactory.createPongFrame();
        assertTrue(clientFrame.isMasked());
    }

    @Test
    void shouldCreateValidClientFrames() {
        var clientFrame = clientFrameFactory.createPongFrame();
        assertTrue(clientFrame.isMasked());
        assertEquals(FrameType.PONG.getOpCode(), clientFrame.getOpcode());

        var pingFrame = clientFrameFactory.createPingFrame();
        assertEquals(FrameType.PING.getOpCode(), pingFrame.getOpcode());
        assertTrue(pingFrame.isMasked());

        var closeFrame = clientFrameFactory.createCloseFrame(1000, "Normal Closure");
        assertEquals(FrameType.CLOSE.getOpCode(), closeFrame.getOpcode());
        assertTrue(closeFrame.isMasked());

        var textFrame = clientFrameFactory.createBinaryFrame("Hello".getBytes());
        assertEquals(FrameType.BINARY.getOpCode(), textFrame.getOpcode());

        var continuationFrame = clientFrameFactory.createContinuationFrame("Continued".getBytes());
        assertEquals(FrameType.CONTINUATION.getOpCode(), continuationFrame.getOpcode());
    }
}
