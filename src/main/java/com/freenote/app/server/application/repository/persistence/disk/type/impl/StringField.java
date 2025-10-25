package com.freenote.app.server.application.repository.persistence.disk.type.impl;

import com.freenote.app.server.application.repository.persistence.disk.type.DataType;

public class StringField implements DataType<String> {

    @Override
    public int getSize() {
        return 0;
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
