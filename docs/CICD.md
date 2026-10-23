# CI/CD — DANASEA

CI chạy cho PR/push main; CD build image gắn commit SHA sau khi CI push main thành công. **Deploy chỉ chạy khi repository variable `PRODUCTION_DEPLOY_ENABLED=true`.** Mặc định biến chưa đặt thì chỉ build/push image, không SSH vào server. Giữ biến tắt trong lần chuyển đổi cấu hình production đầu tiên.

## Thứ tự đưa bản bảo mật lên server

1. Review diff và chạy kiểm tra local. Commit/push nhánh đang làm, tạo PR và đợi CI đạt. Push nhánh tính năng không deploy production.
2. Bật bảo vệ main: bắt buộc PR và required checks `Backend (Java 21)`, `Frontend (Node 22)`; giới hạn quyền bypass. Production environment chỉ cho main. Bật MFA GitHub/Tailscale. Giữ `PRODUCTION_DEPLOY_ENABLED` tắt.
3. Merge PR sau review/CI; đợi CI trên main và CD build image theo SHA hoàn tất. Server vẫn chạy release cũ vì deploy gate đang tắt.
4. Trong cửa sổ bảo trì, SSH bằng key, giữ phiên quản trị mở. Fetch và checkout đúng SHA đã được CI kiểm tra. Đây chỉ đổi source/config trên host; chưa chạy `compose up`.
5. Chuẩn bị `.env`/private config/runtime DB role theo phần dưới, xử lý message đang chờ ở broker cũ. Cài lại script deploy root-owned của release mới. Deploy thủ công và nghiệm thu.
6. Chỉ sau khi release mới đạt nghiệm thu mới bật `PRODUCTION_DEPLOY_ENABLED=true` để những lần merge sau tự deploy.

Không merge rồi để workflow cũ tự deploy trước khi chuẩn bị config. Trong lần chuyển đổi này, xác nhận gate ở workflow trên main thực sự có mặt trước khi coi merge là an toàn cho server.

## Chuẩn bị cấu hình production

Các lệnh sau chạy **trên Ubuntu sau khi đã checkout release mới**, không chạy ở repo Mac để thay cấu hình server. Không chép đè `.env` hiện có bằng `.env.example`.

```bash
python3 scripts/production.py prepare
```

Lệnh tạo secret còn thiếu/placeholder và config trong `private/`, đặt `.env` mode 600; giữ credential đã có, không sửa thiết lập CPU/RAM/heap, không xoá token OAuth. Không in giá trị secret. Không source `.env` như shell script. Phần đọc `.env` dùng chính parser Docker Compose để xử lý ký tự đặc biệt thống nhất.

Private proxy config đã tồn tại sẽ được giữ. Nếu management panel ghi lại thành YAML, cài `python3-yaml` để preflight đọc bằng safe parser. Đối chiếu client key trong private config với `AI_CLIPROXY_API_KEY`; management key phải riêng biệt. Preflight đối chiếu management key dạng plaintext; nếu proxy đã lưu bcrypt hash, cần thử đăng nhập quản trị bằng key dự kiến sau khi khởi động để xác nhận khớp. Không thêm API key trực tiếp của nhà cung cấp vào deployment OAuth-only này.

`CLIPROXY_UID`/`CLIPROXY_GID` phải khớp user không phải root chạy proxy (`id -u`, `id -g`). Giữ nguyên OAuth token khi sửa quyền. Nếu token hiện thuộc root, chỉ sửa ownership thư mục `cli-proxy/auths`, config/static cần thiết; không chown cả repo và không chmod 777.

Kiểm tra bằng trình soạn thảo tin cậy: Cloudinary/mail/payment credential thật, HTTPS CORS origin và URL callback đúng, model proxy được tài khoản OAuth cho phép. Trong production phải điền URL trả về HTTPS cho PayPal/VNPay; các mặc định localhost phục vụ development.

Với PostgreSQL hiện tại đang chạy:

```bash
python3 scripts/production.py init-db
python3 scripts/production.py check
```

`init-db` là thao tác thay đổi database role/grant có chủ đích; không tự chạy khi chỉ gọi `prepare` hoặc `check`. Nó tạo runtime role riêng, giữ tài khoản sở hữu schema cho Flyway, không cấp CREATE schema cho ứng dụng và thu hồi DML trên Flyway history đã có. Không dùng cùng username cho runtime và migration. Với database hoàn toàn mới: sau `prepare`, dùng `IMAGE_TAG=<sha> docker compose -f docker-compose.prod.yml up -d postgres`, rồi `IMAGE_TAG=<sha> docker compose -f docker-compose.prod.yml run --rm migrate`, sau đó mới gọi `init-db` và `check`. Chưa khởi động backend trước bước này; như vậy Flyway history không giữ DML grant cho runtime role.

`check` chỉ đọc và dừng deployment nếu secret yếu/trùng nhau, permission/config/OAuth ownership chưa đúng, runtime role còn đặc quyền, Compose không hợp lệ hoặc subnet Docker xung đột. Nó không thay thế nghiệm thu thực tế.

RabbitMQ 4 dùng volume mới; volume cũ không bị xoá. Cần drain/export/import message và topology cần giữ trước lần chuyển đổi. Không mount trực tiếp dữ liệu 3.12 vào 4.x và không chạy `down -v`.

## IP Docker và ranh giới tin cậy

`172.30.10.0/28` là mạng bridge riêng do Compose khai báo, không phải IP LAN/public/Tailscale:

- `172.30.10.1`: gateway của host trên bridge; chỉ IP này được `frontend/nginx.prod.conf` tin để đọc X-Forwarded-For từ Funnel.
- `172.30.10.2`: frontend/Nginx; chỉ IP này được Tomcat production tin làm proxy.
- `172.30.10.3`: backend. Các service vẫn gọi nhau qua Docker DNS như `backend`, `postgres`, `cli-proxy-api`.

Địa chỉ cố định dùng để thu hẹp trust boundary. Không thay bằng `0.0.0.0/0` hoặc tin toàn bộ private range. Preflight kiểm tra xung đột với mạng Docker và route host/VPN; cần đối chiếu thêm subnet của mạng từ xa có thể dùng sau này. Nếu đổi subnet, phải đổi đồng bộ Compose, Nginx production và Spring production; test deployment guard kiểm tra tính nhất quán này. Cấu hình Nginx dev riêng không giả định gateway production hoặc ép HTTPS.

Funnel phải tiếp tục chỉ trỏ `127.0.0.1:8080`. `127.0.0.1` trong ports là loopback để chặn truy cập trực tiếp từ LAN/Internet. Backend dùng `http://cli-proxy-api:8317/v1`, không dùng localhost của host từ trong container.

## Image digest

`image: repository:tag@sha256:...` khoá đúng nội dung image. Digest công khai, không phải credential; tag chỉ là nhãn và có thể thay đổi. Digest giúp tái lập release nhưng không chứng minh image không có lỗ hổng hoặc nhà phát hành đáng tin.

Chỉ dùng registry/publisher đã kiểm tra; xác nhận platform phù hợp rồi quét trước khi đổi digest. Khi có bản vá, cập nhật tag/digest qua PR, test và quét lại. Migration job dùng chính image backend theo SHA và SQL đóng gói trong JAR; không dùng image Flyway riêng. Không xoá digest để tự động nhận một image chưa được kiểm tra.

```bash
docker buildx imagetools inspect redis:7.4-alpine
```

Kết quả phải chứa đúng digest và platform mong muốn. Action GitHub cũng pin commit SHA và cần quy trình cập nhật tương tự.

## SSH và GitHub settings

GitHub environment `production` cần hai secrets `TS_OAUTH_CLIENT_ID`, `TS_OAUTH_SECRET` từ Tailscale OAuth client. Chỉ cấp scope `Auth Keys: Write` cho `tag:ci`; không cấp quyền sửa policy, người dùng hay DNS. Các secrets `SERVER_TAILSCALE_IP`, `SERVER_USER`, `SSH_PRIVATE_KEY`, `SSH_KNOWN_HOSTS` có thể nằm ở environment hoặc repository. Host key phải được đối chiếu fingerprint qua kênh tin cậy; không chỉ tin `ssh-keyscan`. Dùng key riêng cho CI.

Workflow kiểm tra các giá trị OAuth và đích Tailscale không rỗng mà không in secret, đăng ký runner với `tag:ci`, rồi dùng SSH tới `SERVER_TAILSCALE_IP` với timeout kết nối 15 giây và tối đa 3 lần thử. SSH kiểm tra host key và xác thực bằng key CI trước khi chạy forced command deploy. Kết nối trực tiếp hoặc qua DERP đều được chấp nhận. Không dùng input `targets` của bản Tailscale Action đang pin: phép ping có thể trả mã 1 khi chưa tạo được đường trực tiếp, và Bash `-e` làm action thoát trước nhánh chấp nhận relay. Không mở thêm quyền policy hoặc port router để làm ping thành công.

Workflow không dùng hoặc tự chuyển về `TAILSCALE_AUTHKEY`. Chỉ thu hồi authkey cũ và xoá secret cũ sau khi lần CD đầu tiên bằng OAuth thành công. Không ghi OAuth client secret vào source, log hay chat.

Cài script theo source đã review:

```bash
sudo install -o root -g root -m 755 scripts/deploy.sh /usr/local/bin/danasea-deploy
```

Key CI trong authorized_keys:

```text
command="/usr/local/bin/danasea-deploy",restrict ssh-ed25519 AAAA... github-actions-deploy
```

Key quản trị cá nhân giữ SSH tunnel. Script chỉ chấp nhận `deploy <40-char sha>`, kiểm tra commit thuộc lịch sử origin/main và chạy preflight trước thao tác container. Sau khi script thay đổi cần cài lại bản root-owned; sửa file trong repo không tự cập nhật forced command đã cài.

Tailscale policy cần tách quyền quản trị và runner: chỉ admin sở hữu `tag:ci`, runner chỉ được `tcp:22` tới server production. Đích có thể dùng host alias trỏ tới IP đã đối chiếu trong trang Machines; không gắn tag mới lên server chỉ để chuyển CI sang OAuth. Không giữ grant `src: ["*"]` tới mọi đích vì nó cũng cấp quyền rộng cho runner đã gắn tag. Giữ quyền thiết bị cá nhân và nodeAttrs cho Funnel, thêm policy tests chặn runner tới các cổng khác và máy cá nhân. Merge vào policy đang dùng, chạy tests rồi thử phiên SSH mới.

## Deploy và nghiệm thu

Deploy thủ công qua phiên quản trị sau khi chuẩn bị xong:

```bash
/usr/local/bin/danasea-deploy deploy <sha-da-qua-ci>
```

Script giữ backup database trước deploy và 7 bản gần nhất; image pull chỉ lấy nội dung đã cấu hình. Không tự sinh secret hoặc thay OS/firewall trong pipeline. Sau checkout, script chỉ đặt file cấu hình Nginx công khai thành mode 644 để UID của frontend đọc được; secret/private config vẫn được giữ quyền hạn chế.

Khi healthcheck thất bại, script in trạng thái, log frontend/backend/Redis/RabbitMQ và chi tiết healthcheck. Manifest so sánh rollback được render vào thư mục tạm mode 700 rồi xoá khi kết thúc; không in các manifest chứa secret. Chỉ cho phép rollback tự động khi cấu hình PostgreSQL, Redis, RabbitMQ, mạng và volume giữa hai release không đổi. Rollback chỉ khởi động lại backend/frontend bằng `--no-deps`, không recreate hay hạ phiên bản các dịch vụ dữ liệu. Nếu cấu hình hạ tầng khác hoặc chưa xác minh được, script giữ bản hiện tại và yêu cầu phục hồi có kiểm soát. Không dùng Compose cũ để hạ Redis sau khi bản mới đã ghi RDB mới. Rollback cũng không hoàn tác Flyway; migration phải tương thích phiên bản ứng dụng trước.

Sau deploy: xác nhận prod profile, DB role không đặc quyền, Redis/RabbitMQ authentication, OAuth AI thật, IP/HTTPS từ hai mạng khách, customer/vendor ownership, cổng public, SMTP/payment sandbox và giám sát. Chỉ sau nghiệm thu mới bật auto deploy. Backup ngoài máy tiếp tục là phạm vi vận hành riêng.
