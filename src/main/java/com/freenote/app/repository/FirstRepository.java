package com.freenote.app.repository;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class FirstRepository {
    public List<String> getAllNotes() {
        return List.of("1", "2", "3");
    }
}
