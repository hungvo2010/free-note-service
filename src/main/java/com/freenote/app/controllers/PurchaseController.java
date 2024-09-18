package com.freenote.app.controllers;

import com.freenote.app.service.PurchaseProductDto;
import com.freenote.app.service.PurchaseService;
import com.freenote.app.service.PurchaseServiceWithObjectFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/purchase")
public class PurchaseController {
    private final PurchaseService purchaseService;
    private final PurchaseServiceWithObjectFactory factory;

    public PurchaseController(PurchaseService purchaseService, PurchaseServiceWithObjectFactory factory) {
        this.purchaseService = purchaseService;
        this.factory = factory;
    }

    @PostMapping
    public boolean purchase(@RequestBody PurchaseProductDto dto) {
        return factory.purchase(dto);
    }
}
