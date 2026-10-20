# DANASEA DEVELOPMENT data

Gói độc lập để cài dữ liệu DEMO trên PostgreSQL DEVELOPMENT của thành viên nhóm. Chứa đủ binary và metadata, không cần `Data_Chuan_Hoa`, `DANASEA_DEMO_DATA_PACKAGE` hoặc thư mục báo cáo trên máy người tạo. Chưa commit/push; xem [PUBLIC_RELEASE_REVIEW.md](PUBLIC_RELEASE_REVIEW.md) trước khi đưa lên repository public.

| Dữ liệu | Số lượng |
|---|---:|
| User | 12: 1 ADMIN, 9 VENDOR, 2 CUSTOMER |
| Vendor | 9, giữ nguyên thông tin ngân hàng STRING đã duyệt |
| Category | 12: 5 Category nền có Service sử dụng và 7 Category mới |
| Service / Option / Slot | 24 / 33 / 24 |
| ServiceImage | 72 liên kết, dùng 35 file ảnh |
| VendorDocument | 18 file/bản ghi PENDING, hai loại cho mỗi Vendor |
| ServiceSafetyDocument | 24 file checklist SAMPLE, mỗi Service một bản ghi PENDING |
| File media | 77: 35 ảnh + 18 VendorDocument + 24 checklist |

Tổng cộng 228 row fixture trong chín bảng. UUID, tên, giá, trạng thái, Category mapping và quan hệ kế thừa dữ liệu đã nghiệm thu; không tự publish/approve hoặc bật Category/Option. Slot giữ ngày và sức chứa fixture ban đầu, không cam kết có thể booking ở ngày chạy lại.

## Chuẩn bị

Python 3.10+, PostgreSQL 17, `psql` và `pg_restore`; hoặc container PostgreSQL đã có hai công cụ đó. Nhóm chuẩn bị **schema public DANASEA ở Flyway V24 hoặc V37** trước khi cài. Installer không tạo database/container, không chạy migration. Không chạy V38 để nạp bộ dữ liệu này.

Chỉ cài database DEVELOPMENT riêng có tên bắt đầu `danasea_demo_`, trên localhost hoặc container riêng. Installer chủ động từ chối database `danasea`, container `pbl6_httt-postgres-1` và mount volume chính `pbl6_httt_postgres_data`. Backup DB đích trước khi cài; không dùng DB production/dùng chung.

```powershell
python -m pip install -r installer/requirements.txt
python -B installer/install_demo_data.py --validate-only
python -B -m unittest discover -s tests -p test_package.py -v
```

## Dry-run rồi cài đặt

```powershell
python -B installer/install_demo_data.py --dry-run --development --database danasea_demo_member --docker-container YOUR_PRIVATE_POSTGRES --reuse-storage
python -B installer/install_demo_data.py --apply --development --database danasea_demo_member --docker-container YOUR_PRIVATE_POSTGRES --reuse-storage
```

Nếu dùng PostgreSQL native, thay `--docker-container ...` bằng `--host 127.0.0.1 --port YOUR_PORT --user YOUR_DB_USER`. Mật khẩu PostgreSQL và mật khẩu DEMO được nhập ẩn, không nằm trong command line hoặc file. Mật khẩu DEMO do nhóm tự nhập khi cài; loader sinh BCrypt tại runtime cho tài khoản mới, không mang password hash từ DB nguồn. Tài khoản đã có phải khớp mật khẩu được nhập, loader không tự đổi hash.

12 email: `admin_demo@danasea.test`, `vender_1@danasea.test` đến `vender_9@danasea.test`, `customer_1@danasea.test`, `customer_2@danasea.test`. Activation là **DEV_FIXTURE_ACTIVATION** cho đúng fixture `.test`, không tuyên bố xác minh email/OTP ngoài đời và không tắt verification toàn hệ thống.

`--reuse-storage` tải bytes URL thật và kiểm SHA256 trước khi ghi. Để dùng Cloudinary riêng, chọn `--upload-media` và cung cấp Cloudinary cấu hình qua prompt ẩn hoặc biến môi trường riêng. File media luôn có sẵn trong gói; không có URL giả hoặc đường dẫn Windows dùng làm URL web. Kết quả storage/installation nằm trong `runtime/`, được gitignore và không thuộc manifest.

Installer kế thừa bộ cài cũ: khóa chín bảng trong một transaction, kiểm UUID/natural key/type/nullable/độ dài, quan hệ, hash media, INSERT các UUID thiếu, SELECT đối chiếu rồi COMMIT; SELECT độc lập sau COMMIT. Dry-run ROLLBACK. Chạy lại cùng dữ liệu không tạo bản ghi trùng. Dữ liệu hiện có khác fixture hoặc password khác thì dừng; không UPDATE/DELETE/upsert, không dùng ON CONFLICT để che collision. `created_at/updated_at` do PostgreSQL điền nếu fixture để trống; generated search_vector để PostgreSQL xử lý.

## Kiểm thử API thật

Cấu hình Backend trỏ vào DB riêng đã cài. Bản build mới cần schema V37; artifact cũ V24 chỉ dùng với schema tương thích. Chạy:

```powershell
python -B tests/api_smoke_test.py --base-url http://127.0.0.1:8080
```

Nếu Backend nằm trong container và chỉ bind port nội bộ, dùng thêm `--api-container YOUR_BACKEND_CONTAINER`, địa chỉ base là địa chỉ nhìn từ container. Nhập ẩn cùng mật khẩu DEMO khi cài. Suite chỉ login và GET: 12 User, 9 Vendor/profile, 24 Service, 33 Option, 24 Slot, 72 ảnh, 18 VendorDocument, Admin GET 24 ServiceSafetyDocument, kiểm quyền đọc chéo và GET/SHA256 114 liên kết media. Không tạo Booking, Payment, Invoice, không duyệt tài liệu hoặc thay trạng thái dữ liệu. JSON kết quả không lưu response token, cookie, password/hash hoặc thông tin ngân hàng.

Backend hiện có Vendor POST upload ServiceSafetyDocument và Admin GET/approve/reject; chưa có Vendor GET riêng cho bảng này, nên suite đọc ServiceSafetyDocument bằng tài khoản Admin theo endpoint thật. Vendor vẫn đọc hồ sơ tài liệu Vendor qua `/api/vendor/documents`.

## Ý nghĩa tài liệu và fixture

ServiceSafetyDocument không có `document_type`; chỉ có UUID Service, file URL, status và thông tin review. Điều kiện yêu cầu tài liệu là `weather_sensitive=true` **hoặc** Category `requires_safety_cert=true`. Cả 24 Service hiện thỏa điều kiện; gói chuẩn bị đúng một checklist riêng mỗi Service. Các checklist ghi rõ SAMPLE DEVELOPMENT/không phải chứng nhận; không có dấu, chữ ký, tổ chức cấp, ngưỡng vận hành hoặc kết quả kiểm định giả. Chúng chỉ đủ để kiểm thử upload/quan hệ/trạng thái PENDING, **không đủ để phê duyệt an toàn hoặc hợp pháp hóa Service**. VendorDocument và ServiceSafetyDocument là hai quan hệ độc lập.

Ngân hàng: giữ đúng chín mapping user cung cấp, `DEVELOPMENT_SYNTHETIC`, account number STRING bảo toàn số 0 đầu, holder DANASEA DEMO VENDOR 01-09. Không xác nhận Vendor thật sở hữu tài khoản, không chuyển tiền hoặc dùng như thông tin thanh toán đã xác minh.

Manifest kiểm toàn vẹn dữ liệu, media, provenance, installer và tests. Sau thay đổi có chủ đích, maintainer chạy `python -B tools/update_manifest.py`; installer không tự cập nhật manifest để bỏ qua file sai. Không sửa dữ liệu hoặc media chỉ để làm test PASS.

Storage hiện tại trả HTTP 200 theo URL Cloudinary mà không cần token; đây là các file SAMPLE/ảnh demo, **không phải storage tài liệu riêng tư**. API metadata và upload vẫn được kiểm quyền theo role/ownership. Đường dẫn nguồn trong provenance chỉ là tham chiếu khảo sát lịch sử; loader không mở những thư mục đó. Xem [TEST_RESULTS.md](TEST_RESULTS.md) và [FILES_CREATED.txt](FILES_CREATED.txt) cho kết quả thực thi và danh sách file.
