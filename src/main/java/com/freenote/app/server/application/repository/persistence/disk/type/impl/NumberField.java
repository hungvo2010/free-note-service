package com.freenote.app.server.application.repository.persistence.disk.type.impl;

import com.freenote.app.server.application.repository.persistence.disk.type.DataType;

public abstract class NumberField<T extends Number> implements DataType<T> {
    protected final int size;

    protected NumberField(int size) {
        this.size = size;
    }

    @Override
    public int getSize() {
        return size;
    }
}
