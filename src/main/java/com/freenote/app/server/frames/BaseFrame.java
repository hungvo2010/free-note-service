package com.freenote.app.server.frames;

import java.io.*;
import java.util.Arrays;

import static com.freenote.app.server.frames.FrameUtil.parsePayloadLength;

public class BaseFrame implements Serializable, Externalizable {
    @Serial
    private static final long serialVersionUID = -2140098214102580912L;
    private boolean fin = true;
    private boolean rsv1 = false;
    private boolean rsv2 = false;
    private boolean rsv3 = false;
    private int maskingKeyStart = 0;
    private short opcode; // Using BitSet to represent opcode
    private boolean masked = false;
    private long payloadLength;
    private byte[] maskingKey;
    private byte[] payloadData;
    private byte[] extensionData;
    private byte[] applicationData;

    public BaseFrame() {
    }

    public BaseFrame(short opcode) {
        this.opcode = opcode;
        this.maskingKeyStart = 2;
    }

    public BaseFrame(byte[] bytes) {
        fin = ((bytes[0] & 0x80) >> 7) == 1; // 1000 0000
        rsv1 = ((bytes[0] & 0x40) >> 6) == 1; // 0100 0000
        rsv2 = ((bytes[0] & 0x20) >> 5) == 1; // 0010 0000
        rsv3 = ((bytes[0] & 0x10) >> 4) == 1; // 0001 0000
        opcode = (short) (bytes[0] & 0x0F);
        masked = ((bytes[1] & 0x80) >> 7) == 1; // 1000 0000
        payloadLength = parsePayloadLength(bytes);
        maskingKeyStart = 2 + ((bytes[1] & 0x7F) < 126 ? 0 : ((bytes[1] & 0x7F) == 126 ? 4 : 10));
        maskingKey = masked ? Arrays.copyOfRange(bytes, maskingKeyStart, maskingKeyStart + 4) : new byte[0]; // Masking key is present if masked is true
        payloadData = Arrays.copyOfRange(bytes, maskingKeyStart + 4, bytes.length);
        extensionData = new byte[0];
        applicationData = bytes;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        var firstByte = (byte) (
                (fin ? 0x80 : 0) |    // FIN bit - 1st bit (10000000)
                        (rsv1 ? 0x40 : 0) |   // RSV1 bit - 2nd bit (01000000)
                        (rsv2 ? 0x20 : 0) |   // RSV2 bit - 3rd bit (00100000)
                        (rsv3 ? 0x10 : 0) |   // RSV3 bit - 4th bit (00010000)
                        (opcode & 0x0F)       // Opcode - lower 4 bits (00001111)
        );
        out.writeByte(firstByte);
        var maskByte = masked ? (byte) 0x80 : (byte) 0x00;
        var secondByte = (byte) (maskByte | (byte) (maskingKeyStart == 2 ? payloadLength : (maskingKeyStart == 4 ? 126 : 127)));
        out.writeByte(secondByte);
        if (maskingKeyStart == 4) {
            out.writeShort((short) payloadLength);
        } else if (maskingKeyStart == 10) {
            out.writeLong(payloadLength);
        }
        if (masked) {
            out.write(maskingKey);
        }
        if (payloadData != null && payloadData.length > 0) {
            out.write(payloadData);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
    }
}