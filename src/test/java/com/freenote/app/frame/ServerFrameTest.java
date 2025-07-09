package com.freenote.app.frame;

import com.freenote.app.server.frames.ServerFrame;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class ServerFrameTest {
    @Test
    void testEmptyFrameCreation() {
        var emptyFrame = ServerFrame.emptyFrame();
        assertNull(emptyFrame.getPayloadData());
    }
}
