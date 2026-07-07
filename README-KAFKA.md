# Kafka Producer/Consumer - Session 12

## Kiến trúc hệ thống

```
+-------------+     +-----------------+     +-------------------+
|  Khách hàng  | --> | Order-Service   | --> |   Kafka Topic     |
+-------------+     |   (Producer)    |     |   order-events    |
                    +-----------------+     +---------+---------+
                                                  |
                           +----------------------+------------------+
                           |                      |                  |
              +------------v-----------+ +----------v----------+ +-----v------+ 
              |   Email-Service        | |  Inventory-Service  | |   ...      |
              |   (Consumer)           | |   (Consumer)        | |  (Future)  |
              |   group-id:            | |   group-id:         | |            |
              |   email-service          | |   inventory-service  | |            |
              +------------------------+ +----------------------+ +------------+
```

## Các thành phần đã tạo

### 1. OrderCreatedEvent (`event/OrderCreatedEvent.java`)
Event được đóng gói và gửi qua Kafka khi có đơn hàng mới.

### 2. Order Entity & OrderRepository (`entity/Order.java`, `repository/OrderRepository.java`)
Entity lưu đơn hàng vào database.

### 3. OrderService (`service/OrderService.java`)
**Producer**: Lưu đơn hàng và gửi OrderCreatedEvent vào Kafka.

### 4. OrderController (`controller/OrderController.java`)
API endpoint: `POST /api/orders` - nhận request từ khách hàng đặt hàng.

### 5. EmailService Consumer (`consumer/EmailService.java`)
- Lắng nghe topic `order-events`
- Group-id: `email-service`
- Gửi email xác nhận đơn hàng

### 6. InventoryService Consumer (`consumer/InventoryService.java`)
- Lắng nghe topic `order-events`
- Group-id: `inventory-service`
- Trừ kho sản phẩm

### 7. KafkaProducerConfig (`config/KafkaProducerConfig.java`)
Cấu hình Kafka producer bean.

## Cách chạy thử

### 1. Chạy Kafka server (docker)
```bash
docker run -p 9092:9092 --name kafka apache/kafka:3.7.0 -XX:+UseG1GC -XX:MaxRAMPercentage=75.0
```

Hoặc dùng docker-compose:
```bash
docker-compose up -d kafka
```

### 2. Build và chạy ứng dụng
```bash
./gradlew bootRun
```

### 3. Test API đặt hàng
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "customerEmail": "customer@example.com",
    "items": [
      {
        "productId": 1,
        "productName": "Sản phẩm A",
        "quantity": 2,
        "price": 100000
      }
    ]
  }'
```

### 4. Kiểm tra logs
- Order-Service sẽ log việc gửi event
- EmailService sẽ log việc nhận và gửi email
- InventoryService sẽ log việc trừ kho

## Lưu ý quan trọng

1. **Khác group-id** cho các consumer: Mỗi consumer group sẽ nhận được tất cả messages
2. **Producer không chờ**: Khi gửi Kafka xong, luồng trả về ngay cho khách hàng
3. **Idempotency**: Consumer nên xử lý idempotent để tránh xử lý trùng khi restart

---

# Redis Pub/Sub - Session 11

## Kiến trúc hệ thống

```
+------------------+     +-------------------+     +------------------+
| Admin Portal     | --> | Promotion-Service | --> | Redis Channel    |
| (Cập nhật KM)    |     | (Publisher)       |     | promotion-updates|
+------------------+     +-------------------+     +--------+---------+
                                                            |
                                           +---------------+------------------+
                                           |                  |                |
                                +----------v----------+ +-------v------+ +------v-------+
                                | Web Service         | | App Service   | | API Service  |
                                | (Subscriber)        | | (Subscriber)  | |(Subscriber)  |
                                | Xóa cache           | | Xóa cache     | | Xóa cache    |
                                +---------------------+ +---------------+ +-------------+
```

## Các thành phần đã tạo

### 1. PromotionEvent (`event/PromotionEvent.java`)
Event chứa thông tin thay đổi khuyến mãi.

### 2. PromotionPublisher (`publisher/PromotionPublisher.java`)
**Publisher**: Gửi message vào Redis channel `promotion-updates`.

### 3. ProductCacheSubscriber (`subscriber/ProductCacheSubscriber.java`)
**Subscriber**: Lắng nghe channel, xóa cache Redis để cập nhật giá mới.

### 4. PromotionController (`controller/PromotionController.java`)
API: `POST /api/promotions/update/{productId}` - để admin publish thông báo.

## Cách chạy thử

### 1. Chạy Redis server
```bash
docker run -p 6379:6379 redis:latest
```

### 2. Chạy ứng dụng
```bash
./gradlew bootRun
```

### 3. Test Pub/Sub

**Bước 1**: Gọi API lấy sản phẩm (để cache vào Redis)
```bash
curl http://localhost:8080/api/products/1
# Log: "ĐANG TRUY VẤN DATABASE"
curl http://localhost:8080/api/products/1
# Log: lấy từ Redis (nhanh hơn)
```

**Bước 2**: Publish thông báo cập nhật khuyến mãi
```bash
curl -X POST "http://localhost:8080/api/promotions/update/1?type=DISCOUNT&discountPercent=10"
```

**Bước 3**: Gọi API lấy sản phẩm lại - sẽ truy vấn DB thay vì Redis
```bash
curl http://localhost:8080/api/products/1
# Log: "ĐANG TRUY VẤN DATABASE" - vì cache đã bị xóa
```

## Lưu ý quan trọng

1. **Pub/Sub nhanh**: Redis xử lý trong chưa đầy 1 giây
2. **Subscriber tự đăng ký**: `@PostConstruct` tự động subscribe khi service khởi động
3. **Broadcast**: Một message publish sẽ đến TẤT CẢ các subscriber đang online
4. **Persistence**: Redis channel không lưu trữ - subscriber offline sẽ bỏ lỡ message