package com.freenote.app.server.util;

import com.freenote.app.server.exceptions.InvalidFrameException;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.frames.factory.FrameFactory;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.function.IntUnaryOperator;
import java.util.function.LongUnaryOperator;
import java.util.function.Supplier;

@UtilityClass
public class FrameUtil {

    public static int parsePayloadLength(byte[] bytes) {
        int sevenBitsValue = (bytes[1] & 0x7F); // 0111 1111 // Because in Java bit manipulation will result in integer.
        if (sevenBitsValue < 126) {
            return sevenBitsValue;
        } else if (sevenBitsValue == 126) {
            // 126 means the next two bytes are the payload length
            if (bytes.length < 4) {
                throw new InvalidFrameException("Payload length is too short for extended payload length");
            }
            return bytesToInt(Arrays.copyOfRange(bytes, 2, 4)); // Combine two bytes into one short
        } else {
            // 127 means the next 8 bytes are the payload length
            if (bytes.length < 10) {
                throw new InvalidFrameException("Payload length is too short for extended payload length");
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
            throw new InvalidFrameException("Masking key must be 4 bytes long");
        }
        byte[] result = new byte[payload.length];
        for (int i = 0; i < payload.length; i++) {
            result[i] = (byte) (payload[i] ^ maskingKey[i % 4]); // XOR each byte with the masking key
        }
        return result;
    }

    public static WebSocketFrame createApplicationFrame(Object payload) {
        var textPayload = JSONUtils.toJSONString(payload);
        return FrameFactory.SERVER.createTextFrame(textPayload);
    }

    public Supplier<Integer> getPayloadLengthSupplier(byte[] frame) {
        return () -> parsePayloadLength(frame);
    }

    public static final LongUnaryOperator BIG_PAYLOAD_OPERATOR = length -> length < 65535 ? 126 : 127;
    public static final LongUnaryOperator PAYLOAD_LENGTH_OPERATOR = length -> length < 126 ? length : BIG_PAYLOAD_OPERATOR.applyAsLong(length);
    public static final IntUnaryOperator BIG_PAYLOAD_MASKING_KEY_START_OPERATOR = secondByte -> secondByte == 126 ? 4 : 10;
    public static final IntUnaryOperator MASKING_KEY_START_OPERATOR = secondByte -> secondByte < 126 ? 2 : BIG_PAYLOAD_MASKING_KEY_START_OPERATOR.applyAsInt(secondByte);

    public static LongUnaryOperator getFramePayloadLengthSupplier() {
        return PAYLOAD_LENGTH_OPERATOR;
    }

    public static IntUnaryOperator getMaskingKeyStartSupplier() {
        return MASKING_KEY_START_OPERATOR;
    }

    public static int boolToBit(boolean b) {
        return Boolean.compare(b, false);
    }
}
