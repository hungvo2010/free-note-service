package com.freenote.app.server.frames;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.*;
import java.util.BitSet;

@NoArgsConstructor
@AllArgsConstructor
public class BaseFrame implements Serializable, Externalizable {
    private static BitSet defaultBitset;
    @Serial
    private static final long serialVersionUID = -2140098214102580912L;
    private boolean fin = false;
    private boolean rsv1 = false;
    private boolean rsv2 = false;
    private boolean rsv3 = false;
    private BitSet opcode = defaultBitset; // Using BitSet to represent opcode
    private boolean mask = false;
    private byte payloadLength;
    private byte[] maskingKey;
    private byte[] payloadData;
    private byte[] extensionData;
    private byte[] applicationData;

    static {
        var bitsets = new BitSet(4);
        bitsets.set(0, true); // Set the first bit to true
        bitsets.set(1, 4, false); // Set bits 1, 2, and 3 to false
        defaultBitset = bitsets;
    }

    public BaseFrame(byte[] bytes) {
        payloadLength = (byte) (bytes.length * 8);
        payloadData = bytes;
        maskingKey = new byte[0];
        extensionData = new byte[0];
        applicationData = bytes;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        BitSet frameBits = new BitSet(4);
        frameBits.set(0, fin);
        frameBits.set(1, rsv1);
        frameBits.set(2, rsv2);
        frameBits.set(3, rsv3);
        for (int i = 0; i < 4; i++) {
            frameBits.set(4 + i, opcode.get(i));
        }
        frameBits.set(8, mask);
        boolean[] payloadBits = longToBitArray(payloadLength);
        for (int i = 0; i < payloadBits.length; i++) {
            frameBits.set(9 + i, payloadBits[i]);
        }
        frameBits.set(17, maskingKey != null && maskingKey.length > 0);


//        out.writeBoolean(fin);
//        out.writeBoolean(rsv1);
//        out.writeBoolean(rsv2);
//        out.writeBoolean(rsv3);
//        out.writeByte(opcode.toByteArray()[0]); // Assuming opcode is a single byte
//        out.writeBoolean(mask);
//        out.writeLong(payloadLength);
//        out.writeObject(maskingKey);
//        out.writeObject(payloadData);
        out.writeObject(frameBits);
//        out.writeObject(maskingKey);
        out.writeObject(payloadData);
//        out.writeObject(extensionData);
//        out.writeObject(applicationData);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        fin = in.readBoolean();
    }

    public static boolean[] longToBitArray(byte value) {
        boolean[] bits = new boolean[8];
        for (int i = 0; i < 8; i++) {
            bits[i] = (value & 1) == 1; // Check if the last bit is 1
            value >>= 1; // Right-shift the bits
        }
        return bits;
    }
}
