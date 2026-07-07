package org.example.module.service;

import org.example.module.entity.Order;
import org.example.module.event.OrderCreatedEvent;
import org.example.module.repository.OrderRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private static final String TOPIC_ORDER_EVENTS = "order-events";

    public OrderService(OrderRepository orderRepository,
                        KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public Order createOrder(Long customerId, String customerEmail, List<OrderItemRequest> items) {
        BigDecimal totalAmount = items.stream()
                .map(item -> item.price.multiply(new BigDecimal(item.quantity)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order();
        order.setCustomerId(customerId);
        order.setCustomerEmail(customerEmail);
        order.setTotalAmount(totalAmount);

        List<Order.OrderItemDetail> orderItems = items.stream()
                .map(item -> new Order.OrderItemDetail(
                        null,
                        item.productId,
                        item.productName,
                        item.quantity,
                        item.price
                ))
                .collect(Collectors.toList());
        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        OrderCreatedEvent event = new OrderCreatedEvent();
        event.setOrderId(savedOrder.getId());
        event.setCustomerId(savedOrder.getCustomerId());
        event.setCustomerEmail(savedOrder.getCustomerEmail());
        event.setTotalAmount(savedOrder.getTotalAmount());

        List<OrderCreatedEvent.OrderItem> eventItems = orderItems.stream()
                .map(item -> new OrderCreatedEvent.OrderItem(
                        item.getProductId(),
                        item.getProductName(),
                        item.getQuantity(),
                        item.getPrice()
                ))
                .collect(Collectors.toList());
        event.setItems(eventItems);

        kafkaTemplate.send(TOPIC_ORDER_EVENTS, event);

        return savedOrder;
    }

    public static class OrderItemRequest {
        public Long productId;
        public String productName;
        public Integer quantity;
        public BigDecimal price;

        public OrderItemRequest() {}

        public OrderItemRequest(Long productId, String productName, Integer quantity, BigDecimal price) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
        }
    }
}