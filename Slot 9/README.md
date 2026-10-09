# Slot 9 — Part 5: API Documentation với Swagger

Dự án nằm trong `ShoppingServices/`, copy từ Slot 7 (đã có Part 1–4).
Product Service được đưa ra một cấp thư mục để cả bốn service có cấu trúc giống nhau.
Hai tài liệu yêu cầu: `Part5_swaggerdocs.md` và `Part5_guide.md`.

## Các TODO đã hoàn thành

| TODO | Service | Kết quả |
|---|---|---|
| DOC-1 → DOC-4 | Product | Dependencies, đường dẫn Swagger, OpenAPI metadata, CORS |
| DOC-5 → DOC-8 | Inventory | Dependencies, đường dẫn Swagger, OpenAPI metadata, CORS |
| DOC-9 → DOC-12 | Order | Dependencies, đường dẫn Swagger, OpenAPI metadata, CORS |
| DOC-13 | Gateway | Springdoc Web MVC UI và API |
| DOC-14 | Gateway | Dropdown Product / Order / Inventory |
| DOC-15 | Gateway | Ba route aggregate, rewrite sang `/api-docs` |
| DOC-16 | Gateway | Swagger công khai, CORS, API nghiệp vụ vẫn yêu cầu JWT |

Mỗi DOC có một commit riêng với quy ước `feat(scope): DOC-N ...`.
Các commit bổ sung dùng `fix(build)`, `feat(infra)`, `test(docs)` và `docs`.

## Điều chỉnh để chạy với dự án hiện có

- Package Java giữ nguyên `com.fudn.product_service`, `inventory_service`,
  `order_service` để Spring quét được các configuration.
- Product vẫn dùng endpoint `/api/products` (có `s`), gồm GET, POST, PUT, DELETE.
- Product: Spring Boot 3.5.14, Java 21, Springdoc 2.8.9.
- Inventory / Order: Spring Boot 4.1.1; Gateway: Spring Boot 4.1.0;
  dùng Springdoc 3.1.1. Springdoc 2.5.0 trong mẫu dành cho Boot 3.2,
  không phù hợp các phiên bản Boot hiện có. Tham khảo
  [bảng tương thích Springdoc](https://springdoc.org/v2/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot).
- Gateway dùng `BeforeFilterFunctions.setPath` theo API Spring Cloud hiện có
  và giữ URL đích trong properties để có thể thay đổi khi test.
- Giữ CSRF disabled và stateless JWT của Part 4. CORS cho phép thêm PUT,
  DELETE, OPTIONS để thử đầy đủ CRUD Product.
- OpenAPI có security scheme `bearerAuth`, nút **Authorize** và server Gateway
  mặc định để Try it out đi qua bảo mật Part 4.
- MySQL của Slot 9 dùng cổng **3339**, tránh MySQL khác đang chiếm 3306.
  Có thể đổi datasource qua `ORDER_DATABASE_URL` và `INVENTORY_DATABASE_URL`.
- Compose chung không khởi động Mongo Express ở cổng 8081 vì đây là cổng Order.
- File test properties của Order có cấu hình Springdoc, tránh che mất `/api-docs`.
  Gradle của Order cũng được đồng bộ dependencies; Maven là cách build được kiểm thử.

## Khởi động

Cần JDK 21, Docker Desktop đang chạy. Tại `Slot 9/ShoppingServices`:

```powershell
docker compose up -d
docker compose ps
```

Chạy từng lệnh dưới đây trong **bốn terminal riêng**, từ `ShoppingServices`:

```powershell
cd inventory-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd product-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd order-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd api-gateway
.\mvnw.cmd spring-boot:run
```

Nếu cổng 8080 đang được dùng, chạy Product với
`'-Dspring-boot.run.arguments=--server.port=8083'` và Gateway với
`'-Dspring-boot.run.arguments=--services.product.url=http://localhost:8083'`.
Swagger qua Gateway vẫn hoạt động; server trực tiếp của Product trong docs
đang khai báo cổng mặc định 8080.

| URL | Kết quả |
|---|---|
| http://localhost:8080/swagger-ui.html | Product Swagger UI |
| http://localhost:8081/swagger-ui.html | Order Swagger UI |
| http://localhost:8082/swagger-ui.html | Inventory Swagger UI |
| http://localhost:9000/swagger-ui.html | Swagger UI tổng hợp, không cần token |
| http://localhost:9000/aggregate/product-service/v3/api-docs | Product OpenAPI JSON |
| http://localhost:9000/aggregate/order-service/v3/api-docs | Order OpenAPI JSON |
| http://localhost:9000/aggregate/inventory-service/v3/api-docs | Inventory OpenAPI JSON |
| http://localhost:9000/api/products | 401 nếu không có JWT |

Mỗi service expose JSON tại `/api-docs`.
Keycloak Admin: `http://localhost:8181`, tài khoản dev `admin` / `admin`.
Realm/client được import tự động từ cấu hình Part 4.

## Thử API qua Swagger

Lấy JWT từ Keycloak:

```powershell
$token = Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:8181/realms/spring-microservices-realm/protocol/openid-connect/token' `
  -Body @{grant_type='client_credentials'; client_id='spring-microservices-client'; client_secret='mss301-dev-secret-change-me'}
$token.access_token
```

Mở Swagger của Gateway → chọn service → **Authorize** → dán access token
(không thêm `Bearer`) → chọn server `http://localhost:9000` → **Try it out**.
Inventory có dữ liệu `iphone_15` từ Flyway; có thể kiểm tra tồn kho rồi tạo order.

## Kiểm thử

Tại `ShoppingServices`:

```powershell
# Swagger và tất cả test Gateway; không cần database/Keycloak thật
.\verify.ps1 -SwaggerOnly

# Toàn bộ Part 1–5; Docker cần chạy để Testcontainers tạo MongoDB/MySQL
.\verify.ps1
```

SwaggerIntegrationTest kiểm tra UI/static assets, title/version, endpoint,
bearer scheme, server Gateway và CORS. SwaggerSecurityTest kiểm tra cả ba
aggregate route thực sự forward tới WireMock `/api-docs`, dropdown,
truy cập docs không cần token, preflight và API nghiệp vụ trả 401.

Các test Part 1–4 được giữ lại. Chi tiết kết quả nằm trong
`ShoppingServices/<service>/target/surefire-reports/` sau khi chạy.
Không commit build output, dữ liệu Docker hoặc logs.

## Kết quả xác minh ngày 09/10/2026

| Service | Tests | Failures / Errors / Skipped |
|---|---:|---|
| Product | 16 | 0 / 0 / 0 |
| Inventory | 5 | 0 / 0 / 0 |
| Order | 6 | 0 / 0 / 0 |
| Gateway | 13 | 0 / 0 / 0 |
| **Tổng** | **40** | **0 / 0 / 0** |

Đã kiểm tra HTTP trên hệ thống thật với Product ở 8083, Order 8081,
Inventory 8082, Gateway 9000, Keycloak 8181:

- Swagger UI của cả bốn service trả HTML thành công.
- Swagger config có đúng dropdown ba service; cả ba aggregate URL trả
  JSON với title/version, endpoint, server và bearer scheme đúng.
- API Product, Inventory và Order không có token đều trả 401.
- Keycloak cấp token bằng client credentials; JWT thật gọi được Product
  và Inventory qua Gateway (200), kiểm tra `iphone_15`, quantity 1 trả `true`.
- `docker compose config --quiet` và `git diff --check -- 'Slot 9'` pass.

Gateway được giữ chạy tại `http://localhost:9000/swagger-ui.html` sau khi
kiểm tra. Dừng các terminal Spring Boot bằng Ctrl+C và dừng infrastructure
bằng `docker compose stop` tại `ShoppingServices` khi không cần sử dụng.
