package org.example.module.subscriber;

import org.example.module.event.PromotionEvent;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;

@Service
public class ProductCacheSubscriber {

    private static final Logger log = LoggerFactory.getLogger(ProductCacheSubscriber.class);
    private static final String CHANNEL_PROMOTION_UPDATES = "promotion-updates";

    private final RedissonClient redissonClient;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;

    public ProductCacheSubscriber(RedissonClient redissonClient,
                                 CacheManager cacheManager,
                                 ObjectMapper objectMapper) {
        this.redissonClient = redissonClient;
        this.cacheManager = cacheManager;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void subscribe() {
        RTopic topic = redissonClient.getTopic(CHANNEL_PROMOTION_UPDATES);
        topic.addListener(String.class, this::handlePromotionUpdate);
        log.info("[ProductCacheSubscriber] Subscribed to channel: {}", CHANNEL_PROMOTION_UPDATES);
    }

    private void handlePromotionUpdate(String channel, String message) {
        try {
            PromotionEvent event = objectMapper.readValue(message, PromotionEvent.class);
            log.info("[ProductCacheSubscriber] Received update: productId={}", event.getProductId());
            cacheManager.getCache("products").evict(event.getProductId());
        } catch (Exception e) {
            log.error("[ProductCacheSubscriber] Error processing message", e);
        }
    }
}