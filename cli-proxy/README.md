# Hướng dẫn Quản lý và Vận hành CLI Proxy API

## 1. Thông tin truy cập Web Management & API

| Thông tin | Giá trị |
| :--- | :--- |
| **Giao diện Web Management** | [http://localhost:8318/management.html](http://localhost:8318/management.html) |
| **Mật khẩu quản trị (Secret Key)** | `admin123456` *(Được mã hóa bcrypt an toàn trong `cli-proxy/config.yaml`)* |
| **OpenAI Compatible Base URL** | `http://localhost:8318/v1` |
| **Client API Key (gọi API)** | `sk-cliproxy-pbl6-local-key` |
| **Trạng thái Healthcheck** | `healthy` (`bash -c 'echo > /dev/tcp/127.0.0.1/8317'`) |

> **Lưu ý về cổng host:** Cổng được ánh xạ mặc định là **`8318`** để tránh xung đột với container `aiagent-rag-cliproxyapi-1` (đang chiếm cổng `8317` trên máy). Bạn có thể cấu hình lại cổng tùy ý qua biến môi trường `CLIPROXYAPI_PORT` trong file `.env`.

---

## 2. Hướng dẫn sử dụng Web Management Panel

1. Mở trình duyệt truy cập: [http://localhost:8318/management.html](http://localhost:8318/management.html)
2. Nhập mật khẩu: **`admin123456`** và nhấn Đăng nhập.
3. **Kết nối tài khoản AI (OAuth / API Key):**
   - Vào tab **OAuth** (hoặc **Auth files & OAuth**):
     - Chọn nhà cung cấp bạn muốn kết nối (ví dụ: **Antigravity**, **Gemini**, **Claude**, **Codex**).
     - Nhấn nút đăng nhập, trình duyệt sẽ mở luồng xác thực Google / Anthropic / OpenAI để cấp quyền.
     - Sau khi xác thực thành công, token sẽ tự động lưu bền vững vào thư mục `cli-proxy/auths/` (đã được cấu hình mount volume và đưa vào `.gitignore`).
   - Hoặc vào tab **AI Providers** để nhập trực tiếp Gemini / Claude / OpenAI API Key nếu có.
4. **Kiểm tra mô hình & Quota:**
   - Tab **Models**: Danh sách các model đang hoạt động (ví dụ: `gemini-2.5-pro`, `claude-3-7-sonnet`, `gpt-4o`, `antigravity`,...).

---

## 3. Lệnh vận hành Docker Compose

Triển khai độc lập thông qua file [docker-compose.cli-proxy.yml](file:///Users/capkimkhanh/Documents/DUT4_1/PBL6/docker-compose.cli-proxy.yml):

```bash
# Khởi động dịch vụ ở chế độ chạy ngầm
docker compose -f docker-compose.cli-proxy.yml up -d

# Xem log thời gian thực
docker compose -f docker-compose.cli-proxy.yml logs -f

# Khởi động lại dịch vụ
docker compose -f docker-compose.cli-proxy.yml restart

# Dừng dịch vụ
docker compose -f docker-compose.cli-proxy.yml stop
```

---

## 4. Kết nối với Spring Boot Backend (PBL6)

- **Khi Backend chạy trực tiếp trên máy Host (Local / IDE):**
  - AI Base URL: `http://localhost:8318/v1`
  - AI API Key: `sk-cliproxy-pbl6-local-key`
- **Khi Backend chạy trong Docker (`docker-compose.yml`):**
  - AI Base URL: `http://host.docker.internal:8318/v1`
  - AI API Key: `sk-cliproxy-pbl6-local-key`
