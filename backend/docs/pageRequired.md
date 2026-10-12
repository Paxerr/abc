# DANASEA — Danh sách trang Web và màn hình Mobile cần có

**Phạm vi:** toàn bộ API controller trong checkout hiện tại, các luồng nghiệp vụ backend, các route/màn hình React và Flutter, cùng thiết kế đã có trong repository.

Web có ba vai trò **Khách hàng (CUSTOMER), Nhà cung cấp (VENDOR), Quản trị viên (ADMIN)**. Mobile có hai vai trò **Khách hàng và Vendor**; repository hiện tách thành `mobile/` và `vendor_mobile/`. Admin vận hành qua web.

## 1. Cách đọc, phạm vi và kết quả kiểm kê

Danh sách mô tả **trang/màn hình nghiệp vụ cần có**, không xác nhận mọi giao diện đã tích hợp thật. Một mã có thể là route, tab, drawer hoặc bottom sheet; vẫn phải hoàn thành thao tác, trạng thái và quyền tương ứng. Các URL Web là đề xuất, cần đối chiếu router khi triển khai. Mã cũ được giữ nguyên để bảo toàn tham chiếu giữa Web, Mobile và API.

### 1.1. Mức hỗ trợ và ưu tiên

| Ký hiệu | Ý nghĩa |
| --- | --- |
| **Có API** | Endpoint hỗ trợ chức năng chính đã tồn tại; UI vẫn cần tích hợp, xử lý trạng thái và nghiệm thu. |
| **Một phần** | Có API chính nhưng thiếu một phần dữ liệu, thao tác hoặc bước nghiệp vụ được trang yêu cầu. |
| **Thiếu API** | Chưa có endpoint cho chức năng chính; đây là trang cần có nếu nghiệm thu toàn bộ phạm vi sản phẩm. |
| **Client/tĩnh** | Nội dung, điều hướng hoặc dữ liệu client không nhất thiết cần API riêng. |

**P0:** xác thực, mở bán, tồn/hold, checkout với giảm giá/waiver, thanh toán và vé/check-in. **P1:** hủy/hoàn/đổi lịch, an toàn, dispute, chat, review, AI, hỗ trợ thủ công, báo cáo, đối soát và quản trị. **P2:** chi trả và cấu hình vận hành qua UI, push/media hoặc các mở rộng chưa có contract. Một chức năng P2 vẫn cần hoàn thiện nếu đưa toàn bộ nhóm đó vào nghiệm thu.

### 1.2. Những điều phải biết trước khi thiết kế

- Có **67 controller, 208 handler và 212 tổ hợp HTTP method/path**: 4 tổ hợp bổ sung là alias. Bao gồm 4 API authorization chỉ profile dev/test và 1 internal webhook chỉ profile test. Mục 9 ánh xạ đầy đủ 212 tổ hợp; callback/alias/tác vụ nền không tạo thêm trang.
- Đã có API cho chat theo đơn, review/reply/moderation, discount codes/checkout preview/voucher cá nhân, dashboard/report/export, dispute khách/vendor/admin, notification read/unread, public vendor, waiver, receipt và reschedule/disruption.
- AI đã có preferences/feedback, preview–save–accept–archive lịch trình, proposals/revisions, job theo dõi nguồn khi cấu hình bật, support request khách và hàng đợi xử lý admin. Các chức năng này phải được thể hiện trên Web/Mobile, không chỉ ở trang chatbot.
- Các khoảng trống còn thật nằm ở mục 7: duyệt/resubmit giấy tờ vendor; detail vận hành vendor; đọc lại một số hồ sơ; bước COMPLETED để review; chi trả; settings; một số read/media/follow-up/deep link. Không coi việc có API cho một thao tác là toàn bộ trang đã hoàn chỉnh.
- Một tài khoản có một role. Đăng ký hồ sơ vendor đổi role sang VENDOR; UI phải phục hồi phiên/claims đúng quyền. Không có contract chuyển qua lại CUSTOMER/VENDOR bằng nút chọn role. Admin không đăng ký công khai.
- Route/component/mock hiện có chỉ chứng minh giao diện đã được thiết kế. Trạng thái hỗ trợ trong tài liệu lấy từ backend; nghiệm thu cần kiểm tra dữ liệu và thao tác thật của từng client.
- Nội dung hủy/hoàn lấy từ policy snapshot của đơn và cancellation-preview. Mặc định mới là STANDARD_24H_V2; đơn cũ có thể giữ LEGACY_TIERED_V1. Xem mục 7.1.

### 1.3. Số chức năng logic cần triển khai

| Phạm vi | Vai trò | Số mã | Mã |
| --- | --- | --- | --- |
| Web dùng chung | CUSTOMER/VENDOR/ADMIN | 6 | G01–G06 |
| Web khách hàng | CUSTOMER | 33 | C01–C33 |
| Web vendor | VENDOR | 27 | V01–V27 |
| Web admin | ADMIN | 33 | A01–A33 |
| Mobile khách hàng | CUSTOMER | 37 | MC01–MC37 |
| Mobile vendor | VENDOR | 32 | MV01–MV32 |
| **Tổng** | **99 chức năng Web, 69 chức năng Mobile** | **168** | Có thể gộp tab/dialog nhưng phải giữ đủ chức năng. |

## 2. Web — Trang dùng chung và xác thực

Các trang này dùng chung cho ba vai trò, sau đăng nhập điều hướng theo role do backend trả về. Admin không đăng ký công khai bằng lựa chọn vai trò.

| Mã | Trang / đường dẫn đề xuất | Chức năng và API liên quan | Backend | Ưu tiên |
| --- | --- | --- | --- | --- |
| G01 | Đăng nhập / đăng ký — `/auth` | Tab đăng nhập, đăng ký khách hàng, Google OAuth; `POST /api/auth/login`, `/register`, `/oauth2/google`. Vendor mới bắt đầu từ tài khoản khách rồi đăng ký hồ sơ đối tác. | Có API | P0 |
| G02 | Xác minh email — `/auth/verify-email` | Nhập OTP, gửi lại mã, đếm thời gian và báo vượt giới hạn; `POST /api/auth/otp/send`, `/otp/verify`. Hai API này cần phiên đăng nhập, khác OTP khôi phục mật khẩu. | Có API | P0 |
| G03 | Khôi phục mật khẩu — `/auth/forgot-password` | Hai bước yêu cầu mã và nhập OTP/mật khẩu mới; `POST /api/auth/forgot-password`, `/reset-password`. Sau thành công quay về đăng nhập. Có thể tách `/auth/reset-password` nếu cần deep link. | Có API | P0 |
| G04 | Cài đặt cá nhân — `/settings` hoặc tab tài khoản | Ngôn ngữ VI/EN và đăng xuất qua `PATCH /api/users/me`, `POST /api/auth/logout`; gửi Accept-Language nhất quán. Khách hàng có điểm vào C31 để cá nhân hóa AI. Push token và tùy chọn notification riêng còn thiếu. | Một phần | P1 |
| G05 | Hướng dẫn, chính sách, điều khoản an toàn — `/help`, `/policies` | Hướng dẫn đặt, an toàn, chính sách theo version, quyền riêng tư và liên hệ hỗ trợ. Nội dung/version waiver đọc từ service/order; lưu chấp thuận bằng `POST /api/sub-orders/{id}/waiver-acceptance` trong C07. Chính sách áp dụng lấy từ đơn/preview. | Client/tĩnh + API waiver | P1 |
| G06 | Trạng thái truy cập và lỗi — `/403`, `/404`, màn hình lỗi dùng chung | Sai vai trò, phiên hết hạn, tài khoản khóa, dữ liệu không còn tồn tại, lỗi mạng/rate limit và thử lại. Refresh phiên bằng `POST /api/auth/refresh`; không cần trang riêng cho refresh/logout. | Client/tĩnh + API auth | P0 |

## 3. Web — Khách hàng

Khách chưa đăng nhập có thể xem catalog, danh mục, slot công khai và thời tiết. Yêu thích, AI, đặt/ thanh toán và dữ liệu cá nhân cần đăng nhập theo security hiện tại.

### 3.1. Khám phá và đặt dịch vụ

| Mã | Trang / đường dẫn đề xuất | Chức năng và API liên quan | Backend | Ưu tiên |
| --- | --- | --- | --- | --- |
| C01 | Trang chủ — `/` | Danh mục, trải nghiệm, thời tiết, recently viewed qua `GET /api/categories`, `GET /api/services`, `GET /api/recently-viewed`, `GET /api/weather/current`. Khách đăng nhập dùng `POST /api/ai/recommendations`; lưu recommendationId và phản hồi SHOWN/CLICK/POSITIVE/NEGATIVE qua `POST /api/ai/recommendations/{id}/feedback` theo consent C31. | Có API; banner có thể tĩnh | P0 |
| C02 | Tìm kiếm / khám phá — `/search` | `GET /api/services`: keyword/category/price/location/radius, date/timeSlot hoặc dateTime, guests hoặc participantCount, vendorId, minRating, sortBy, page/size. `POST /api/ai/search`, `POST /api/ai/nearby` cho khám phá AI; phản hồi gợi ý theo recommendationId. Filter phải giữ khi tải lại/chuyển trang, không suy ra sort ngoài contract. | Có API | P0 |
| C03 | Chi tiết trải nghiệm — `/experiences/:serviceId` | `GET /api/services/{id}`: ảnh, giá, vendor/badge, vị trí, điều kiện tham gia, duration, waiver/version/ngôn ngữ, policy/an toàn. `GET /api/services/{id}/slots` có optionId/from/to/quantity/allowSplit. `GET /api/services/{id}/reviews`; `POST /api/reviews/{id}/flag`; public vendor C28. Weather safety/AI và review summary là thông tin hỗ trợ; nút Đặt đưa lựa chọn sang giỏ/hold. | Có API | P0 |
| C04 | So sánh trải nghiệm — `/compare` | Chọn nhiều dịch vụ và ngữ cảnh chuyến đi, so sánh giá/khả dụng/đặc điểm; `POST /api/ai/services/compare`. Có thể làm drawer trên C02/C03. | Có API | P1 |
| C05 | Yêu thích — `/account/wishlist` | Xem, thêm, bỏ dịch vụ yêu thích; `GET /api/wishlists`, `POST/DELETE /api/wishlists/{serviceId}`. | Có API | P1 |
| C06 | Giỏ trải nghiệm — `/cart` | Nhiều dịch vụ, option, slot, số người/gói, giá tạm tính; kiểm tra lại slot trước checkout. Giỏ lưu client hiện tại; không có CRUD cart server. Chỉ tạo hold khi khách tiếp tục đặt. | Client + API catalog/slot/hold | P0 |
| C07 | Checkout / giữ chỗ — `/checkout` | Giữ chỗ qua `POST /api/bookings/hold`, đọc booking và holdExpiresAt, hủy hold hợp lệ khi rời luồng. `POST /api/checkout/discount-preview` với bookingId/code; `POST /api/orders` với bookingId/discountCode và Idempotency-Key. Đọc snapshot từng sub-order, `POST /api/sub-orders/{id}/waiver-acceptance` trước `POST /api/payments/{orderId}/create-intent`. Xử lý 409 WAIVER_ACCEPTANCE_REQUIRED, hết tồn/hold, mã không hợp lệ và giá cuối backend. | Có API | P0 |
| C08 | Kết quả thanh toán — `/checkout/result` | Return/deep link → `GET /api/payments/{orderId}/status`, `GET /api/payments/detail/{paymentId}`, `GET /api/orders/{id}`. PayPal return gọi `POST /api/payments/paypal/capture` có idempotency. Hiển thị đang chờ/thành công/thất bại/hết hạn; phục hồi C07 nếu thiếu waiver. Booking chỉ xác nhận khi backend đã nhận đủ tiền; không lấy query success làm PAID. | Có API | P0 |

### 3.2. Theo dõi và xử lý sau đặt

| Mã | Trang / đường dẫn đề xuất | Chức năng và API liên quan | Backend | Ưu tiên |
| --- | --- | --- | --- | --- |
| C09 | Đơn hàng / booking của tôi — `/account/orders` | Danh sách đơn, booking giữ chỗ/chờ thanh toán/đã xác nhận; `GET /api/orders`, `GET /api/bookings`. Tách tab booking và order, tránh hiển thị cùng một giao dịch thành hai đơn khác nhau. Bộ lọc order nâng cao chưa có. | Có API cơ bản | P0 |
| C10 | Chi tiết đơn — `/account/orders/:orderId` | `GET /api/orders/{id}`, `GET /api/bookings/{id}`, `GET /api/orders/{id}/payments`, `GET /api/orders/{id}/refunds`. Theo từng sub-order: waiver, vé, đổi lịch/disruption, dispute/review/chat/support đúng eligibility. `GET /api/orders/{id}/receipt` trả JSON/PDF khi có payment thành công/đã hoàn hợp lệ; hỗ trợ tải/in/chia sẻ. Dữ liệu liên hệ/participants/timeline còn cần contract nếu chưa được DTO trả. | Một phần | P0 |
| C11 | Vé điện tử / QR — `/account/orders/:orderId/tickets/:subOrderId` | `POST /api/bookings/{id}/qr-code` dùng subOrderId cho vé từng dịch vụ; hiển thị ca, hiệu lực và state backend. Sau reschedule phải tải lại slot và cấp QR mới; không dùng bookingId nhầm hoặc cache QR cũ như vé còn hợp lệ. | Có API | P0 |
| C12 | Hủy đặt / yêu cầu hoàn — `/account/orders/:orderId/cancel` | `GET /api/orders/{id}/cancellation-preview` trả chính sách/version/tỷ lệ/số tiền. Chọn một lệnh hủy booking hoặc refund-request phù hợp trạng thái; không gửi hai lệnh mặc định. Khi có disruption đã xác minh, chọn full refund theo sub-order qua `POST /api/sub-orders/{id}/disruption-refund`; phân biệt với khách tự hủy. | Có API | P1 |
| C13 | Theo dõi hoàn tiền — `/account/orders/:orderId/refunds` | `GET /api/orders/{id}/refunds`; số tiền, reason, sub-order, policy và PENDING/PROCESSED/FAILED. PENDING hoặc disruption REFUND_REQUESTED chưa phải tiền về; làm mới theo backend và dẫn tới hỗ trợ/dispute nếu cần. | Có API | P1 |
| C14 | Khiếu nại của tôi — `/account/disputes` | `GET /api/disputes` của khách, phân trang, trạng thái, đơn/sub-order liên quan; mở C16 theo disputeId và C15 từ đơn. Không dùng list admin cho khách. | Có API | P1 |
| C15 | Tạo khiếu nại — `/account/orders/:orderId/disputes/new` | Chọn sub-order đủ điều kiện CONFIRMED/COMPLETED; `POST /api/disputes/evidence` multipart file → evidenceUrls → `POST /api/orders/{id}/disputes` với masterOrderId ở path và subOrderId trong body. Hiển thị tiến trình upload, lỗi/trùng dispute và kết quả tạo. | Có API | P1 |
| C16 | Chi tiết khiếu nại — `/account/disputes/:disputeId` | `GET /api/disputes/{id}`: lý do, chứng cứ, vendorResponse/vendorEvidenceUrls/vendorRespondedAt, kết luận admin và tỷ lệ hoàn. Có thể tải lại trực tiếp bằng disputeId. Khách bổ sung/sửa chứng cứ sau khi gửi hoặc thread nhiều lượt chưa có endpoint. | Một phần | P1 |
| C17 | Viết / quản lý đánh giá — `/account/reviews` | `POST /api/sub-orders/{id}/reviews`; `PUT /api/sub-orders/{id}/reviews` hoặc `PUT /api/reviews/{id}`; `POST /api/reviews/{id}/flag`. Chỉ tạo cho sub-order COMPLETED thuộc khách, một review/sub-order. Có vendor reply trong dữ liệu review. List/detail review của tôi và upload ảnh riêng còn thiếu; bước hoàn tất sau CHECKED_IN phải được khép kín. | Một phần | P1 |
| C30 | Đổi lịch / lựa chọn khi gián đoạn — `/account/orders/:orderId/sub-orders/:subOrderId/reschedule` | Chọn subOrderId, `GET /api/sub-orders/{id}/reschedule-options`: options, rescheduleAllowed/eligibilityReason, rescheduleVersion, deadline và fullRefundAvailable. `POST /api/sub-orders/{id}/reschedule` với targetSlotId/proposalId/expectedVersion và Idempotency-Key. Chỉ đổi slot cùng service đúng điều kiện; hiển thị referencePrice/additionalFee từ response, không tự thu tiền chênh lệch. Thành công đọc lại đơn và QR mới; chọn full refund khi contract cho phép. | Có API | P1 |

### 3.3. AI, liên lạc và tài khoản

| Mã | Trang / đường dẫn đề xuất | Chức năng và API liên quan | Backend | Ưu tiên |
| --- | --- | --- | --- | --- |
| C18 | Trợ lý AI — `/assistant` | Hội thoại tư vấn, dịch vụ gợi ý, confirmation card và chuyển sang checkout; `POST /api/assistant/chat`, `GET /api/assistant/conversations/{id}`, `.../{id}/history`, `POST .../{id}/confirm`. Chưa có API liệt kê toàn bộ hội thoại AI của user. | Có API hội thoại cụ thể; danh sách một phần | P1 |
| C19 | Lịch trình của tôi / tạo lịch trình — `/account/itineraries` | Danh sách qua `GET /api/ai/itineraries` hoặc `GET /api/ai/itineraries/page`. Form ngày/ngân sách/nhóm/sở thích → `POST /api/ai/itineraries/preview` → so sánh alternatives/tradeOffs/TTL → `POST /api/ai/itineraries/previews/{previewId}/save` để lưu DRAFT. `POST /api/ai/itineraries` vẫn hỗ trợ tạo trực tiếp. Chốt lịch qua C20, không coi save là đặt chỗ. | Có API | P1 |
| C20 | Chi tiết / điều chỉnh lịch trình — `/account/itineraries/:itineraryId` | GET detail; lifecycle DRAFT/ACCEPTED/STALE/ARCHIVED. Accept/archive với expectedVersion; replan tạo proposal PENDING. Tab proposals có diff/trigger/baseVersion, accept/reject theo proposalId; tab revisions có lịch sử. Revalidate giá/slot/weather trước accept. Job nguồn có thể tạo proposal khi bật; notification thiếu parent itineraryId cần contract tra ngược. Đặt qua C07 riêng. | Một phần: lifecycle có API; deep link proposal thiếu parent | P1 |
| C21 | Hỗ trợ đơn hàng — `/account/orders/:orderId/support` | `POST /api/ai/support` giải thích đơn và cancellation-preview. Form HANDOFF/CHANGE_REQUEST → `POST /api/ai/support/requests/preview` → xác nhận explicit confirmed=true → `POST /api/ai/support/requests` với Idempotency-Key. desiredDate/desiredSlotId theo kind và eligibility. Điều hướng C32/C33 để theo dõi. Tạo/xử lý ticket không tự hủy/hoàn/đổi booking. | Có API | P1 |
| C22 | Hộp thư khách–vendor — `/messages` | `GET /api/conversations` có page/lastMessage/unreadCount; `POST /api/conversations` tạo hoặc lấy hội thoại theo masterOrderId, chọn vendorId khi đơn nhiều vendor. Mở từ C10 hoặc danh sách. Chat trước khi có đơn từ service chưa được contract hiện tại hỗ trợ. | Một phần: chat theo đơn có API | P1 |
| C23 | Hội thoại khách–vendor — `/messages/:conversationId` | `GET /api/conversations/{id}/messages` với afterSequence hoặc after, page/size; POST messages với content/attachmentUrl. Polling, lịch sử, read receipt và unread badge; batch được trả về được đánh dấu đã đọc. Reconnect dùng sequence; upload attachment riêng chưa có. Có thể dùng khung chung C22. | Một phần: tin nhắn có API; upload riêng thiếu | P1 |
| C24 | Thông báo — `/notifications` | `GET /api/notifications`; `PATCH /api/notifications/{id}/read`, `PATCH /api/notifications/read-all`, `GET /api/notifications/unread-count`. Đồng bộ badge/list và mở relatedEntityType/relatedEntityId đúng vai trò. Proposal AI cần tra parent itineraryId; push token/device registration chưa có. | Một phần: in-app có API; push/deep link AI thiếu | P1 |
| C25 | Tài khoản cá nhân — `/account` | `GET /api/users/me`; tài khoản/email/locale, đơn/vé, wishlist, lịch trình, C31 preferences, C32 hỗ trợ, C29 voucher, notification và logout. Account user khác business profile vendor; các điểm truy cập vẫn phải dùng đúng role. | Có API | P0 |
| C26 | Chỉnh sửa hồ sơ — `/account/profile` | Các trường backend cho phép cập nhật và locale; `PATCH /api/users/me`. Upload avatar cần contract riêng nếu có yêu cầu giao diện. | Có API trường hồ sơ; upload một phần | P1 |
| C27 | Đổi mật khẩu — `/account/change-password` | Mật khẩu cũ/mới; `POST /api/users/me/change-password`. Xử lý phiên cũ bị thu hồi và quay về đăng nhập. | Có API | P1 |
| C28 | Hồ sơ vendor công khai — `/vendors/:vendorId` | `GET /api/vendors/{id}` trả businessName/address/badgeTier/ratingAvg/ratingCount/activeServicesCount; `GET /api/vendors/{id}/services` có sort/page. Hiển thị hồ sơ public và mở từng service/review. List review aggregate theo vendor hoặc nội dung doanh nghiệp phong phú chưa có; không dùng admin profile lộ ngân hàng/documents. | Một phần: public profile/services có API | P1 |
| C29 | Voucher của tôi / ưu đãi — `/account/vouchers`; `/promotions` khi có feed công khai | Ví voucher cá nhân qua `GET /api/customers/me/discount-codes`, gồm mã thay thế SYSTEM_FAULT, đã dùng/hết hạn, recipient/source sub-order, thời hạn và lượt dùng. Áp mã ở C07 qua discount-preview; preview không giữ quota cuối. Feed campaign/voucher công khai toàn sàn chưa có. | Một phần: voucher cá nhân/preview có API | P1 |
| C31 | Sở thích / cá nhân hóa AI — `/account/preferences` | `GET /api/ai/preferences`, `PUT /api/ai/preferences` với expectedVersion/enabled/interests/exclusions. Bật/tắt consent, sửa sở thích và loại trừ, xử lý xung đột version bằng đọc lại. Không đồng nhất với context của một yêu cầu tạo lịch trình. | Có API | P1 |
| C32 | Yêu cầu hỗ trợ của tôi — `/account/support-requests` | `GET /api/ai/support/requests`: HANDOFF/CHANGE_REQUEST, đơn liên quan, createdAt/status; WAITING_REVIEW/IN_REVIEW/RESOLVED/DECLINED/CANCELLED. API hiện trả list tối đa 50, chưa có pagination server; không thiết kế total giả. Tạo từ C21, mở C33. | Có API; list có giới hạn | P1 |
| C33 | Chi tiết / rút yêu cầu hỗ trợ — `/account/support-requests/:requestId` | `GET /api/ai/support/requests/{id}`: nội dung, desiredDate/slot, processingMode, responseNote và version. `POST /api/ai/support/requests/{id}/cancel` với expectedVersion khi hợp lệ. RESOLVED chỉ là xử lý ticket; đọc lại order/booking trước khi thể hiện thay đổi thực tế. | Có API | P1 |

**Điều hướng web khách hàng:** Trang chủ → Khám phá → Chi tiết → Giỏ → Checkout → Kết quả → Đơn/vé; nhóm phụ là Yêu thích, AI/lịch trình, Tin nhắn, Thông báo và Tài khoản. Sở thích AI, Voucher của tôi và Yêu cầu hỗ trợ có điểm vào từ Tài khoản. C04, C12, C13, C15, C21, C26, C27, C33 có thể là drawer/tab khi vẫn hỗ trợ điều hướng và tải lại dữ liệu.

## 4. Web — Vendor

Vendor cần cả luồng đăng ký/xác minh và luồng vận hành hằng ngày. Không bỏ màn hình hồ sơ chờ duyệt vì chỉ thiết kế dashboard cho vendor đã được chấp thuận.

### 4.1. Hồ sơ và quản lý dịch vụ

| Mã | Trang / đường dẫn đề xuất | Chức năng và API liên quan | Backend | Ưu tiên |
| --- | --- | --- | --- | --- |
| V01 | Tổng quan vendor — `/vendor` | `GET /api/vendor/dashboard` với from/to: revenue, netPayout, new/completedOrders, completion/cancellationRate và alerts. Các tác vụ sắp tới lấy từ services/bookings/orders; mở V27 reports, V18 settlements và check-in. netPayout là chỉ số tính toán, không xác nhận ngân hàng đã trả. | Có API | P1 |
| V02 | Đăng ký trở thành vendor — `/vendor/onboarding` | Thông tin doanh nghiệp, thuế, địa chỉ, ngân hàng; `POST /api/vendor/profile`. Sau đổi role cần lấy lại phiên/claims phù hợp. Không tự cấp role bằng lựa chọn trên giao diện. | Có API | P0 |
| V03 | Hồ sơ xác minh / giấy tờ — `/vendor/verification` | Trạng thái PENDING/APPROVED/REJECTED, tải BUSINESS_LICENSE và SAFETY_CERT; `GET /api/vendor/profile`, `GET/POST /api/vendor/documents`. Thiếu luồng duyệt giấy tờ vendor và gửi lại hồ sơ bị từ chối; xem mục 7. | Một phần | P0 |
| V04 | Hồ sơ doanh nghiệp / ngân hàng — `/vendor/profile` | Xem/sửa doanh nghiệp và tài khoản nhận tiền; `GET/PATCH /api/vendor/profile`. Có thể tách tab doanh nghiệp/ngân hàng. | Có API | P0 |
| V05 | Danh sách dịch vụ — `/vendor/services` | Xem trạng thái, mở soạn thảo, gửi duyệt, tạm ngừng/mở lại, xóa khi hợp lệ; `GET /api/vendor/services`, `POST .../{id}/submit`, `PATCH .../{id}/pause`, `.../{id}/resume`, `DELETE .../{id}`. | Có API | P0 |
| V06 | Tạo / chỉnh sửa dịch vụ — `/vendor/services/new`, `/vendor/services/:serviceId/edit` | Nội dung VI/EN, danh mục, vị trí, thời lượng, waiver và weather-sensitive; `POST /api/vendor/services`, `GET/PATCH .../{id}`. Các tab ảnh, option, chứng từ phải có đủ chức năng như mô tả dưới bảng. | Có API chính; đọc chứng từ một phần | P0 |
| V07 | Chi tiết / tiến trình duyệt dịch vụ — `/vendor/services/:serviceId` | `GET /api/vendor/services/{id}`, GET options/slots; trạng thái/lý do từ chối, ảnh/nội dung, điều kiện publish và thao tác hợp lệ. Đọc DRAFT/REJECTED bằng API vendor. Xem lại safety documents sau reload còn thiếu GET như V06. | Một phần: detail chính có API; đọc chứng từ thiếu | P0 |
| V08 | Lịch dịch vụ / sức chứa — `/vendor/schedule` | Lịch ngày/tuần, chọn dịch vụ, booked/held/khả dụng và trạng thái ca; `GET /api/vendor/services/{id}/slots`. API hiện theo từng dịch vụ, chưa có lịch tổng toàn vendor hoặc lịch lặp hàng loạt. | Có API theo dịch vụ | P0 |
| V09 | Tạo / cấu hình ca — `/vendor/services/:serviceId/slots/new`, `.../slots/:slotId` | `POST/PATCH /api/vendor/services/{id}/slots...`; PERSON_LIMIT hoặc SHARED_CAPACITY_UNITS, sức chứa từng đơn vị, OPEN/CLOSED. PATCH hiện sửa status/capacity/units, không đổi date/start/end. | Có API; đổi giờ ca chưa có | P0 |

**Các tab bắt buộc trong V06**, cũng phải có trên mobile:

1. **Nội dung và an toàn:** form nghiệp vụ, bản xem trước, lưu nháp và gửi duyệt. Tùy chọn hỗ trợ phân loại qua `POST /api/ai/classifications/service`, rà soát văn bản qua `POST /api/ai/content-assessments`; kết quả là gợi ý để người dùng xem xét, không tự duyệt nội dung.
2. **Ảnh dịch vụ:** upload, xóa, đổi thứ tự bằng `POST /api/vendor/services/{serviceId}/images`, `DELETE .../images/{imageId}`, `PATCH .../images/reorder`. Đây là tab, không cần ba trang riêng.
3. **Lựa chọn bán:** `GET/POST /api/vendor/services/{id}/options`, `PATCH .../options/{optionId}`; SHARED/PRIVATE, PER_PERSON/PER_PACKAGE, giá và giới hạn khách/gói, kích hoạt/ngừng bán.
4. **Chứng từ an toàn dịch vụ:** upload qua `POST /api/vendor/services/{serviceId}/safety-documents`, xem trạng thái và lý do từ chối. Vendor chưa có GET riêng cho danh sách chứng từ này, ServiceResult cũng chưa trả danh sách; cần bổ sung để tải lại trang vẫn theo dõi được.

### 4.2. Vận hành, tài chính và liên lạc

| Mã | Trang / đường dẫn đề xuất | Chức năng và API liên quan | Backend | Ưu tiên |
| --- | --- | --- | --- | --- |
| V10 | Đơn / lịch khách đặt — `/vendor/orders` | Tab booking item và sub-order; `GET /api/vendor/bookings`, `/api/vendor/orders`. Booking có bộ lọc trạng thái; order hiện chủ yếu phân trang. | Có API cơ bản | P0 |
| V11 | Chi tiết đơn của vendor — `/vendor/orders/:subOrderId` | Chi tiết theo subOrderId: ca/option/participants/liên hệ, status, reject item bằng `PATCH /api/vendor/bookings/{bookingItemId}/reject`. Thiếu GET detail và liên kết ID đầy đủ trong list DTO. Gửi proposal qua `POST /api/vendor/sub-orders/{id}/reschedule-proposals` với proposedSlotId/reason/reasonType/expiresAt; chọn ca từ API slots của vendor, không dùng customer reschedule-options. | Một phần | P0 |
| V12 | Check-in QR — `/vendor/check-in` | Camera, nhập token thủ công, xác minh và kết quả; `POST /api/vendor/checkin/verify`. Hiển thị hết hạn, đã dùng, sai vendor, sai trạng thái/khung giờ. Quét ảnh là công việc client, backend nhận token. | Có API | P0 |
| V13 | Chi tiết hoàn tiền của đơn vendor — `/vendor/orders/:subOrderId/refunds` | Xem refund của sub-order thuộc vendor và ảnh hưởng doanh thu. Chưa có endpoint refund được cô lập theo vendor; không dùng API admin/customer hoặc tự suy ra đã hoàn từ settlement. | Thiếu API | P1 |
| V14 | Hộp thư — `/vendor/messages` | `GET /api/conversations` phân trang/lastMessage/unreadCount; `POST /api/conversations` theo đơn và vendorId đúng ownership. Mở từ V11 hoặc danh sách, sang V15; chỉ chat theo master order hiện có. | Có API | P1 |
| V15 | Hội thoại khách hàng — `/vendor/messages/:conversationId` | GET/POST messages theo conversationId; polling afterSequence, read receipt, reconnect và lịch sử. attachmentUrl được hỗ trợ nhưng upload riêng chưa có. Có thể dùng khung hai cột V14. AI Smart Reply chưa có contract. | Một phần: tin nhắn có API | P1 |
| V16 | Thông báo / cảnh báo an toàn — `/vendor/notifications` | Notification list/read/read-all/unread-count của user, mở dịch vụ/ca/sub-order theo ownership; weather API xem an toàn. Push và queue weather riêng cho vendor chưa có; không dùng admin queue thay thế. | Một phần | P1 |
| V17 | Đánh giá / phản hồi khách — `/vendor/reviews` | `GET /api/vendor/reviews`, `POST /api/vendor/reviews/{id}/reply`; đọc rating/comment/images/vendorReply, report qua `POST /api/reviews/{id}/flag`. List hiện phân trang, chưa có filter serviceId; upload media riêng thiếu. Review source cần COMPLETED hợp lệ. | Một phần: list/reply có API | P1 |
| V18 | Đối soát / doanh thu — `/vendor/settlements` | `GET /api/vendor/settlements`, lọc status/from/to/page, gross/refund/commission/net. Mở V19, V27 báo cáo theo kỳ và V20 payout. Không cộng một trang list thành tổng toàn vendor, không đồng nhất FINALIZED/net với tiền đã chuyển. | Có API đối soát | P1 |
| V19 | Chi tiết kỳ đối soát — `/vendor/settlements/:settlementId` | Các dòng sub-order, khoản hoàn/khấu trừ/hoa hồng và số phải trả; `GET /api/vendor/settlements/{id}`. | Có API | P1 |
| V20 | Yêu cầu chi trả — `/vendor/payouts` | Danh sách, tạo yêu cầu từ kỳ đủ điều kiện, ngân hàng nhận tiền. Có model payout request nhưng chưa có API. | Thiếu API | P2 |
| V21 | Chi tiết yêu cầu chi trả — `/vendor/payouts/:payoutId` | REQUESTED/APPROVED/PAID/REJECTED, chứng từ chuyển tiền và lý do từ chối. | Thiếu API | P2 |
| V22 | Khiếu nại liên quan — `/vendor/disputes` | `GET /api/vendor/disputes` với status/page; nội dung đơn/sub-order, mở V23 theo id. Hạn phản hồi chỉ hiển thị khi có contract; DisputeResponse hiện chưa trả deadline. | Một phần: list có API | P1 |
| V23 | Chi tiết / phản hồi khiếu nại — `/vendor/disputes/:disputeId` | `GET /api/vendor/disputes/{id}`, `POST /api/vendor/disputes/{id}/responses` với response/evidence URLs theo DTO. `POST /api/disputes/evidence` để upload trước; hiển thị phản hồi/kết luận sau đọc lại. Kiểm tra quyền, status và không gửi lặp khi state đã đổi. | Có API | P1 |
| V24 | Khuyến mãi vendor — `/vendor/promotions` | GET list/detail, POST create, PATCH update qua /api/vendor/discount-codes. Form code/scope/service, FIXED/PERCENTAGE, min/cap, quotas, validFrom/validTo và isActive; backend giới hạn sponsor/vendor theo quyền. Bật/tắt qua PATCH, không đề xuất DELETE ngoài contract. | Có API | P1 |
| V25 | Tài khoản người vận hành — `/vendor/account` | Hồ sơ user gắn vendor, locale, đăng xuất; `GET/PATCH /api/users/me`, API auth. Phân biệt user account với business profile V04. Không có quản lý đội ngũ nhiều nhân viên riêng. | Có API tài khoản cá nhân | P1 |
| V26 | Đổi mật khẩu — `/vendor/account/change-password` | `POST /api/users/me/change-password`; xử lý thu hồi phiên. Có thể là tab V25. | Có API | P1 |
| V27 | Báo cáo kinh doanh — `/vendor/reports` | `GET /api/vendor/reports/revenue`, `GET /api/vendor/reports/bookings`, `GET /api/vendor/reports/export`. from/to bắt buộc, groupBy day/week/quarter/year; phân tích doanh thu/booking và CSV. VendorId lấy từ phiên backend. Tách báo cáo kinh doanh khỏi settlement và payout. | Có API | P1 |

**Điều hướng web vendor:** Tổng quan; Dịch vụ; Lịch/sức chứa; Đơn/check-in; Báo cáo kinh doanh; Đối soát/chi trả; Tin nhắn; Đánh giá; Khiếu nại; Khuyến mãi; Hồ sơ/xác minh; Thông báo. Các mục thiếu API cần nằm trong kế hoạch hoàn thiện, không hiển thị như thao tác thật khi còn dùng mock.

## 5. Web — Admin

Admin cần quản trị cả vận hành và ngoại lệ, không chỉ các trang duyệt dịch vụ. Dùng tài khoản có role ADMIN; API chẩn đoán RBAC không phải chức năng quản trị người dùng.

### 5.1. Tài khoản, đối tác, catalog và an toàn

| Mã | Trang / đường dẫn đề xuất | Chức năng và API liên quan | Backend | Ưu tiên |
| --- | --- | --- | --- | --- |
| A01 | Tổng quan điều hành — `/admin` | `GET /api/admin/dashboard` với from/to/vendorId trả KPI revenue/netPayout, new/completedOrders, completion/cancellationRate và alerts. status ADMIN_ACCESS_GRANTED là field tương thích, không phải response duy nhất. Hàng chờ vendor/service/dispute/support có điểm vào API/page riêng; không suy ra tổng từ một trang list. | Có API | P1 |
| A02 | Người dùng — `/admin/users` | Tìm kiếm, lọc role/trạng thái khóa, phân trang; `GET /api/admin/users`. Hiện không có API gán role tùy ý hoặc tạo admin công khai. | Có API đọc/khóa | P1 |
| A03 | Chi tiết người dùng — `/admin/users/:userId` | `GET /api/admin/users/{id}`, `PATCH .../{id}/lock`, `.../{id}/unlock`, lý do khóa. Lịch sử đơn/vendor có thể cần endpoint liên kết bổ sung. | Có API chính | P1 |
| A04 | Danh sách / hàng chờ vendor — `/admin/vendors` | Lọc trạng thái xác minh và phân trang; `GET /api/admin/vendors`. | Có API | P0 |
| A05 | Chi tiết / duyệt vendor — `/admin/vendors/:vendorId` | Doanh nghiệp, ngân hàng, toàn bộ documents; `GET /api/admin/vendors/{id}`, `PATCH .../{id}/approve`, `.../{id}/reject`. Luồng duyệt documents đang thiếu dù approve yêu cầu document APPROVED. | Một phần, chặn onboarding đầy đủ | P0 |
| A06 | Danh mục trải nghiệm — `/admin/categories` | Cây root/child, tạo/sửa/ngừng hoạt động; `GET/POST /api/admin/categories`, `PATCH .../{id}`, `.../{id}/deactivate`. Dùng dialog tạo/sửa; không cần một trang cho mỗi thao tác. | Có API | P0 |
| A07 | Dịch vụ / hàng chờ duyệt — `/admin/services` | Danh sách theo status, mở hồ sơ kiểm duyệt; `GET /api/admin/services`. API trả list, chưa có tìm kiếm/phân trang đầy đủ. | Có API cơ bản | P0 |
| A08 | Chi tiết / duyệt dịch vụ và chứng từ — `/admin/services/:serviceId` | Nội dung, ảnh, giá, waiver, an toàn; `PATCH /api/admin/services/{id}/approve`, `.../{id}/reject`; `GET /api/admin/services/{serviceId}/safety-documents`, `PATCH .../{docId}/approve`, `.../{docId}/reject`. Chi tiết listing hiện lấy từ list, chưa có GET admin detail; phải bổ sung để deep link DRAFT/PENDING hoạt động độc lập. | Một phần | P0 |
| A09 | Cảnh báo thời tiết / an toàn — `/admin/weather-alerts` | `GET /api/admin/weather-alerts`; `POST /api/admin/weather-alerts/{evaluationId}/resolve` với CANCEL_AND_REFUND hoặc DISMISSED. CANCEL_AND_REFUND tạo yêu cầu hoàn, chưa phải tiền đã về; DISMISSED giữ đánh giá an toàn. Mở A12 để ghi nhận disruption theo sub-order, khách quyết định đổi/hoàn ở C30. | Có API | P1 |
| A10 | Quy tắc an toàn theo danh mục — `/admin/safety-rules` | Xem/sửa ngưỡng thời tiết/biển; `GET /api/admin/category-safety-rules`, `PATCH .../{categoryId}`. | Có API | P1 |

### 5.2. Đơn hàng, tiền, khiếu nại và chi trả

| Mã | Trang / đường dẫn đề xuất | Chức năng và API liên quan | Backend | Ưu tiên |
| --- | --- | --- | --- | --- |
| A11 | Đơn hàng toàn sàn — `/admin/orders` | `GET /api/admin/orders` với status/paymentStatus/vendorId/customerId/fromDate hoặc from/toDate hoặc to và page/size. Master/sub-order liên quan, mở A12; không dùng `GET /api/orders` list CUSTOMER cho toàn sàn. | Có API | P1 |
| A12 | Chi tiết đơn toàn sàn — `/admin/orders/:orderId` | `GET /api/orders/{id}`, `GET /api/bookings/{id}` có quyền admin; `GET /api/orders/{id}/payments`, GET refunds/receipt và admin payment list/detail. `POST /api/admin/sub-orders/{id}/disruptions` với reason/details/Idempotency-Key để ghi nhận đã xác minh; đọc deadline/options đúng contract. Timeline/liên kết vận hành còn cần bổ sung nếu DTO chưa đủ. | Một phần | P1 |
| A13 | Giao dịch thanh toán — `/admin/payments` | `GET /api/admin/payments` phân trang; lọc status/provider/orderId/customerId/vendorId/from/to theo controller. Xem amount/reference/state và mở A14/A12; chưa có thao tác query/capture admin qua UI. | Có API | P1 |
| A14 | Chi tiết giao dịch — `/admin/payments/:paymentId` | `GET /api/admin/payments/{id}`; trạng thái, mã cổng, số tiền và liên kết order. Chưa có API admin gửi lại capture/đối soát thủ công; chỉ cung cấp thao tác đọc đã có. | Có API đọc | P1 |
| A15 | Theo dõi hoàn tiền — `/admin/refunds` | `GET /api/admin/refunds` phân trang; lọc status/reason/subOrderId/orderId/provider/customerId/vendorId/from/to. Refund xử lý theo policy/worker/provider, không thêm nút duyệt tiền mặc định. Mở A16/A12; PENDING không hoàn tất. | Có API đọc | P1 |
| A16 | Chi tiết hoàn tiền / ngoại lệ — `/admin/refunds/:refundId` | `GET /api/admin/refunds/{id}`; nguyên nhân PENDING/FAILED, mã thao tác/cổng và số tiền. Cần contract riêng nếu muốn retry hoặc xác nhận giao dịch ngoại lệ bằng thao tác admin. | Có API đọc; thao tác ngoại lệ thiếu | P1 |
| A17 | Hàng chờ khiếu nại — `/admin/disputes` | Danh sách theo status/reason và phân trang; `GET /api/admin/disputes`. | Có API | P1 |
| A18 | Chi tiết / phân xử khiếu nại — `/admin/disputes/:disputeId` | `GET /api/admin/disputes/{id}` để refresh/deep link; description/evidence, vendorResponse/vendorEvidenceUrls/vendorRespondedAt, order/sub-order. `PATCH /api/admin/disputes/{id}/resolve` với resolution/refundPercentage/adminNote đúng DTO và trạng thái. Kết luận có hoàn phải theo dõi refund riêng. | Có API | P1 |
| A19 | Kỳ đối soát — `/admin/settlements` | Lọc vendor/status/from/to; `GET /api/admin/settlements`; tạo kỳ bằng `POST .../generate`. | Có API | P1 |
| A20 | Chi tiết / chốt kỳ đối soát — `/admin/settlements/:settlementId` | Dòng đơn, khấu trừ/hoa hồng/net; `GET /api/admin/settlements/{id}`, `PATCH .../{id}/finalize`. Form generate có thể là dialog A19. FINALIZED là chốt sổ, chưa phải PAID. | Có API chốt sổ | P1 |
| A21 | Yêu cầu chi trả / thực hiện chi trả — `/admin/payouts` | Danh sách và detail, duyệt/từ chối, xác nhận chuyển tiền/chứng từ. Chưa có API payout admin và chưa có endpoint chuyển settlement sang PAID theo bằng chứng chi trả. | Thiếu API | P2 |

### 5.3. Nội dung, AI và quản trị chung

| Mã | Trang / đường dẫn đề xuất | Chức năng và API liên quan | Backend | Ưu tiên |
| --- | --- | --- | --- | --- |
| A22 | Quản lý đánh giá — `/admin/reviews` | `GET /api/admin/reviews` với serviceId/vendorId/isFlagged/isVisible/page; `PATCH /api/admin/reviews/{id}/visibility` với isVisible/note. Hiển thị nguồn sub-order, báo cáo/reply; ẩn/hiện có tác động rating theo backend, AI assessment không tự moderation. | Có API | P1 |
| A23 | Khuyến mãi toàn sàn — `/admin/promotions` | GET list/detail, POST create, PATCH update qua /api/admin/discount-codes; lọc code/scope/vendorId/isActive. Scope/sponsor/vendor/service, FIXED/PERCENTAGE, min/cap, quotas/date/active; phân biệt tài trợ sàn/vendor và voucher cá nhân nguồn SYSTEM_FAULT. | Có API | P1 |
| A24 | Thống kê / báo cáo — `/admin/reports` | `GET /api/admin/reports/revenue`, GET bookings/vendors/export. from/to, groupBy day/week/quarter/year, vendorId; doanh thu/booking/vendor diagnostics/CSV theo DTO. Không có groupBy month hoặc generic export tùy ý; KPI/netPayout không đồng nghĩa payout thực tế. | Có API | P1 |
| A25 | Hàng chờ đánh giá AI — `/admin/ai/assessment-cases` | Case văn bản và rủi ro giao dịch, lọc status/limit; `GET /api/admin/ai/assessment-cases`. Không hiển thị nhãn AI như kết luận vi phạm/gian lận đã được xác nhận. | Có API | P1 |
| A26 | Chi tiết / xử lý case AI — `/admin/ai/assessment-cases/:caseId` | Evidence, model/rubric, tín hiệu backend, note; `POST /api/admin/ai/assessment-cases/{id}/resolve`. Tạo risk case từ order bằng `POST /api/admin/ai/risk-cases`; rà văn bản qua `/api/ai/content-assessments`. Thiếu GET case detail độc lập, hiện đọc từ queue. Resolve không khóa user hay sửa tiền/publication. | Một phần | P1 |
| A27 | Thông báo admin — `/admin/notifications` | GET notifications, PATCH read/read-all, GET unread-count cho user admin; badge và deep link đúng quyền. Soạn/broadcast notification và push device token chưa có contract. | Một phần: in-app có API | P1 |
| A28 | Nhật ký kiểm toán — `/admin/audit-logs` | Danh sách có phân trang; `GET /api/admin/audit-logs`. Bộ lọc actor/action/thời gian và export cần bổ sung. | Có API cơ bản | P1 |
| A29 | Chi tiết nhật ký — `/admin/audit-logs/:logId` | `GET /api/admin/audit-logs/{id}`; đối tượng, người thao tác, thời điểm và nội dung đã lưu. Có thể dùng drawer A28. | Có API | P1 |
| A30 | Cấu hình hệ thống — `/admin/settings` | Hoa hồng, policy, thanh toán/thông báo và tham số vận hành có thể chỉnh qua UI sau khi thiết kế contract. Hiện chủ yếu cấu hình server; không có API settings CRUD. Quy tắc thời tiết đã có trang A10 riêng. | Thiếu API | P2 |
| A31 | Tài khoản admin / bảo mật — `/admin/account` | `GET/PATCH /api/users/me`, `POST /api/users/me/change-password`, locale và logout. Dùng tab hồ sơ/bảo mật, không cần trang mới cho mọi thao tác. | Có API | P1 |
| A32 | Hàng đợi hỗ trợ thủ công — `/admin/support-requests` | `GET /api/admin/support/requests` với status/limit (1–100), mặc định WAITING_REVIEW/50. Phân biệt support request với dispute và risk case; hiển thị kind/order/desired target, nhận xử lý qua A33. API là queue list, chưa có pagination server. | Có API | P1 |
| A33 | Chi tiết / xử lý hỗ trợ — `/admin/support-requests/:requestId` | `GET /api/admin/support/requests/{id}`; `POST /api/admin/support/requests/{id}/handle` với expectedVersion/status/responseNote. Chuyển IN_REVIEW/RESOLVED/DECLINED hợp lệ, xử lý conflict; phản hồi khách. Không thể dùng RESOLVED để tự đổi slot, giữ tồn hoặc xác nhận tiền đã hoàn. | Có API | P1 |

**Điều hướng web admin:** Tổng quan; Người dùng; Vendor; Dịch vụ/danh mục; An toàn; Đơn hàng; Thanh toán/hoàn tiền; Khiếu nại; Đối soát/chi trả; Đánh giá; Khuyến mãi; AI; Hỗ trợ thủ công; Báo cáo; Nhật ký; Cấu hình; Tài khoản.

## 6. Mobile — Khách hàng và Vendor

Mobile dùng cùng API, ID, state và kiểm tra quyền với Web. Các mã G/C/V trong bảng là tham chiếu đầy đủ tới chức năng/API ở mục 2–4; cột backend/ưu tiên là mức tối thiểu cho cả màn hình. Không có API Mobile riêng. Tab/bottom sheet vẫn phải có loading, validation, retry và các trạng thái nghiệp vụ; không cắt bỏ bước waiver, proposal hoặc xác nhận chỉ vì màn hình nhỏ.

### 6.1. App khách hàng — mobile/

| Mã | Màn hình cần có | Nội dung / đối chiếu Web | Backend | Ưu tiên |
| --- | --- | --- | --- | --- |
| MC01 | Khởi động / phục hồi phiên | G06; khởi động đọc phiên/cookie jar, refresh, kiểm tra role và phục hồi deep link sau đăng nhập. Không vào màn dữ liệu riêng tư khi chưa xác định phiên. | Client + API auth | P0 |
| MC02 | Đăng nhập / đăng ký / Google | G01; đăng ký khách hàng và chuyển sang xác minh email. | Có API | P0 |
| MC03 | Xác minh email OTP | G02; gửi lại mã và hiển thị giới hạn thử. | Có API | P0 |
| MC04 | Quên / đặt lại mật khẩu | G03; flow hai bước có thể cùng screen. | Có API | P0 |
| MC05 | Trang chủ | C01; categories/weather/recently viewed/gợi ý. Feedback theo recommendationId và consent MC35; xử lý AI cần bổ sung dữ liệu/không khả dụng. | Có API | P0 |
| MC06 | Tìm kiếm / khám phá gần tôi | C02; filter sheet ngày/giờ/nhóm/rating/vendor/sort/giá/vị trí, phân trang; smart search/nearby và feedback gợi ý. Xin quyền vị trí khi dùng tính năng. | Có API | P0 |
| MC07 | Chi tiết trải nghiệm / chọn ca | C03; nội dung, vendor công khai, waiver/version/policy, review thật; sheet option/slot/quantity/participants/allowSplit, revalidate availability và safety. | Có API | P0 |
| MC08 | So sánh dịch vụ | C04; chọn nhiều dịch vụ, xem kết quả phù hợp màn hình nhỏ. | Có API | P1 |
| MC09 | Yêu thích | C05. | Có API | P1 |
| MC10 | Giỏ trải nghiệm | C06; nhiều item, sửa lựa chọn, kiểm tra availability. | Client/tĩnh + API | P0 |
| MC11 | Checkout / giữ chỗ | C07; holdExpiresAt countdown, discount preview và giá order cuối; chấp thuận waiver từng sub-order rồi intent. 409 waiver/version đổi phải quay lại nội dung đúng; mở cổng bên ngoài. | Có API | P0 |
| MC12 | Kết quả thanh toán / quay lại app | C08; return/deep link hoặc browser callback → đọc state backend, capture PayPal đúng contract; đang chờ/thành công/thất bại/hết hạn. Phục hồi checkout/waiver, retry có idempotency. | Có API | P0 |
| MC13 | Đơn hàng / booking của tôi | C09; tab trạng thái và phục hồi đơn đang chờ thanh toán. | Có API | P0 |
| MC14 | Chi tiết đơn / sub-order | C10; master/sub-order, payment/refund, waiver, QR, JSON/PDF receipt để tải/chia sẻ; điểm vào đổi lịch/disruption, chat, dispute/review và support theo state. | Một phần | P0 |
| MC15 | Vé điện tử QR | C11; QR theo subOrderId, hiệu lực/state check-in. Sau đổi slot phải tạo QR mới; ảnh vé offline không chứng minh hiệu lực hiện tại. | Có API | P0 |
| MC16 | Xem trước hủy / xác nhận hủy | C12; sheet preview theo policy snapshot và xác nhận đúng lệnh. Full refund do disruption đã xác minh là nhánh riêng; không tự chọn lý do để nâng mức hoàn. | Có API | P1 |
| MC17 | Theo dõi hoàn tiền | C13; list refund với amount/reason/status; PENDING/REFUND_REQUESTED chưa phải tiền đã về, đọc lại trạng thái provider qua backend. | Có API | P1 |
| MC18 | Khiếu nại của tôi | C14; `GET /api/disputes` của khách, page và status; mở MC20 theo disputeId, MC19 từ đơn. | Có API | P1 |
| MC19 | Tạo khiếu nại | C15; chọn sub-order đúng eligibility, upload evidence multipart rồi gửi URLs khi create. Xin ảnh/camera theo thao tác; xử lý upload lỗi và state không hợp lệ. | Có API | P1 |
| MC20 | Chi tiết / bổ sung khiếu nại | C16; GET detail có phản hồi/chứng cứ vendor và kết luận admin; deep link theo id. Bổ sung evidence/thread sau tạo chưa có API, cần hoàn thiện riêng. | Một phần | P1 |
| MC21 | Viết / xem đánh giá của tôi | C17; tạo/sửa/flag review đã có, chỉ mở form khi sub-order COMPLETED. List/detail review của tôi, media upload và bước hoàn tất sau CHECKED_IN còn thiếu. | Một phần | P1 |
| MC22 | Trợ lý AI | C18; chat AI/history theo conversationId và card confirm; backend giữ chỗ khi xác nhận hợp lệ. Chat người dùng là MC26/MC27; list mọi conversation AI còn thiếu. | Một phần | P1 |
| MC23 | Danh sách / tạo lịch trình | C19; danh sách/page, form context, preview alternative/tradeOffs/TTL, save DRAFT. Chọn lịch và sang MC24 để accept; form không tự giữ chỗ. | Có API | P1 |
| MC24 | Chi tiết / replan lịch trình | C20; lifecycle/version, accept/archive, proposal PENDING/diff/accept/reject và revisions. Xử lý STALE, conflict và nguồn thay đổi; notification proposal cần itineraryId để khôi phục. | Một phần | P1 |
| MC25 | Hỗ trợ đơn hàng | C21; AI giải thích đơn → preview/create HANDOFF hoặc CHANGE_REQUEST với explicit confirmation và key. Mở MC36/MC37 để theo dõi; tạo ticket không thay booking. | Có API | P1 |
| MC26 | Danh sách hội thoại khách–vendor | C22; list/create conversation theo masterOrderId/vendorId, lastMessage/unreadCount. Hiện chỉ liên hệ theo đơn; chọn đúng vendor cho đơn nhiều dịch vụ. | Một phần | P1 |
| MC27 | Chat với vendor | C23; send/get messages, cursor polling/read receipt/reconnect và attachmentUrl. Upload attachment riêng còn thiếu; không dùng assistant API thay chat người dùng. | Một phần | P1 |
| MC28 | Thông báo | C24; notification list/read/read-all/unread-count, badge và related entity deep link. Push còn thiếu; proposal AI cần metadata/tra parent để mở sau cold start. | Một phần | P1 |
| MC29 | Tài khoản | C25; profile và menu đơn/vé, wishlist/lịch trình, MC35 sở thích, MC36 hỗ trợ, MC32 voucher, settings và logout. | Có API | P0 |
| MC30 | Sửa hồ sơ / đổi mật khẩu | C26/C27; hai màn hoặc hai tab, session sau đổi mật khẩu phải được xử lý. | Một phần | P1 |
| MC31 | Hồ sơ vendor công khai | C28; public vendor profile/services, mở từng service/review; chưa có list review aggregate của vendor. | Một phần | P1 |
| MC32 | Ưu đãi / áp dụng voucher | C29; ví voucher cá nhân/replacement, used/expired/recipient/source; sheet áp mã trong MC11. Public campaign feed chưa có contract. | Một phần | P1 |
| MC33 | Đổi lịch thực tế | C30; chọn sub-order → options/eligibility/deadline/fullRefundAvailable → confirm với version/key hoặc disruption-refund. Sau đổi đọc lại order/slot và cấp QR mới; khác AI replan. | Có API | P1 |
| MC34 | Cài đặt / chính sách / quyền thiết bị | G04/G05/G06; VI/EN, policy đúng version, quyền vị trí/camera/ảnh, deep link, offline/expired session/retry. Điểm vào MC35; xin quyền khi người dùng mở tác vụ. | Một phần | P1 |
| MC35 | Sở thích / cá nhân hóa AI | C31; GET/PUT preferences, enabled consent/interests/exclusions/expectedVersion. Hiển thị saving/saved/conflict, tải lại trước khi ghi đè. | Có API | P1 |
| MC36 | Yêu cầu hỗ trợ của tôi | C32; list request của tôi tối đa theo contract, kind/status/order, tạo từ MC25; mở MC37. Không hiển thị tổng phân trang server khi API chưa trả. | Có API | P1 |
| MC37 | Chi tiết / rút yêu cầu hỗ trợ | C33; request detail/responseNote/version, cancel hợp lệ và mở lại order. RESOLVED không chứng minh đổi slot/hoàn tiền. | Có API | P1 |

**Bottom navigation khách:** Trang chủ — Khám phá — Đơn hàng — Tin nhắn/AI — Tài khoản. Thông báo/yêu thích có điểm vào rõ ràng; sở thích, voucher và yêu cầu hỗ trợ nằm trong Tài khoản. Đề xuất tách điểm vào AI và chat người dùng theo chức năng dù có thể chung navigation.

### 6.2. App vendor — vendor_mobile/

| Mã | Màn hình cần có | Nội dung / đối chiếu Web | Backend | Ưu tiên |
| --- | --- | --- | --- | --- |
| MV01 | Khởi động / kiểm tra phiên và role | G06; phục hồi phiên/claims, kiểm tra role và vendor profile. PENDING/REJECTED mở MV06; chỉ mở chức năng vận hành khi quyền/state cho phép. | Client + API auth | P0 |
| MV02 | Đăng nhập / tạo tài khoản / Google | G01; tài khoản mới không tự có VENDOR, chuyển sang MV05 sau flow đăng ký. | Có API | P0 |
| MV03 | Xác minh email | G02. | Có API | P0 |
| MV04 | Quên / đặt lại mật khẩu | G03. | Có API | P0 |
| MV05 | Đăng ký vendor | V02; thông tin doanh nghiệp và ngân hàng, làm mới quyền sau đổi role. | Có API | P0 |
| MV06 | Giấy tờ / trạng thái xác minh | V03; upload/list BUSINESS_LICENSE/SAFETY_CERT và trạng thái. Duyệt documents và resubmit còn thiếu backend; không coi onboarding hoàn chỉnh chỉ vì upload thành công. | Một phần | P0 |
| MV07 | Tổng quan vận hành | V01; GET vendor dashboard cho kỳ from/to, alerts và nút QR; mở MV32 reports/MV23 settlements. KPI/netPayout chưa xác nhận tiền đã chuyển. | Có API | P1 |
| MV08 | Hồ sơ doanh nghiệp / ngân hàng | V04; dữ liệu doanh nghiệp tách hồ sơ người đăng nhập. | Có API | P0 |
| MV09 | Danh sách dịch vụ | V05; trạng thái, pause/resume, gửi duyệt và mở editor. | Có API | P0 |
| MV10 | Tạo / sửa dịch vụ | V06; nội dung, option/giá, ảnh, waiver và chứng từ. Dùng các bước hoặc tab, không cắt bớt option/tồn khi lên mobile. | Một phần | P0 |
| MV11 | Chi tiết / kết quả duyệt dịch vụ | V07; detail nội dung/ảnh/options/slot/state/rejection và tiến trình gửi duyệt. Safety docs xem lại sau reload cần MV12/contract còn thiếu. | Một phần | P0 |
| MV12 | Chứng từ an toàn dịch vụ | V06/V07; upload safety documents, preview/trạng thái/lý do từ chối. GET vendor safety-documents còn thiếu để phục hồi dữ liệu độc lập sau reload. | Một phần | P0 |
| MV13 | Lịch / tồn chỗ | V08; lịch theo dịch vụ, booked/held/khả dụng. | Có API | P0 |
| MV14 | Tạo / cấu hình ca | V09; PERSON_LIMIT/SHARED_CAPACITY_UNITS, units, đóng/mở và bảo vệ tồn đã cam kết. | Có API | P0 |
| MV15 | Đơn / booking item | V10; bộ lọc đúng API và phân trang. | Có API | P0 |
| MV16 | Chi tiết đơn / từ chối item | V11; detail/khách/liên kết ID còn thiếu; reject dùng bookingItemId. Gửi reschedule proposal theo subOrderId, chọn target từ slots của vendor; không gọi customer reschedule-options. | Một phần | P0 |
| MV17 | Quét QR / kết quả check-in | V12; camera/nhập token, check-in window/expiry/used/wrong vendor/state, kết quả CHECKED_IN. Bước COMPLETED để review chưa có endpoint hoàn tất. | Có API | P0 |
| MV18 | Chi tiết hoàn tiền liên quan | V13; refund phần vendor và ảnh hưởng doanh thu; cần API đọc đúng ownership. | Thiếu API | P1 |
| MV19 | Danh sách hội thoại | V14; conversation list/create theo master order, lastMessage/unreadCount; chọn đúng order/vendor. | Có API | P1 |
| MV20 | Chat với khách | V15; get/send messages, afterSequence polling/read receipt và reconnect. Attachment upload riêng chưa có. | Một phần | P1 |
| MV21 | Thông báo / cảnh báo an toàn | V16; in-app list/read/read-all/unread-count và mở service/slot/order đúng role. Push và queue weather vendor riêng còn thiếu. | Một phần | P1 |
| MV22 | Đánh giá / phản hồi | V17; vendor review list/reply/flag đã có; rating/comment/images/reply. Service filter/media upload và completion flow vẫn có giới hạn. | Một phần | P1 |
| MV23 | Đối soát / doanh thu | V18; settlement list/filter/amount/state, mở MV24 và MV32 báo cáo. FINALIZED/net không phải tiền đã nhận. | Có API | P1 |
| MV24 | Chi tiết đối soát | V19. | Có API | P1 |
| MV25 | Danh sách / tạo yêu cầu chi trả | V20; cần API payout. | Thiếu API | P2 |
| MV26 | Chi tiết yêu cầu chi trả | V21; cần API payout/status/chứng từ. | Thiếu API | P2 |
| MV27 | Khiếu nại liên quan | V22; vendor dispute list/status/page đã có, mở MV28; chưa hiển thị hạn phản hồi giả khi DTO không có deadline. | Một phần | P1 |
| MV28 | Chi tiết / gửi giải trình | V23; detail/vendor response/evidence upload và kết luận đã có. Đọc lại sau gửi, xử lý state/ownership conflict. | Có API | P1 |
| MV29 | Voucher / khuyến mãi | V24; list/detail/create/update/active discount codes theo vendor; scopes/limits/date/sponsor đúng DTO, không có delete riêng. | Có API | P1 |
| MV30 | Tài khoản người vận hành / đổi mật khẩu | V25/V26; hồ sơ user, bảo mật, locale, logout. | Có API | P1 |
| MV31 | Cài đặt / chính sách / trạng thái lỗi | G04/G05/G06; VI/EN, quyền camera/ảnh, mất mạng, hết phiên, deep link vào đơn/ca. | Một phần | P1 |
| MV32 | Báo cáo kinh doanh vendor | V27; revenue/bookings theo from/to/groupBy và export CSV để tải/chia sẻ. Backend lấy vendorId từ phiên, không dùng kỳ settlement thay báo cáo tùy chọn. | Có API | P1 |

**Bottom navigation vendor:** Tổng quan — Dịch vụ — Lịch — Đơn — Tài khoản. QR nổi bật từ Tổng quan/Đơn; Tin nhắn/Thông báo có điểm vào rõ ràng; báo cáo, đối soát/chi trả, dispute/review/voucher nằm trong menu nghiệp vụ.

App khách và app vendor hiện tách riêng. Nếu gộp sau này, vẫn cần hai bộ navigation và contract role tương ứng; tài khoản hiện không có hai role đồng thời. Mobile cần cookie jar cho refresh, lưu phiên theo nền tảng, xử lý browser return/cold start và quyền camera/ảnh/vị trí theo thao tác.

## 7. Các khoảng trống phải hoàn thiện để toàn bộ trang vận hành

Bảng này chỉ giữ **khoảng trống thực sự còn trong backend** hoặc chức năng chưa có đủ contract. Chat/review/discount/report/public vendor/dispute/reschedule/notification read tracking đã có endpoint chính và được mô tả ở các trang tương ứng. Trang mang mức Một phần/Thiếu API vẫn nằm trong danh mục cần có; phải bổ sung backend hoặc giới hạn rõ chức năng trước nghiệm thu.

| Ưu tiên | Khoảng trống | Trang và quyết định cần có |
| --- | --- | --- |
| P0 | Duyệt giấy tờ vendor | V03/A05/MV06: upload lưu PENDING, approve vendor đòi đủ loại giấy tờ APPROVED nhưng inventory chưa có lệnh duyệt documents vendor. Không nhầm với documents an toàn của từng dịch vụ. |
| P0 | Gửi lại vendor bị từ chối | V03/A05/MV06: PATCH profile sửa thông tin, chưa có chuyển REJECTED → PENDING để nộp lại. |
| P0 | Đọc chi tiết vận hành vendor | V11/MV16: thiếu GET theo subOrderId và dữ liệu khách/liên hệ/option/participants, liên kết bookingItemId ↔ subOrderId; không thể deep link hoàn chỉnh bằng list DTO hiện tại. |
| P0 | Đọc lại hồ sơ dịch vụ sau reload | V06/V07/MV12: thiếu GET safety documents của vendor; A08 thiếu GET admin service detail độc lập cho DRAFT/PENDING/REJECTED. |
| P1 | Hoàn tất dịch vụ để review | C17/V11/V12/MC21/MV16/MV17: cần bước chuyển CHECKED_IN → COMPLETED có contract; tạo review đã có nhưng chưa khép kín từ check-in. |
| P1 | Refund detail theo vendor | V13/MV18: chưa có API đọc refund được cô lập theo vendor; không dùng endpoint admin/customer thay thế. |
| P1 | Review của tôi và media | C17/MC21: thiếu đọc list/detail review theo khách và upload ảnh review riêng; V17 lọc service cần contract nếu bắt buộc. |
| P1 | Khách bổ sung dispute đã gửi | C16/MC20: thiếu update/follow-up; V22 chưa có deadline phản hồi trong DTO. |
| P1 | Khôi phục detail assessment case | A26: chưa có GET assessment case theo id; queue/resolve chưa đủ deep link độc lập. |
| P1 | Deep link proposal AI | C20/MC24/C24/MC28: notification có proposalId nhưng endpoint detail/list cần itineraryId; cần metadata parent hoặc tra ngược có quyền để mở sau reload. |
| P2 | Chi trả thực tế | V20/V21/A21/MV25/MV26: chưa có payout API và xác nhận settlement PAID theo bằng chứng chuyển tiền. |
| P2 | Settings vận hành | A30: chưa có settings CRUD có quyền/audit; giữ server config nếu không làm phần này. |
| P2 | Liên hệ trước khi đặt / attachment chat | C03/C22/C23/V15 và mobile: chat hiện gắn order; thêm pre-booking chat hoặc giới hạn đúng scope. Attachment URL có trong message nhưng upload riêng chưa có. |
| P2 | Khuyến mãi công khai / vendor review aggregate | C28/C29: chưa có feed chiến dịch/voucher public hoặc list review tổng theo vendor. Có thể giảm scope về profile, dịch vụ và voucher cá nhân. |
| P2 | Push, broadcast và avatar upload | G04/C24/V16/A27/C26/mobile: chưa có endpoint device registration/broadcast/avatar riêng; in-app read tracking đã có. |
| P2 | Lịch tổng, lịch lặp, chỉnh giờ slot | V08/V09/MV13/MV14: API theo từng dịch vụ; PATCH slot chưa đổi date/start/end; tránh nút chỉnh giờ/lặp lịch chưa có contract. |
| P2 | Công cụ xử lý tiền ngoại lệ | A14/A16: chưa có UI API query/retry/capture/reconcile admin; worker/provider adapter không tự tạo endpoint thao tác cho admin. |

### 7.1. Contract bắt buộc cho Web và Mobile

- **ID/ownership:** bookingId/holdId, bookingItemId, masterOrderId, subOrderId, requestId, itineraryId và proposalId là tài nguyên khác nhau. QR dùng subOrderId; reject vendor dùng bookingItemId; dispute path dùng masterOrderId, body có subOrderId. Master order nhiều vendor phải chọn vendorId khi mở conversation. Get reschedule-options chỉ cho chủ đơn/admin; vendor chọn ca từ API slot của mình.
- **Nhóm/gói và tồn:** SHARED/PER_PERSON dùng quantity là số người; PRIVATE/PER_PACKAGE dùng quantity là số gói và participantsCount theo gói. Nhóm có số khách/gói khác nhau gửi item riêng. allowSplit=false mặc định; chỉ bật khi khách đồng ý. PRIVATE dùng SHARED_CAPACITY_UNITS phù hợp. Slot/capacity phải đọc lại khi tạo hold, không coi giỏ hoặc lịch trình AI là giữ tồn.
- **Hold/payment TTL:** countdown booking lấy holdExpiresAt. Intent có expiresAt riêng; không thay thế hai trường cho nhau. Hết TTL, slot/giá đổi hoặc state không còn payable phải đọc lại rồi cho khách xác nhận bước mới.
- **Giảm giá:** preview nhận bookingId/code, CreateOrderRequest nhận bookingId/discountCode. Order amount là giá cuối; preview không giữ quota hay cam kết áp mã. Voucher cá nhân chỉ cho recipient, có usedCount/quota/validFrom/validTo/source sub-order; phải xử lý hết hạn/hết lượt/sai phạm vi.
- **Waiver:** service hiển thị nội dung/version/ngôn ngữ; order có snapshot từng sub-order. Lưu accepted/version/language qua waiver-acceptance trước tạo intent. 409 WAIVER_ACCEPTANCE_REQUIRED có missing items: mở lại nội dung/version đúng, không tự đánh dấu đã chấp thuận. Checkbox client không thay chứng cứ lưu backend.
- **Thanh toán và return:** VNPAY/PAYPAL được hỗ trợ, MOMO chưa cấu hình dù có webhook. Thanh toán theo master order. Query success/browser return không đủ thành PAID; đọc backend, capture PayPal theo contract và xác nhận booking khi đủ tiền. Admin dùng order payments/admin payments, không gọi endpoint payment chỉ cho CUSTOMER.
- **Idempotency/version:** giữ cùng Idempotency-Key cho một thao tác có cùng payload khi retry do mạng; payload thay đổi dùng thao tác/key mới theo contract. Không tự tạo nhiều order/intent/capture/refund. Itinerary version, preference version, support request version và rescheduleVersion là khóa của các tài nguyên riêng; expectedVersion phải lấy từ response tương ứng. 409 thì đọc lại state trước khi yêu cầu xác nhận lại.
- **Hủy thường:** STANDARD_24H_V2 trả 100% khi còn hơn 24 giờ; đúng 24 giờ hoặc ít hơn là 0%, bao gồm no-show. LEGACY_TIERED_V1 của đơn cũ giữ >48 giờ:100%, 24–48:70%, 2–<24:30%, <2:0%. Hiển thị policy snapshot và cancellation-preview từng đơn/sub-order; không hardcode chính sách mới cho đơn cũ.
- **Disruption/full refund:** WEATHER/VENDOR_FAULT/SYSTEM_FAULT phải theo sự kiện backend đã xác minh. Customer choice đọc decisionDeadline/fullRefundAvailable, chọn reschedule hoặc disruption-refund đúng eligibility. REFUND_REQUESTED và HTTP 202 chỉ chứng minh đã yêu cầu; chỉ refund PROCESSED mới hoàn tất. Refund FAILED có hướng dẫn hỗ trợ; không dùng nút admin mark-success/mark-refunded khi thiếu contract.
- **Reschedule thật:** confirm theo sub-order, targetSlotId/proposalId/expectedVersion và key. Chỉ slot tương lai của cùng service, đúng trạng thái/tồn/an toàn và deadline. Dùng referencePrice/additionalFee/additionalAmountCharged trả về; contract hiện không có checkout riêng thu/hoàn chênh lệch tùy ý. Sau đổi tải lại order/slot/QR; QR cũ bị thu hồi. Đổi lịch AI hoặc xử lý ticket không thay slot booking.
- **Check-in/review/dispute:** check-in trả CHECKED_IN, không tự COMPLETED. Review create chỉ cho COMPLETED thuộc khách; một review/sub-order. Dispute create hiện cho CONFIRMED hoặc COMPLETED, không suy ra mọi trạng thái khác đều được tạo. Bước hoàn tất cần bổ sung; UI không tự ghi state để mở review/dispute.
- **Settlement/report/payout:** DRAFT → FINALIZED là tạo/chốt sổ; PAID cần bước chi trả/bằng chứng riêng. Dashboard netPayout là metric tính toán, chưa xác nhận tiền vào ngân hàng. Report date dùng from/to LocalDate, groupBy day/week/quarter/year theo TimeRange; query order/payment/refund dùng OffsetDateTime theo controller. CSV report khác PDF receipt.
- **Receipt:** owner/admin đọc receipt khi có payment SUCCESS hoặc REFUNDED hợp lệ và order paymentStatus không UNPAID. Client xử lý đơn chưa trả/receipt chưa có; JSON/PDF là biên nhận backend, không suy ra một loại chứng từ khác ngoài contract.
- **An toàn:** backend rule engine quyết định. DISMISSED chỉ đánh dấu xử lý alert, giữ đánh giá meteorological safety. CANCEL_AND_REFUND tạo yêu cầu tiền chờ provider. Snapshot weather có thể thiếu/hết hạn; UI phải hiển thị uncertainty và revalidate khi đặt/đổi lịch.
- **AI lifecycle:** preview alternatives có expiresAt; save thành DRAFT, accept sau revalidation thành ACCEPTED. Nguồn đổi có thể STALE; replan tạo PENDING proposal với diff/baseVersion, cần accept/reject; archive/revisions là thao tác riêng. Job chỉ hoạt động theo cấu hình scheduler/monitoring. AI và accept itinerary không tự tạo booking/payment hoặc giữ tồn.
- **AI consent/feedback:** GET/PUT preferences với enabled/interests/exclusions/expectedVersion. Feedback SHOWN/CLICK/POSITIVE/NEGATIVE gắn recommendationId/serviceId và key; không gửi signal cho service ngoài recommendation. State NEEDS_INPUT/UNAVAILABLE/uncertainty yêu cầu input/fallback thay cho kết quả giả. Assessment resolve không tự sửa tiền/publication/role/safety.
- **Hỗ trợ thủ công:** HANDOFF không mang target đổi lịch; CHANGE_REQUEST cần desiredDate hoặc desiredSlotId, preview eligibility rồi confirmed=true mới create. WAITING_REVIEW/IN_REVIEW/RESOLVED/DECLINED/CANCELLED theo backend; cancel/handle dùng request version. RESOLVED không khẳng định financialActionPerformed hoặc inventoryReserved; hành động booking/tiền cần endpoint nghiệp vụ riêng.
- **Chat:** chỉ participant thuộc master order/vendor được đọc/gửi. afterSequence hoặc after dùng một trong hai; polling cursor phải tiến sau batch trả về, read receipt chỉ cho tin đã nhận. attachmentUrl có trong send nhưng upload riêng còn thiếu; không dùng assistant conversation thay cuộc trò chuyện người dùng.
- **Notification/deep link:** read/read-all/unread-count có API. Xử lý relatedEntityType/relatedEntityId đúng role; auth trước khi mở dữ liệu. AI_ITINERARY_PROPOSAL hiện chỉ có proposalId, endpoint cần itineraryId: cần metadata parent/tra ngược có quyền hoặc quét danh sách để phục hồi; không coi push/cold-start deep link đã khép kín.
- **Phiên/ngôn ngữ:** refresh_token là cookie HttpOnly; Mobile dùng cookie jar, không giả định token trong body. Đổi/reset password thu hồi phiên cũ; vendor role đổi phải phục hồi claims. Locale profile và Accept-Language nhất quán; conversation AI khác locale cần xử lý theo contract, nội dung fallback phải hiển thị rõ.

## 8. Luồng liên trang, thứ tự triển khai và điểm vào

### 8.1. Các luồng phải khép kín xuyên vai trò

1. **Mở bán:** G01/G02 → V02 → V03 → A04/A05 → V06/V08/V09 → A07/A08 → C02/C03. Mobile vendor dùng MV05/MV06/MV10/MV13/MV14. Duyệt documents/resubmit, đọc lại hồ sơ và detail vendor còn là dependency backend P0.
2. **Đặt có giảm giá/waiver:** C03 → C06 → C07 hold → preview code → order → waiver từng sub-order → intent/cổng → C08 backend confirmed → C09/C10 → C11/receipt. Mobile MC07/MC10/MC11/MC12/MC13/MC14/MC15 phải đủ cùng bước; API/QR/giá theo item, không bỏ waiver trên app.
3. **Thực hiện và review:** C11/MC15 → V12/MV17 → CHECKED_IN → bước hoàn tất backend hợp lệ → COMPLETED → C17/MC21 → V17/MV22 → A22. Chưa có bước hoàn tất qua endpoint hiện tại; review là chức năng đã có API nhưng chưa thể nghiệm thu từ mọi vé vừa check-in.
4. **Hủy thường:** C10 → C12 cancellation-preview theo snapshot → một lệnh hủy/refund phù hợp → C13; admin theo dõi A15/A16. Mobile MC14/MC16/MC17. Vendor reject tại V11/MV16 có thể phát sinh refund theo backend, chưa xác nhận tiền đã về ngay.
5. **An toàn/disruption:** C03/C07 xem safety; A09 xử lý alert hoặc A12 xác minh disruption → C24/V16/MC28/MV21 → C10/C30/MC14/MC33 xem deadline/options. V11/MV16 có thể gửi proposal; khách chọn reschedule hoặc disruption-refund → cấp QR mới hoặc C13/MC17. SYSTEM_FAULT có voucher cá nhân tại C29/MC32 theo kết quả backend.
6. **Dispute:** C15/MC19 upload evidence và create theo master/sub-order → C14/C16/MC18/MC20; vendor V22/V23/MV27/MV28 gửi giải trình → admin A17/A18 đọc detail/resolve → A15/A16 và C13 nếu có tiền. Follow-up evidence của khách sau create còn thiếu, không dùng upload độc lập như đã sửa dispute.
7. **AI lịch trình:** C19/MC23 preview → save DRAFT → C20/MC24 accept → nguồn đổi/STALE/proposal → xem diff/accept/reject → revisions/archive. C18/MC22 có thể mở artifact tương ứng; mọi đặt chỗ/tiền vẫn quay về C07/MC11. Deep link proposal cần phục hồi parent theo mục 7.
8. **Cá nhân hóa:** C25/G04/MC29/MC34 → C31/MC35 consent/preferences → C01/C02/MC05/MC06 recommendations/feedback theo id. Preference version khác itinerary version; không tự bật consent khi gửi feedback.
9. **Hỗ trợ thủ công:** C10/C18 → C21 preview/create xác nhận → C32/C33; mobile MC14/MC22/MC25 → MC36/MC37. Admin A32 → A33 IN_REVIEW/RESOLVED/DECLINED và note; khách đọc lại/rút request đúng state. Không thay thao tác tiền/slot bằng việc đóng ticket.
10. **Chat/notification:** từ C10/V11/MC14/MV16 chọn master order và vendor → C22/C23/V14/V15 hoặc MC26/MC27/MV19/MV20. Poll cursor/read receipt/unread; notification C24/V16/A27 hoặc MC28/MV21 mark-read rồi mở tài nguyên đúng quyền.
11. **Tài chính:** V01/V27/MV07/MV32 và A01/A24 xem KPI/report theo kỳ/CSV → A19/A20 generate/finalize → V18/V19/MV23/MV24 đối chiếu → A21/V20/V21/MV25/MV26 payout khi bổ sung contract. Report/settlement/payout là ba loại dữ liệu khác nhau.

### 8.2. Thứ tự triển khai đề xuất

- **Đợt 1 — P0:** auth/OTP/reset/cookie/role guard; onboarding–documents vendor–duyệt; service/category/options/images/safety docs và slot/tồn; public catalog, giỏ/hold; discount preview/order amount/waiver; intent/return/capture; detail order/QR/check-in. Hoàn thiện dependencies backend P0 ở mục 7 cùng giao diện.
- **Đợt 2 — P1:** tích hợp API đã có cho public vendor, chat, dispute hai phía, review/reply/moderation, voucher cá nhân/management, reschedule/disruption/hủy/hoàn, notification read/unread, KPI/report/CSV, settlement, AI lifecycle/preferences/support queue và audit log. Khép kín COMPLETED, review read, detail vendor và deep link; không gọi nhóm này là chưa có API toàn bộ.
- **Đợt 3 — P2:** payout/bằng chứng PAID, settings qua UI, push/device registration/broadcast, media/avatar, pre-booking chat, campaign public/vendor review aggregate, lịch tổng/lặp/chỉnh giờ và công cụ tiền ngoại lệ. Chỉ triển khai khi có contract, quyền và dữ liệu đúng; vẫn cần nếu nghiệm thu toàn bộ phạm vi.

### 8.3. Điều hướng và phục hồi liên kết

- Mỗi vai trò có shell/menu riêng; kiểm tra role/ownership khi mở URL, từ notification, refresh trang hoặc app cold start. Sau login quay lại tác vụ hợp lệ; dữ liệu đã xóa/đổi quyền/hết hạn có trạng thái rõ.
- Detail chính phải lấy bằng id. Với A08/A26/V11 hoặc proposal AI chưa đủ endpoint/parent metadata, ghi dependency và hoàn thiện trước nghiệm thu; không chỉ dựa record đang nằm trong list.
- Web cho phép gộp list/detail hai cột, tab hồ sơ/tài chính/AI; Mobile dùng sheet/wizard. Gộp route không loại bỏ mã chức năng, trạng thái hoặc thao tác trong bảng.
- Callback cổng, refresh/logout, diagnostic RBAC, upload/reorder và từng nút API không cần route riêng. Admin vận hành Web; không thêm app admin vào phạm vi hiện tại.

