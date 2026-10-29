# Hotel — DE190468

| Project trong Code_PE | Artifact | Port |
|---|---|---:|
| DE190468RoomService | room | 8081 |
| DE190468ReservationService | reservation | 8082 |
| DE190468HotelGateway | hotelgateway | 8080 |

JDK 21, Boot 4.1.0 (đáp ứng >=3.5.11), Cloud 2025.1.2, SQL Server
`MSS301_2026_PE`. Room/Reservation đều có
`spring.jpa.hibernate.ddl-auto=none`. Tạo bảng bằng
`database/create-database.sql`, sau đó chạy `database/seed.sql`.
Nếu schema đã tồn tại thì kiểm tra cấu trúc thay vì CREATE TABLE lại.

Cấu hình mặc định theo đề: jdbc:sqlserver://localhost:1433;
databaseName=MSS301_2026_PE;encrypt=false; username/password sa/sa.
Ghi đè bằng DB_USERNAME/DB_PASSWORD/DB_URL nếu máy dùng thông tin khác.

Mở từng pom.xml bằng IntelliJ và chạy ba Application theo thứ tự Room,
Reservation, Gateway. Hoặc tại thư mục **Slot 16**:

```powershell
mvn clean verify
.\scripts\Start-Services.ps1 -Suite Hotel
```

Swagger: http://localhost:8080/swagger-ui.html. API tập trung:
`/api/rooms`, `/api/reservations`. Mỗi resource có POST/PUT/GET detail/GET list/
DELETE. Gateway có Spring Security WebFlux cho phép truy cập API/Swagger theo
đề; CORS chấp nhận mọi host.

Import hai file trong `postman/`, chọn environment Hotel Local, chạy toàn bộ
37 request theo thứ tự. HTTP/status: 201 hoặc 200/1 thành công; 406/2 validation;
226/3 duplicate roomNumber; 404/4 không tìm thấy; 400/5 phòng không AVAILABLE;
500/0 lỗi server/service offline.

Checkin/checkout dùng dd/MM/yyyy; checkout phải sau checkin. Số khách phải <=
capacity lấy từ RoomService bằng Feign. Tổng tiền = pricePerNight × số đêm;
ReservationDetailDTO chứa object room đầy đủ. DELETE Room đặt MAINTENANCE;
DELETE Reservation đặt CANCELLED, giữ bản ghi. Seed có A-101 DOUBLE 850000/
capacity 2 AVAILABLE, A-102 SINGLE 500000/capacity 1 AVAILABLE và B-201 SUITE
1500000/capacity 4 MAINTENANCE.

Khi xong: `scripts/Stop-Services.ps1 -Suite Hotel`. Dừng Media trước khi chạy
Hotel vì hai đề quy định cùng cổng. Nộp source tại Slot 16: `mvn clean`,
`python scripts/package.py` → `dist/DE190468_Hotel_Slot16.zip`. Với từng project
Code_PE mở riêng, chạy `mvn clean` tại mỗi project trước khi nén.

Chi tiết: [đối chiếu yêu cầu](../REQUIREMENTS.md),
[hướng dẫn chung](../README.md), [kết quả test](../test-results/README.md).
