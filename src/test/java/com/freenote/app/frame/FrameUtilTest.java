package com.freenote.app.frame;

import com.freenote.app.server.frames.BaseFrame;
import com.freenote.app.server.util.FrameUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FrameUtilTest {

    @Test
    void testParsePayloadLength_LessThan126() {
        byte[] frame = new byte[]{0x00, 0x7D}; // 125
        int length = FrameUtil.parsePayloadLength(frame);
        assertEquals(125, length);
    }

    @Test
    void testParsePayloadLength_126WithTwoByteExtendedPayload() {
        byte[] frame = new byte[]{
                0x00,
                126,       // Marker for 2-byte length
                0x01, 0x00 // 256 (1*256 + 0)
        };
        int length = FrameUtil.parsePayloadLength(frame);
        assertEquals(256, length);
    }

    @Test
    void testParsePayloadLength_127WithEightByteExtendedPayload() {
        byte[] frame = new byte[]{
                0x00,
                127,                 // Marker for 8-byte length
                0x00, 0x00, 0x00, 0x00, // high 4 bytes = 0
                0x00, 0x00, 0x01, 0x00  // low 4 bytes = 256
        };
        int length = FrameUtil.parsePayloadLength(frame);
        assertEquals(256, length);
    }

    @Test
    void testParsePayloadLength_ThrowsOnInvalidShortLengthFor126() {
        byte[] frame = new byte[]{
                0x00, 126, 0x01 // Only one byte after 126 marker
        };
        assertThrows(IllegalArgumentException.class, () -> FrameUtil.parsePayloadLength(frame));
    }

    @Test
    void testParsePayloadLength_ThrowsOnInvalidShortLengthFor127() {
        byte[] frame = new byte[]{
                0x00, 127, 0x01, 0x02, 0x03, 0x04 // Less than 8 bytes
        };
        assertThrows(IllegalArgumentException.class, () -> FrameUtil.parsePayloadLength(frame));
    }

    @Test
    void testBytesToInt() {
        byte[] input = new byte[]{0x00, 0x00, 0x01, 0x00}; // 256
        int result = FrameUtil.bytesToInt(input);
        assertEquals(256, result);
    }

    @Test
    void testMaskPayload_XORMasking() {
        byte[] payload = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05};
        byte[] key = new byte[]{0x0F, 0x0F, 0x0F, 0x0F};

        byte[] masked = FrameUtil.maskPayload(payload, key);

        // Reverse the mask to verify we get the original back
        byte[] unmasked = FrameUtil.maskPayload(masked, key);
        assertArrayEquals(payload, unmasked);
    }

    @Test
    void testMaskPayload_InvalidKeyLength() {
        byte[] payload = new byte[]{0x01, 0x02};
        byte[] invalidKey = new byte[]{0x0F, 0x0F}; // Only 2 bytes
        assertThrows(IllegalArgumentException.class, () -> FrameUtil.maskPayload(payload, invalidKey));
    }

    @Test
    void testBaseFrame_WithAllBits() {
        byte[] payload = new byte[]{(byte) 0x71, 0x02};
        BaseFrame frame = new BaseFrame(payload);
        assertFalse(frame.isFin());
        assertTrue(frame.isRsv1());
        assertTrue(frame.isRsv2());
        assertTrue(frame.isRsv3());
    }
}
