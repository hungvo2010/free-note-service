package com.freenote.app.frame;

import com.freenote.app.server.exceptions.WebSocketException;
import com.freenote.app.server.frames.factory.FrameFactory;
import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.frames.ws.WebSocketFrame;
import com.freenote.app.server.util.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class DataFrameTest {
    static FrameFactory.ClientFrameFactory clientFrameFactory = null;
    static FrameFactory.ServerFrameFactory serverFrameFactory = null;

    @BeforeAll
    static void setup() {
        clientFrameFactory = new FrameFactory.ClientFrameFactory();
        serverFrameFactory = new FrameFactory.ServerFrameFactory();
    }

    @Test
    void givenDefaultDataFrame_whenCreated_thenSuccess() {
        assertThrows(UnsupportedOperationException.class, DataFrame::new);
    }

    @Test
    void givenClientFrame_whenParsedToDataFrame_thenSuccess() throws IOException {
        var clientFrame = clientFrameFactory.createTextFrame("Hello World");
        var dataFrame = DataFrame.fromRawFrameBytes(writeToBytes(clientFrame));
        assertEquals(FrameType.TEXT.getOpCode(), dataFrame.getOpcode());
        assertEquals("Hello World", new String(WebSocketFrame.applyMask(dataFrame.getPayloadData(), dataFrame.getMaskingKey())));
    }

    @Test
    void givenServerFrame_whenParsedToDataFrame_thenSuccess() throws IOException {
        var serverFrame = serverFrameFactory.createTextFrame("Hello World");
        var dataFrame = DataFrame.fromRawFrameBytes(writeToBytes(serverFrame));

        assertEquals(FrameType.TEXT.getOpCode(), dataFrame.getOpcode());
        assertFalse(dataFrame.isMasked());
        assertThrows(WebSocketException.InvalidFrameException.class, () -> new String(WebSocketFrame.applyMask(dataFrame.getPayloadData(), dataFrame.getMaskingKey())));
    }

    @Test
    void givenFreshDataFrame_whenGetTotalFrameLength_thenSuccess() throws IOException {
        var dataFrame = serverFrameFactory.createTextFrame("");
        assertEquals(2, dataFrame.getTotalFrameLength());
    }

    @Test
    void givenShortServerTextDataFrame_whenGetTotalFrameLength_thenSuccess() throws IOException {
        var shortTextFrame = serverFrameFactory.createTextFrame("!"); // 2 + 1 = 3
        assertEquals(3, shortTextFrame.getTotalFrameLength());

        var mediumDataFrame = serverFrameFactory.createTextFrame("123456789012345"); // 2 + 1 + 2 = 5
        assertEquals(17, mediumDataFrame.getTotalFrameLength());

        var intermediateDataFrame = serverFrameFactory.createTextFrame("1".repeat(129)); // 2 + 1 + 2 = 5
        assertEquals(2 + 2 + 129, intermediateDataFrame.getTotalFrameLength());

        var superLargeDataFrame = serverFrameFactory.createTextFrame("1".repeat(67855)); // 2 + 1 + 2 = 5
        assertEquals(2 + 8 + 67855, superLargeDataFrame.getTotalFrameLength());
    }

    @Test
    void givenClientFrame_whenGetTotalFrameLength_thenSuccess() throws IOException {
        var clientFrame = clientFrameFactory.createTextFrame("123"); // 2 + 1 = 3
        assertEquals(5 + 4, clientFrame.getTotalFrameLength());

        var mediumDataFrame = clientFrameFactory.createTextFrame("123456789012345"); // 2 + 1 + 2 = 5
        assertEquals(17 + 4, mediumDataFrame.getTotalFrameLength());

        var intermediateDataFrame = clientFrameFactory.createTextFrame("1".repeat(129)); // 2 + 1 + 2 = 5
        assertEquals(2 + 2 + 129 + 4, intermediateDataFrame.getTotalFrameLength());

        var superLargeDataFrame = clientFrameFactory.createTextFrame("1".repeat(67855)); // 2 + 1 + 2 = 5
        assertEquals(2 + 8 + 67855 + 4, superLargeDataFrame.getTotalFrameLength());
    }

    @Test
    void givenSevenBitPayloadLength_whenParsed_thenLengthMatches() throws IOException {
        var dataFrame = DataFrame.fromRawFrameBytes(writeToBytes(serverFrameFactory.createTextFrame("!")));
        assertEquals(1, dataFrame.getPayloadLength());
    }

    @Test
    void givenTwoByteExtendedPayloadLength_whenParsed_thenLengthMatches() throws IOException {
        var dataFrame = DataFrame.fromRawFrameBytes(writeToBytes(serverFrameFactory.createTextFrame("1".repeat(200))));
        assertEquals(200, dataFrame.getPayloadLength());
    }

    @Test
    void givenEightByteExtendedPayloadLength_whenParsed_thenLengthMatches() throws IOException {
        var dataFrame = DataFrame.fromRawFrameBytes(writeToBytes(serverFrameFactory.createTextFrame("1".repeat(67855))));
        assertEquals(67855, dataFrame.getPayloadLength());
    }

    @Test
    void givenTruncatedTwoByteExtendedPayloadLength_whenParsed_thenThrows() {
        assertThrows(WebSocketException.InvalidFrameException.class, () -> DataFrame.fromRawFrameBytes(new byte[]{0x00, 126, 0x01}));
    }

    @Test
    void givenTruncatedEightByteExtendedPayloadLength_whenParsed_thenThrows() {
        assertThrows(WebSocketException.InvalidFrameException.class, () -> DataFrame.fromRawFrameBytes(new byte[]{0x00, 127, 0x01, 0x02, 0x03, 0x04}));
    }

    @Test
    void givenMaskingKey_whenAppliedTwice_thenOriginalPayloadReturned() {
        byte[] payload = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05};
        byte[] key = new byte[]{0x0F, 0x0F, 0x0F, 0x0F};

        assertArrayEquals(payload, WebSocketFrame.applyMask(WebSocketFrame.applyMask(payload, key), key));
    }

    @Test
    void givenInvalidMaskingKey_whenApplied_thenThrows() {
        assertThrows(WebSocketException.InvalidFrameException.class, () -> WebSocketFrame.applyMask(new byte[]{0x01, 0x02}, new byte[]{0x0F, 0x0F}));
    }

    private static byte[] writeToBytes(WebSocketFrame frame) throws IOException {
        var bytesOutputStream = new ByteArrayOutputStream();
        IOUtils.writeOutPut(bytesOutputStream, frame);
        return bytesOutputStream.toByteArray();
    }
}
