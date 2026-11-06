# Phân tích và đối chiếu yêu cầu đến TODO5

Nguồn: `../Assignment_Readme.md`, `../Assignment_Solution.md` (ở thư mục
Assignment2) và Word `MMS301_Assignment 02_Watch Management_Saga Pattern.docx`.
Người thực hiện: DE190468 — Lê Ngọc Minh Kiên.

## Quyết định phạm vi

Yêu cầu mới nhất là đến **hết Bước 5/TODO-5.6**, gồm cả Bước 0 và Bước 1 tạo
project. Do đó các entity Watch, WatchCategory, SagaState/SagaLog, orchestrator,
Feign client và logic gateway chưa được tạo. Không dùng kết quả TODO0–5 để
tuyên bố đã hoàn tất Definition of Done cho toàn Assignment2.

`Assignment_Readme.md` quy định code-first `ddl-auto: update`; Word gốc cũng
yêu cầu Hibernate tạo schema. Quy định `none` của Slot16 không áp dụng vào bài
này. DBAccount và DBBrand có bảng sau khi MSAccount/MSBrand chạy; DBWatch
được tạo nhưng chưa có bảng đến khi thực hiện TODO6.

## TODO0 — Git

| Mã | Yêu cầu | Đáp ứng |
|---|---|---|
| 0.1 | git init tại WatchManagement, commit template | Repo riêng main; `git config commit.template .gitmessage` |
| 0.2 | .gitignore, .env.example, không commit secret/build/IDE | Tạo ở Assignment2 rồi copy vào WatchManagement; env dùng placeholder |
| 0.3 | Commit khởi tạo; nhánh theo từng bước | main, feature/scaffold, feature/ms-account, docs/saga-design, feature/ms-brand-participant |

Repo cha vẫn là `D:\F\FALL26\MSS301`; nhánh remote **Slot18**. Source trong
WatchManagement được stage dưới dạng regular blob để không biến thành gitlink.
Metadata `.git/` bên trong không push. Mỗi commit chức năng có subject tiếng Anh,
imperative, không dấu chấm, <=72 ký tự và footer `Refs: TODO-x.y`.
Chỉ tên/email Git sinh viên được dùng cho author và committer, không co-author.

## TODO1 — Spring Initializr

| Mã | Đáp ứng |
|---|---|
| 1.1 | MSAccount_DE190468, MSBrand_DE190468, MSWatch_DE190468, APIGateway_DE190468; Maven, Java 21, parent 4.1.1, group com.mss301, jar |
| 1.2 | Ba MVC service: webmvc, data-jpa, validation, mssql-jdbc runtime, Lombok; MSAccount có Security |
| 1.3 | MSWatch/Gateway: Cloud BOM 2025.1.3; Watch có OpenFeign; Gateway có server-webflux và circuitbreaker-reactor-resilience4j |
| 1.4 | Properties jjwt.version=0.13.0 ở Account/Gateway; springdoc.version=3.0.2 ở ba MVC service |
| 1.5 | Lombok annotationProcessorPaths trong compiler plugin; loại khỏi fat-jar cả bốn project |

Initializr đã bỏ patch 4.1.1 khỏi danh sách lựa chọn. Các project được generate
thực tế qua API Initializr với 4.1.2, sau đó parent được pin về đúng 4.1.1;
Cloud BOM và các dependency thủ công cũng được pin đúng đề. Có Maven Wrapper
do Initializr sinh. `docs/INITIALIZR.json` lưu URL/tham số để truy vết.
Thêm root reactor để build cả bốn bằng một lệnh, không làm mất tính độc lập
của từng project. Gateway khởi động ở 8080 nhưng chưa có route nghiệp vụ.

## TODO2 — SQL Server

| Mã | Đáp ứng |
|---|---|
| 2.1 | Script tạo DBAccount/DBBrand/DBWatch nếu chưa tồn tại, giữ nguyên DB và login đang có |
| 2.2 | URL DB_HOST/DB_PORT, DB_USERNAME mặc định sa, DB_PASSWORD từ env; open-in-view=false; ddl-auto=update; PhysicalNamingStrategyStandardImpl |

Không khai báo dialect; Hibernate 7 tự nhận SQL Server. Tài khoản `sa` ở máy
kiểm thử đã enabled; không reset password của người dùng. Kiểm thử dùng login
riêng có quyền DDL/đọc/ghi giới hạn vào ba DB bài này. Credential chỉ nằm trong
`.env` và file `.work/local.json` được ignore.

## TODO3 — MSAccount

| Mã | Đáp ứng và kiểm chứng |
|---|---|
| 3.1 | StoreAccount map StoreAccounts với AccountID, Username(20), Email(100), Password(100), Role, IsActive; @Builder.Default true; findByUsername; seed đúng 5 account BCrypt |
| 3.2 | JwtService HS256, sub/role/type/iat/exp; access 900000ms, refresh 604800000ms; secret từ env, kiểm tra độ dài, không có secret mặc định |
| 3.3 | Login: tìm username → inactive 403 → BCrypt mismatch 401; refresh kiểm tra chữ ký/hạn/type rồi account tồn tại/active |
| 3.4 | AuthController login/refresh/logout; BCrypt và permitAll; LoginRequest validation; DTO không lộ password; lỗi ApiResponse |
| 3.5 | Collection Postman direct 8081; tự lưu token, kiểm tra claim/hạn ngay cục bộ; kết quả thực chạy trong test-results |

Seed theo Word: adminsys role1 active; staff02 role2 active; member01 role3
active; member02 role3 inactive; staff01 role2 inactive. Tất cả mật khẩu `@1`
chỉ được lưu BCrypt. Refresh dùng active status mới nhất, từ chối tài khoản đã
vô hiệu hóa. Logout là client discard theo solution; chưa có server-side revoke.

## TODO4 — Thiết kế trước implementation participant

| Mã | Đáp ứng |
|---|---|
| 4.1 | docs/SAGA_DESIGN.md được commit trước code MSBrand: 3 bảng bước/compensation, LIFO, idempotency, snapshot, pivot, state machine và luồng brand999 |
| 4.2 | Giải thích local @Transactional không bao phủ HTTP; lý do không chọn 2PC; tx log REQUIRES_NEW, retry3 và COMPENSATING khi hết lượt |

Giảng viên duyệt thiết kế là việc cần sinh viên trình trên lớp. Chưa có bằng
chứng phê duyệt; không đánh dấu đã duyệt. Việc triển khai source TODO5 theo
phạm vi người dùng giao không đồng nghĩa đã hoàn thành thủ tục học vụ đó.

## TODO5 — MSBrand participant

| Mã | Đáp ứng |
|---|---|
| 5.1 | WatchBrand → WatchBrands; BrandWatch → BrandWatches với WatchID unique; BrandRepository, BrandWatchRepository |
| 5.2 | assignWatch upsert theo WatchID, brand thiếu trả 404, không đổi timestamp khi gán lặp cùng brand; khóa SQL transaction theo WatchID để bảo vệ insert đồng thời |
| 5.3 | unassignWatch xóa link, trả UNASSIGNED; lặp khi không có link trả 200/NOTHING_TO_DO |
| 5.4 | InternalBrandController PUT/DELETE; ApiResponse và GlobalExceptionHandler; log ASSIGN/UNASSIGN |
| 5.5 | GET /api/brands, /{id}, /{id}/watches có count và ID tăng dần; /{id}/exists theo solution |
| 5.6 | Seed 5 brand và 10 link đúng ánh xạ WatchID/BrandID; restart không làm sống lại link đã gỡ |

WatchBrand dài BrandName 200 và CountryOfOrigin 50 theo Word. BrandWatch có
UNIQUE WatchID, không có FK xuyên sang DBWatch. Assign/unassign đều là local
transaction chỉ ghi DBBrand. `sp_getapplock` thuộc transaction, tự nhả khi
commit/rollback; không phải lock phân tán hay 2PC. Kết quả assign có thể trả
previousBrandId khác nhau giữa lần đầu/lần lặp nhưng trạng thái dữ liệu cuối
giống nhau, đúng ý nghĩa idempotency.

Seed mapping: 1→1, 2→2, 3→3, 4→4, 5→5, 6→2, 7→3, 8→1, 9→4, 10→4.
Count: Rolex2, Omega2, Seiko2, Casio3, TAG Heuer1. Không tự reset IDENTITY khi
database có dữ liệu. Khi làm TODO6 và cần reset phải reset đồng bộ DBWatch và
DBBrand theo đề, không xóa riêng một database để tránh lệch ID.

## Giới hạn hiện tại

Gateway JWT verification/role enforcement và việc chặn giả mạo X-User-* là
TODO7. Các cổng service đang phục vụ kiểm thử cục bộ. End-to-end Saga qua
gateway, fault injection, screenshot báo cáo Saga và Docker là TODO6–10;
không có kết quả kiểm thử giả cho phần chưa làm.
