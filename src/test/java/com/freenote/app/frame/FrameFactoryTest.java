package com.freenote.app.frame;

import com.freenote.app.server.frames.factory.ServerFrameFactory;
import com.freenote.app.server.frames.FrameFactory;
import com.freenote.app.server.frames.FrameType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FrameFactoryTest {
    @Test
    void givenFrameTypeThenCreateFrame() {
        FrameType frameType = FrameType.TEXT;
        byte[] frameBytes = FrameFactory.createServerFrame("Hello World".getBytes(), frameType);
        assertEquals(frameBytes[0], frameType.getHexValue());
        assertEquals(frameBytes[1], "Hello World".getBytes().length);
    }

    @Test
    void givenFrameTypeThenCreateServerFrame() {
        FrameType frameType = FrameType.BINARY;
        var factory = new ServerFrameFactory();
        var frame = factory.createBinaryFrame("Hello World".getBytes());
        assertEquals(frame.getOpcode(), frameType.getOpCode());
    }
}
