package com.freenote.app.server.application.repository.persistence.disk.type;

public interface DataType<T> {
    int getSize();

    byte[] toBytes(T value);

    T fromBytes(byte[] bytes);
}
