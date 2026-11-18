# Demo quản lý đơn hàng — DDD REST API

Backend demo đơn giản để tạo, xem, thanh toán và hủy đơn hàng. Dự án minh họa cách tách nghiệp vụ khỏi REST API và cơ sở dữ liệu theo hướng Domain-Driven Design (DDD).

Xem [hướng dẫn giải thích source](HUONG-DAN-SOURCE.md) để tìm hiểu vai trò từng file và luồng xử lý.

## Công nghệ

- Java 17 theo cấu hình Maven, Spring Boot 4.1.2.
- Spring Web, Jakarta Validation, Spring Data JPA.
- PostgreSQL và Flyway để quản lý schema.
- Springdoc OpenAPI / Swagger UI.
- JUnit cho unit test.

## Cấu trúc

```text
src/main/java/com/example/demo1/
├── presentation/rest/          # REST controller
├── application/dto/           # Dữ liệu đầu vào và phản hồi
├── application/service/       # Điều phối use case và transaction
├── domain/model/              # Order, OrderItem, Money, OrderStatus
├── domain/repository/         # Interface lưu và tìm đơn hàng
└── infrastructure/persistance/ # JPA entity, mapper và repository implementation
```

Luồng xử lý: Controller → Application Service → Domain / Repository → JPA → PostgreSQL.
Domain không phụ thuộc Spring hoặc JPA. Các bảng `orders` và `order_items` được tạo bằng migration trong `src/main/resources/db/migration`.

## Chạy ứng dụng

Cần JDK tương thích (cấu hình biên dịch Java 17) và PostgreSQL đang chạy. Maven Wrapper đi kèm dự án; lần đầu cần kết nối mạng để tải Maven và dependency.

Tạo database dành riêng cho demo, ví dụ `demo_orders`, rồi tạo file `.env` ở thư mục gốc:

```properties
DB_URL=jdbc:postgresql://localhost:5432/demo_orders
DB_USERNAME=postgres
DB_PASSWORD=your_local_password
```

Ứng dụng tự đọc `.env`; cũng có thể đặt các biến trên trong cấu hình chạy của IDE. File `.env` đã được đưa vào `.gitignore`.

Chạy từ thư mục gốc trên Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Trên Linux/macOS:

```bash
./mvnw spring-boot:run
```

- Base URL: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

Flyway tạo schema; Hibernate chỉ kiểm tra schema bằng `ddl-auto=validate`. Nên dùng database trống cho lần chạy đầu. Nếu database từng chạy migration của bản quản lý trường học cũ, cần kiểm tra lịch sử/checksum Flyway; migration V2 không tự khắc phục checksum của V1 đã bị thay đổi.

## API

### Tạo đơn hàng

```http
POST http://localhost:8080/api/v1/orders
Content-Type: application/json
```

```json
{
  "customerId": "11111111-1111-1111-1111-111111111111",
  "items": [
    {
      "productId": "22222222-2222-2222-2222-222222222222",
      "quantity": 2,
      "priceVnd": 15000
    }
  ]
}
```

Trả về `201 Created`, header `Location` trỏ đến đơn vừa tạo và body:

```json
{
  "orderId": "33333333-3333-3333-3333-333333333333"
}
```

ID trên chỉ là ví dụ; dùng `orderId` thực tế nhận được cho các request tiếp theo.

### Xem đơn hàng

```http
GET http://localhost:8080/api/v1/orders/33333333-3333-3333-3333-333333333333
```

Trả về `200 OK`, ví dụ:

```json
{
  "id": "33333333-3333-3333-3333-333333333333",
  "customerId": "11111111-1111-1111-1111-111111111111",
  "items": [
    {
      "productId": "22222222-2222-2222-2222-222222222222",
      "quantity": 2,
      "unitPriceVnd": 15000
    }
  ],
  "totalPriceVnd": 30000,
  "status": "PENDING",
  "createdAt": "2026-11-17T10:30:00"
}
```

### Thanh toán

```http
POST http://localhost:8080/api/v1/orders/33333333-3333-3333-3333-333333333333/pay
```

Không cần body. Thành công trả về `204 No Content`. Chỉ đơn `PENDING` được chuyển sang `PAID`; đây là thao tác đổi trạng thái, chưa tích hợp cổng thanh toán.

### Hủy đơn

```http
POST http://localhost:8080/api/v1/orders/33333333-3333-3333-3333-333333333333/cancel
```

Không cần body. Thành công trả về `204 No Content` và trạng thái `CANCELLED`. Hiện cho phép hủy đơn `PENDING`, `PAID` và gọi hủy lại đơn `CANCELLED`; chặn hủy đơn `SHIPPED` hoặc `DELIVERED`. Hủy đơn đã thanh toán chưa có xử lý hoàn tiền.

## Quy tắc và giới hạn demo

- `customerId`, `productId` phải là UUID và không được thiếu trong request; chưa kiểm tra tồn tại trong hệ thống khách hàng/sản phẩm.
- Đơn phải có ít nhất một item; số lượng phải lớn hơn 0, giá không âm và có tối đa 17 chữ số phần nguyên, 2 chữ số phần thập phân. Giá vượt giới hạn bị từ chối với HTTP 400, không tự làm tròn.
- Tổng tiền được tính từ số lượng × đơn giá của từng item bằng `BigDecimal`, đơn vị VND. Đơn giá hiện do client gửi.
- Đơn mới có trạng thái `PENDING`. Enum có `SHIPPED` và `DELIVERED`, nhưng chưa có API chuyển sang hai trạng thái này.
- Chưa có frontend, đăng nhập, phân quyền hoặc API liệt kê đơn hàng.
- Đơn không tồn tại trả `404`; chuyển trạng thái không hợp lệ trả `409`. Validation request dùng xử lý mặc định của Spring và trả `400`.

## Unit test

```powershell
.\mvnw.cmd test
```

Các test chạy bằng database H2 trong bộ nhớ, không cần PostgreSQL:

- `MoneyTest`: tính toán thập phân, nhân theo số lượng, từ chối tiền âm/rỗng và tiền tệ không hợp lệ/khác loại.
- `CreateOrderCommandTest`: validation request, item rỗng và validation các trường bên trong item.
- `OrderItemTest`: tính thành tiền, từ chối số lượng không dương, sản phẩm hoặc đơn giá rỗng.
- `OrderTest`: tổng tiền, bảo vệ danh sách item, chuyển trạng thái, đầu vào không hợp lệ và giữ thời gian tạo khi khôi phục đơn.
- `OrderApplicationServiceTest`: tạo/lưu đơn, ánh xạ response, lưu thanh toán/hủy, đơn không tồn tại và từ chối thao tác không hợp lệ.
- `OrderApplicationServiceMockTest`: dùng Mockito kiểm tra việc lưu đơn đã thanh toán và không lưu khi không tìm thấy đơn.
- `OrderIntegrationTest`: khởi động Spring, chạy Flyway và gửi HTTP thật để kiểm tra tạo, xem, thanh toán, hủy đơn, lỗi request, lỗi trạng thái và lưu nhiều sản phẩm với giá thập phân.

Service test dùng repository trong bộ nhớ với bản sao tách biệt để phát hiện trường hợp quên lưu thay đổi. Integration test dùng H2 ở chế độ tương thích PostgreSQL; chưa kiểm tra trực tiếp trên PostgreSQL thật.
