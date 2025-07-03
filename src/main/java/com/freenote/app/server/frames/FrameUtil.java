package com.freenote.app.server.frames;

import lombok.experimental.UtilityClass;

import java.util.Arrays;

@UtilityClass
public class FrameUtil {
    public static int parsePayloadLength(byte[] bytes) {
        int sevenBitsValue = (bytes[1] & 0x7F); // 0111 1111 // Because in Java bit manipulation will result in integer.
        if (sevenBitsValue < 126) {
            return sevenBitsValue;
        } else if (sevenBitsValue == 126) {
            // 126 means the next two bytes are the payload length
            if (bytes.length < 4) {
                throw new IllegalArgumentException("Payload length is too short for extended payload length");
            }
            return bytesToInt(Arrays.copyOfRange(bytes, 2, 4)); // Combine two bytes into one short
        } else {
            // 127 means the next 8 bytes are the payload length
            if (bytes.length < 10) {
                throw new IllegalArgumentException("Payload length is too short for extended payload length");
            }
            return bytesToInt(Arrays.copyOfRange(bytes, 2, 10)); // Combine eight bytes into one long
        }
    }

    public static int bytesToInt(byte[] bytes) {
        int result = 0;
        for (byte eachByte : bytes) {
            result = result << 8 | (eachByte & 0xFF); // Ensure to mask with 0xFF to avoid sign extension
        }
        return result;
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
