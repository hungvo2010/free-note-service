package com.freedraw.repository.persistence.disk.type.impl;

import java.nio.ByteBuffer;

public class FloatField extends NumberField<Float> {

    public FloatField() {
        super(Float.BYTES);
    }

    @Override
    public byte[] toBytes(Float value) {
        return ByteBuffer.allocate(size).putFloat(value).array();
    }

    @Override
    public Float fromBytes(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getFloat();
    }
}
