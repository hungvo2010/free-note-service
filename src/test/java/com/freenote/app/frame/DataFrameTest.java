package com.freenote.app.frame;

import com.freenote.app.server.factory.ClientFrameFactory;
import com.freenote.app.server.factory.ServerFrameFactory;
import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.frames.base.FrameTypeWithBehavior;
import com.freenote.app.server.util.FrameUtil;
import io.NoHeaderObjectOutputStream;
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
        var dataFrame = new DataFrame();
        assertEquals(FrameTypeWithBehavior.TEXT.getOpcode(), dataFrame.getOpcode());
    }

    @Test
    void givenClientFrame_whenParsedToDataFrame_thenSuccess() throws IOException {
        var clientFrame = clientFrameFactory.createTextFrame("Hello World");
        ByteArrayOutputStream bytesOutputStream = new ByteArrayOutputStream();
        var outputStream = new NoHeaderObjectOutputStream(bytesOutputStream);
        clientFrame.writeExternal(outputStream);
        outputStream.flush();
        var dataFrame = new DataFrame(bytesOutputStream.toByteArray());
        assertEquals(FrameTypeWithBehavior.TEXT.getOpcode(), dataFrame.getOpcode());
        assertEquals("Hello World", new String(FrameUtil.maskPayload(dataFrame.getPayloadData(), dataFrame.getMaskingKey())));
    }

    @Test
    void givenServerFrame_whenParsedToDataFrame_thenSuccess() throws IOException {
        // given
        var serverFrame = serverFrameFactory.createTextFrame("Hello World");
        ByteArrayOutputStream bytesOutputStream = new ByteArrayOutputStream();
        var outputStream = new NoHeaderObjectOutputStream(bytesOutputStream);
        serverFrame.writeExternal(outputStream);
        outputStream.flush();

        // when
        var dataFrame = new DataFrame(bytesOutputStream.toByteArray());

        // then
        assertEquals(FrameTypeWithBehavior.TEXT.getOpcode(), dataFrame.getOpcode());
        assertFalse(dataFrame.isMasked());
        assertThrows(IllegalArgumentException.class, () -> new String(FrameUtil.maskPayload(dataFrame.getPayloadData(), dataFrame.getMaskingKey())));
    }
}
