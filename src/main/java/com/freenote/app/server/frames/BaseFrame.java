package com.freenote.app.server.frames;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.*;
import java.util.Arrays;

import static com.freenote.app.server.frames.FrameUtil.parsePayloadLength;

@NoArgsConstructor
@AllArgsConstructor
public class BaseFrame implements Serializable, Externalizable {
    @Serial
    private static final long serialVersionUID = -2140098214102580912L;
    private boolean fin = true;
    private boolean rsv1 = false;
    private boolean rsv2 = false;
    private boolean rsv3 = false;
    private short opcode; // Using BitSet to represent opcode
    private boolean masked = false;
    private int payloadLength;
    private byte[] maskingKey;
    private byte[] payloadData;
    private byte[] extensionData;
    private byte[] applicationData;

    public BaseFrame(byte[] bytes) {
        fin = ((bytes[0] & 0x80) >> 7) == 1; // 1000 0000
        rsv1 = ((bytes[0] & 0x40) >> 6) == 1; // 0100 0000
        rsv2 = ((bytes[0] & 0x20) >> 5) == 1; // 0010 0000
        rsv3 = ((bytes[0] & 0x10) >> 4) == 1; // 0001 0000
        opcode = (short) (bytes[0] & 0x0F);
        masked = ((bytes[1] & 0x80) >> 7) == 1; // 1000 0000
        payloadLength = parsePayloadLength(bytes);
        int maskingKeyStart = 2 + ((bytes[1] & 0x7F) < 126 ? 0 : ((bytes[1] & 0x7F) == 126 ? 4 : 10));
        maskingKey = masked ? Arrays.copyOfRange(bytes, maskingKeyStart, maskingKeyStart + 4) : new byte[0]; // Masking key is present if masked is true
        payloadData = Arrays.copyOfRange(bytes, maskingKeyStart + 4, bytes.length);
        extensionData = new byte[0];
        applicationData = bytes;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        System.out.println("hello from BaseFrame");
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
    }
}