package com.freenote.app.server.application.repository.persistence.disk.service;

public interface SearchFieldByOffset<T> {
    T getData(long offset);
    int append(T item);
}
