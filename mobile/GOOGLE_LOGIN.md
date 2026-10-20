# Google login — mobile và vendor_mobile

Chỉ sửa ứng dụng Flutter; không thay đổi backend.

## Luồng đã nối
- Bộ chọn tài khoản Google lấy ID token.
- POST /api/auth/oauth2/google với JSON {"idToken":"..."}.
- Lưu access token và refresh cookie qua AuthSession, tải GET /api/users/me.
- Mobile vào Home; vendor vào VendorAccessScreen để kiểm tra/tạo hồ sơ đối tác.
- Google đã xác minh email nên không đi qua OTP của ứng dụng.
- Hủy bộ chọn tài khoản không gửi request. Backend từ chối thì không lưu phiên đăng nhập.

## Chạy Android
Trong thư mục mobile hoặc vendor_mobile:

```powershell
flutter run --dart-define-from-file=config/google.local.json --dart-define=API_BASE_URL=http://10.0.2.2:8080
```

10.0.2.2 chỉ dành cho Android Emulator. Với điện thoại thật kết nối USB và backend local hiện tại, chạy:

```powershell
adb reverse tcp:8080 tcp:8080
flutter run --dart-define-from-file=config/google.local.json --dart-define=API_BASE_URL=http://127.0.0.1:8080
```

Chạy lại `adb reverse` sau khi kết nối lại điện thoại. Khi đổi `API_BASE_URL`, dừng và chạy lại app; hot reload không cập nhật cấu hình build này.
config/google.local.json đã được tạo local từ GOOGLE_CLIENT_ID hiện có, chỉ chứa Client ID công khai và được gitignore. Không chứa client secret.
Nếu clone sang máy khác, tạo file với nội dung:

```json
{"GOOGLE_SERVER_CLIENT_ID":"WEB_CLIENT_ID.apps.googleusercontent.com"}
```

## Cấu hình Google cần xác nhận
Trong cùng Google Cloud project với Web Client ID của backend, đăng ký OAuth client loại Android cho:
- mobile: com.example.mobile
- vendor_mobile: com.danasea.vendor_mobile

Mỗi client cần SHA-1 của keystore ký bản cài đang chạy. Lấy thông tin bằng `./gradlew.bat signingReport` trong thư mục android của từng app. Debug và release có thể dùng chữ ký khác nhau. Không cần tạo website; Web Client ID là audience của token backend kiểm tra.
Nếu consent screen ở chế độ Testing, tài khoản thử phải nằm trong Test users.

## iOS
mobile có dự án iOS nhưng chưa cấu hình Google URL scheme. Trước khi chạy iOS cần OAuth client iOS tương ứng bundle ID, GOOGLE_IOS_CLIENT_ID khi build và reversed client ID trong CFBundleURLTypes của Runner/Info.plist. Không điền giá trị giả. vendor_mobile hiện không có thư mục ios.

## Khác biệt giao diện / API
- Mobile: nút Google trước đây disabled, nay gọi đăng nhập; Facebook vẫn chưa hỗ trợ.
- Vendor: thêm nút Tiếp tục với Google. Tài khoản Google mới được backend tạo với vai trò CUSTOMER, không tự cấp quyền vendor. App đưa qua luồng hồ sơ đối tác hiện có.
- Vendor đăng ký bằng Google vẫn kiểm tra checkbox điều khoản trên tab đăng ký.

## Kiểm chứng
Đã bổ sung test HTTP cho payload ID token, refresh-cookie/session restore, xóa trạng thái OTP cũ, token bị từ chối và token rỗng. Các test dùng server local giả lập; không thay thế kiểm tra đăng nhập Google thật trên Android.

Tài liệu SDK: https://pub.dev/packages/google_sign_in và https://pub.dev/packages/google_sign_in_android
