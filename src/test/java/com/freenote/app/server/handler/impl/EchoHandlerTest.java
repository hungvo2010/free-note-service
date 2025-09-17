package com.freenote.app.server.handler.impl;

import com.freenote.app.server.util.FrameUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class EchoHandlerTest {

    private EchoHandler echoHandler;
    private ByteArrayInputStream inputStream;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() {
        echoHandler = new EchoHandler();
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void testHandle_SuccessfulEcho_UnmaskedFrame() {
        // Create an input stream with unmasked text frame data
        byte[] frameData = createSimpleTextFrame("Hello World");
        inputStream = new ByteArrayInputStream(frameData);

        boolean result = echoHandler.handle(inputStream, outputStream);

        assertTrue(result);
        assertTrue(outputStream.size() > 0);
    }

    @Test
    void testHandle_SuccessfulEcho_MaskedFrame() throws IOException {
        // Create a masked text frame
        String message = "Hello Masked";
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        byte[] maskingKey = {0x01, 0x02, 0x03, 0x04};
        byte[] maskedPayload = FrameUtil.maskPayload(payload, maskingKey);

        // Create completion frame with masking
        byte[] frameData = createMaskedTextFrame(message, maskingKey);
        inputStream = new ByteArrayInputStream(frameData);

        boolean result = echoHandler.handle(inputStream, outputStream);

        assertTrue(result);
        assertTrue(outputStream.size() > 0);
    }

    @Test
    void testHandle_EmptyInputStream() {
        inputStream = new ByteArrayInputStream(new byte[0]);

        boolean result = echoHandler.handle(inputStream, outputStream);

        assertFalse(result);
        assertEquals(0, outputStream.size());
    }

    @Test
    void testHandle_EndOfStreamReached() {
        // Create input stream that returns -1 immediately
        inputStream = new ByteArrayInputStream(new byte[1]) {
            @Override
            public int read(byte[] b) {
                return -1; // Simulate end of stream
            }
        };

        // Make stream ready
        inputStream = new ByteArrayInputStream(new byte[]{0x01}) {
            private boolean firstCall = true;

            @Override
            public boolean markSupported() {
                return false;
            }

            @Override
            public int read(byte[] b) throws IOException {
                if (firstCall) {
                    firstCall = false;
                    return -1;
                }
                return super.read(b);
            }
        };

        boolean result = echoHandler.handle(inputStream, outputStream);

        assertFalse(result);
    }

    @Test
    void testHandle_IOException() {
        // Create input stream that throws IOException
        inputStream = new ByteArrayInputStream(new byte[]{0x01}) {
            @Override
            public int read(byte[] b) throws IOException {
                throw new IOException("Simulated IO error");
            }
        };

        boolean result = echoHandler.handle(inputStream, outputStream);

        assertFalse(result);
    }

    @Test
    void testHandle_OutputStreamError() throws IOException {
        String message = "Test message";
        byte[] frameData = createSimpleTextFrame(message);
        inputStream = new ByteArrayInputStream(frameData);

        // Create output stream that throws exception on write
        OutputStream errorOutputStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("Output stream error");
            }
        };

        boolean result = echoHandler.handle(inputStream, errorOutputStream);

        assertFalse(result);
    }

    @Test
    void testHandle_MultipleFrames() throws IOException {
        String message1 = "First message";
        String message2 = "Second message";

        // Create multiple frames
        byte[] frame1 = createSimpleTextFrame(message1);
        byte[] frame2 = createSimpleTextFrame(message2);

        // Combine frames
        ByteArrayOutputStream combinedFrames = new ByteArrayOutputStream();
        combinedFrames.write(frame1);
        combinedFrames.write(frame2);

        inputStream = new ByteArrayInputStream(combinedFrames.toByteArray());

        boolean result = echoHandler.handle(inputStream, outputStream);

        assertTrue(result);
        assertTrue(outputStream.size() > 0);
    }

    @Test
    void testHandle_LargePayload() throws IOException {
        // Create large payload (near the 70_000 byte buffer limit)
        StringBuilder largeMessage = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeMessage.append("This is a large message test. ");
        }

        byte[] frameData = createSimpleTextFrame(largeMessage.toString());
        inputStream = new ByteArrayInputStream(frameData);

        boolean result = echoHandler.handle(inputStream, outputStream);

        assertTrue(result);
        assertTrue(outputStream.size() > 0);
    }

    @Test
    void testHandle_DifferentFrameTypes() throws IOException {
        // Test with different frame types (binary, close, etc.)
        // For now, testing with text frame but different opcodes
        String message = "Binary test";
        byte[] frameData = createFrameWithOpcode(message, (byte) 0x02); // Binary frame
        inputStream = new ByteArrayInputStream(frameData);

        boolean result = echoHandler.handle(inputStream, outputStream);

        assertTrue(result);
    }

    @Test
    void testHandle_FragmentedFrame() throws IOException {
        String message = "Fragmented";
        byte[] frameData = createFragmentedFrame(message);
        inputStream = new ByteArrayInputStream(frameData);

        boolean result = echoHandler.handle(inputStream, outputStream);

        assertTrue(result);
    }

    @Test
    void testHandle_ZeroBytesRead() {
        inputStream = new ByteArrayInputStream(new byte[10]) {
            @Override
            public int read(byte[] b) {
                return 0; // Return 0 bytes read but not end of stream
            }
        };

        boolean result = echoHandler.handle(inputStream, outputStream);

        assertFalse(result);
    }

    // Helper methods to create test frames

    private byte[] createSimpleTextFrame(String message) {
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream frame = new ByteArrayOutputStream();

        try {
            // FIN + TEXT opcode
            frame.write(0x81);

            // Payload length (assuming < 126)
            if (payload.length < 126) {
                frame.write(payload.length);
            } else if (payload.length < 65536) {
                frame.write(126);
                frame.write((payload.length >> 8) & 0xFF);
                frame.write(payload.length & 0xFF);
            }

            // Payload
            frame.write(payload);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return frame.toByteArray();
    }

    private byte[] createMaskedTextFrame(String message, byte[] maskingKey) {
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        byte[] maskedPayload = FrameUtil.maskPayload(payload, maskingKey);
        ByteArrayOutputStream frame = new ByteArrayOutputStream();

        try {
            // FIN + TEXT opcode
            frame.write(0x81);

            // Masked flag + payload length
            if (payload.length < 126) {
                frame.write(0x80 | payload.length);
            }

            // Masking key
            frame.write(maskingKey);

            // Masked payload
            frame.write(maskedPayload);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return frame.toByteArray();
    }

    private byte[] createFrameWithOpcode(String message, byte opcode) {
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream frame = new ByteArrayOutputStream();

        try {
            // FIN + custom opcode
            frame.write(0x80 | opcode);

            // Payload length
            frame.write(payload.length);

            // Payload
            frame.write(payload);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return frame.toByteArray();
    }

    private byte[] createFragmentedFrame(String message) {
        byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream frame = new ByteArrayOutputStream();

        try {
            // No FIN + TEXT opcode (fragmented)
            frame.write(0x01);

            // Payload length
            frame.write(payload.length);

            // Payload
            frame.write(payload);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return frame.toByteArray();
    }
}