package org.example.module.service;

import org.example.module.entity.Product;
import org.example.module.event.OrderCreatedEvent;
import org.example.module.repository.ProductRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class FlashSaleService {

    private static final Logger log = LoggerFactory.getLogger(FlashSaleService.class);
    private final ProductRepository productRepository;
    private final RedissonClient redissonClient;
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private static final String TOPIC_ORDER_EVENTS = "order-events";
    private static final String LOCK_PREFIX = "lock:product:";

    public FlashSaleService(ProductRepository productRepository,
                            RedissonClient redissonClient,
                            KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate) {
        this.productRepository = productRepository;
        this.redissonClient = redissonClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    public FlashSaleResponse purchaseFlashSale(Long productId, Long customerId, String customerEmail) {
        String lockKey = LOCK_PREFIX + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                return FlashSaleResponse.failed("Sản phẩm đang được xử lý, vui lòng thử lại sau!");
            }
            return processPurchase(productId, customerId, customerEmail);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return FlashSaleResponse.failed("Hệ thống đang bận, vui lòng thử lại!");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional
    private FlashSaleResponse processPurchase(Long productId, Long customerId, String customerEmail) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return FlashSaleResponse.failed("Sản phẩm không tồn tại!");
        }

        Product product = productOpt.get();
        if (product.getStock() <= 0) {
            return FlashSaleResponse.failed("Sản phẩm đã hết hàng!");
        }

        product.setStock(product.getStock() - 1);
        productRepository.save(product);

        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(System.currentTimeMillis());
        event.setCustomerId(customerId);
        event.setCustomerEmail(customerEmail);
        event.setTotalAmount(product.getPrice());

        OrderCreatedEvent.OrderItem item = new OrderCreatedEvent.OrderItem(
                productId, product.getName(), 1, product.getPrice());
        event.setItems(List.of(item));

        kafkaTemplate.send(TOPIC_ORDER_EVENTS, event);
        return FlashSaleResponse.success(product.getStock());
    }

    public static class FlashSaleResponse {
        private boolean success;
        private String message;
        private Integer remainingStock;

        public FlashSaleResponse() {}
        public FlashSaleResponse(boolean success, String message, Integer remainingStock) {
            this.success = success;
            this.message = message;
            this.remainingStock = remainingStock;
        }

        public static FlashSaleResponse success(Integer remainingStock) {
            return new FlashSaleResponse(true, "Mua hàng thành công!", remainingStock);
        }

        public static FlashSaleResponse failed(String message) {
            return new FlashSaleResponse(false, message, null);
        }

        // Getters/Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Integer getRemainingStock() { return remainingStock; }
        public void setRemainingStock(Integer remainingStock) { this.remainingStock = remainingStock; }
    }
}