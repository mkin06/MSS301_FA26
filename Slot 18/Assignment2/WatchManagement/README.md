# Assignment2 — Watch Management — đến TODO5

**DE190468 — Lê Ngọc Minh Kiên**. Java 21, Maven, Spring Boot **4.1.1**,
Spring Cloud **2025.1.3**, springdoc **3.0.2**, jjwt **0.13.0**, SQL Server.

## Phạm vi đã làm

| Bước | Nội dung | Kết quả hiện tại |
|---|---|---|
| TODO0 | Git init, template, ignore và env example | Git riêng trong WatchManagement; source cũng được track trong repo ngoài |
| TODO1 | Bốn project bằng Spring Initializr | Maven, Java 21, com.mss301, jar; Lombok annotation processing và loại khỏi fat-jar |
| TODO2 | Database per Service và datasource | DBAccount, DBBrand, DBWatch; code-first `ddl-auto: update`; tên bảng/cột giữ nguyên |
| TODO3 | MSAccount | Seed BCrypt; login, refresh, logout; validation và ApiResponse |
| TODO4 | Thiết kế Saga | [SAGA_DESIGN.md](docs/SAGA_DESIGN.md): bảng bước, compensation, pivot, state machine và giải thích local tx/2PC |
| TODO5 | MSBrand participant | Gán/gỡ idempotent, public brand API, seed 5 brand/10 liên kết |

MSWatch và Gateway mới khởi tạo/cấu hình ở TODO1–2. Chưa triển khai CRUD watch,
orchestrator, SagaStates/SagaLogs, fault injection, gateway JWT/routing hay
Docker (TODO6–10). DBWatch còn trống là đúng mốc TODO5; bảng `Watches` chỉ có
ở TODO6.16. Kiểm thử ở giai đoạn này gọi **trực tiếp 8081/8082**, không qua 8080.
JWT access và refresh đã được kiểm tra chữ ký/payload cục bộ; không gửi token
hoặc secret lên jwt.io hay dịch vụ bên ngoài.

Spring Initializr hiện chỉ cung cấp các patch mới hơn nên đã generate bản
4.1.2 rồi pin parent về **4.1.1** đúng yêu cầu. Thông số/generator URL được
lưu trong [INITIALIZR.json](docs/INITIALIZR.json).

## Tạo database và môi trường

Chạy trên SQL Server có sẵn bằng SSMS hoặc từ thư mục WatchManagement:

```powershell
sqlcmd -S localhost -E -C -b -i .\database\create-databases.sql
Copy-Item .env.example .env
```

Điền `.env` với DB_PASSWORD thực tế và JWT_SECRET ngẫu nhiên ít nhất 32 byte.
DB_USERNAME mặc định `sa`. Nếu tài khoản này không dùng được, đặt tài khoản
SQL riêng có quyền tạo/cập nhật schema, đọc/ghi trên ba database. Script SQL
không sửa mật khẩu, không xoá database hoặc login đang có. `sa` trên máy kiểm
thử đã bật, vì vậy không cần đổi cấu hình SQL Server.

Nạp các giá trị cho terminal hiện tại:

```powershell
.\scripts\Import-Env.ps1
mvn clean verify
```

`.env` không được Spring Boot tự động đọc; chạy Import-Env ở mỗi terminal
hoặc khai báo biến môi trường trong IntelliJ Run Configuration. Không commit
`.env`, secret JWT, DB password hoặc response có token thật. `.env.example`
chỉ chứa placeholder. DB_HOST mặc định localhost, DB_PORT 1433.

**Assignment2 này dùng code-first `ddl-auto: update` theo đề**, không dùng
`none` của bài Slot16. Hibernate giữ chính xác StoreAccounts, AccountID,
WatchBrands, BrandWatches, WatchID, BrandID nhờ PhysicalNamingStrategyStandardImpl.
Không cấu hình dialect thủ công.

## Chạy bằng IntelliJ hoặc terminal

Mở `pom.xml` ở WatchManagement để import bốn module hoặc mở từng project riêng.
Bật annotation processing nếu IDE chưa nhận Lombok.

| Project | Main class | Port |
|---|---|---:|
| MSAccount_DE190468 | com.mss301.account.MSAccountApplication | 8081 |
| MSBrand_DE190468 | com.mss301.brand.MSBrandApplication | 8082 |
| MSWatch_DE190468 | com.mss301.watch.MSWatchApplication | 8083 |
| APIGateway_DE190468 | com.mss301.gateway.GatewayApplication | 8080 |

Sau khi build, trong bốn terminal đã nạp env, chạy lần lượt:

```powershell
java -jar .\MSAccount_DE190468\target\ms-account-1.0.0.jar
java -jar .\MSBrand_DE190468\target\ms-brand-1.0.0.jar
java -jar .\MSWatch_DE190468\target\ms-watch-1.0.0.jar
java -jar .\APIGateway_DE190468\target\api-gateway-1.0.0.jar
```

Mỗi lệnh chạy trong terminal riêng. Swagger trực tiếp ở
`http://localhost:8081/swagger-ui.html`, 8082 và 8083. Gateway chưa có routes.

## MSAccount — TODO3

Seed mới có đúng 5 tài khoản, mật khẩu `@1` được lưu BCrypt:

| Username | Email | Role | Active |
|---|---|---:|---|
| adminsys | admin@drawingpen.com.vn | 1 | true |
| staff02 | staff02@drawingpen.com.vn | 2 | true |
| member01 | member01@drawingpen.com.vn | 3 | true |
| member02 | member02@drawingpen.com.vn | 3 | false |
| staff01 | staff01@drawingpen.com.vn | 2 | false |

- POST `/api/auth/login` body `{"username":"adminsys","password":"@1"}`:
  200, ApiResponse.success=true, data chứa accessToken, refreshToken, tokenType,
  role và username; không chứa password.
- Không tồn tại/sai mật khẩu: 401. Inactive: 403 trước khi kiểm tra mật khẩu.
  Thiếu/blank input: 400.
- Access token HS256 chứa sub=AccountID, role, type=access, iat/exp (15 phút).
  Refresh có type=refresh, hạn 7 ngày. JWT_SECRET phải được cung cấp, không có
  secret mặc định trong source. Mỗi token có jti riêng.
- POST `/api/auth/refresh` body `{"refreshToken":"..."}`: phát cặp mới;
  access token/malformed/expired → 401. Account bị vô hiệu hóa → 403.
- POST `/api/auth/logout`: client xóa token. Đây là logout stateless theo đề,
  không có blacklist/revoke token phía server.

## MSBrand — TODO5

Seed khớp đề: Rolex 2, Omega 2, Seiko 2, Casio 3, TAG Heuer 1 đồng hồ.
WatchID → BrandID: 1→1, 2→2, 3→3, 4→4, 5→5, 6→2, 7→3, 8→1, 9→4, 10→4.
Seed chỉ chạy với DBBrand mới; restart không tái tạo link đã bị gỡ.

| Endpoint trực tiếp 8082 | Hành vi |
|---|---|
| GET /api/brands | Danh sách tăng dần BrandID, có watchCount |
| GET /api/brands/{id} | Chi tiết brand và watchCount; không có → 404 |
| GET /api/brands/{id}/watches | brandId, brandName, watchCount, watchIds tăng dần |
| GET /api/brands/{id}/exists | Boolean phục vụ quan sát; không thay thế bước ghi Saga |
| PUT /internal/brands/{brandId}/watches/{watchId} | Upsert trong local tx; brand không có → 404; ASSIGNED |
| DELETE /internal/brands/watches/{watchId} | UNASSIGNED hoặc NOTHING_TO_DO; lặp đều 200 |

BrandWatches.WatchID có UNIQUE. Transaction-owned SQL `sp_getapplock` theo
WatchID tuần tự hóa cả trường hợp chưa có row, bảo đảm request đồng thời không
race INSERT. Không giữ khóa xuyên service, không dùng distributed transaction.
Gán lại cùng brand không cập nhật timestamp; chuyển brand sửa chính dòng cũ.
Lỗi 404 có message đúng `Brand not found with id: 999` và success=false.
Console có `[SAGA-PARTICIPANT] ASSIGN`/`UNASSIGN`.

## Test trực tiếp đến TODO5

Import `postman/Assignment2_TODO5.postman_collection.json` và
`postman/Local.postman_environment.json` vào Postman. Chọn environment Local,
chạy toàn bộ collection từ đầu; request tự lưu access/refresh token vào
collection variables, không cần copy thủ công. Xoá token variables hoặc clear
environment khi dùng xong. Test dùng WatchID 99 rồi gỡ lại; chỉ chạy trên dữ
liệu seed của bài, không dùng khi ID 99 là đồng hồ nghiệp vụ thật.

```powershell
mvn test
python .\scripts\verify_todo5.py
```

Python dùng các biến môi trường đã nạp, tự khởi động/dừng bốn JAR trên đúng cổng;
chỉ chạy khi đã tắt các service đang chạy thủ công. `sqlcmd -E` cần quyền đọc
metadata và bảng của ba database để kiểm chứng. Có thể truyền `--newman-js`
trỏ tới `newman/bin/newman.js` nếu muốn chạy collection cùng lượt kiểm tra.
Không cần Newman cho Postman GUI.

Xem [phân tích TODO](docs/REQUIREMENTS_TODO5.md) và
[kết quả kiểm tra](test-results/README.md). Thiết kế cần sinh viên trình giảng
viên theo checklist TODO4; không có tuyên bố giảng viên đã duyệt.

## Git và phạm vi push

Repo ngoài ở `D:\F\FALL26\MSS301`, nhánh nộp **Slot18**. Git riêng được init
trong WatchManagement, commit template `.gitmessage`, main và các nhánh
feature/scaffold, feature/ms-account, docs/saga-design,
feature/ms-brand-participant. Commit có footer `Refs: TODO-x.y`.

Source được track bằng file thường trong repo ngoài, không phải gitlink/submodule;
clone Slot18 có đủ cả bốn project. Thư mục Git bên trong chỉ là metadata cục bộ,
không commit `.git/`. Sau khi clone mới, nếu cần Git riêng, chạy `git init -b main`
ở WatchManagement và `git config commit.template .gitmessage`.
Author/committer dùng duy nhất danh tính sinh viên, không có co-author.
