package org.example.module.consumer;

import org.example.module.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @KafkaListener(topics = "order-events", groupId = "email-service")
    public void sendOrderConfirmationEmail(OrderCreatedEvent event) {
        log.info("[EmailService] Received: orderId={}, email={}",
                event.getOrderId(), event.getCustomerEmail());
        log.info("[EmailService] Email sent to: {}", event.getCustomerEmail());
    }
}