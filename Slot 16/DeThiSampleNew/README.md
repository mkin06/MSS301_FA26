# Media — DE190468

| Project trong Code_PE | Artifact | Port |
|---|---|---:|
| DE190468UserService | user | 8081 |
| DE190468PostService | post | 8082 |
| DE190468MediaGateway | mediagateway | 8080 |

JDK 21, Boot 4.1.0, Cloud 2025.1.2; database SQL Server
`MSS301_2026_PE_MEDIA`. Trong cả User/Post:
`spring.jpa.hibernate.ddl-auto=none`. Tạo bảng bằng
`database/create-database.sql`, sau đó chạy `database/seed.sql`.
Nếu schema đã tồn tại thì kiểm tra cấu trúc thay vì CREATE TABLE lại.

Cấu hình mặc định theo đề: jdbc:sqlserver://localhost:1433;
databaseName=MSS301_2026_PE_MEDIA;encrypt=false; username/password sa/sa.
Có thể đặt DB_USERNAME/DB_PASSWORD/DB_URL bằng biến môi trường cho máy thực tế.

Mở ba pom.xml trong IntelliJ và chạy ba Application theo thứ tự User, Post,
Gateway. Hoặc tại thư mục **Slot 16**:

```powershell
mvn clean verify
.\scripts\Start-Services.ps1 -Suite Media
```

Swagger: http://localhost:8080/swagger-ui.html. API tập trung:
`/api/users`, `/api/posts`. Mỗi resource có POST/PUT/GET detail/GET list/DELETE.
DELETE User đặt DEACTIVATED, DELETE Post đặt CANCELLED.

Import hai file trong `postman/`, chọn environment Media Local, chạy toàn bộ
38 request theo thứ tự. HTTP/status: 201 hoặc 200/1 thành công; 406/2 validation;
226/3 duplicate username; 404/4 không tìm thấy; 400/5 user không ACTIVE; 500/0
lỗi server/service offline. Ngày startDate/endDate dùng dd/MM/yyyy.

PostService gọi UserService bằng OpenFeign; PostDetailDTO chứa object user.
Tổng tiền = ratePerPost × số ngày, cập nhật dùng giá hiện tại. Seed tạo
creator_a ACTIVE 500000/3 bài; creator_b ACTIVE 300000/1 bài; creator_c
SUSPENDED 800000/4 bài. ID test lấy từ response, không giả định giá trị seed ID.

Khi xong: `scripts/Stop-Services.ps1 -Suite Media`. Hai bộ đề dùng cùng port
theo đặc tả nên dừng Media trước khi chạy Hotel.

Nộp source: tại Slot 16 chạy `mvn clean`, `python scripts/package.py`.
ZIP: `dist/DE190468_Media_Slot16.zip`. Nếu dùng riêng ba project trong Code_PE,
chạy `mvn clean` tại từng project rồi zip folder cùng database scripts.

Chi tiết: [đối chiếu yêu cầu](../REQUIREMENTS.md),
[hướng dẫn chung](../README.md), [kết quả test](../test-results/README.md).
