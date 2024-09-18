package com.freenote.app.service.impl;

import com.freenote.app.repository.FirstRepository;
import com.freenote.app.service.FirstService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class FirstServiceImpl implements FirstService {
    Logger logger;
    @Autowired
    @Value("${spring.profiles.actived:dev}")
    private String testValue;
    private FirstRepository repository;
    @Autowired
    private String strBean;
    private Integer counter = 0;

    @Autowired
    public FirstServiceImpl(Logger log) {
        this.logger = log;
    }

//    public FirstServiceImpl(@Autowired FirstRepository repository, @Autowired String strBean) {
//        this.repository = repository;
//        this.strBean = strBean;
//    }

    @Override
    public synchronized List<String> getAllNotes() {
        this.logger.info(strBean);
        this.counter++;
        if (Objects.equals(strBean, "singular-values")) {
            try {
                Thread.sleep(Thread.currentThread().getId() * 10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return List.of("1, 2, ", "124");
    }

    @Override
    public int getCounter() {
        return counter;
    }
}
