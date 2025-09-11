package com.freenote.app.frame;

import com.freenote.app.server.exceptions.InvalidFrameStateException;
import com.freenote.app.server.factory.ClientFrameFactory;
import com.freenote.app.server.factory.FrameFactory;
import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.LargeFrame;
import com.freenote.app.server.frames.base.DataFrame;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class LargeFrameTest {
    private final FrameFactory clientFrameFactory = new ClientFrameFactory();

    @Test
    void givenFreshLargeFrame_whenCallMergeFrames_thenThrowFrameStateException() {
        LargeFrame largeFrame = new LargeFrame();
        assertThrows(InvalidFrameStateException.class, largeFrame::getMergedFrame);
    }

    @Test
    void givenFinalizedLargeFrame_whenCallMergeFrames_thenSuccess() {
        LargeFrame largeFrame = new LargeFrame();
        var finalFrame = clientFrameFactory.createTextFrame("part1");
        largeFrame.addFragmentMessage((DataFrame) finalFrame);
        var mergedFrame = largeFrame.getMergedFrame();
        assertNotNull(mergedFrame);
    }

    @Test
    void givenLargeFrame_whenAddNullFragment_thenThrowIllegalArgumentException() {
        LargeFrame largeFrame = new LargeFrame();
        assertThrows(IllegalArgumentException.class, () -> {
            largeFrame.addFragmentMessage(null);
        });
    }

    @Test
    void givenCompletedLargeFrame_whenAddFragment_thenThrowIllegalStateException() {
        LargeFrame largeFrame = new LargeFrame();
        var finalFrame = clientFrameFactory.createTextFrame("part1");
        largeFrame.addFragmentMessage((DataFrame) finalFrame);
        var anotherFrame = clientFrameFactory.createTextFrame("part2");
        assertThrows(IllegalStateException.class, () -> {
            largeFrame.addFragmentMessage((DataFrame) anotherFrame);
        });
    }

    @Test
    void givenLargeFrameWithFirstFrame_whenAddContinuationFrame_thenSuccess() {
        LargeFrame largeFrame = new LargeFrame();
        var firstFrame = clientFrameFactory.createNonFinalFrame(FrameType.TEXT.getOpCode(), "part1".getBytes(StandardCharsets.UTF_8));
        largeFrame.addFragmentMessage((DataFrame) firstFrame);

        var continuationFrame = clientFrameFactory.createContinuationFrame("part2".getBytes(StandardCharsets.UTF_8));
        largeFrame.addFragmentMessage((DataFrame) continuationFrame);

        assertFalse(largeFrame.isComplete());

        var nextContinuationFrame = clientFrameFactory.createContinuationFrame("part3".getBytes(StandardCharsets.UTF_8));
        largeFrame.addFragmentMessage((DataFrame) nextContinuationFrame);

        assertFalse(largeFrame.isComplete());

        var finalFrame = clientFrameFactory.createTextFrame("part4");
        largeFrame.addFragmentMessage((DataFrame) finalFrame);

        assertTrue(largeFrame.isComplete());
    }
}
