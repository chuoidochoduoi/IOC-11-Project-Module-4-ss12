package org.example.module.consumer;

import org.example.module.entity.Product;
import org.example.module.event.OrderCreatedEvent;
import org.example.module.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private final ProductRepository productRepository;

    public InventoryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @KafkaListener(topics = "order-events", groupId = "inventory-service")
    @Transactional
    public void reduceInventory(OrderCreatedEvent event) {
        log.info("[InventoryService] Received order: {}", event.getOrderId());

        for (OrderCreatedEvent.OrderItem item : event.getItems()) {
            Optional<Product> productOpt = productRepository.findById(item.getProductId());
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                int newStock = product.getStock() - item.getQuantity();
                if (newStock >= 0) {
                    product.setStock(newStock);
                    productRepository.save(product);
                    log.info("[InventoryService] Reduced stock for product: {}", item.getProductName());
                }
            }
        }
    }
}