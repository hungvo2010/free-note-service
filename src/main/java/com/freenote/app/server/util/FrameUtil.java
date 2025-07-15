package com.freenote.app.server.util;

import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.function.LongUnaryOperator;
import java.util.function.Supplier;

@UtilityClass
public class FrameUtil {
    public static int calculatePayloadLength(byte byteData) {
        int sevenBitsValue = (byteData & 0x7F); // 0111 1111 // Because in Java bit manipulation will result in integer.
        if (sevenBitsValue < 126) {
            return sevenBitsValue;
        } else if (sevenBitsValue == 126) {
            return 2; // Next two bytes are the payload length
        } else {
            return 8; // Next eight bytes are the payload length
        }
    }

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

    public Supplier<Integer> getPayloadLengthSupplier(byte[] frame) {
        return () -> parsePayloadLength(frame);
    }

    public static final LongUnaryOperator BIG_PAYLOAD_OPERATOR = length -> length < 65535 ? 126 : 127;
    public static final LongUnaryOperator PAYLOAD_LENGTH_OPERATOR = length -> length < 126 ? length : BIG_PAYLOAD_OPERATOR.applyAsLong(length);

    public static LongUnaryOperator getFramePayloadLengthSupplier() {
        return PAYLOAD_LENGTH_OPERATOR;
    }
}
