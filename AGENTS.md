# Repository Guidelines

## Cấu trúc dự án

Đây là backend demo quản lý đơn hàng bằng Java 17 và Spring Boot. Mã nguồn nằm trong `src/main/java/com/example/demo1`:

- `presentation/rest`: controller và xử lý lỗi HTTP.
- `application/dto`, `application/service`: request, response và điều phối nghiệp vụ.
- `domain/model`, `domain/repository`: quy tắc nghiệp vụ và interface repository; giữ lớp domain độc lập với Spring/JPA.
- `infrastructure/persistance`: entity, mapper và triển khai repository JPA. Giữ nguyên tên package hiện tại khi bổ sung code.

Cấu hình nằm trong `src/main/resources`; migration Flyway nằm trong `db/migration`. Test tương ứng nằm trong `src/test/java`. Dự án không có frontend hoặc thư mục assets.

## Build, test và chạy

Dùng Maven Wrapper từ thư mục gốc (trên Linux/macOS thay `.\mvnw.cmd` bằng `./mvnw`):

- `.\mvnw.cmd test`: chạy unit test, mock test và integration test.
- `.\mvnw.cmd verify`: chạy test và đóng gói, kiểm tra toàn bộ build.
- `.\mvnw.cmd spring-boot:run`: khởi động API trên cổng 8080; cần PostgreSQL và cấu hình kết nối.
- `.\mvnw.cmd -o test`: chạy offline khi dependency đã có sẵn.

Swagger UI ở `http://localhost:8080/swagger-ui.html`. Xem README để biết ví dụ request và cấu hình database.

## Phong cách code

Dùng thụt lề 4 dấu cách, tên class PascalCase, method và biến camelCase; tên package viết thường. Theo cấu trúc lớp và constructor injection hiện có. Đặt logic đơn hàng trong domain, xử lý transaction trong service và validation request ở DTO. Dùng `BigDecimal` cho tiền. Chưa có formatter hoặc linter được cấu hình trong Maven; giữ định dạng nhất quán với file đang sửa.

## Kiểm thử

Dùng JUnit Jupiter và Mockito; đặt tên file `*Test.java` và tên test mô tả hành vi. Integration test khởi động Spring, gọi HTTP và chạy Flyway/JPA trên H2 trong bộ nhớ; đây không thay thế kiểm thử PostgreSQL thật. Chạy `.\mvnw.cmd test` trước khi gửi thay đổi, và thêm test cho hành vi hoặc lỗi quan trọng. Dự án chưa đặt ngưỡng coverage.

## Commit và pull request

Workspace hiện không có Git history để xác định quy ước commit. Viết tiêu đề ngắn, nêu hành động và phạm vi thay đổi. Pull request nên mô tả mục đích, thay đổi API hoặc database, kết quả kiểm thử và issue liên quan nếu có.

## Cấu hình và database

Đặt `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` trong `.env` hoặc biến môi trường; không đưa mật khẩu vào commit. Dùng database phát triển riêng. Thêm migration Flyway mới khi đổi schema, tránh sửa migration đã áp dụng; Hibernate đang dùng `ddl-auto=validate`.