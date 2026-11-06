# Kết quả kiểm tra Assignment2 đến TODO5

Ngày chạy **06/11/2026**, Asia/Bangkok. Sinh viên DE190468 — Lê Ngọc Minh Kiên.

| Kiểm tra | Kết quả thực tế |
|---|---|
| `mvn clean verify` cả bốn project | BUILD SUCCESS, Java 21, Boot 4.1.1 |
| JUnit/Mockito MSAccount | 19 test pass: JWT 8, AuthService 11 |
| JUnit/Mockito MSBrand | 11 test pass |
| Postman collection chạy bằng Newman | 29 requests, 77 assertions, 0 failure |
| Khởi động bốn ứng dụng | Đúng cổng 8081/8082/8083/8080 |
| Swagger trực tiếp ba MVC service | HTTP 200 |
| Code-first SQL metadata | StoreAccounts, WatchBrands, BrandWatches; AccountID/WatchID/BrandID đúng tên |
| Account seed | 5 dòng Password bắt đầu `$2a$`, active/role đúng đề |
| Brand seed | 10 liên kết; count Rolex2/Omega2/Seiko2/Casio3/TAG Heuer1 |
| Concurrent assignment | 8 PUT lần đầu và 6 PUT chuyển brand đều thành công, chỉ 1 dòng WatchID99 |
| Restart MSBrand sau khi gỡ một link seed | Link không bị tạo lại; test phục hồi về đủ 10 link |
| Fat-jar cả bốn project | Không chứa lombok jar |

Chi tiết runtime/SQL và thống kê Newman nằm ở `todo5-runtime.json`. Kết quả unit
test tổng hợp ở `unit-summary.json`. Các file không chứa token/mật khẩu thật.
Log runtime và Newman report đầy đủ chỉ giữ cục bộ trong `Assignment2/.work/`.

## Những trường hợp đã kiểm tra

- Login admin/staff/member; inactive 403 trước password, sai password/không có
  account 401; thiếu username/blank password 400.
- Token HS256 có sub, role, type=access và thời hạn 900 giây; refresh có
  type=refresh, thời hạn 604800 giây. Kiểm tra sai chữ ký, hết hạn, malformed,
  secret quá ngắn, thời hạn không hợp lệ, jti khác nhau ở unit test.
- Refresh hợp lệ tạo cặp mới; access token không dùng refresh được; refresh
  thiếu/malformed bị 401; tài khoản inactive/mất/subject sai bị từ chối.
- Brand list/detail/watch IDs/count đúng seed; ID999 trả 404 với message đúng;
  input ID không dương trả 400.
- PUT watch99 hai lần cùng brand chỉ còn 1 dòng; chuyển brand giữ dòng cũ;
  PUT brand999 không làm mất link hiện có; DELETE lần1 UNASSIGNED, lần2
  NOTHING_TO_DO đều HTTP200. Request đồng thời vẫn một dòng theo UNIQUE WatchID.
- Console có `[SAGA-PARTICIPANT] ASSIGN` và `UNASSIGN`.

## Phát hiện và sửa qua kiểm thử

SQL Server/JDBC có thể chưa bắt đầu transaction vật lý khi `sp_getapplock`
được gọi trước truy vấn JPA đầu tiên. Nhánh unassign cần xử lý cả trường hợp
không có row. WatchAssignmentLock yêu cầu transaction MANDATORY, thực hiện
truy vấn bảng rỗng để bắt đầu transaction rồi mới lấy application lock. Sau
sửa, toàn bộ request lặp và request đồng thời đã được chạy lại, không còn 500.

## Phạm vi chưa kiểm tra vì chưa được giao triển khai

DBWatch vẫn chưa có bảng/entity ở mốc TODO5. Orchestrator CRUD, compensation
đa service, log Saga, gateway auth/routing, fault injection và Docker thuộc
TODO6–10, chưa có và không được ghi nhận là pass. Gateway 8080 chỉ được kiểm
tra khởi động, không có route `/internal/**`.

Giảng viên duyệt tài liệu thiết kế là checklist học vụ chưa có bằng chứng duyệt.
Sinh viên cần trình `docs/SAGA_DESIGN.md` theo yêu cầu đề bài.

## Tái chạy

Sau khi tạo ba DB và nạp biến môi trường theo README:

```powershell
mvn clean verify
python scripts/verify_todo5.py
```

Chạy collection bằng Postman GUI ở 8081/8082 khi các service đã mở; hoặc truyền
`--newman-js <duong-dan-newman/bin/newman.js>` cho script Python để chạy tự động.
Script kiểm chứng SQL metadata bằng `sqlcmd -E`, yêu cầu tài khoản Windows
đọc được các DB bài này. Test dùng watch99 và tạm gỡ/phục hồi watch1; chỉ dùng
với database bài tập không có dữ liệu nghiệp vụ của các ID test đó.

## Build ? t?ng m?c commit

?? duy?t 35 commit kh?ng ph?i merge trong Git ri?ng WatchManagement; m?i m?c
thay ??i Java/POM/resources ??u ch?y l?i Maven package th?nh c?ng trong
worktree t?m. C?c m?c ch? c? docs/config kh?ng c?n build l?i.
`commit-build-audit.json` ghi hash c?a Git ri?ng, subject, project ???c build
v? k?t qu?; thay ??i t??ng ?ng ?? ???c mirror v?o l?ch s? Slot18 c?a repo ngo?i.
