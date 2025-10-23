package com.freenote.app.server.util;

import com.freenote.app.server.frames.base.WebSocketFrame;
import io.NoHeaderObjectOutputStream;
import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

        // Calculate total frame length needed
        int baseLength = 2; // opcode + length/mask byte
        int payloadLength = secondByte & 0x7F;
        boolean masked = (secondByte & 0x80) != 0;

        // Handle extended payload length
        if (payloadLength == 126) {
            baseLength += 2; // 2 more bytes for length
        } else if (payloadLength == 127) {
            baseLength += 8; // 8 more bytes for length
        }

        // Add masking key length
        if (masked) {
            baseLength += 4;
        }

        // Add actual payload length (simplified for small frames)
        int totalFrameLength = baseLength + (payloadLength < 126 ? payloadLength : 0);

        // Read complete frame
        byte[] frameData = new byte[totalFrameLength];
        frameData[0] = (byte) firstByte;
        frameData[1] = (byte) secondByte;

        int totalRead = 2;
        while (totalRead < totalFrameLength) {
            int read = inputStream.read(frameData, totalRead, totalFrameLength - totalRead);
            if (read == -1) {
                log.warn("Stream ended before complete frame read");
                return null;
            }
            totalRead += read;
        }

        return frameData;
    }
}
