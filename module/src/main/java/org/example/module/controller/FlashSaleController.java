package org.example.module.controller;

import org.example.module.service.FlashSaleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/flash-sale")
public class FlashSaleController {

    private final FlashSaleService flashSaleService;

    public FlashSaleController(FlashSaleService flashSaleService) {
        this.flashSaleService = flashSaleService;
    }

    @PostMapping("/purchase/{productId}")
    public ResponseEntity<FlashSaleService.FlashSaleResponse> purchase(
            @PathVariable Long productId,
            @RequestParam Long customerId,
            @RequestParam String customerEmail) {

        FlashSaleService.FlashSaleResponse response = flashSaleService.purchaseFlashSale(
                productId, customerId, customerEmail);

        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }
}