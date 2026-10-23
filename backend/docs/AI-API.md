# Tổng hợp các tính năng AI cho hệ thống DANASEA


DANASEA là sàn thương mại điện tử đa nhà cung cấp chuyên về trải nghiệm du lịch biển tại Đà Nẵng. AI sẽ hỗ trợ 3 nhóm đối tượng: Khách hàng, Nhà cung cấp và Quản trị viên (Admin).

## 1. AI dành cho khách hàng

| Tính năng                | Mô tả ngắn gọn                                                                                   |
| ------------------------ | ------------------------------------------------------------------------------------------------ |
| 1. AI Travel Assistant   | Trợ lý AI tư vấn, giải đáp về dịch vụ, giá, lịch trống, chính sách và đặt trải nghiệm.           |
| 2. AI Smart Search       | Tìm dịch vụ bằng ngôn ngữ tự nhiên thay vì chỉ sử dụng bộ lọc truyền thống.                      |
| 3. AI Recommendation     | Cá nhân hóa gợi ý theo sở thích, ngân sách, số người, vị trí và thời gian.                       |
| 4. AI Itinerary Planner  | Tự động lập lịch trình du lịch biển theo ngân sách, thời gian và dịch vụ thực tế đang còn chỗ.   |
| 5. Weather-Aware AI      | Phân tích dữ liệu thời tiết và kết quả kiểm tra quy tắc an toàn để cảnh báo, gợi ý lịch phù hợp. |
| 6. AI Re-planning        | Đề xuất điều chỉnh lịch trình khi thời tiết thay đổi hoặc dịch vụ bị hủy.                        |
| 7. AI Service Comparison | So sánh nhiều dịch vụ về giá, chất lượng, đánh giá, thời lượng và chính sách.                    |
| 8. AI Review Summary     | Tóm tắt ưu điểm, nhược điểm từ đánh giá thực tế của khách hàng.                                  |
| 9. AI Nearby Discovery   | Đề xuất trải nghiệm gần vị trí hiện tại, phù hợp thời gian và lịch còn trống.                    |
| 10. AI Customer Support  | Tra cứu đơn hàng, giải đáp chính sách, hỗ trợ yêu cầu đổi lịch, hủy hoặc hoàn tiền.              |

## 2. AI dành cho nhà cung cấp (Vendor)

| Tính năng                | Mô tả ngắn gọn                                                                            |
| ------------------------ | ----------------------------------------------------------------------------------------- |
| 11. AI Listing Copilot   | Hỗ trợ tạo tiêu đề, mô tả, điểm nổi bật, FAQ và nội dung giới thiệu dịch vụ.              |
| 12. AI Smart Reply       | Gợi ý câu trả lời tự động cho tin nhắn khách hàng dựa trên thông tin dịch vụ và lịch đặt. |
| 13. AI Review Insights   | Phân tích đánh giá để xác định điểm mạnh, điểm yếu và đề xuất cải thiện dịch vụ.          |
| 14. AI Business Insights | Phân tích doanh thu, số đơn, tỷ lệ hủy, hiệu quả dịch vụ và gợi ý cải thiện kinh doanh.   |

## 3. AI dành cho quản trị viên (Admin)

| Tính năng                        | Mô tả ngắn gọn                                                                                   |
| -------------------------------- | ------------------------------------------------------------------------------------------------ |
| 15. AI Content Moderation        | Phát hiện văn bản dịch vụ và đánh giá có dấu hiệu vi phạm; chuyển admin duyệt.                            |
| 16. AI Fraud Detection           | Phát hiện dấu hiệu bất thường trong giao dịch, tài khoản, nhà cung cấp hoặc đánh giá.            |
| 17. AI Complaint Analysis        | Tự động phân loại, tóm tắt khiếu nại và đề xuất hướng xử lý để admin xem xét.                    |
| 18. AI Provider Quality Analysis | Phân tích chất lượng nhà cung cấp dựa trên đánh giá, khiếu nại, tỷ lệ hủy và hiệu suất vận hành. |

## 4. API đã thực hiện

Hiện có **40 API** trong module AI, đối chiếu với controller và file `danasea-api-tracking.md`. Quyet Small hỗ trợ phân loại, đánh giá văn bản/JSON; CLI Proxy phục vụ hội thoại. Giá, tồn chỗ, quyền sở hữu và trạng thái giao dịch được kiểm tra bằng nghiệp vụ backend.

### 4.1. Trợ lý hội thoại — 4 API

| API | Mô tả ngắn gọn |
| --- | --- |
| `POST /api/assistant/chat` | Gửi tin nhắn cho trợ lý AI; nhận câu trả lời, thẻ dịch vụ, nguồn dữ liệu và hành động đề xuất. |
| `POST /api/assistant/conversations/{id}/confirm` | Xác nhận thẻ đặt dịch vụ để tạo booking hold sau khi kiểm tra lại giá và chỗ trống. |
| `GET /api/assistant/conversations/{id}` | Xem thông tin hội thoại thuộc người dùng hiện tại. |
| `GET /api/assistant/conversations/{id}/history` | Lấy lịch sử tin nhắn trong hội thoại của người dùng. |

### 4.2. Khám phá dịch vụ và cá nhân hóa — 9 API

| API | Mô tả ngắn gọn |
| --- | --- |
| `POST /api/ai/search` | Tìm dịch vụ từ yêu cầu ngôn ngữ tự nhiên và tiêu chí ngân sách, ngày đi, số người. |
| `POST /api/ai/recommendations` | Gợi ý dịch vụ theo nhu cầu chuyến đi và sở thích đã lưu khi người dùng bật cá nhân hóa. |
| `POST /api/ai/nearby` | Tìm trải nghiệm gần tọa độ được cung cấp, theo bán kính và điều kiện chuyến đi. |
| `POST /api/ai/services/compare` | So sánh các dịch vụ được chọn theo giá, đánh giá, chính sách và khả năng đáp ứng nhu cầu. |
| `POST /api/ai/weather` | Đánh giá điều kiện thời tiết và quy tắc an toàn cho dịch vụ, lựa chọn và ca dự kiến. |
| `GET /api/ai/review-summaries/{serviceId}` | Tổng hợp ưu điểm, hạn chế từ các đánh giá hợp lệ của dịch vụ. |
| `GET /api/ai/preferences` | Xem hồ sơ sở thích và trạng thái bật/tắt cá nhân hóa của người dùng. |
| `PUT /api/ai/preferences` | Cập nhật sở thích, hoạt động muốn loại trừ và trạng thái cá nhân hóa. |
| `POST /api/ai/recommendations/{id}/feedback` | Ghi nhận phản hồi của người dùng về dịch vụ trong một lượt gợi ý. |

### 4.3. Lập và quản lý lịch trình — 13 API

| API | Mô tả ngắn gọn |
| --- | --- |
| `POST /api/ai/itineraries/preview` | Xem trước các phương án lịch trình theo thời gian, ngân sách và dịch vụ có thể đáp ứng. |
| `POST /api/ai/itineraries/previews/{previewId}/save` | Lưu phương án được chọn từ bản xem trước thành lịch trình cá nhân. |
| `POST /api/ai/itineraries` | Tạo và lưu lịch trình trực tiếp từ yêu cầu chuyến đi. |
| `GET /api/ai/itineraries` | Liệt kê lịch trình đã lưu của người dùng. |
| `GET /api/ai/itineraries/page` | Liệt kê lịch trình đã lưu theo phân trang `page` và `size`. |
| `GET /api/ai/itineraries/{id}` | Xem chi tiết một lịch trình thuộc người dùng. |
| `POST /api/ai/itineraries/{id}/accept` | Chấp nhận lịch trình sau khi kiểm tra lại dữ liệu hiện tại. |
| `POST /api/ai/itineraries/{id}/archive` | Lưu trữ lịch trình khi người dùng không tiếp tục sử dụng. |
| `POST /api/ai/itineraries/{id}/replan` | Lập phương án thay thế theo yêu cầu mới, thời tiết hoặc dịch vụ/ca bị loại trừ. |
| `GET /api/ai/itineraries/{id}/proposals` | Xem các đề xuất điều chỉnh của lịch trình. |
| `POST /api/ai/itineraries/{id}/proposals/{proposalId}/accept` | Chấp nhận đề xuất điều chỉnh và cập nhật phiên bản lịch trình. |
| `POST /api/ai/itineraries/{id}/proposals/{proposalId}/reject` | Từ chối đề xuất điều chỉnh lịch trình. |
| `GET /api/ai/itineraries/{id}/revisions` | Xem lịch sử các phiên bản và thay đổi của lịch trình. |

Việc lưu hoặc chấp nhận lịch trình ghi nhận kế hoạch của khách. Giữ chỗ và thanh toán được thực hiện qua luồng booking/order/payment.

### 4.4. Hỗ trợ khách hàng — 6 API

| API | Mô tả ngắn gọn |
| --- | --- |
| `POST /api/ai/support` | Tra cứu đơn của khách, phân loại yêu cầu và gợi ý bước xử lý; có thể xem trước chính sách hủy/hoàn tiền. |
| `POST /api/ai/support/requests/preview` | Kiểm tra khả năng gửi yêu cầu đổi lịch hoặc chuyển cho bộ phận hỗ trợ. |
| `POST /api/ai/support/requests` | Tạo yêu cầu đổi lịch hoặc hỗ trợ thủ công sau khi khách xác nhận. |
| `GET /api/ai/support/requests` | Liệt kê các yêu cầu hỗ trợ của người dùng. |
| `GET /api/ai/support/requests/{id}` | Xem chi tiết và tiến độ xử lý yêu cầu hỗ trợ của người dùng. |
| `POST /api/ai/support/requests/{id}/cancel` | Hủy yêu cầu hỗ trợ theo trạng thái và phiên bản hiện tại. |

Yêu cầu đổi lịch được chuyển cho người có thẩm quyền xử lý; thao tác tạo yêu cầu chỉ ghi nhận hồ sơ hỗ trợ.

### 4.5. Phân loại và đánh giá nội dung — 2 API

| API | Mô tả ngắn gọn |
| --- | --- |
| `POST /api/ai/classifications/service` | Gợi ý phân loại dịch vụ dựa trên văn bản mô tả. |
| `POST /api/ai/content-assessments` | Đánh giá văn bản, phát hiện thông tin liên hệ ngoài nền tảng và dấu hiệu vi phạm; lưu hồ sơ để xem xét. |

### 4.6. Quản trị hồ sơ AI và hỗ trợ — 6 API

| API | Mô tả ngắn gọn |
| --- | --- |
| `POST /api/admin/ai/risk-cases` | Phân tích tín hiệu bất thường của đơn/giao dịch và tạo hồ sơ đánh giá rủi ro cho admin. |
| `GET /api/admin/ai/assessment-cases` | Liệt kê hồ sơ đánh giá nội dung và rủi ro theo trạng thái. |
| `POST /api/admin/ai/assessment-cases/{id}/resolve` | Ghi nhận quyết định và ghi chú của admin đối với hồ sơ đánh giá. |
| `GET /api/admin/support/requests` | Xem hàng đợi yêu cầu hỗ trợ theo trạng thái và giới hạn số bản ghi. |
| `GET /api/admin/support/requests/{id}` | Xem chi tiết yêu cầu hỗ trợ để xử lý thủ công. |
| `POST /api/admin/support/requests/{id}/handle` | Cập nhật tiến độ xử lý yêu cầu và phản hồi cho khách. |

Kết quả kiểm duyệt và đánh giá rủi ro là thông tin hỗ trợ admin xem xét; quyết định xuất bản hoặc xử lý giao dịch vẫn theo nghiệp vụ có thẩm quyền.
