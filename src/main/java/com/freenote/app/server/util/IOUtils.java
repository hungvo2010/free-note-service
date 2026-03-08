package com.freenote.app.server.util;

import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.io.NoHeaderObjectOutputStream;
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

    private static final Logger log = LogManager.getLogger(IOUtils.class);

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
            payloadLength = ((ext[0] & 0xFF) << 8) | (ext[1] & 0xFF);
        } else if (extLen == 8) {
            ext = new byte[8];
            dis.readFully(ext);
            // Per RFC 6455, the most significant bit must be 0 for 64-bit length
            if ((ext[0] & 0x80) != 0) {
                throw new IOException("Invalid 64-bit payload length (MSB set)");
            }
            long len = 0L;
            for (int i = 0; i < 8; i++) {
                len = (len << 8) | (ext[i] & 0xFF);
            }
            payloadLength = len;
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
