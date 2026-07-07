package org.example.module.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionEvent {
    private Long productId;
    private String promotionType;
    private Double discountPercent;
    private String message;
    private long timestamp;

    public PromotionEvent(Long productId, String promotionType) {
        this.productId = productId;
        this.promotionType = promotionType;
        this.timestamp = System.currentTimeMillis();
    }
}