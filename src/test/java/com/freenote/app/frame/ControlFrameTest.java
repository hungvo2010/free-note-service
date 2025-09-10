package com.freenote.app.frame;

import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.base.ControlFrame;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ControlFrameTest {
    @Test
    public void giveControlFrame_whenCallTotalLength_ThenMustReturn_2() {
        ControlFrame controlFrame = new ControlFrame(FrameType.BINARY.getOpCode());
        assertEquals(2, controlFrame.getTotalFrameLength());
        assertThrows(UnsupportedOperationException.class, ControlFrame::new);
    }

    @Test
    public void giveRawBytes_whenParseToControlFrame_thenSuccess() {
        byte[] rawBytes = new byte[]{(byte) 0x88, 0x00};
        var socketFrame = new ControlFrame(rawBytes);
        assertEquals(FrameType.CLOSE.getOpCode(), socketFrame.getOpcode());
        assertEquals(2, socketFrame.getTotalFrameLength());
        assertEquals(0, socketFrame.getPayloadLength());
    }

}
