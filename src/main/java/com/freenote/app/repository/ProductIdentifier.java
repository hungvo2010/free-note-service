package com.freenote.app.repository;

import jakarta.persistence.Embeddable;

@Embeddable
public class ProductIdentifier {
    private String service;
    private String productId;
}
