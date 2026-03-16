package com.freenote.app.server.util;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.freenote.app.server.frames.base.DataFrame.DEFAULT_MASKING_KEY_LENGTH;

@Log4j2
public class FullFrameParser {
    @Setter
    private boolean masked;
    @Setter
    private byte[] extendedPayloadLength;
    @Getter
    private int headerLength;
    private long totalFrameLength;
    @Getter
    private long payloadLength;
    private int payloadLen7;
    private int finOpcodeByte;
    private int maskPayload7Byte;

    public void setMasked(int b1) {
        masked = (b1 & 0x80) != 0;
        payloadLen7 = b1 & 0x7F;
        this.maskPayload7Byte = b1;
    }

    public void setPayloadLength(int payloadLen7) {
        this.payloadLength = this.extendedPayloadLength.length == 0 ? payloadLen7 : convertBytesToDecimal(this.extendedPayloadLength);
    }

    public void buildExtendedPayload(DataInputStream dis, int payloadLen7) throws IOException {
        int extLen = (payloadLen7 == 126) ? 2 : (payloadLen7 == 127 ? 8 : 0);
        extendedPayloadLength = new byte[0];

        if (extLen == 2) {
            extendedPayloadLength = new byte[2];
            dis.readFully(extendedPayloadLength);
        } else if (extLen == 8) {
            extendedPayloadLength = new byte[8];
            dis.readFully(extendedPayloadLength);
            // Per RFC 6455, the most significant bit must be 0 for 64-bit length
            if ((extendedPayloadLength[0] & 0x80) != 0) {
                throw new IOException("Invalid 64-bit payload length (MSB set)");
            }
        }
    }

    public long getExtendedPayloadLength(byte[] extendedPayload) {
        return convertBytesToDecimal(extendedPayload);
    }

    public void setHeaderLength() {
        this.headerLength = 2 + this.extendedPayloadLength.length + (masked ? 4 : 0);
    }

    public void setTotalFrameLength() {
        this.totalFrameLength = this.headerLength + this.payloadLength;
    }

    public byte[] buildFullFrameData(DataInputStream dis) throws IOException {
        byte[] frameData = new byte[(int) totalFrameLength];
        int offset = 0;
        frameData[offset++] = (byte) this.finOpcodeByte;
        frameData[offset++] = (byte) this.maskPayload7Byte;

        int extLen = copyExtendedPayloadLength(frameData, offset);
        offset += extLen;

        offset = copyMaskingKey(dis, frameData, offset);

        // Read payload bytes fully
        if (payloadLength > 0) {
            dis.readFully(frameData, offset, (int) payloadLength);
        }

        log.debug("Read websocket frame: headerLen={}, payloadLen={}, total={}, (bytes)", getHeaderLength()
                , payloadLength, totalFrameLength);

        return frameData;
    }

    private int copyMaskingKey(DataInputStream dis, byte[] frameData, int offset) throws IOException {
        byte[] mask;
        if (masked) {
            mask = new byte[DEFAULT_MASKING_KEY_LENGTH];
            dis.readFully(mask);
            System.arraycopy(mask, 0, frameData, offset, DEFAULT_MASKING_KEY_LENGTH);
            offset += DEFAULT_MASKING_KEY_LENGTH;
        }
        return offset;
    }

    private int copyExtendedPayloadLength(byte[] frameData, int offset) {
        int extLen = this.extendedPayloadLength.length;
        if (extLen > 0) {
            System.arraycopy(this.extendedPayloadLength, 0, frameData, offset, extLen);
        }
        return extLen;
    }

    private static long convertBytesToDecimal(byte[] ext) {
        long len = 0L;
        for (byte b : ext) {
            len = (len << 8) | (b & 0xFF);
        }
        return len;
    }


    public byte[] getRawBytes(InputStream inputStream) throws IOException {
        DataInputStream dis = (inputStream instanceof DataInputStream)
                ? (DataInputStream) inputStream
                : new DataInputStream(inputStream);

        // Read first two bytes of the frame header
        var b0 = dis.read();
        var b1 = dis.read();
        setFinOpcodeByte(b0);
        setMasked(b1);
        this.buildExtendedPayload(dis, payloadLen7);
        this.setHeaderLength();
        this.setPayloadLength(payloadLen7);
        this.setTotalFrameLength();
        return this.buildFullFrameData(dis);
    }

    private void setFinOpcodeByte(int b0) {
        this.finOpcodeByte = b0;
    }

    public void clearState() {

    }
}
