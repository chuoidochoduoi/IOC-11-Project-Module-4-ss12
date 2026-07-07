package org.example.module.controller;

import org.example.module.entity.Order;
import org.example.module.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        List<OrderService.OrderItemRequest> items = request.items.stream()
                .map(item -> new OrderService.OrderItemRequest(
                        item.productId, item.productName, item.quantity, item.price))
                .toList();

        Order order = orderService.createOrder(request.customerId, request.customerEmail, items);

        OrderResponse response = new OrderResponse();
        response.orderId = order.getId();
        response.status = "ORDER_CREATED";
        return ResponseEntity.ok(response);
    }

    public static class CreateOrderRequest {
        public Long customerId;
        public String customerEmail;
        public List<ItemRequest> items;

        public static class ItemRequest {
            public Long productId;
            public String productName;
            public Integer quantity;
            public BigDecimal price;
        }
    }

    public static class OrderResponse {
        public Long orderId;
        public String status;
    }
}