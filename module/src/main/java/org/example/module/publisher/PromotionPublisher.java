package org.example.module.publisher;

import org.example.module.event.PromotionEvent;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PromotionPublisher {

    private static final Logger log = LoggerFactory.getLogger(PromotionPublisher.class);
    private static final String CHANNEL_PROMOTION_UPDATES = "promotion-updates";

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    public PromotionPublisher(RedissonClient redissonClient, ObjectMapper objectMapper) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
    }

    public void publishPromotionUpdate(Long productId, String promotionType) {
        try {
            PromotionEvent event = new PromotionEvent(productId, promotionType);
            String message = objectMapper.writeValueAsString(event);
            RTopic topic = redissonClient.getTopic(CHANNEL_PROMOTION_UPDATES);
            topic.publish(message);
            log.info("[PromotionPublisher] Published update for productId: {}", productId);
        } catch (Exception e) {
            log.error("[PromotionPublisher] Error publishing promotion", e);
        }
    }
}