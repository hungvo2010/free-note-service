package com.freenote.app.service;

import lombok.Data;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Data
@Component
@Scope(value = "prototype")
public class ProductPurchasedEvent {
    private String product;
    private String buyer;
    private String eventId;

    ProductPurchasedEvent() {
        this.eventId = RandomStringUtils.randomAlphanumeric(10);
        MessageBroker.eventsCount.incrementAndGet();
        MessageBroker.createdEventsIdList.add(eventId);
    }

    public boolean fire() {
        String message = buyer + " purchased " + product;
        System.out.println("fire method called _ Thread : " + Thread.currentThread().getName() + " " + message + " eventId: " + eventId);
        MessageBroker.messages.add(new Message(eventId, message));
        return true;
    }
}
