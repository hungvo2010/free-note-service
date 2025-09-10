package com.freenote.app.frame;

import com.freenote.app.server.exceptions.InvalidFrameException;
import com.freenote.app.server.factory.ClientFrameFactory;
import com.freenote.app.server.factory.ServerFrameFactory;
import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.util.FrameUtil;
import com.freenote.app.server.util.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class DataFrameTest {
    static ClientFrameFactory clientFrameFactory = null;
    static ServerFrameFactory serverFrameFactory = null;

    @BeforeAll
    static void setup() {
        clientFrameFactory = new ClientFrameFactory();
        serverFrameFactory = new ServerFrameFactory();
    }

    @Test
    void givenDefaultDataFrame_whenCreated_thenSuccess() {
        assertThrows(UnsupportedOperationException.class, DataFrame::new);
    }

    @Test
    void givenClientFrame_whenParsedToDataFrame_thenSuccess() throws IOException {
        var clientFrame = clientFrameFactory.createTextFrame("Hello World");
        ByteArrayOutputStream bytesOutputStream = new ByteArrayOutputStream();
        IOUtils.writeOutPut(bytesOutputStream, clientFrame);
        var dataFrame = DataFrame.fromRawFrameBytes(bytesOutputStream.toByteArray());
        assertEquals(FrameType.TEXT.getOpCode(), dataFrame.getOpcode());
        assertEquals("Hello World", new String(FrameUtil.maskPayload(dataFrame.getPayloadData(), dataFrame.getMaskingKey())));
    }

    @Test
    void givenServerFrame_whenParsedToDataFrame_thenSuccess() throws IOException {
        // given
        var serverFrame = serverFrameFactory.createTextFrame("Hello World");
        ByteArrayOutputStream bytesOutputStream = new ByteArrayOutputStream();
        IOUtils.writeOutPut(bytesOutputStream, serverFrame);
        // when
        var dataFrame = DataFrame.fromRawFrameBytes(bytesOutputStream.toByteArray());

        // then
        assertEquals(FrameType.TEXT.getOpCode(), dataFrame.getOpcode());
        assertFalse(dataFrame.isMasked());
        assertThrows(InvalidFrameException.class, () -> new String(FrameUtil.maskPayload(dataFrame.getPayloadData(), dataFrame.getMaskingKey())));
    }

    @Test
    void givenFreshDataFrame_whenGetTotalFrameLength_thenSuccess() throws IOException {
        var dataFrame = serverFrameFactory.createTextFrame("");
        assertEquals(2, dataFrame.getTotalFrameLength());
    }

    @Test
    void givenShortServerTextDataFrame_whenGetTotalFrameLength_thenSuccess() throws IOException {
        var shortTextFrame = serverFrameFactory.createTextFrame("123"); // 2 + 1 = 3
        assertEquals(5, shortTextFrame.getTotalFrameLength());

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
}
