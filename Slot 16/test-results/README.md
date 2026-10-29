# Kết quả kiểm thử Slot 16

Ngày thực hiện: **29/10/2026**, múi giờ Asia/Bangkok. Mã sinh viên DE190468.

| Kiểm tra | Kết quả |
|---|---:|
| Maven clean verify — sáu module, JDK 21 | BUILD SUCCESS |
| Unit test JUnit/Mockito/Bean Validation | 112 pass; 0 failure/error/skip |
| HTTP/OpenAPI/Swagger/CORS trên SQL Server thật | 164 pass |
| Newman Media | 38 requests, 150 assertions, 0 failure |
| Newman Hotel | 37 requests, 146 assertions, 0 failure |
| Khởi động/dừng bằng PowerShell | Cả Media và Hotel thành công, đúng cổng 8080/8081/8082 |

## Môi trường

- Database MSS301_2026_PE_MEDIA và MSS301_2026_PE được tạo mới bằng các SQL
  script của bài; seed được chạy trước test.
- Bốn service có `spring.jpa.hibernate.ddl-auto=none`; schema không được tạo
  bằng Hibernate. JDBC kết nối SQL Server cục bộ, dùng login test riêng có
  quyền đọc/ghi; không thay đổi tài khoản sa hiện có.
- `scripts/smoke.py` tự mở service ở 18081/18082 và gateway ở 18080, kiểm tra
  xong tự dừng process. Newman chạy qua gateway mặc định 8080 với hai service
  8081/8082, chạy lần lượt hai bộ đề. Các script Start/Stop được chạy thực tế.

## Nội dung đã kiểm tra

- Mandatory fields, độ dài, email, enum, số dương, phạm vi 1–10, decimal(18,2),
  từ chối số thập phân cho trường integer, JSON/date sai format và ngày không
  tồn tại, paging sai kiểu/âm/quá 100.
- HTTP/status: 201/1, 200/1, 406/2, 226/3, 404/4, 400/5, 500/0.
- Unique ở create/update, cập nhật cùng giá trị unique của chính bản ghi;
  unit test bổ sung trường hợp race vi phạm UNIQUE tại DB.
- Partial update giữ dữ liệu cũ; ngày/count sai không làm dirty entity hoặc
  thay đổi bản ghi SQL; tổng tiền dùng giá hiện tại và số ngày đúng công thức.
- OpenFeign thật: parent tồn tại/không tồn tại/không ACTIVE hoặc AVAILABLE;
  count vượt limit; đổi parentId; detail/list nhúng đầy đủ object từ service
  bên kia. Tắt parent service thì API trả 500/0 thay vì 404/4.
- Soft delete, GET lại bản ghi đã CANCELLED/DEACTIVATED/MAINTENANCE.
- Ngày SQL DATE trả đúng dd/MM/yyyy, không lệch múi giờ; unit test regression
  round-trip ở Asia/Bangkok, America/New_York và UTC.
- OpenAPI/Swagger trên cả sáu app; gateway proxy hai bộ docs; CORS preflight
  mọi host và response qua gateway không có header allow-origin trùng.

`http-results.json` ghi từng kiểm tra HTTP. `summary.json` tổng hợp JUnit và
Newman, cùng tên test, kết quả và thời gian chạy, không chứa mật khẩu.
Log và Newman report đầy đủ được giữ cục bộ dưới `.work/` (không commit).

Tái chạy: làm theo README của Slot 16, `mvn verify`, `python scripts/smoke.py`,
hoặc import Postman rồi chạy hai collection. Kiểm thử tạo thêm các bản ghi
test có prefix ngẫu nhiên, giữ nguyên dữ liệu đã có và không reset database.
