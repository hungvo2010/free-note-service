package com.freenote.app.frame;

import com.freenote.app.server.frames.TextFrame;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextFrameTest {
    @Test
    void testTextFrameCreation() {
        var textFrame  = new TextFrame();
        assertEquals(1, textFrame.getOpcode());
    }
}
