# Slot 16 — DE190468 — Lê Ngọc Minh Kiên

Hai bài thực hành được triển khai độc lập trên **JDK 21, Spring Boot 4.1.0,
Spring Cloud 2025.1.2, springdoc 3.0.3 và SQL Server**.

| Bộ đề | Ba project Maven độc lập | Database |
|---|---|---|
| [Media](DeThiSampleNew/README.md) | DE190468UserService, DE190468PostService, DE190468MediaGateway | MSS301_2026_PE_MEDIA |
| [Hotel](DeThi_SU26/README.md) | DE190468RoomService, DE190468ReservationService, DE190468HotelGateway | MSS301_2026_PE |

Các project nằm trong `Code_PE/` của từng bộ đề. Mở từng `pom.xml` trong IntelliJ
hoặc mở `Slot 16/pom.xml` để import cả sáu module. Package dùng
`fu.de190468.user`, `.post`, `.room`, `.reservation`, `.gateway` đúng tên service
trong đề. Controller có prefix `DE190468`.

## 1. Tạo database bằng SQL trước khi chạy

**Cả bốn service JPA đều cấu hình `spring.jpa.hibernate.ddl-auto=none`.**
Ứng dụng không tạo/cập nhật schema. Hai gateway không dùng JPA/database.

Tại thư mục `Slot 16`, dùng tài khoản Windows có quyền tạo database:

```powershell
sqlcmd -S localhost -E -C -b -i .\DeThiSampleNew\database\create-database.sql
sqlcmd -S localhost -E -C -b -i .\DeThiSampleNew\database\seed.sql
sqlcmd -S localhost -E -C -b -i .\DeThi_SU26\database\create-database.sql
sqlcmd -S localhost -E -C -b -i .\DeThi_SU26\database\seed.sql
```

Chạy `create-database.sql` một lần trên database chưa có bảng tương ứng; script
giữ nguyên tên bảng, cột, FK, UNIQUE và CHECK của đề. Nếu bảng đã tồn tại thì
kiểm tra schema thay vì chạy lại lệnh CREATE TABLE. `seed.sql` có thể chạy lại
vì có kiểm tra dữ liệu tồn tại.

Với SQL authentication, thay `-E` bằng `-U sa` để sqlcmd hỏi mật khẩu. Cấu hình
mặc định trong ứng dụng theo đề: localhost:1433, username/password `sa`/`sa`,
encrypt=false, cổng gateway/service 8080/8081/8082, context path mặc định.
Nếu máy dùng tài khoản/mật khẩu khác, đặt biến môi trường trong terminal chạy app:

```powershell
$env:DB_USERNAME = 'sa'
$env:DB_PASSWORD = '<mat-khau-SQL-Server-cua-ban>'
```

`DB_URL` có thể ghi đè URL kết nối khi cần; vẫn phải trỏ đúng database của bộ đề.
Không ghi mật khẩu thực tế vào Git. Lượt kiểm thử tại máy này dùng một SQL login
riêng chỉ có quyền đọc/ghi hai database mới, không đổi mật khẩu tài khoản `sa`.
Thông tin kết nối cục bộ nằm trong `.work/local-db.json` được Git bỏ qua.

## 2. Build và chạy

```powershell
mvn clean verify
.\scripts\Start-Services.ps1 -Suite Media
```

Mở `http://localhost:8080/swagger-ui.html`. Dropdown có hai bộ API; OpenAPI được
proxy qua gateway (`/openapi/users`, `/openapi/posts` hoặc `/openapi/rooms`,
`/openapi/reservations`). Swagger riêng ở 8081 và 8082 cũng hoạt động.
`/v3/api-docs` được bật trên cả sáu ứng dụng.

Mỗi bộ đề dùng cùng bộ cổng theo đề, vì vậy chạy lần lượt:

```powershell
.\scripts\Stop-Services.ps1 -Suite Media
.\scripts\Start-Services.ps1 -Suite Hotel
# Khi xong:
.\scripts\Stop-Services.ps1 -Suite Hotel
```

Script đợi OpenAPI sẵn sàng trước khi báo ready, ghi PID/log dưới `.work/`, kiểm
tra cổng trước khi mở app và chỉ dừng đúng process Java/JAR đã lưu. Có thể chạy
thủ công bằng `java -jar target/<artifact>-1.0.0.jar` trong mỗi project.
`-PortOffset 10000` dành cho kiểm thử cổng 18080–18082; cấu hình mặc định vẫn là
cổng của đề. Khi đổi cổng, chỉnh `baseUrl` trong Postman và URL server ở Swagger
Try it out tương ứng.

## 3. Import Postman và chạy kiểm thử

Mỗi bộ có `postman/Slot16.postman_collection.json` và
`postman/Local.postman_environment.json`. Import cả hai, chọn environment, chạy
collection từ đầu đến cuối bằng Collection Runner. `baseUrl` mặc định là
`http://localhost:8080`; ID được tự lấy từ response, tên test được tạo riêng cho
mỗi lượt chạy. Collection tạo dữ liệu test và dùng soft delete ở cuối.

- Media: 38 requests, 150 assertions.
- Hotel: 37 requests, 146 assertions.

Không cần cài Newman để dùng Postman. Nếu dùng Newman CLI:

```powershell
newman run .\DeThiSampleNew\postman\Slot16.postman_collection.json
newman run .\DeThi_SU26\postman\Slot16.postman_collection.json
```

Unit test không cần database: `mvn test`. Kiểm thử end-to-end bằng Python chuẩn
thư viện (không cần pip), sau khi tạo schema/seed và build JAR:

```powershell
python .\scripts\smoke.py
```

Script tự khởi động từng bộ trên 18080–18082, gọi SQL Server và OpenFeign thật,
kiểm tra JSON/HTTP/Swagger/CORS, thử lỗi service offline, rồi dừng các process
do nó tạo. Nếu dùng credential cục bộ của lượt kiểm thử này:
`python scripts/smoke.py --credentials .work/local-db.json`.
Chỉ chạy khi các cổng test còn trống. Các request tạo thêm bản ghi với prefix
ngẫu nhiên, không reset database.

Xem [đối chiếu yêu cầu](REQUIREMENTS.md) và [kết quả kiểm thử](test-results/README.md).

## 4. Đóng gói nộp bài

```powershell
mvn clean
python .\scripts\package.py
```

Hai file nén được tạo ở `dist/DE190468_Media_Slot16.zip` và
`dist/DE190468_Hotel_Slot16.zip`, mỗi file chứa ba project, database scripts,
Postman và hướng dẫn. Không chứa `target/`, IDE cache, log hoặc credential.
Tên ZIP có prefix mã sinh viên. ZIP là sản phẩm build cục bộ; script và source
được commit để tái tạo. Việc nộp lên PEA do sinh viên thực hiện.

## Tài liệu nền tảng

- [Spring Cloud 2025.1.2 hỗ trợ Spring Boot 4.1.0](https://spring.io/blog/2026/06/11/spring-cloud-2025-1-2-aka-oakwood-has-been-released/).
- [springdoc và Spring Boot 4](https://springdoc.org/).
