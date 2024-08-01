package com.freenote.app.service.impl;

import com.freenote.app.repository.FirstRepository;
import com.freenote.app.service.FirstService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class FirstServiceImpl implements FirstService {
    @Value("${spring.profiles.actived:dev}")
    private String testValue;
    @Autowired
    private FirstRepository repository;
    @Override
    public List<String> getAllNotes() {
        if (Objects.equals(testValue, "prod")){
            return repository.getAllNotes();
        }
        return List.of("1, 2, ", "124");
    }
}
