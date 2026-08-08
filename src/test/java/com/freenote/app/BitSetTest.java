package com.freenote.app;

import org.junit.jupiter.api.Test;

import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BitSetTest {
    static void main(String[] args) {
        BitSet bs0 = new BitSet();
        System.out.println("created:\tLength,Size bs0: " + bs0.length() + " , " + bs0.size());
        bs0.set(15);
        System.out.println("set(15):\tLength,Size bs0: " + bs0.length() + " , " + bs0.size());
        bs0.set(63);
        System.out.println("set(63):\tLength,Size bs0: " + bs0.length() + " , " + bs0.size());
        bs0.set(86);
        System.out.println("set(86):\tLength,Size bs0: " + bs0.length() + " , " + bs0.size());
        bs0.clear(86);
        System.out.println("clear(86):\tLength,Size bs0: " + bs0.length() + " , " + bs0.size());
        bs0.clear(63);
        System.out.println("clear(63):\tLength,Size bs0: " + bs0.length() + " , " + bs0.size());

        BitSet bs1;
        System.out.println("Cloning to bs1...\n");
        bs1 = (BitSet) bs0.clone();
        System.out.println("Length,Size bs0: " + bs0.length() + " , " + bs0.size());
        System.out.println("Length,Size bs1: " + bs1.length() + " , " + bs1.size());
    }

    @Test
    void testBitSet() {
        BitSet bs0 = new BitSet();
        bitsetIndexAndSizeWhenFirstInit(bs0);

        lengthUpdateAfterSetBit(bs0);

        bs0.set(63);
        assertEquals(64, bs0.length());

        bs0.set(86);
        assertEquals(87, bs0.length());
        assertEquals(128, bs0.size());

        bs0.clear(86);
        assertEquals(64, bs0.length());
        assertEquals(128, bs0.size());

        bs0.clear(63);
        assertEquals(16, bs0.length());
        assertEquals(128, bs0.size());
    }

    private void lengthUpdateAfterSetBit(BitSet bs0) {
        bs0.set(15);
        assertEquals(16, bs0.length());
    }

    private void bitsetIndexAndSizeWhenFirstInit(BitSet bs0) {
        assertEquals(0, bs0.length());
        assertEquals(64, bs0.size());
    }
}
