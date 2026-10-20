# Kiểm tra trước khi đưa lên GitHub public

**Chưa được coi là PUBLIC_RELEASE_READY.** Folder trên máy chứa đủ dữ liệu; không tự commit/push hoặc loại dữ liệu đã nghiệm thu.

| Nhóm | Kết quả kiểm tra | Xử lý |
|---|---|---|
| 12 User | Chỉ tài khoản fixture `.test`; password_hash=NULL trong JSON; mật khẩu/hash mới sinh khi cài; không token/secret | Có thể trình Leader duyệt |
| 9 bộ ngân hàng | Người dùng xác nhận tự tạo; giữ đủ account number STRING/bank/holder, gắn DEVELOPMENT_SYNTHETIC; không xác nhận sở hữu hoặc cho phép chuyển tiền | Giữ nguyên dữ liệu, công bố kèm nhãn fixture rõ ràng khi Leader duyệt |
| 24 checklist an toàn mới | Nội dung nguyên gốc dành cho đồ án, không tài sản bên thứ ba, không cơ quan cấp/chữ ký/dấu; SAMPLE/PENDING | Có thể trình Leader duyệt; không phải chứng nhận pháp lý/an toàn |
| 18 VendorDocument cũ | Giữ nguyên bytes; template ghi SYNTHETIC TEST DOCUMENT / NOT A VALID LEGAL CERTIFICATE; không tuyên bố đã được duyệt | Leader xác nhận việc công khai fixture; đang được gitignore để tránh thêm nhầm |
| 35 file ảnh dịch vụ / 72 liên kết | Có provenance nguồn khảo sát nhưng **chưa có bằng chứng quyền chia sẻ lại** | PUBLIC_RIGHTS_BLOCKED: cần quyền sử dụng/giấy phép tương ứng hoặc thay thế được người dùng duyệt; không tự bỏ/thay ảnh |
| Source/license ảnh | Nguồn `Data_Chuan_Hoa/DANASEA_LEADER_REVIEW/IMAGES/README.md` ghi `BLOCKED_RIGHTS_PENDING` | Ghi nhận nguồn trong provenance; không coi dẫn nguồn là giấy phép |
| Cấu hình hệ thống | Không đóng gói .env, DB credential, Cloudinary key/secret, JWT secret, token hoặc password hash | Kết quả kiểm secrets nằm trong runtime acceptance |

`.gitignore` bảo vệ hai nhóm media cần review và toàn bộ `runtime/`; các file vẫn hiện diện trên đĩa, nằm trong manifest và bộ cài đầy đủ. **Đừng commit folder thiếu media rồi báo rằng người khác clone là cài đủ được**: cần Leader giải quyết quyền chia sẻ và xác nhận nhóm media trước khi bỏ ignore/công bố bộ hoàn chỉnh. Bàn giao riêng toàn bộ folder trên máy không cần tìm file ở các thư mục nguồn.

Không cấp license thay cho chủ sở hữu ảnh, không coi dữ liệu có mặt trên website là quyền sao chép, không tự xóa hoặc đổi ảnh đã nghiệm thu để vượt kiểm tra public.
