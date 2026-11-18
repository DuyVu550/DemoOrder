# Giải thích source demo quản lý đơn hàng

## 1. Dự án làm gì?

Đây là backend REST API viết bằng Java 17 và Spring Boot. Ứng dụng nhận yêu cầu tạo, xem, thanh toán và hủy đơn; lưu đơn hàng trong PostgreSQL. Thanh toán trong demo chỉ đổi trạng thái, chưa kết nối cổng thanh toán. Không có giao diện frontend.

Ví dụ luồng tạo đơn:

```text
Client gửi POST /api/v1/orders
  → OrderController nhận JSON và kiểm tra đầu vào
  → OrderApplicationService chuyển request thành đối tượng domain
  → Order kiểm tra quy tắc và tính tổng tiền
  → OrderRepositoryImpl dùng OrderMapper chuyển sang JPA entity
  → Spring Data JPA lưu vào bảng orders và order_items
  → API trả 201, orderId và header Location
```

Domain giữ logic nghiệp vụ; application điều phối; presentation làm việc với HTTP; infrastructure làm việc với database. DTO là dữ liệu đi vào hoặc đi ra API, còn JPA entity là cấu trúc ánh xạ database.

## 2. Các file Java chính

Các đường dẫn trong mục này đều nằm dưới `src/main/java/com/example/demo1/`.

### Khởi động và HTTP

| File | Công dụng và phần code cần đọc |
| --- | --- |
| `Demo1Application.java` | Điểm khởi động. `main()` gọi `SpringApplication.run`; `@SpringBootApplication` bật tự cấu hình và quét các component trong package con. |
| `presentation/rest/OrderController.java` | Khai báo gốc `/api/v1/orders`. `@PostMapping` tạo, thanh toán hoặc hủy; `@GetMapping` lấy chi tiết. `@Valid @RequestBody` đọc JSON và chạy validation. Tạo đơn trả `201` cùng `Location`; pay/cancel trả `204`. |
| `presentation/rest/OrderExceptionHandler.java` | `@RestControllerAdvice` xử lý exception của controller. Chuyển `OrderNotFoundException` thành `404`, `InvalidOrderStateException` thành `409` bằng `ProblemDetail`. Lỗi validation/JSON do Spring xử lý mặc định. |

### Application: dữ liệu và use case

| File | Công dụng và phần code cần đọc |
| --- | --- |
| `application/dto/CreateOrderCommand.java` | Record chứa `customerId` và danh sách `items`; record con chứa `productId`, `quantity`, `priceVnd`. `@NotNull`, `@NotEmpty`, `@Positive` và `@DecimalMin` kiểm tra dữ liệu; `@Valid` kiểm tra tiếp từng item. `@Digits(17,2)` giới hạn đơn giá theo cột database. |
| `application/dto/OrderResponse.java` | Cấu trúc JSON chi tiết đơn. `from(Order)` chuyển domain sang DTO, gồm các item, tổng tiền, trạng thái và thời gian tạo. |
| `application/service/OrderApplicationService.java` | Điều phối `createOrder`, `getOrder`, `payOrder`, `cancelOrder`. `@Transactional` bao các thao tác database; bản `readOnly = true` dùng cho đọc. `findOrder` báo lỗi nếu không có đơn. Service gọi hành vi của domain trước khi lưu. |
| `application/service/OrderNotFoundException.java` | Exception chứa ID đơn không tìm thấy; handler dùng loại exception này để trả HTTP `404`. |

### Domain: quy tắc nghiệp vụ

| File | Công dụng và phần code cần đọc |
| --- | --- |
| `domain/model/Order.java` | Đơn hàng gồm ID, khách hàng, item, tổng tiền, trạng thái và thời gian tạo. `create` kiểm tra khách hàng/danh sách item, tạo UUID, trạng thái `PENDING`; `recalculateTotal` cộng thành tiền. `pay` chỉ chấp nhận `PENDING`; `cancel` chặn `SHIPPED`/`DELIVERED`. `reconstruct` khôi phục đơn từ dữ liệu đã lưu. |
| `domain/model/OrderItem.java` | Một sản phẩm trong đơn, gồm ID sản phẩm, số lượng, đơn giá. Constructor từ chối dữ liệu rỗng hoặc số lượng không dương; `getSubTotal()` nhân giá với số lượng. |
| `domain/model/Money.java` | Record biểu diễn số tiền và loại tiền. Dùng `BigDecimal`; constructor chặn số âm/loại tiền rỗng. `vnd`, `add`, `multiply` lần lượt tạo tiền VND, cộng cùng loại tiền và nhân số lượng. |
| `domain/model/OrderStatus.java` | Enum `PENDING`, `PAID`, `SHIPPED`, `CANCELLED`, `DELIVERED`. Hiện chưa có API chuyển đơn sang `SHIPPED` hoặc `DELIVERED`. |
| `domain/model/InvalidOrderStateException.java` | Exception khi thao tác không phù hợp trạng thái đơn; REST handler chuyển thành `409`. |
| `domain/repository/OrderRepository.java` | Interface khai báo `save` và `findById`. Domain/service dùng interface này mà không cần biết cách JPA lưu dữ liệu. `Optional` biểu diễn kết quả có thể không tồn tại. |

### Infrastructure: ánh xạ và lưu dữ liệu

| File | Công dụng và phần code cần đọc |
| --- | --- |
| `infrastructure/persistance/OrderJpaEntity.java` | `@Entity` ánh xạ bảng `orders`; `@Id` là khóa chính. `@ElementCollection` và `@CollectionTable` lưu item tại `order_items`; `@AttributeOverride` chỉ rõ tên cột. `@Enumerated(STRING)` lưu tên trạng thái. |
| `infrastructure/persistance/OrderItemJpaEntity.java` | `@Embeddable` mô tả các cột của item trong collection, gồm sản phẩm, số lượng và đơn giá. Constructor không tham số được JPA sử dụng. |
| `infrastructure/persistance/OrderMapper.java` | `toJpaEntity` chuyển domain sang entity để lưu; `toDomainEntity` dùng `Order.reconstruct` để khôi phục domain, gồm trạng thái và `createdAt`. |
| `infrastructure/persistance/OrderRepositoryImpl.java` | Triển khai interface domain. `save` chuyển sang entity, lưu qua Spring Data rồi chuyển lại; `findById` tìm entity và map về domain. `@Repository` đăng ký thành phần với Spring. |
| `infrastructure/persistance/SpringDataOrderRepository.java` | Kế thừa `JpaRepository<OrderJpaEntity, UUID>` để Spring Data cung cấp các thao tác persistence như `save` và `findById`. |

## 3. Database và cấu hình

| File | Công dụng |
| --- | --- |
| `pom.xml` | Cấu hình Maven: phiên bản Java/Spring Boot, dependencies Web, Validation, JPA, PostgreSQL, Flyway, H2 test, Swagger và plugin đóng gói. |
| `src/main/resources/application.properties` | Nạp `.env` nếu có; cấu hình cổng 8080, biến `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, Flyway và Swagger. `ddl-auto=validate` kiểm tra schema; `open-in-view=false` không giữ session JPA suốt request. |
| `src/main/resources/db/migration/V1__create_orders_schema.sql` | Tạo bảng `orders`, `order_items`, khóa ngoại, ràng buộc số tiền/số lượng và index. |
| `src/main/resources/db/migration/V2__ensure_orders_schema_exists.sql` | Dùng `IF NOT EXISTS` tạo lại các bảng/index còn thiếu ở một số database cũ. Nếu V1 đã thay đổi sau khi chạy thì vẫn cần xử lý checksum Flyway; V2 không tự sửa checksum. |
| `.env` | Cấu hình kết nối local; có thể chứa mật khẩu và đã nằm trong `.gitignore`. Không đưa thông tin thật vào tài liệu hoặc commit. |
| `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` | Maven Wrapper và cấu hình phiên bản Maven để chạy build trên Linux/macOS và Windows. |
| `README.md`, `AGENTS.md` | README hướng dẫn chạy/API; AGENTS.md là hướng dẫn làm việc trong repository. |

## 4. Các file kiểm thử

Các file dưới `src/test/java/com/example/demo1/` dùng JUnit Jupiter; tên test mô tả hành vi đang kiểm tra.

| File | Kiểm tra |
| --- | --- |
| `domain/model/MoneyTest.java` | Phép cộng, nhân, số tiền/loại tiền không hợp lệ và cộng khác tiền tệ. |
| `domain/model/OrderItemTest.java` | Thành tiền, số lượng không dương và sản phẩm/đơn giá rỗng. |
| `domain/model/OrderTest.java` | Tạo đơn, tổng tiền, bảo vệ danh sách, chuyển trạng thái, dữ liệu không hợp lệ và khôi phục thời gian tạo. |
| `application/dto/CreateOrderCommandTest.java` | Jakarta Validation cho item rỗng, dữ liệu item sai và request hợp lệ. |
| `application/service/OrderApplicationServiceTest.java` | Luồng service với repository giả trong bộ nhớ; bản sao tách biệt giúp phát hiện việc quên gọi lưu. |
| `application/service/OrderApplicationServiceMockTest.java` | Dùng Mockito giả repository để kiểm tra lưu đơn đã thanh toán và không lưu khi đơn không tồn tại. |
| `OrderIntegrationTest.java` | Khởi động Spring trên cổng ngẫu nhiên, chạy Flyway/JPA với H2 trong bộ nhớ và gọi HTTP thật. Kiểm tra luồng đơn, validation, lỗi và nhiều item. Đây chưa phải kiểm thử PostgreSQL thực tế. |

## 5. Đọc các annotation thường gặp

- `@Service`, `@Component`, `@Repository`: đăng ký class để Spring tạo và inject qua constructor.
- `@Transactional`: nhóm thao tác database trong transaction; exception không kiểm tra thường khiến transaction rollback.
- `@RequestBody`, `@PathVariable`: lấy dữ liệu từ JSON body và đoạn tương ứng trên URL.
- `@Column`, `@Id`, `@Entity`: mô tả ánh xạ giữa field Java và bảng/cột database.
- `@Test`, `@ParameterizedTest`: khai báo test JUnit; parameterized test chạy cùng logic với nhiều đầu vào.

## 6. Lệnh thường dùng

Chạy tại thư mục gốc trên Windows PowerShell:

```powershell
.\mvnw.cmd test                       # chạy toàn bộ test
.\mvnw.cmd -o test                    # chạy offline nếu đã tải đủ dependency
.\mvnw.cmd verify                     # chạy test và đóng gói
.\mvnw.cmd "-Dtest=OrderTest" test     # chạy riêng một class test
.\mvnw.cmd spring-boot:run            # chạy API; cần PostgreSQL và cấu hình .env
```

Trên Linux/macOS, dùng `./mvnw` thay cho `.\mvnw.cmd`. Xem `README.md` để lấy JSON mẫu và thiết lập kết nối. Swagger UI: `http://localhost:8080/swagger-ui.html`.

Giới hạn cần biết: API chưa kiểm tra tổng tiền vượt kích thước cột database; chưa có xác thực, hoàn tiền hay kiểm thử cạnh tranh đồng thời. Không dùng các dữ liệu trong test để suy ra hệ thống đã sẵn sàng cho môi trường thực tế.
