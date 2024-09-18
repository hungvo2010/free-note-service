package com.freenote.app.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class PurchaseServiceWithObjectFactory {
    private final ObjectProvider<ProductPurchasedEvent> productPurchasedEventObjectFactory;


    public PurchaseServiceWithObjectFactory(
            ObjectProvider<ProductPurchasedEvent> productPurchasedEventObjectFactory
    ) {
        this.productPurchasedEventObjectFactory = productPurchasedEventObjectFactory;
    }

    public boolean purchase(PurchaseProductDto dto) {
        System.out.println("Thread: " + Thread.currentThread().getName() + "product ---: " + dto.getProduct() + " purchased successfully");
        ProductPurchasedEvent productPurchasedEventInstance = productPurchasedEventObjectFactory.getObject();
        System.out.println("Thread: eventId" + productPurchasedEventInstance.getEventId());
        productPurchasedEventInstance.setProduct(dto.getProduct());
        productPurchasedEventInstance.setBuyer(dto.getBuyer());
        productPurchasedEventInstance.fire();
        return true;
    }
}
