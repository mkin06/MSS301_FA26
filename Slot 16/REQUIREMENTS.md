# Đối chiếu đề bài Slot 16

Người thực hiện: **DE190468 — Lê Ngọc Minh Kiên**.

Nguồn đã đọc: `DeThiSampleNew/DeThiSampleNew/DeThiSample_New.docx`, toàn bộ nội
dung hướng dẫn `HuongDanSample_New.docx` (hai bản giống nhau), 10 ảnh trang đề
`DeThi_SU26/DeThi/1_page_1.jpg` đến `1_page_10.jpg`, và bốn script SQL gốc.
Các tài liệu gốc được giữ tại vị trí ban đầu trên máy. Source triển khai nằm
trong `Code_PE/`, tách khỏi thư mục đề.

## A. Yêu cầu chung và cấu hình bắt buộc

| Yêu cầu | Triển khai |
|---|---|
| JDK 21, jar, Maven, mở được trong IntelliJ | Sáu project độc lập, mỗi project có pom.xml; reactor ở Slot 16 |
| Tên `<StudentID><ServiceName>` | Prefix DE190468 cho project, application và controller |
| Layered MVC | entity → repository; service interface và service.impl → controller; dto, config, common |
| Package đúng mã sinh viên | fu.de190468.user/post/room/reservation/gateway |
| SQL Server 2016+ và JDBC driver | mssql-jdbc; script giữ nguyên bảng/cột, IDENTITY, FK, UNIQUE, CHECK |
| `spring.jpa.hibernate.ddl-auto=none` | Bốn application.properties JPA; SQL tạo schema trước khi chạy |
| Datasource theo đề | localhost:1433, sa/sa, encrypt=false; DB MEDIA hoặc PE; ghi đè qua env nếu máy khác |
| Port/context | Service 8081, 8082; gateway 8080; context path mặc định |
| Swagger + SpringDoc | /swagger-ui.html và /v3/api-docs trên cả sáu app; gateway gom docs hai service |
| CORS mọi host | CorsFilter ở MVC, CorsWebFilter ở WebFlux; dedupe header khi proxy |
| JSON envelope | status, message, data, timestamp; lỗi có data=null |
| ISO timestamp UTC | `YYYY-MM-DDThh:mm:ssZ`, không có phần lẻ giây |
| Phân trang | page mặc định 0, size mặc định 10, size 1–100; kiểm tra page âm và sai kiểu |
| Validation DB/DTO | Create kiểm tra mandatory; update chỉ kiểm tra trường được cung cấp; enum, length, email, numeric và precision |
| Partial update | Trường null/không cung cấp giữ giá trị cũ; kiểm tra giá trị hiệu lực trước khi sửa entity |
| Dọn target và ZIP | mvn clean; scripts/package.py tạo hai ZIP có prefix DE190468 |

Gateway Hotel có Spring Security WebFlux theo đề. Đề không đưa luồng đăng nhập;
SecurityWebFilterChain cho phép các API và Swagger, tắt form/basic và CSRF cho
API JSON. Gateway Media dùng WebFlux đúng dependency của bộ đề mẫu.

## B. User Service — TODO Media 1

Entity User map `users`: user_id, username, email, role, rate_per_post,
max_daily_posts, level, status. Role và UserStatus là enum `@Enumerated(STRING)`;
giá trị chuỗi vẫn được lưu trong các cột NVARCHAR của SQL.

UserDTO: userId (Long), username (unique, max 30, bắt buộc), email (email hợp lệ,
max 100, bắt buộc), role (USER/CREATOR/ADMIN), ratePerPost (BigDecimal >0,
decimal(18,2)), maxDailyPosts (1–10), level (>=1), status
(ACTIVE/SUSPENDED/DEACTIVATED). Default create ACTIVE.

| Endpoint | Thành công | Nhánh lỗi bắt buộc |
|---|---|---|
| POST /api/users | 201/1, UserDTO | 406/2 validation; 226/3 duplicate; 500/0 |
| PUT /api/users/{userId} | 200/1, UserDTO, partial | 406/2; 226/3; 404/4; 500/0 |
| GET /api/users/{userId} | 200/1, UserDTO | 404/4; 500/0 |
| DELETE /api/users/{userId} | 200/1, data=null, DEACTIVATED | 404/4; 500/0 |
| GET /api/users | 200/1, PageDTO<UserDTO> | 406/2; 500/0 |

List lọc role, status; PageDTO đủ size, page, totalPages, totalElements, first,
last, content. User Service không có OpenFeign.

## C. Post Service — TODO Media 2

Entity Post map `posts`: post_id, advertiser_name/email/phone, start_date,
end_date, number_of_posts, total_cost, status, user_id. DTO dùng java.util.Date,
JSON **dd/MM/yyyy**, không có giờ. Entity PostStatus:
DRAFT/SCHEDULED/PUBLISHED/CANCELLED.

Advertiser name/email max 100, phone max 20, các trường bắt buộc theo SQL;
email đúng format; endDate > startDate; numberOfPosts >=1 và không vượt
maxDailyPosts lấy qua Feign. userId bắt buộc và phải tồn tại.

POST kiểm tra theo thứ tự: dữ liệu/ngày → User tồn tại (404/4) → ACTIVE (400/5)
→ maxDailyPosts (406/2) → lưu SCHEDULED. totalCost = ratePerPost × số ngày
giữa startDate và endDate, đúng công thức đề (không nhân thêm numberOfPosts).
ID và totalCost do server quyết định. PUT tính lại bằng rate hiện tại và hỗ trợ
đổi userId, ngày, số bài và thông tin advertiser.

| Endpoint | Thành công | Nhánh lỗi bắt buộc |
|---|---|---|
| POST /api/posts | 201/1, PostDTO | 406/2; user 404/4; user không ACTIVE 400/5; 500/0 |
| PUT /api/posts/{postId} | 200/1, PostDTO, partial | 406/2; post 404/4; 500/0 |
| GET /api/posts/{postId} | 200/1, PostDetailDTO | 404/4; 500/0 |
| DELETE /api/posts/{postId} | 200/1, data=null, CANCELLED | 404/4; 500/0 |
| GET /api/posts | 200/1, PageDTO<PostDetailDTO> | 406/2; 500/0 |

PostDetailDTO thay userId bằng object `user` đầy đủ từ Feign. List lọc
advertiserName partial match và status. `FeignException.NotFound` được đổi
thành 404/4; timeout/offline/response lỗi giữ 500/0.

Media Gateway — TODO Media 3: `/api/users/**` → 8081, `/api/posts/**` → 8082,
cổng 8080, WebFlux, Swagger, CORS. Đường dẫn collection không có dấu `/` cuối
cũng được route.

## D. Room Service — TODO Hotel 1

Entity Room map `rooms`: room_id, room_number, room_type, price_per_night,
capacity, floor, status. RoomDTO có roomId, roomNumber (unique, max 20),
roomType (SINGLE/DOUBLE/SUITE/DELUXE), pricePerNight (BigDecimal >0,
decimal(18,2)), capacity (1–10), floor (>=1), status
(AVAILABLE/OCCUPIED/MAINTENANCE). Default create AVAILABLE.

| Endpoint | Thành công | Nhánh lỗi bắt buộc |
|---|---|---|
| POST /api/rooms | 201/1, RoomDTO | 406/2; 226/3 duplicate; 500/0 |
| PUT /api/rooms/{roomId} | 200/1, RoomDTO, partial | 406/2; 226/3; 404/4; 500/0 |
| GET /api/rooms/{roomId} | 200/1, RoomDTO | 404/4; 500/0 |
| DELETE /api/rooms/{roomId} | 200/1, data=null, MAINTENANCE | 404/4; 500/0 |
| GET /api/rooms | 200/1, PageDTO<RoomDTO> | 406/2; 500/0 |

List lọc roomType/status; soft delete giữ bản ghi để FK hiện có vẫn hợp lệ.

## E. Reservation Service — TODO Hotel 2

Entity Reservation map `reservations`: reservation_id, guest_name/email/phone,
check_in_date, check_out_date, number_of_guests, total_amount, status, room_id.
ReservationDTO: reservationId, guestName/Email (max 100), guestPhone (max 20),
checkInDate/checkOutDate (**java.util.Date, dd/MM/yyyy**), numberOfGuests,
totalAmount (BigDecimal), status, roomId. Enum ReservationStatus:
CONFIRMED/CHECKED_IN/CHECKED_OUT/CANCELLED.

POST: ngày checkout > checkin; room tồn tại qua RoomClient (404/4), AVAILABLE
(400/5); khách >=1, <=10 và <=capacity lấy qua Feign (406/2). Default CONFIRMED.
totalAmount = pricePerNight × số đêm; bỏ qua số tiền client nhập. PUT dùng ngày
hiệu lực và giá phòng hiện tại, kiểm tra capacity trước khi ghi, hỗ trợ đổi roomId.

| Endpoint | Thành công | Nhánh lỗi bắt buộc |
|---|---|---|
| POST /api/reservations | 201/1, ReservationDTO | 406/2; room 404/4; room unavailable 400/5; 500/0 |
| PUT /api/reservations/{reservationId} | 200/1, ReservationDTO | 406/2; reservation 404/4; 500/0 |
| GET /api/reservations/{reservationId} | 200/1, ReservationDetailDTO | 404/4; 500/0 |
| DELETE /api/reservations/{reservationId} | 200/1, data=null, CANCELLED | 404/4; 500/0 |
| GET /api/reservations | 200/1, PageDTO<ReservationDetailDTO> | 406/2; 500/0 |

ReservationDetailDTO thay roomId bằng object `room` đầy đủ; list lọc guestName
partial match và status. RoomService được gọi qua OpenFeign, không qua repository
của Reservation Service. SQL DATE được ánh xạ qua DateOnlyConverter để giữ đúng
ngày trong mọi múi giờ khi chuyển sang java.util.Date và JSON UTC.

Hotel Gateway — TODO Hotel 3: `/api/rooms/**` → 8081,
`/api/reservations/**` → 8082, cổng 8080, WebFlux, Spring Security, Swagger, CORS.

## F. Bằng chứng kiểm tra và commit

Unit test: DTO create/update groups, boundary validation, duplicate/race,
missing record, soft delete, partial update giữ trường cũ, rollback khi ngày
sai, tính tiền, giới hạn từ Feign, Feign 404, remote lỗi, date round-trip.

HTTP với SQL Server thật: scripts/smoke.py. Collection import Postman:
`postman/Slot16.postman_collection.json` của từng bộ, chạy được bằng Newman.
Kết quả ghi tại `test-results/`.

Commit tách scaffold/schema, User, Post, Room, Reservation, Gateway,
kiểm thử và tài liệu; message theo `<type>(<scope>): <subject>`.
Git author/committer: Lê Ngọc Minh Kiên, lengocminhkien06@gmail.com.
