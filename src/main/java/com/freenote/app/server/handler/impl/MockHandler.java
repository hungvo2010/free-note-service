package com.freenote.app.server.handler.impl;

import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.handler.URIHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.BitSet;

import static com.freenote.app.server.frames.FrameFactory.createServerFrame;

public class MockHandler implements URIHandler {
    private static final Logger log = LogManager.getLogger(MockHandler.class);

    @Override
    public void handle(InputStream inputStream, OutputStream outputStream) {
        try {
            var reader = new BufferedReader(new InputStreamReader(inputStream));
            while (reader.ready()) {
                byte[] data = new byte[50];
                var byteNumber = inputStream.read(data);
                log.info("byteNumber: {}", byteNumber);
                log.info("opcode: {}", data[0] & 0x0F); // Extracting the opcode from the first byte
                log.info("Reading from input stream: {}", data);
                log.info("Frame masked: {}", (data[1] & 0xFF) >>> 7);
                log.info("Payload length: {}", data[1] & 0x7F);
                var payloadLength = data[1] & 0x7F;
                log.info("Masking key: {}", Arrays.toString(Arrays.copyOfRange(data, 2, 6)));
                log.info("Payload: {}", Arrays.toString(maskPayload(Arrays.copyOfRange(data, 6, 6 + payloadLength), Arrays.copyOfRange(data, 2, 6))));
                log.info("Payload in text: {}", new String(maskPayload(Arrays.copyOfRange(data, 6, 6 + payloadLength), Arrays.copyOfRange(data, 2, 6)), StandardCharsets.UTF_8));
                outputStream.write(createServerFrame("chúng ta là một gia đình".getBytes(StandardCharsets.UTF_8), FrameType.TEXT));
                outputStream.flush();
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

    public static byte[] toLittleEndian(int value) {
        ByteBuffer buffer = ByteBuffer.allocate(4); // 4 bytes for an integer
        buffer.order(ByteOrder.LITTLE_ENDIAN); // Set byte order to little-endian
        buffer.putInt(value);
        return buffer.array();
    }


    public static byte[] bitSetToByteArray(BitSet bitSet) {
        log.info("bitSet: {}", bitSet);
        log.info("opcode: {}", bitSet.get(4, 8).toString());
        var littleEndians = bitSet.toByteArray();
        log.info("Little Endian: {}", Arrays.toString(littleEndians));
        var bigEndians = littleEndianToBigEndian(littleEndians);
        log.info("Big Endian: {}", Arrays.toString(bigEndians));
        return bitSetToByteArrayExact(bitSet, 24);
    }

    public static byte[] bitSetToByteArrayExact(BitSet bitSet, int bitLength) {
        int byteLength = (bitLength + 7) / 8;
        byte[] bytes = new byte[byteLength];
        for (int i = 0; i < bitLength; i++) {
            if (bitSet.get(i)) {
                bytes[i / 8] |= 1 << (7 - (i % 8)); // big-endian bit placement
            }
        }
        return bytes;
    }

    public static byte[] toLittleEndian(byte[] bigEndianBytes) {
        byte[] littleEndianBytes = new byte[bigEndianBytes.length];
        for (int i = 0; i < bigEndianBytes.length; i++) {
            littleEndianBytes[i] = bigEndianBytes[bigEndianBytes.length - 1 - i];
        }
        return littleEndianBytes;
    }

    public static byte[] littleEndianToBigEndian(byte[] littleEndianBytes) {
        int length = littleEndianBytes.length;

        // Reverse the little-endian array to get big-endian
        byte[] bigEndian = new byte[length];
        for (int i = 0; i < length; i++) {
            bigEndian[i] = littleEndianBytes[length - 1 - i];
        }
        return bigEndian;
    }
}
