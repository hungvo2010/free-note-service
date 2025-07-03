package com.freenote.app.server.handler.impl;


import com.freenote.app.server.frames.PingFrame;
import com.freenote.app.server.handler.URIHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.freenote.app.server.frames.FrameUtil.bytesToInt;

public class MockHandler implements URIHandler {
    private static final Logger log = LogManager.getLogger(MockHandler.class);

    @Override
    public void handle(InputStream inputStream, OutputStream outputStream) {
        try {
            var reader = new BufferedReader(new InputStreamReader(inputStream));
            while (reader.ready()) {
                byte[] data = new byte[70000];
                var byteNumber = inputStream.read(data);
                log.info("byteNumber: {}", byteNumber);
                log.info("opcode: {}", data[0] & 0x0F); // Extracting the opcode from the first byte
                log.info("Frame masked: {}", (data[1] & 0xFF) >>> 7);
                log.info("Payload length: {}, {}", data[1] & 0x7F,
                        (data[1] & 0x7F) == 126 ?
                                bytesToInt(Arrays.copyOfRange(data, 2, 4)) :
                                bytesToInt(Arrays.copyOfRange(data, 2, 10)));

                var payloadLength = (data[1] & 0x7F) < 126 ? (data[1] & 0x7F) : bytesToInt(Arrays.copyOfRange(data, 2, 4));
//                var startMaskingKey = 2 + ((data[1] & 0x7F) < 126 ? 0 : ((data[1] & 0x7F) == 126 ? 2 : 8));
                var startMaskingKey = 2 + (((payloadLength / 255) == 0) ? 0 : (payloadLength / 255 == 2 ? 2 : 8));
                var maskingKey = Arrays.copyOfRange(data, startMaskingKey, startMaskingKey + 4);
                var startPayload = startMaskingKey + 4;
                log.info("Start masking key: {}", startMaskingKey);
                log.info("Masking key: {}", maskingKey);
                log.info("Payload: {}", Arrays.toString(maskPayload(Arrays.copyOfRange(data, startPayload, startPayload + payloadLength), maskingKey)));
                log.info("Payload in text: {}", new String(
                        maskPayload(Arrays.copyOfRange(data, startPayload, startPayload + payloadLength), maskingKey), StandardCharsets.UTF_8));
                var objectOutputStream = new NoHeaderObjectOutputStream(outputStream);
                log.info("Writing to output stream: {}", "");
//                objectOutputStream.writeObject(new TextFrame("chúng ta là một gia đình".getBytes(StandardCharsets.UTF_8)));
                objectOutputStream.writeObject(new PingFrame());
//                outputStream.write(createServerFrame("chúng ta là một gia đình".getBytes(StandardCharsets.UTF_8), FrameType.TEXT));
//                outputStream.flush();
                objectOutputStream.flush();
            }
        } catch (IOException e) {
            log.error("Error handling input stream", e);
        }
    }


    public static byte[] maskPayload(byte[] payload, byte[] maskingKey) {
        if (maskingKey.length != 4) {
            throw new IllegalArgumentException("Masking key must be 4 bytes long");
        }
        byte[] result = new byte[payload.length];
        for (int i = 0; i < payload.length; i++) {
            result[i] = (byte) (payload[i] ^ maskingKey[i % 4]); // XOR each byte with the masking key
        }
        return result;
    }
}
