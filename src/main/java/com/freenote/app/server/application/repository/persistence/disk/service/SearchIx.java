package com.freenote.app.server.application.repository.persistence.disk.service;

import java.util.List;

public interface SearchIx<T> {
    T getData(int idx);

    int insert(T item);

    List<T> getAll();
}
