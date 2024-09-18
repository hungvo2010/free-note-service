package com.freenote.app.kafka;

import org.slf4j.Logger;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;

@Configuration
public class TrackingConsumers {
    private final Logger logger;

    public TrackingConsumers(Logger log) {
        this.logger = log;
    }

    @KafkaListener(topics = "dev.dc1.backend.telco.routing-social", groupId = "telco")
    public void listenGroupFoo(String message) {
        logger.info("Received Message in group foo: " + message);
    }
}
