# Kết quả thực thi — 20/10/2026

| Kiểm tra | Kết quả thật |
|---|---|
| Offline contract / negative tests | 9/9 PASS; chặn thiếu safety doc, orphan, UUID trùng, giả lập approval, sai target main; bảo toàn số 0 đầu ngân hàng và URL đã nghiệm thu khi chạy lại generator |
| Full package | 228 row trong chín bảng; 77 binary, mọi SHA256 và mapping hợp lệ |
| DB nguồn so với dữ liệu cũ | Mọi field của tám bảng đã nghiệm thu khớp; không sửa/import lại |
| Backup trước khi thêm tài liệu | pg_dump/pg_restore thực sự exit0; restore trên clone, fingerprint dữ liệu khớp |
| SQL dry-run | Thử cài 228 row trên DB trống V37, ROLLBACK; kết nối độc lập xác nhận không còn row |
| Cài mới | COMMIT_SELECT_PASS — 228/228 row, SELECT độc lập khớp |
| Tính độc lập / cài lần hai | Chạy từ bản sao trong TEMP không chứa runtime/cache hoặc thư mục nguồn; INSERT0 row, 228 row có sẵn khớp |
| Backend source mới trên clone V37 | API PASS: 9 Vendor + 1 Admin + 2 Customer login; đủ24 Service,33 Option,24 Slot,72 ảnh,18 VendorDocument,24 ServiceSafetyDocument |
| API upload thật trên clone | Owner201 PENDING; Vendor khác403; tải file/checksum khớp. Chỉ row probe mới được dọn, 24 fixture và dữ liệu clone gốc giữ nguyên |
| Thêm vào DB chính | INSERT24 ServiceSafetyDocument SAMPLE PENDING; dry-run/ROLLBACK, INSERT RETURNING, COMMIT và SELECT độc lập PASS |
| API chính sau bổ sung | 12/12 login PASS; 9 Vendor/profile,24 Service,33 Option,24 Slot,72 ảnh,18 VendorDocument,24 checklist đúng quan hệ |
| Storage | 114/114 liên kết đọc HTTP200 và SHA256 khớp; dùng77 file riêng biệt |
| Backup sau bổ sung | pg_dump/pg_restore exit0; restore đủ24 checklist cùng các bảng nghiệp vụ, fingerprint khớp |
| Bảo toàn dữ liệu chính | Chỉ thêm24 row PENDING; tám bảng cũ và Flyway giữ nguyên. Main V24, clone V37, không V38 |
| Cleanup | JVM test và container tạm đã dừng; volume/backup giữ; backend chính không restart |
| Secrets | Không token, credential, plaintext password hoặc password hash DB nguồn trong package |

Đã build source hiện tại bằng Maven clean/package, tests skipped; **không chạy hoặc tuyên bố PASS toàn bộ Maven suite**. Artifact cũ trong target gặp ClassFormatError, đã thay bằng output biên dịch sạch để thực hiện vòng API source mới; không sửa source Backend để làm xanh test.

Evidence chạy thật đã lược bỏ dữ liệu nhạy cảm ở `tests/evidence/`. Chưa duyệt giấy tờ, chưa thay workflow publish/Category/Option và chưa thử Booking/Payment/Invoice trong nhiệm vụ này. Các file checklist không chứng minh an toàn hoặc tính hợp pháp.

Gói đầy đủ trên máy; công khai GitHub vẫn cần xử lý quyền chia sẻ35 ảnh và Leader duyệt nhóm chứng từ SAMPLE theo `PUBLIC_RELEASE_REVIEW.md`. Chưa commit/push.
