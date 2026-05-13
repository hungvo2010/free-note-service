package com.freenote.app.server.messages.ws;

import com.freenote.app.server.exceptions.InvalidFrameException;
import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.handler.frames.WebSocketFrameDispatcher;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.util.Arrays;

import static com.freenote.app.server.util.FrameUtil.boolToBit;

@Getter
@Setter
public abstract class WebSocketFrame implements Serializable, Externalizable {
    @Serial
    private static final long serialVersionUID = -2140098214102580912L;
    private boolean fin = true;
    private boolean rsv1 = false;
    private boolean rsv2 = false;
    private boolean rsv3 = false;
    protected short opcode;
    protected boolean isMasked = false;
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
    }

    protected WebSocketFrame(boolean isFinal, short opcode, byte[] payloadData) {
        this.fin = isFinal;
        this.opcode = opcode;
        this.payloadData = payloadData;
        this.payloadLength = payloadData.length;
    }

    protected WebSocketFrame(short opcode) {
        this(false, opcode, new byte[0]);
    }

    protected WebSocketFrame(short opcode, boolean isMasked) {
        this(false, opcode, new byte[0]);
        this.isMasked = isMasked;
    }

    protected WebSocketFrame(byte[] bytes) {
        try {
            parseHeader(bytes);
            parsePayloadLength(bytes);
            parseMaskingKey(bytes);
            parsePayload(bytes);
        } catch (Exception e) {
            throw new InvalidFrameException("Exception when parsing raw bytes to WebSocket frame", e);
        }
    }

    protected abstract void parsePayloadLength(byte[] bytes);

    protected abstract void parseMaskingKey(byte[] bytes);

    protected abstract void parsePayload(byte[] bytes);

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
        if (isMasked) {
            out.write(maskingKey);
        }
    }

    public abstract void writePayloadLength(ObjectOutput out) throws IOException;

    public abstract void writeFrameMaskHeader(ObjectOutput out) throws IOException;

    private void writeFrameOpcode(ObjectOutput out) throws IOException {
        var firstByte = (byte) ((boolToBit(fin) << 7) | (boolToBit(rsv1) << 6) | (boolToBit(rsv2) << 5) | (boolToBit(rsv3) << 4) | (opcode & 0x0F));
        out.writeByte(firstByte);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        // TODO: Implement readExternal method if needed
    }

    public abstract int getTotalFrameLength();

    @Override
    public String toString() {
        return String.join("\n",
                "FIN: " + isFin(),
                "Opcode: " + getOpcode() + " - " + FrameType.fromHexValue(getOpcode()),
                "Masked: " + isMasked(),
                "Payload Length: " + getPayloadLength(),
                "Masking Key: " + Arrays.toString(getMaskingKey()),
                "Payload: " + WebSocketFrameDispatcher.getContent(this)
        );
    }
}