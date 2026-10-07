# Phân tích yêu cầu Assignment 1

## Phạm vi và cách thực hiện

Bốn ứng dụng độc lập dùng Java 21, Spring Boot 4.1.0 và Spring Cloud 2025.1.3. Gateway MVC là điểm vào duy nhất; customer-service cấp JWT HS256, gateway xác thực và chuyển ngữ cảnh người dùng. SQL Server lưu customer và tên Unicode; MongoDB lưu danh mục và Decimal128; MySQL lưu giao dịch và snapshot vé.

Mỗi TODO có commit riêng với footer Refs: TODO x.y. Thứ tự dựa trên phụ thuộc để mọi commit Java đều compile được: entity/repository trước service, DTO trước controller, dữ liệu danh mục trước seeder. Chỉ author và committer đã cấu hình của chủ repository được dùng. Không thêm co-author. Hai file Word không thuộc source nộp bài.

## Phân rã từng TODO

### F0 – Hạ tầng & khởi tạo project

| TODO | Nội dung | File / Vị trí |
|---|---|---|
| **TODO 0.1** | Tạo thư mục gốc `fu-cinema/`, `docker-compose.yml` chạy **3 database**: SQL Server 2022 (`sa`/`Fucinema@2026`, port 1433, có `healthcheck`), MongoDB 7.0.5 (`root`/`password`, port 27017), MySQL 8.3.0 (`root`/`mysql`, port 3306) | `fu-cinema/docker-compose.yml` |
| **TODO 0.2** | Script tạo database: `sqlserver/init.sql` (`cinema_customer`, chạy bởi service `sqlserver-init` sau khi SQL Server healthy) và `mysql/init.sql` (`cinema_booking`). MongoDB tự tạo `cinema_movie` | `fu-cinema/sqlserver/init.sql`, `fu-cinema/mysql/init.sql` |
| **TODO 0.3** | Generate 3 project service tại start.spring.io (Boot 4.1.0, Java 21, Group `com.fudn`), Lombok + Validation + Spring Web cho cả 3, thêm:<br>• `customer-service`: Spring Data JPA, **MS SQL Server Driver**, Flyway<br>• `movie-service`: **Spring Data MongoDB**<br>• `booking-service`: Spring Data JPA, **MySQL Driver**, Flyway, OpenFeign | `customer-service/`, `movie-service/`, `booking-service/` |
| **TODO 0.4** | Generate project `api-gateway`: Gateway (Server Web MVC), OAuth2 Resource Server, Actuator | `api-gateway/` |
| **TODO 0.5** | Cấu hình `application.properties`: customer → `jdbc:sqlserver://...;encrypt=true;trustServerCertificate=true`; movie → `spring.mongodb.uri` (Boot 4); booking → `jdbc:mysql://...`; port & `ddl-auto=none` | `src/main/resources/application.properties` |
| **TODO 0.6** | Tạo package chung `exception` (`ApiException`, `ErrorResponse`, `GlobalExceptionHandler`) cho mỗi service | `exception/*.java` |

### F1 – Authentication (customer-service + Gateway)

| TODO | Nội dung | File |
|---|---|---|
| **TODO 1.1** | Khai báo tài khoản Admin & JWT secret/expiration trong properties | `customer-service/application.properties` |
| **TODO 1.2** | Bean `PasswordEncoder` (BCrypt) | `config/PasswordConfig.java` |
| **TODO 1.3** | `JwtService.generateToken(uid, email, role)` – ký HS256, claims `sub`, `uid`, `role`, `iat`, `exp` | `security/JwtService.java` |
| **TODO 1.4** | `AuthService.login()` – kiểm tra Admin trong properties trước, sau đó Customer trong DB (BCrypt), chặn `INACTIVE` | `service/AuthService.java` |
| **TODO 1.5** | `POST /api/auth/login` trả `LoginResponse` | `controller/AuthController.java` |

### F2 – Customer: Register & Profile (customer-service)

| TODO | Nội dung | File |
|---|---|---|
| **TODO 2.1** | Flyway **T-SQL** `V1__init.sql` (bảng `customer`: `IDENTITY`, `NVARCHAR`) + `V2__seed.sql` (3 customer mẫu với chuỗi `N'...'`, 1 `INACTIVE`); dependency `flyway-sqlserver` | `db/migration/` |
| **TODO 2.2** | Entity `Customer`, enum `CustomerStatus`, `CustomerRepository` | `model/`, `repository/` |
| **TODO 2.3** | DTO: `RegisterRequest`, `ProfileUpdateRequest`, `ChangePasswordRequest`, `CustomerResponse` (có Bean Validation) | `dto/` |
| **TODO 2.4** | `register()` – BR01, mã hóa password, status `ACTIVE` | `service/CustomerService.java` |
| **TODO 2.5** | `getProfile()`, `updateProfile()`, `changePassword()` (kiểm tra mật khẩu cũ) – lấy ID từ header `X-User-Id` | `service/CustomerService.java` |
| **TODO 2.6** | Endpoint `/register`, `/me`, `/me/password` | `controller/CustomerController.java` |

### F3 – Admin: Manage customers (customer-service)

| TODO | Nội dung | File |
|---|---|---|
| **TODO 3.1** | DTO `AdminCustomerRequest` (có `customerStatus`, password bắt buộc khi tạo) | `dto/` |
| **TODO 3.2** | `search(keyword)`, `getById()`, `create()`, `update()`, `delete()` (soft delete) | `service/CustomerService.java` |
| **TODO 3.3** | Endpoint CRUD `/api/customers` | `controller/CustomerController.java` |

### F4 – Admin: Manage genres & cinema rooms (movie-service)

| TODO | Nội dung | File |
|---|---|---|
| **TODO 4.1** | `DataSeeder` (`CommandLineRunner`) nạp genres, rooms, movies, showtimes mẫu với **ObjectId cố định**; bỏ qua nếu collection đã có dữ liệu | `config/DataSeeder.java` |
| **TODO 4.2** | Document `Genre`, `CinemaRoom` (`@Document`, `@Indexed(unique = true)`) + enum `RoomType`, `RoomStatus` + `MongoRepository<…, String>` | `model/`, `repository/` |
| **TODO 4.3** | `GenreService`, `RoomService` CRUD, BR03 khi xóa | `service/` |
| **TODO 4.4** | `GenreController`, `RoomController` | `controller/` |

### F5 – Admin: Manage movies (movie-service)

| TODO | Nội dung | File |
|---|---|---|
| **TODO 5.1** | Document `Movie` (tham chiếu `genreId` dạng `String`), enum `AgeRating`, `MovieStatus` | `model/` |
| **TODO 5.2** | Tìm kiếm phim bằng **`MongoTemplate` + `Criteria`** (keyword regex không phân biệt hoa thường, `genreId`, `status` tùy chọn) | `service/MovieService.java` |
| **TODO 5.3** | `MovieService` CRUD + BR03 + BR15 (kiểm tra `genreId` tồn tại); `genreName` trong response lấy bằng **application-side join** | `service/MovieService.java` |
| **TODO 5.4** | `MovieController` (GET public, ghi cần ADMIN – phân quyền tại Gateway) | `controller/MovieController.java` |

### F6 – Admin: Manage showtimes (movie-service)

| TODO | Nội dung | File |
|---|---|---|
| **TODO 6.1** | Document `Showtime` (`movieId`, `roomId`; `ticketPrice` kiểu `Decimal128`; `@CompoundIndex` `roomId + startTime`), enum `ShowtimeStatus` | `model/` |
| **TODO 6.2** | **Derived query** `countByRoomIdAndShowtimeStatusAndStartTimeLessThanAndEndTimeGreaterThanAndShowtimeIdNot(...)` kiểm tra trùng giờ cùng phòng | `repository/ShowtimeRepository.java` |
| **TODO 6.3** | `ShowtimeService.create/update` – BR04, BR05, BR15, tự tính `endTime` | `service/ShowtimeService.java` |
| **TODO 6.4** | `ShowtimeService.cancel` – BR06; `search(movieId, date)` | `service/ShowtimeService.java` |
| **TODO 6.5** | `ShowtimeController` – `GET /{id}` trả đủ `seatRows`, `seatsPerRow`, `ticketPrice` cho Booking Service | `controller/ShowtimeController.java` |

### F7 – Customer: Create booking (booking-service + OpenFeign)

| TODO | Nội dung | File |
|---|---|---|
| **TODO 7.1** | Thêm `spring-cloud-starter-openfeign` + BOM `spring-cloud-dependencies` 2025.1.3; `@EnableFeignClients` | `pom.xml`, `BookingServiceApplication.java` |
| **TODO 7.2** | Flyway **MySQL** `V1__init.sql` (bảng `booking`, `booking_detail`; `showtime_id`, `movie_id` kiểu `VARCHAR(24)` để chứa ObjectId) | `db/migration/` |
| **TODO 7.3** | Entity `Booking` (`@OneToMany` details, cascade) và `BookingDetail`, enum `BookingStatus` | `model/` |
| **TODO 7.4** | `MovieClient` (`@FeignClient`, `GET /api/showtimes/{id}` với `id` kiểu `String`) + `ShowtimeResponse`; `movie.service.url` trong properties | `client/MovieClient.java` |
| **TODO 7.5** | `BookingService.create()` – BR07 → BR10, BR14; snapshot thông tin phim vào detail | `service/BookingService.java` |
| **TODO 7.6** | `getSeatMap(showtimeId)` – trả ghế đã đặt + số ghế trống | `service/BookingService.java` |
| **TODO 7.7** | `POST /api/bookings`, `GET /api/bookings/showtimes/{id}/seats` | `controller/BookingController.java` |

### F8 – Customer: Booking history & cancel (booking-service)

| TODO | Nội dung | File |
|---|---|---|
| **TODO 8.1** | `getMyBookings(customerId)` sắp xếp `bookingDate` giảm dần | `service/BookingService.java` |
| **TODO 8.2** | `getById(id, userId, role)` – BR11 | `service/BookingService.java` |
| **TODO 8.3** | `cancel(id, userId, role)` – BR11, BR12 | `service/BookingService.java` |
| **TODO 8.4** | Endpoint `/my`, `/{id}`, `/{id}/cancel`, `GET /api/bookings` (Admin) | `controller/BookingController.java` |

### F9 – Admin: Report statistic (booking-service)

| TODO | Nội dung | File |
|---|---|---|
| **TODO 9.1** | Query booking `CONFIRMED` trong khoảng `[startDate 00:00, endDate 23:59:59]` sắp xếp giảm dần | `repository/BookingRepository.java` |
| **TODO 9.2** | `report(startDate, endDate)` – BR13, tính `totalBookings`, `totalTickets`, `totalRevenue`, `revenueByMovie` (giảm dần theo doanh thu) | `service/BookingService.java` |
| **TODO 9.3** | `GET /api/bookings/report` | `controller/BookingController.java` |

### F10 – API Gateway: Routing & Security

| TODO | Nội dung | File |
|---|---|---|
| **TODO 10.1** | `server.port=9000`, URL 3 service, `app.jwt.secret` (trùng customer-service) | `api-gateway/application.properties` |
| **TODO 10.2** | `UserHeaderFilter` – xóa header `X-User-*` từ client, chèn lại từ JWT | `filter/UserHeaderFilter.java` |
| **TODO 10.3** | `Routes` – 3 route: customer (`/api/auth/**`, `/api/customers/**`), movie (`/api/genres/**`, `/api/rooms/**`, `/api/movies/**`, `/api/showtimes/**`), booking (`/api/bookings/**`) | `routes/Routes.java` |
| **TODO 10.4** | `SecurityConfig` – `JwtDecoder` HS256, `JwtAuthenticationConverter` (claim `role` → `ROLE_*`), phân quyền theo bảng mục 5 | `config/SecurityConfig.java` |

### F11 – Kiểm thử bằng Postman

| TODO | Nội dung |
|---|---|
| **TODO 11.1** | Tạo Environment `FUCinema-Local` (`gateway`, `adminToken`, `customerToken`, `customer2Token`, `genreId`, `roomId`, `movieId`, `showtimeId`, `bookingId`) |
| **TODO 11.2** | Tạo Collection theo folder F1 → F10, mỗi request có **Tests script** kiểm tra status code & lưu biến |
| **TODO 11.3** | Chạy toàn bộ Collection bằng **Collection Runner** – tất cả test **pass**; export collection + environment nộp kèm bài |

---


## Các điểm cần xác minh

- BR01–BR02: email duy nhất, không trùng admin, BCrypt và chặn tài khoản INACTIVE.
- BR03–BR06, BR15: bảo vệ tham chiếu, tự tính giờ kết thúc, không trùng lịch, hủy mềm.
- BR07–BR10, BR14: 1–8 vé, ghế hợp lệ và duy nhất, giá từ server, Feign lỗi trả 503.
- BR11–BR13: quyền sở hữu, hạn hủy 2 giờ, report chỉ CONFIRMED và sắp giảm dần.
- BR16: đọc/ghi tên tiếng Việt qua SQL Server NVARCHAR.
- Gateway phải xóa header ngữ cảnh giả, kiểm tra token sai/hết hạn và quyền từng endpoint.
- Postman kiểm thử qua port 9000; kết quả thực chạy được ghi trong README và báo cáo kiểm thử.
