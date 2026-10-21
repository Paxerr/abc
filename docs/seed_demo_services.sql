-- DANASEA — SCRIPT SEED DỮ LIỆU DEMO SERVICES
-- Mục đích: Nạp dữ liệu mẫu chuẩn nghiệp vụ cho 6 bộ môn thể thao biển Đà Nẵng
-- Chạy lệnh: docker exec -i pbl6-postgres-1 psql -U danasea_user -d danasea_db < docs/seed_demo_services.sql

BEGIN;

-- =========================================================================
-- 1. BẢNG SERVICES (6 Trải nghiệm chính)
-- =========================================================================
INSERT INTO services (
    id, vendor_id, category_id, name, name_en, slug,
    description, description_en, price, duration_minutes, capacity_per_slot,
    location_name, address, latitude, longitude,
    weather_sensitive, min_wind_kmh, max_wavem,
    avg_rating, rating_count, view_count, status,
    waiver_content, waiver_content_en, waiver_required, waiver_version,
    created_at, updated_at
) VALUES 
-- Tour 1: SUP Mân Thái & Mỹ Khê (cheo-sup-kayak)
(
    'd0000001-0000-0000-0000-000000000001',
    '22222222-2222-2222-2222-222222222221',
    'a0000001-0000-0000-0000-000000000001',
    'Chèo SUP Đón Bình Minh Biển Mỹ Khê & Mân Thái',
    'Sunrise SUP Paddleboarding at My Khe & Man Thai',
    'cheo-sup-don-binh-minh-my-khe-man-thai',
    'Trải nghiệm chèo ván đứng SUP ngắm bình minh trên mặt vịnh Mân Thái tĩnh lặng, kèm huấn luyện viên và chụp ảnh kỷ niệm.',
    'Experience sunrise stand-up paddleboarding in serene Man Thai waters with professional guide and photoshoot included.',
    280000.00, 120, 15,
    'Bãi biển Mân Thái, Sơn Trà', 'Đường Hoàng Sa, Phường Mân Thái, Quận Sơn Trà, TP. Đà Nẵng',
    16.0890, 108.2495,
    true, 5.0, 0.8,
    4.98, 342, 1850, 'PUBLISHED',
    'Khách hàng cam kết đủ sức khỏe tham gia, bắt buộc mặc áo phao và tuân thủ hướng dẫn của huấn luyện viên.',
    'Participants must confirm good health, wear life vests at all times, and follow instructor safety guidance.',
    true, 1,
    NOW(), NOW()
),
-- Tour 2: Lặn Biển San Hô Hòn Sụp - Sơn Trà (lan-ngam-san-ho)
(
    'd0000001-0000-0000-0000-000000000002',
    '22222222-2222-2222-2222-222222222221',
    'a0000001-0000-0000-0000-000000000002',
    'Lặn Bình Khí Khám Phá Rạn San Hô Hòn Sụp Bán Đảo Sơn Trà',
    'Scuba Diving at Hon Sup Coral Reefs - Son Tra Peninsula',
    'lan-binh-khi-san-ho-hon-sup-son-tra',
    'Khám phá hệ sinh thái san hô tự nhiên tại Hòn Sụp cùng huấn luyện viên PADI chuyên nghiệp, trang bị bình dưỡng khí cao cấp.',
    'Explore pristine natural coral reefs at Hon Sup with certified PADI instructors and professional diving gear.',
    550000.00, 210, 10,
    'Hòn Sụp, Bán đảo Sơn Trà', 'Khu vực biển Hòn Sụp, Bán đảo Sơn Trà, TP. Đà Nẵng',
    16.1132, 108.2758,
    true, 0.0, 0.7,
    4.96, 188, 1420, 'PUBLISHED',
    'Khách hàng xác nhận không có tiền sử bệnh tim mạch, huyết áp hoặc hen suyễn; tuân thủ quy chuẩn an toàn lặn biển PADI.',
    'Participants certify no history of cardiac, hypertension or asthma conditions and comply with PADI diving safety rules.',
    true, 1,
    NOW(), NOW()
),
-- Tour 3: Bay Dù Lượn Cano Bãi Biển Mỹ Khê (cano-du-bay)
(
    'd0000001-0000-0000-0000-000000000003',
    '22222222-2222-2222-2222-222222222221',
    'a0000001-0000-0000-0000-000000000003',
    'Bay Dù Lượn Cano Kéo Bãi Biển Mỹ Khê',
    'Tandem Parasailing Flight at My Khe Beach',
    'bay-du-luon-cano-keo-bien-my-khe',
    'Bay lượn trên độ cao 70m ngắm toàn cảnh bãi biển Mỹ Khê và tượng Phật Bà Sơn Trà, cano cao tốc đời mới kéo dù an toàn.',
    'Fly 70m above stunning My Khe Beach with panoramic views of Son Tra coastline towed by high-speed speedboat.',
    500000.00, 60, 20,
    'Bãi biển Mỹ Khê', 'Bãi tắm số 1 Võ Nguyên Giáp, Quận Sơn Trà, TP. Đà Nẵng',
    16.0592, 108.2435,
    true, 10.0, 1.0,
    4.95, 210, 2100, 'PUBLISHED',
    'Tuân thủ quy chuẩn an toàn ASTM F3099, tải trọng tối đa 160kg, không mang vật sắc nhọn hoặc thiết bị rơi rớt khi bay.',
    'Complies with ASTM F3099 safety standards, maximum weight 160kg, no loose items permitted during flight.',
    true, 1,
    NOW(), NOW()
),
-- Tour 4: Mô Tô Nước Jetski Biển Mỹ Khê (mo-to-nuoc-jetski)
(
    'd0000001-0000-0000-0000-000000000004',
    '22222222-2222-2222-2222-222222222221',
    'a0000001-0000-0000-0000-000000000004',
    'Thuê Mô Tô Nước Jetski Cảm Giác Mạnh Vịnh Đà Nẵng',
    'High-Speed Jetski Wave Riding in Da Nang Bay',
    'thue-mo-to-nuoc-jetski-cam-giac-manh',
    'Tự do lái mô tô nước Yamaha công suất lớn rẽ sóng tốc độ cao trong vùng biển tiêu chuẩn an toàn ngoài vùng tắm Mỹ Khê.',
    'Experience high-speed Yamaha watercraft wave riding within designated marine zones outside swimmer boundaries.',
    450000.00, 30, 8,
    'Biển Mỹ Khê - Khu thể thao', 'Bãi biển Võ Nguyên Giáp, Phường Phước Mỹ, Quận Sơn Trà, TP. Đà Nẵng',
    16.0592, 108.2435,
    true, 0.0, 1.0,
    4.92, 165, 1340, 'PUBLISHED',
    'Người lái phải đủ 18 tuổi, mặc áo phao có còi cứu sinh, chỉ chạy trong vùng phao tiêu quy định theo Thông tư 17/2018.',
    'Drivers must be 18+, wear life vests with whistle, and operate strictly inside buoy-marked sport zones.',
    true, 1,
    NOW(), NOW()
),
-- Tour 5: Trượt Phao Chuối Nhóm Bạn (truot-phao-chuoi)
(
    'd0000001-0000-0000-0000-000000000005',
    '22222222-2222-2222-2222-222222222221',
    'a0000001-0000-0000-0000-000000000005',
    'Trượt Phao Chuối Tốc Độ Cao Cho Nhóm Bạn',
    'High-Speed Banana Boat Ride for Group',
    'truot-phao-chuoi-toc-do-cao-nhom-ban',
    'Hoạt động gắn kết nhóm cực kỳ phấn khích: cano kéo phao chuối lướt trên đầu sóng tạo những pha rẽ cua ngoạn mục.',
    'Exciting group water activity: high-speed banana boat rides skimming ocean waves with thrilling spray turns.',
    200000.00, 45, 24,
    'Bãi biển Mỹ Khê', 'Bãi tắm Phạm Văn Đồng, Quận Sơn Trà, TP. Đà Nẵng',
    16.0592, 108.2435,
    true, 0.0, 0.8,
    4.90, 142, 980, 'PUBLISHED',
    'Người tham gia bắt buộc mặc áo phao cài khóa an toàn, giữ chặt tay nắm phao và biết bơi cơ bản.',
    'All riders must fasten life vests, hold secure grip handles firmly, and possess basic water survival skills.',
    true, 1,
    NOW(), NOW()
),
-- Tour 6: Du Thuyền Hoàng Hôn Vịnh Đà Nẵng (du-thuyen-ngam-hoang-hon)
(
    'd0000001-0000-0000-0000-000000000006',
    '22222222-2222-2222-2222-222222222221',
    'a0000001-0000-0000-0000-000000000006',
    'Du Thuyền Hạng Sang Ngắm Hoàng Hôn Vịnh Đà Nẵng',
    'Luxury Sunset Catamaran Yacht Tour',
    'du-thuyen-hang-sang-ngam-hoang-hon-vinh-da-nang',
    'Du ngoạn ngắm hoàng hôn rực rỡ từ bến DHC Marina xuôi dòng sông Hàn ra vịnh, thưởng thức trà chiều, rượu vang và nhạc sống.',
    'Sail into breathtaking sunset waters from DHC Marina out to Da Nang Bay with afternoon tea, wine, and live acoustics.',
    1200000.00, 120, 30,
    'Bến du thuyền DHC Marina', 'Đường Trần Hưng Đạo, Quận Sơn Trà, TP. Đà Nẵng',
    16.0680, 108.2250,
    true, 0.0, 1.2,
    5.00, 96, 2400, 'PUBLISHED',
    'Tuân thủ quy tắc an toàn đường thủy nội địa và hàng hải, có mặt tại bến trước 20 phút giờ khởi hành.',
    'Comply with inland waterway and maritime safety guidelines, arrive at marina dock 20 minutes before departure.',
    true, 1,
    NOW(), NOW()
)
ON CONFLICT (id) DO NOTHING;

-- =========================================================================
-- 2. BẢNG SERVICE_IMAGES (Hình ảnh sắc nét chất lượng cao)
-- =========================================================================
INSERT INTO service_images (id, service_id, url, sort_order, created_at, updated_at) VALUES
-- SUP Images
('d0000002-0000-0000-0000-000000000001', 'd0000001-0000-0000-0000-000000000001', 'https://images.unsplash.com/photo-1502680390469-be75c86b636f?auto=format&fit=crop&w=800&q=80', 1, NOW(), NOW()),
('d0000002-0000-0000-0000-000000000002', 'd0000001-0000-0000-0000-000000000001', 'https://images.unsplash.com/photo-1544551763-8dd44758c2dd?auto=format&fit=crop&w=800&q=80', 2, NOW(), NOW()),

-- Diving Images
('d0000002-0000-0000-0000-000000000003', 'd0000001-0000-0000-0000-000000000002', 'https://images.unsplash.com/photo-1544551763-46a013bb70d5?auto=format&fit=crop&w=800&q=80', 1, NOW(), NOW()),
('d0000002-0000-0000-0000-000000000004', 'd0000001-0000-0000-0000-000000000002', 'https://images.unsplash.com/photo-1682687220063-4742bd7fd538?auto=format&fit=crop&w=800&q=80', 2, NOW(), NOW()),

-- Parasailing Images
('d0000002-0000-0000-0000-000000000005', 'd0000001-0000-0000-0000-000000000003', 'https://images.unsplash.com/photo-1605559424843-9e4c228bf1c2?auto=format&fit=crop&w=800&q=80', 1, NOW(), NOW()),

-- Jetski Images
('d0000002-0000-0000-0000-000000000006', 'd0000001-0000-0000-0000-000000000004', 'https://images.unsplash.com/photo-1569263979104-865ab7cd8d13?auto=format&fit=crop&w=800&q=80', 1, NOW(), NOW()),

-- Banana Boat Images
('d0000002-0000-0000-0000-000000000007', 'd0000001-0000-0000-0000-000000000005', 'https://images.unsplash.com/photo-1519046904884-53103b34b206?auto=format&fit=crop&w=800&q=80', 1, NOW(), NOW()),

-- Yacht Images
('d0000002-0000-0000-0000-000000000008', 'd0000001-0000-0000-0000-000000000006', 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=800&q=80', 1, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- =========================================================================
-- 3. BẢNG SERVICE_OPTIONS (Gói dịch vụ chi tiết)
-- =========================================================================
INSERT INTO service_options (
    id, service_id, name, option_type, pricing_unit, price, max_pax_per_package, benefits, status, created_at, updated_at
) VALUES
('d0000003-0000-0000-0000-000000000001', 'd0000001-0000-0000-0000-000000000001', 'Vé Tiêu Chuẩn (Kèm Hướng Dẫn & Ảnh)', 'SHARED', 'PER_PERSON', 280000.00, 1, 'Ván SUP, áo phao, mái chèo carbon, hướng dẫn viên 15 phút, bộ 10 ảnh kỷ niệm kỹ thuật số.', 'ACTIVE', NOW(), NOW()),
('d0000003-0000-0000-0000-000000000002', 'd0000001-0000-0000-0000-000000000002', 'Lặn Biển Cơ Bản (HLV 1 kèm 2)', 'SHARED', 'PER_PERSON', 550000.00, 1, 'Bộ đồ lặn cao cấp, bình khí, kính lặn, chân vịt, HLV theo sát 100% thời gian.', 'ACTIVE', NOW(), NOW()),
('d0000003-0000-0000-0000-000000000003', 'd0000001-0000-0000-0000-000000000003', 'Bay Đơn Tiêu Chuẩn (1 người)', 'SHARED', 'PER_PERSON', 500000.00, 1, 'Bay 10 phút trên không, cano kéo tốc độ cao, áo phao đạt chuẩn ASTM.', 'ACTIVE', NOW(), NOW()),
('d0000003-0000-0000-0000-000000000004', 'd0000001-0000-0000-0000-000000000004', 'Thuê Jetski 30 Phút Tự Lái', 'SHARED', 'PER_PERSON', 450000.00, 1, 'Mô tô nước Yamaha FX Cruiser, xăng đầy bình, hướng dẫn an toàn 5 phút.', 'ACTIVE', NOW(), NOW()),
('d0000003-0000-0000-0000-000000000005', 'd0000001-0000-0000-0000-000000000005', 'Vé Trượt Phao Chuối Cá Nhân', 'SHARED', 'PER_PERSON', 200000.00, 1, 'Lướt sóng 20 phút cùng nhóm, áo phao chuyên dụng chống va đập.', 'ACTIVE', NOW(), NOW()),
('d0000003-0000-0000-0000-000000000006', 'd0000001-0000-0000-0000-000000000006', 'Vé Du Thuyền Hoàng Hôn VIP', 'SHARED', 'PER_PERSON', 1200000.00, 1, 'Du ngoạn 2 tiếng trên sông Hàn & vịnh Đà Nẵng, tiệc trà, rượu vang, canapé nhẹ.', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- =========================================================================
-- 4. BẢNG SERVICE_SLOTS (Khung giờ hoạt động thực tế: Hôm nay, Ngày mai, Ngày kia)
-- =========================================================================
-- SUP Slots (Sáng sớm & Chiều)
INSERT INTO service_slots (id, service_id, date, start_time, end_time, capacity, booked_count, status, inventory_type, created_at, updated_at) VALUES
('d0000004-0000-0000-0000-000000000001', 'd0000001-0000-0000-0000-000000000001', CURRENT_DATE, '05:00:00', '07:00:00', 15, 3, 'OPEN', 'PERSON_LIMIT', NOW(), NOW()),
('d0000004-0000-0000-0000-000000000002', 'd0000001-0000-0000-0000-000000000001', CURRENT_DATE, '07:30:00', '09:30:00', 15, 1, 'OPEN', 'PERSON_LIMIT', NOW(), NOW()),
('d0000004-0000-0000-0000-000000000003', 'd0000001-0000-0000-0000-000000000001', CURRENT_DATE + 1, '05:00:00', '07:00:00', 15, 0, 'OPEN', 'PERSON_LIMIT', NOW(), NOW()),

-- Diving Slots
('d0000004-0000-0000-0000-000000000004', 'd0000001-0000-0000-0000-000000000002', CURRENT_DATE, '08:00:00', '11:30:00', 10, 2, 'OPEN', 'PERSON_LIMIT', NOW(), NOW()),
('d0000004-0000-0000-0000-000000000005', 'd0000001-0000-0000-0000-000000000002', CURRENT_DATE + 1, '08:00:00', '11:30:00', 10, 0, 'OPEN', 'PERSON_LIMIT', NOW(), NOW()),

-- Parasailing Slots
('d0000004-0000-0000-0000-000000000006', 'd0000001-0000-0000-0000-000000000003', CURRENT_DATE, '08:30:00', '10:30:00', 20, 4, 'OPEN', 'PERSON_LIMIT', NOW(), NOW()),
('d0000004-0000-0000-0000-000000000007', 'd0000001-0000-0000-0000-000000000003', CURRENT_DATE, '15:00:00', '17:00:00', 20, 2, 'OPEN', 'PERSON_LIMIT', NOW(), NOW()),

-- Jetski Slots
('d0000004-0000-0000-0000-000000000008', 'd0000001-0000-0000-0000-000000000004', CURRENT_DATE, '09:00:00', '11:00:00', 8, 1, 'OPEN', 'PERSON_LIMIT', NOW(), NOW()),

-- Banana Boat Slots
('d0000004-0000-0000-0000-000000000009', 'd0000001-0000-0000-0000-000000000005', CURRENT_DATE, '10:00:00', '11:30:00', 24, 6, 'OPEN', 'PERSON_LIMIT', NOW(), NOW()),

-- Yacht Slots (Hoàng hôn)
('d0000004-0000-0000-0000-000000000010', 'd0000001-0000-0000-0000-000000000006', CURRENT_DATE, '16:30:00', '18:30:00', 30, 8, 'OPEN', 'PERSON_LIMIT', NOW(), NOW()),
('d0000004-0000-0000-0000-000000000011', 'd0000001-0000-0000-0000-000000000006', CURRENT_DATE + 1, '16:30:00', '18:30:00', 30, 0, 'OPEN', 'PERSON_LIMIT', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

COMMIT;

-- Kiểm tra kết quả nạp
SELECT count(*) AS total_demo_services FROM services WHERE id::text LIKE 'd0000001%';
