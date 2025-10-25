package com.freenote.app.server.application.repository.persistence.disk.type.impl;

import java.nio.ByteBuffer;

public class IntegerField extends NumberField<Integer> {

    public IntegerField() {
        super(Integer.BYTES);
    }

    @Override
    public byte[] toBytes(Integer value) {
        return ByteBuffer.allocate(size).putInt(value).array();
    }

    @Override
    public Integer fromBytes(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }
}
