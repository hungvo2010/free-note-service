package com.freenote.app.server.application.repository.persistence.disk.type.impl;

import com.freenote.app.server.application.repository.persistence.disk.type.DataType;

import java.nio.ByteBuffer;

public class CompositeField implements DataType<Integer[]> {
    @Override
    public int getSize() {
        return 8;
    }

    @Override
    public byte[] toBytes(Integer[] value) {
        var newBytes = new byte[value.length * Integer.BYTES];
        for (int i = 0; i < value.length; i++) {
            var intBytes = ByteBuffer.allocate(Integer.BYTES).putInt(value[i]).array();
            System.arraycopy(intBytes, 0, newBytes, i * Integer.BYTES, Integer.BYTES);
        }
        return newBytes;
    }

    @Override
    public Integer[] fromBytes(byte[] bytes) {
        var intCount = bytes.length / Integer.BYTES;
        var values = new Integer[intCount];
        for (int i = 0; i < intCount; i++) {
            var intBytes = new byte[Integer.BYTES];
            System.arraycopy(bytes, i * Integer.BYTES, intBytes, 0, Integer.BYTES);
            values[i] = ByteBuffer.wrap(intBytes).getInt();
        }
        return values;
    }
}
