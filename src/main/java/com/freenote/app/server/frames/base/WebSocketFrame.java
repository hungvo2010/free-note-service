package com.freenote.app.server.frames.base;

import lombok.Getter;

import java.io.*;
import java.util.Arrays;

@Getter
public abstract class WebSocketFrame implements Serializable, Externalizable {
    @Serial
    private static final long serialVersionUID = -2140098214102580912L;
    private boolean fin = true;
    private boolean rsv1 = false;
    private boolean rsv2 = false;
    private boolean rsv3 = false;
    private int maskingKeyStart = 0;
    protected short opcode;
    protected boolean masked = false;
    protected long payloadLength;
    protected byte[] maskingKey;
    protected byte[] payloadData;
    private byte[] extensionData;
    private byte[] applicationData;

    protected WebSocketFrame() {
    }

    protected WebSocketFrame(short opcode, byte[] payloadData) {
        this.opcode = opcode;
        this.payloadData = payloadData;
        this.payloadLength = payloadData.length;
        this.maskingKeyStart = payloadData.length < 126 ? 2 : (payloadData.length < 65536 ? 4 : 10);
    }

    protected WebSocketFrame(boolean isFinal, short opcode, byte[] payloadData) {
        this.fin = isFinal;
        this.opcode = opcode;
        this.payloadData = payloadData;
        this.payloadLength = payloadData.length;
        this.maskingKeyStart = payloadData.length < 126 ? 2 : (payloadData.length < 65536 ? 4 : 10);
    }

    protected WebSocketFrame(byte[] bytes) {
        parseHeader(bytes);
        parsePayloadLength(bytes);
        parseMaskingKey(bytes);
        parsePayload(bytes);
    }

    protected WebSocketFrame(FrameBuilder frameBuilder, FrameType frameType) {
        this.fin = frameBuilder.isFin();
        this.rsv1 = frameBuilder.isRsv1();
        this.rsv2 = frameBuilder.isRsv2();
        this.rsv3 = frameBuilder.isRsv3();
        this.opcode = frameType.getOpcode();
        this.masked = frameBuilder.isMasked();
        frameType.handleVariableLength(frameBuilder);
    }

    protected abstract void parsePayloadLength(byte[] bytes);

    protected void parseMaskingKey(byte[] bytes) {
        var maskingKeyStart = 2 + ((bytes[1] & 0x7F) < 126 ? 0 : ((bytes[1] & 0x7F) == 126 ? 4 : 10));
        maskingKey = masked ? Arrays.copyOfRange(bytes, maskingKeyStart, maskingKeyStart + 4) : new byte[0]; // Masking key is present if masked is true
    }

    protected void parsePayload(byte[] bytes) {
        payloadData = Arrays.copyOfRange(bytes, maskingKeyStart + maskingKey.length, bytes.length);
        extensionData = new byte[0];
        applicationData = bytes;
    }

    private void parseHeader(byte[] bytes) {
        fin = ((bytes[0] & 0x80) >> 7) == 1; // 1000 0000
        rsv1 = ((bytes[0] & 0x40) >> 6) == 1; // 0100 0000
        rsv2 = ((bytes[0] & 0x20) >> 5) == 1; // 0010 0000
        rsv3 = ((bytes[0] & 0x10) >> 4) == 1; // 0001 0000
        opcode = (short) (bytes[0] & 0x0F);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        writeFrameOpcode(out);
        writeFrameMaskHeader(out);
        writePayloadLength(out);
        writeMaskingKey(out);
        writePayload(out);
    }

    public abstract void writePayload(ObjectOutput out) throws IOException;

    private void writeMaskingKey(ObjectOutput out) throws IOException {
        if (masked) {
            out.write(maskingKey);
        }
    }

    public abstract void writePayloadLength(ObjectOutput out) throws IOException;

    public abstract void writeFrameMaskHeader(ObjectOutput out) throws IOException;

    private void writeFrameOpcode(ObjectOutput out) throws IOException {
        var firstByte = (byte) (
                (fin ? 0x80 : 0) |    // FIN bit - 1st bit (10000000)
                        (rsv1 ? 0x40 : 0) |   // RSV1 bit - 2nd bit (01000000)
                        (rsv2 ? 0x20 : 0) |   // RSV2 bit - 3rd bit (00100000)
                        (rsv3 ? 0x10 : 0) |   // RSV3 bit - 4th bit (00010000)
                        (opcode & 0x0F)       // Opcode - lower 4 bits (00001111)
        );
        out.writeByte(firstByte);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        // TODO: Implement readExternal method if needed
    }
}