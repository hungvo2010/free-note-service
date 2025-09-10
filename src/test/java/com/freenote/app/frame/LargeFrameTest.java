package com.freenote.app.frame;

import com.freenote.app.server.exceptions.InvalidFrameStateException;
import com.freenote.app.server.frames.LargeFrame;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class LargeFrameTest {
    @Test
    public void givenFreshLargeFrame_whenCallMergeFrames_thenThrowFrameStateException() {
        LargeFrame largeFrame = new LargeFrame();
        assertThrows(InvalidFrameStateException.class, () -> {
            largeFrame.getMergedFrame();
        });
    }
}
