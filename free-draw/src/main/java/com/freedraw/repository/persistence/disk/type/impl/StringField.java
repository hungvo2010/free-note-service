package com.freedraw.repository.persistence.disk.type.impl;

import com.freedraw.repository.persistence.disk.type.DataType;

public class StringField implements DataType<String> {

    @Override
    public int getSize() {
        return 36;
    }

    @Override
    public byte[] toBytes(String value) {
        return value.getBytes();
    }

    @Override
    public String fromBytes(byte[] bytes) {
        return new String(bytes);
    }
}
