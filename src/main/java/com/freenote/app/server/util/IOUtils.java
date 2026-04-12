package com.freenote.app.server.util;

import com.freenote.app.server.exceptions.ConnectionException;
import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.io.NoHeaderObjectOutputStream;
import com.freenote.app.server.messages.WebSocketFrame;
import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

@UtilityClass
public class IOUtils {
    public static void writeOutPut(OutputStream outputStream, WebSocketFrame mergedFrame) throws IOException {
        try {
            var objectOutputStream = new NoHeaderObjectOutputStream(outputStream);
            objectOutputStream.writeObject(mergedFrame);
            objectOutputStream.flush();
        } catch (IOException e) {
            log.error("Error writing output stream", e);
            throw e;
        }
    }

    public static void writeOutPut(OutputStream outputStream, byte[] bytesData) throws ConnectionException {
        try {
            outputStream.write(bytesData);
            outputStream.flush();
        } catch (IOException e) {
            log.error("Error writing output stream", e);
            throw new ConnectionException("Error writing output stream", e);
        }
    }

    private static final Logger log = LogManager.getLogger(IOUtils.class);

    public static byte[] createRawFrame(byte[] payload, FrameType frameType) {
        byte[] frame = new byte[payload.length + 2];
        frame[0] = frameType.getHexValue();
        frame[1] = (byte) payload.length;
        System.arraycopy(payload, 0, frame, 2, payload.length);
        return frame;
    }

    public static byte[] getRawBytes(InputStream inputStream) throws IOException {
        DataInputStream dis = (inputStream instanceof DataInputStream)
                ? (DataInputStream) inputStream
                : new DataInputStream(inputStream);

        // Read first two bytes of the frame header
        int b0 = dis.read();
        if (b0 == -1) {
            throw new EOFException("End of stream while reading first byte");
        }
        int b1 = dis.read();
        if (b1 == -1) {
            throw new EOFException("End of stream while reading second byte");
        }

        boolean masked = (b1 & 0x80) != 0;
        int payloadLen7 = b1 & 0x7F;

        // Extended payload length (if any)
        int extLen = (payloadLen7 == 126) ? 2 : (payloadLen7 == 127 ? 8 : 0);
        byte[] ext = null;
        long payloadLength;
        if (extLen == 2) {
            ext = new byte[2];
            dis.readFully(ext);
            payloadLength = convertBytesToDecimal(ext);
        } else if (extLen == 8) {
            ext = new byte[8];
            dis.readFully(ext);
            // Per RFC 6455, the most significant bit must be 0 for 64-bit length
            if ((ext[0] & 0x80) != 0) {
                throw new IOException("Invalid 64-bit payload length (MSB set)");
            }
            payloadLength = convertBytesToDecimal(ext);
        } else {
            payloadLength = payloadLen7;
        }

        // Compute header and total lengths
        int headerLen = 2 + extLen + (masked ? 4 : 0);
        long totalFrameLength = (long) headerLen + payloadLength;

        if (totalFrameLength > Integer.MAX_VALUE) {
            throw new IOException("Frame too large: " + totalFrameLength);
        }

        byte[] frameData = new byte[(int) totalFrameLength];
        int offset = 0;
        frameData[offset++] = (byte) b0;
        frameData[offset++] = (byte) b1;

        if (extLen > 0) {
            System.arraycopy(ext, 0, frameData, offset, extLen);
            offset += extLen;
        }

        byte[] mask;
        if (masked) {
            mask = new byte[4];
            dis.readFully(mask);
            System.arraycopy(mask, 0, frameData, offset, 4);
            offset += 4;
        }

        // Read payload bytes fully
        if (payloadLength > 0) {
            dis.readFully(frameData, offset, (int) payloadLength);
        }

        log.info("Read websocket frame: headerLen={}, payloadLen={}, total={}, (bytes)",
                headerLen, payloadLength, totalFrameLength);

        return frameData;
    }

    private static long convertBytesToDecimal(byte[] ext) {
        long len = 0L;
        for (byte b : ext) {
            len = (len << 8) | (b & 0xFF);
        }
        return len;
    }

    public static InputStream newInputStream(java.nio.ByteBuffer byteBuffer) {
        return new InputStream() {
            @Override
            public int read() {
                return byteBuffer.hasRemaining() ? byteBuffer.get() & 0xFF : -1;
            }

            @Override
            public int read(byte[] b, int off, int len) {
                if (!byteBuffer.hasRemaining()) return -1;
                int toRead = Math.min(len, byteBuffer.remaining());
                byteBuffer.get(b, off, toRead);
                return toRead;
            }
        };
    }
}
