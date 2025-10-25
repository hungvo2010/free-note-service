package com.freenote.app.server.util;

import com.freenote.app.server.frames.base.WebSocketFrame;
import io.NoHeaderObjectOutputStream;
import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

@UtilityClass
public class IOUtils {
    public static void writeOutPut(OutputStream outputStream, WebSocketFrame mergedFrame) {
        try {
            var objectOutputStream = new NoHeaderObjectOutputStream(outputStream);
            objectOutputStream.writeObject(mergedFrame);
            objectOutputStream.flush();
        } catch (IOException e) {
            log.error("Error writing output stream", e);
        }
    }

    private static final Logger log = LogManager.getLogger(IOUtils.class);

    public static byte[] getRawBytes(InputStream inputStream) throws IOException {
        // Read first byte (opcode)
        log.info("Reading first byte...");
        int firstByte = inputStream.read();
        if (firstByte == -1) {
            log.warn("End of stream reached");
            return null;
        }

        // Read second byte (payload length + mask)
        int secondByte = inputStream.read();
        if (secondByte == -1) {
            log.warn("Incomplete frame - missing second byte");
            return null;
        }

        boolean masked = (secondByte & 0x80) != 0;
        int payloadLen7 = secondByte & 0x7F;

        // Determine payload length
        long payloadLength;
        if (payloadLen7 == 126) {
            byte[] ext = inputStream.readNBytes(2);
            if (ext.length < 2) return null;
            payloadLength = ((ext[0] & 0xFF) << 8) | (ext[1] & 0xFF);
        } else if (payloadLen7 == 127) {
            byte[] ext = inputStream.readNBytes(8);
            if (ext.length < 8) return null;
            payloadLength = ByteBuffer.wrap(ext).getLong();
        } else {
            payloadLength = payloadLen7;
        }

        // Read mask if present
        byte[] mask = null;
        if (masked) {
            mask = inputStream.readNBytes(4);
            if (mask.length < 4) return null;
        }

        // Compute total bytes to read
        long totalFrameLength = 2
                + (payloadLen7 == 126 ? 2 : (payloadLen7 == 127 ? 8 : 0))
                + (masked ? 4 : 0)
                + payloadLength;

        log.info("Total frame length: {}", totalFrameLength);

        if (totalFrameLength > Integer.MAX_VALUE || totalFrameLength < 2) {
            log.warn("Invalid total frame length: {}", totalFrameLength);
            return null;
        }

        byte[] frameData = new byte[(int) totalFrameLength];
        frameData[0] = (byte) firstByte;
        frameData[1] = (byte) secondByte;

        int totalRead = 2;
        while (totalRead < totalFrameLength) {
            int read = inputStream.read(frameData, totalRead, (int) (totalFrameLength - totalRead));
            if (read == -1) {
                log.warn("Stream ended before complete frame read ({} of {})", totalRead, totalFrameLength);
                return null;
            }
            totalRead += read;
        }

        if (totalRead != totalFrameLength) {
            log.warn("Frame truncated: expected {}, got {}", totalFrameLength, totalRead);
            return null;
        }

        return frameData;
    }
}
