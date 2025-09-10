package com.freenote.app.server.frames.base;

import com.freenote.app.server.util.FrameUtil;

import java.io.IOException;
import java.io.ObjectOutput;
import java.util.Arrays;

import static com.freenote.app.server.util.FrameUtil.getFramePayloadLengthSupplier;
import static com.freenote.app.server.util.FrameUtil.getMaskingKeyStartSupplier;

public class DataFrame extends WebSocketFrame {

    private static final int DEFAULT_MASKING_KEY_LENGTH = 4;
    private static final int MAX_PAYLOAD_LENGTH_2_BYTES = 65536;
    private static final int MAX_PAYLOAD_LENGTH_7_BITS = 126;

    public DataFrame() {
        throw new UnsupportedOperationException("Default constructor is not supported for DataFrame. Use parameterized constructors instead.");
    }

    public DataFrame(short opCode, byte[] bytes) {
        super(opCode, bytes);
    }

    public DataFrame(short opCode, byte[] bytes, boolean isMasked, byte[] maskingKey) {
        super(opCode, bytes);
        this.isMasked = isMasked;
        this.maskingKey = maskingKey;
    }

    private DataFrame(byte[] payload) {
        super(payload);
    }

    public static DataFrame fromRawFrameBytes(byte[] rawFrameBytes) {
        return new DataFrame(rawFrameBytes);
    }

    @Override
    protected void parsePayloadLength(byte[] bytes) {
        isMasked = ((bytes[1] & 0x80) >> 7) == 1;
        payloadLength = FrameUtil.parsePayloadLength(bytes);
    }

    @Override
    protected void parsePayload(byte[] bytes) {
        var maskingKeyStart = getMaskingKeyStartSupplier().applyAsInt(bytes[1] & 0x7F);
        var payloadDataStart = maskingKeyStart + maskingKey.length;
        payloadData = Arrays.copyOfRange(bytes, payloadDataStart, payloadDataStart + (int) payloadLength);
    }

    @Override
    public void writeFrameMaskHeader(ObjectOutput out) throws IOException {
        var maskByte = isMasked ? (byte) 0x80 : (byte) 0x00;
        var secondByte = (byte) (maskByte | (byte) (getFramePayloadLengthSupplier().applyAsLong(payloadLength)));
        out.writeByte(secondByte);
    }

    @Override
    public int getTotalFrameLength() {
        var fixedHeaderLength = 2;
        var extendedPayloadLength = payloadLength >= MAX_PAYLOAD_LENGTH_2_BYTES ? 8 : (payloadLength >= MAX_PAYLOAD_LENGTH_7_BITS ? 2 : 0);
        var maskingKeyLength = isMasked ? DEFAULT_MASKING_KEY_LENGTH : 0;
        return fixedHeaderLength + extendedPayloadLength + maskingKeyLength + (int) this.payloadLength;
    }

    @Override
    public void writePayloadLength(ObjectOutput out) throws IOException {
        if (payloadLength < MAX_PAYLOAD_LENGTH_7_BITS) {
            return;
        }
        if (payloadLength < MAX_PAYLOAD_LENGTH_2_BYTES) {
            out.writeShort((short) payloadLength);
            return;
        }
        out.writeLong(payloadLength);
    }

    @Override
    public void parseMaskingKey(byte[] bytes) {
        var maskingKeyStart = getMaskingKeyStartSupplier().applyAsInt(bytes[1] & 0x7F);
        maskingKey = isMasked ? Arrays.copyOfRange(bytes, maskingKeyStart, maskingKeyStart + DEFAULT_MASKING_KEY_LENGTH) : new byte[0]; // Masking key is present if masked is true
    }

    @Override
    public void writePayload(ObjectOutput out) throws IOException {
        out.write(isMasked ? FrameUtil.maskPayload(payloadData, maskingKey) : payloadData);
    }
}
