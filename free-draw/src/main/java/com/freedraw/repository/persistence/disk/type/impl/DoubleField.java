package com.freedraw.repository.persistence.disk.type.impl;

import java.nio.ByteBuffer;

public class DoubleField extends NumberField<Double> {

    public DoubleField() {
        super(Double.BYTES);
    }

    @Override
    public byte[] toBytes(Double value) {
        return ByteBuffer.allocate(size).putDouble(value).array();
    }

    @Override
    public Double fromBytes(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getDouble();
    }
}
