package com.freenote.app.controllers;

import com.freenote.app.service.FirstService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
public class FirstController {
    private FirstService firstService;

    @GetMapping(path = "/hello", produces = "application/json")
    public List<String> doGet() {
        return firstService.getAllNotes();
    }

    @GetMapping(path = "/hello/post", produces = "application/json")
    public ResponseEntity<String> doPost() {
        return new ResponseEntity<>("hello", null, null);
    }
}
