package com.freenote.app.frame;

import com.freenote.app.server.frames.ClientFrame;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientFrameTest {
    @Test
    void testClientFrameCreation() {
        var clientFrame = new ClientFrame(new byte[]{(byte) 0x70, (byte) 0x80, (byte) 0x90, (byte) 0x80, (byte) 0xF0, (byte) 0x80, (byte) 0x90, (byte) 0x80});
        assertTrue(clientFrame.isMasked());
    }
}
