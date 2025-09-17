package com.freenote.app.server.handler.impl;


import com.freenote.annotations.URIHandlerImplementation;
import com.freenote.app.server.factory.ClientFrameFactory;
import com.freenote.app.server.factory.ServerFrameFactory;
import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.util.FrameUtil;
import com.freenote.app.server.util.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@URIHandlerImplementation("/example")
public class EchoHandler implements URIHandler {
    private static final Logger log = LogManager.getLogger(EchoHandler.class);
    private static final ServerFrameFactory serverFrameFactory = new ServerFrameFactory();
    private static final ClientFrameFactory clientFrameFactory = new ClientFrameFactory();

    @Override
    public boolean handle(InputStream inputStream, OutputStream outputStream) {
        try {
            var reader = new BufferedReader(new InputStreamReader(inputStream));
//            while (reader.ready()) {
                byte[] actualData = getRawBytes(inputStream);
                if (actualData == null) return false;

                log.info("Raw bytes length: {}", actualData.length);
                log.info("First 10 bytes: {}", Arrays.toString(Arrays.copyOf(actualData, Math.min(10, actualData.length))));

                // Show as unsigned values
                for (int i = 0; i < Math.min(10, actualData.length); i++) {
                    log.info("Byte[{}]: signed={}, unsigned={}, hex=0x{}",
                            i, actualData[i], actualData[i] & 0xFF, Integer.toHexString(actualData[i] & 0xFF));
                }
                WebSocketFrame frame = clientFrameFactory.createFrameFromBytes(actualData);

                log.info("FIN: {}", frame.isFin());
                log.info("Opcode: {} - {}", frame.getOpcode(), FrameType.fromHexValue(frame.getOpcode()));
                log.info("Masked: {}", frame.isMasked());
                log.info("Payload Length: {}", frame.getPayloadLength());
                log.info("Masking Key: {}", frame.getMaskingKey());

                byte[] payload = frame.isMasked() ? FrameUtil.maskPayload(frame.getPayloadData(), frame.getMaskingKey()) : frame.getPayloadData();
                String content = new String(payload, StandardCharsets.UTF_8);

                log.info("Writing to output stream with payload: {}", content);
                log.info("===========================================================================");
                IOUtils.writeOutPut(outputStream, serverFrameFactory.createTextFrame(content));
//            }
            return true;
        } catch (IOException e) {
            log.error("Error handling input stream", e);
            return false;
        }
    }

    @Override
    public boolean continuationHandler(List<WebSocketFrame> clientFrame, InputStream inputStream, OutputStream outputStream) {
        return false;
    }

    public byte[] getRawBytes(InputStream inputStream) throws IOException {
        // Read first byte (opcode)
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
