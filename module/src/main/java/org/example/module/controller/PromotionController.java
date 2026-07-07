package org.example.module.controller;

import org.example.module.publisher.PromotionPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final PromotionPublisher promotionPublisher;

    public PromotionController(PromotionPublisher promotionPublisher) {
        this.promotionPublisher = promotionPublisher;
    }

    @PostMapping("/update/{productId}")
    public ResponseEntity<String> updatePromotion(
            @PathVariable Long productId,
            @RequestParam String type) {

        promotionPublisher.publishPromotionUpdate(productId, type);
        return ResponseEntity.ok("Promotion update published for product " + productId);
    }
}