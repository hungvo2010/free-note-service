package com.freenote.app.server.frames;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.BitSet;

@NoArgsConstructor
@AllArgsConstructor
public class BaseFrame implements Serializable, Externalizable {
    private static final Logger log = LogManager.getLogger(BaseFrame.class);
    private static BitSet defaultBitset;
    @Serial
    private static final long serialVersionUID = -2140098214102580912L;
    private boolean fin = true;
    private boolean rsv1 = false;
    private boolean rsv2 = false;
    private boolean rsv3 = false;
    private BitSet opcode = defaultBitset; // Using BitSet to represent opcode
    private boolean masked = false;
    private byte payloadLength;
    private byte[] maskingKey;
    private byte[] payloadData;
    private byte[] extensionData;
    private byte[] applicationData;

    static {
        var bitsets = new BitSet(4);
        bitsets.set(0, 4, false);
        bitsets.set(3, true);
        defaultBitset = bitsets;
    }

    public BaseFrame(byte[] bytes) {
        payloadLength = (byte) (bytes.length);
        payloadData = bytes;
        maskingKey = new byte[0];
        extensionData = new byte[0];
        applicationData = bytes;
    }

    public BitSet toBitSet() {
        BitSet frameBits = new BitSet();
        frameBits.set(0, fin);
        frameBits.set(1, rsv1);
        frameBits.set(2, rsv2);
        frameBits.set(3, rsv3);
        for (int i = 0; i < 4; i++) {
            frameBits.set(4 + i, opcode.get(i));
        }
        frameBits.set(8, masked);
        boolean[] payloadBits = longToBitArray(payloadLength, 7);
        for (int i = 0; i < payloadBits.length; i++) {
            frameBits.set(9 + i, payloadBits[i]);
        }
        appendBytesToBitSet(frameBits, payloadData);
        return (frameBits);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        BitSet frameBits = new BitSet(50);
        frameBits.set(0, fin);
        frameBits.set(1, rsv1);
        frameBits.set(2, rsv2);
        frameBits.set(3, rsv3);
        for (int i = 0; i < 4; i++) {
            frameBits.set(4 + i, opcode.get(i));
        }
        frameBits.set(8, masked);
        boolean[] payloadBits = longToBitArray(payloadLength, 7);
        for (int i = 0; i < payloadBits.length; i++) {
            frameBits.set(9 + i, payloadBits[i]);
        }
        frameBits.set(17, maskingKey != null && maskingKey.length > 0);
        out.writeObject(frameBits);
        out.writeObject(payloadData);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException {
        fin = in.readBoolean();
    }

    public static void appendBytesToBitSet(BitSet target, byte[] bytesToAppend) {
        BitSet appended = (BitSet.valueOf(bytesToAppend));
        int offset = target.length(); // append after the last set bit
        int length = bytesToAppend.length * 8; // each byte has 8 bits

        for (int i = appended.nextSetBit(0); i >= 0; i = appended.nextSetBit(i + 1)) {
            target.set(offset + length - i - 1);
        }
    }

    public static boolean[] longToBitArray(byte value, int length) {
        boolean[] bits = new boolean[length];
        for (int i = length - 1; i >= 0; --i) {
            bits[i] = (value & 1) == 1; // Check if the last bit is 1
            value >>= 1; // Right-shift the bits
        }
        return bits;
    }

    public static BitSet reverseBitSet(BitSet original) {
        BitSet reversed = new BitSet(original.length());
        int length = original.length();

        for (int i = 0; i < length; i++) {
            reversed.set(length - 1 - i, original.get(i));
        }

        return reversed;
    }
}
