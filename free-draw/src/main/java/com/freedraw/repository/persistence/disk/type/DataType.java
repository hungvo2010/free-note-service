package com.freedraw.repository.persistence.disk.type;

public interface DataType<T> {
    int getSize();

    byte[] toBytes(T value);

    T fromBytes(byte[] bytes);
}
