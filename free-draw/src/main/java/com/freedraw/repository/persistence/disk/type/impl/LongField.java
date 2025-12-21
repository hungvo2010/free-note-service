package com.freedraw.repository.persistence.disk.type.impl;

import java.nio.ByteBuffer;

public class LongField extends NumberField<Long> {

    public LongField() {
        super(Long.BYTES);
    }

    @Override
    public byte[] toBytes(Long value) {
        return ByteBuffer.allocate(size).putLong(value).array();
    }

    @Override
    public Long fromBytes(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getLong();
    }
}
