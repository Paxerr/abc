export interface FlashSaleExperience {
  id: string;
  title: string;
  titleEn: string;
  category: string;
  categoryEn: string;
  location: string;
  locationEn: string;
  duration: string;
  durationEn: string;
  originalPrice: number;
  salePrice: number;
  discountPercent: number;
  rating: number;
  reviewCount: number;
  slotsLeft: number;
  totalSlots: number;
  badge?: string;
  badgeEn?: string;
  imageUrl: string;
}

export interface ActivityCardData {
  id: string;
  title: string;
  titleEn: string;
  category: string;
  categoryEn: string;
  location: string;
  duration: string;
  durationEn: string;
  price: number;
  rating: number;
  reviewCount: number;
  badge?: string;
  badgeEn?: string;
  imageUrl: string;
}

export interface CoastalBayData {
  id: string;
  name: string;
  nameEn: string;
  tagline: string;
  taglineEn: string;
  bestTime: string;
  bestTimeEn: string;
  waterDepth: string;
  tideCondition: string;
  tideConditionEn: string;
  imageUrl: string;
  tourCount: number;
}

export interface OceanSportShowcase {
  id: string;
  index: string;
  name: string;
  nameEn: string;
  level: string;
  levelEn: string;
  description: string;
  descriptionEn: string;
  highlights: string[];
  highlightsEn: string[];
  startingPrice: number;
  imageUrl: string;
}

// 1. Flash Sale Items
export const FLASH_SALE_EXPERIENCES: FlashSaleExperience[] = [
  {
    id: 'd0000001-0000-0000-0000-000000000001',
    title: 'Chèo SUP Bình Minh Bãi Biển Mỹ Khê + Nhiếp Ảnh',
    titleEn: 'Sunrise SUP Paddleboarding at My Khe + Photoshoot',
    category: 'Chèo SUP',
    categoryEn: 'Stand-up Paddleboard',
    location: 'Biển Mỹ Khê',
    locationEn: 'My Khe Beach',
    duration: '2 giờ',
    durationEn: '2 hours',
    originalPrice: 420000,
    salePrice: 280000,
    discountPercent: 33,
    rating: 4.98,
    reviewCount: 324,
    slotsLeft: 3,
    totalSlots: 15,
    imageUrl: 'https://images.unsplash.com/photo-1502680390469-be75c86b636f?auto=format&fit=crop&w=800&q=80',
  },
  {
    id: 'd0000001-0000-0000-0000-000000000002',
    title: 'Lặn Bình Khí Khám Phá Rạn San Hô Hòn Sụp Bán Đảo Sơn Trà',
    titleEn: 'Scuba Diving at Hon Sup Coral Reefs - Son Tra Peninsula',
    category: 'Lặn biển Scuba',
    categoryEn: 'Scuba Diving',
    location: 'Sơn Trà, Đà Nẵng',
    locationEn: 'Son Tra Peninsula',
    duration: '3.5 giờ',
    durationEn: '3.5 hours',
    originalPrice: 780000,
    salePrice: 550000,
    discountPercent: 30,
    rating: 4.96,
    reviewCount: 188,
    slotsLeft: 2,
    totalSlots: 10,
    imageUrl: 'https://images.unsplash.com/photo-1544551763-46a013bb70d5?auto=format&fit=crop&w=800&q=80',
  },
  {
    id: 'd0000001-0000-0000-0000-000000000003',
    title: 'Bay Dù Lượn Trên Vịnh Biển Ngắm Bán Đảo Sơn Trà',
    titleEn: 'Tandem Paragliding Over Da Nang Coastline & Son Tra',
    category: 'Thể thao cảm giác mạnh',
    categoryEn: 'Thrill & Flight',
    location: 'Đỉnh Bàn Cờ, Sơn Trà',
    locationEn: 'Son Tra Summit',
    duration: '1.5 giờ',
    durationEn: '1.5 hours',
    originalPrice: 1650000,
    salePrice: 1290000,
    discountPercent: 22,
    rating: 5.0,
    reviewCount: 96,
    slotsLeft: 1,
    totalSlots: 6,
    imageUrl: 'https://images.unsplash.com/photo-1605559424843-9e4c228bf1c2?auto=format&fit=crop&w=800&q=80',
  },
  {
    id: 'd0000001-0000-0000-0000-000000000004',
    title: 'Tour Cano Cao Tốc Lặn Ngắm San Hô Bãi Rạng & Câu Cá',
    titleEn: 'Speedboat Snorkeling & Island Fishing at Bai Rang',
    category: 'Cano & Lặn biển',
    categoryEn: 'Speedboat & Snorkel',
    location: 'Bãi Rạng - Nam Thọ',
    locationEn: 'Bai Rang Bay',
    duration: '4 giờ',
    durationEn: '4 hours',
    originalPrice: 650000,
    salePrice: 480000,
    discountPercent: 26,
    rating: 4.92,
    reviewCount: 145,
    slotsLeft: 4,
    totalSlots: 12,
    imageUrl: 'https://images.unsplash.com/photo-1569263979104-865ab7cd8d13?auto=format&fit=crop&w=800&q=80',
  },
];

// 2. Activity Carousels by Category (Airbnb Experiences Style)

// Category A: Chèo SUP & Kayak
export const SUP_KAYAK_EXPERIENCES: ActivityCardData[] = [
  {
    id: 'd0000001-0000-0000-0000-000000000001',
    title: 'Chèo SUP đón bình minh rực rỡ tại biển Mỹ Khê cùng nhiếp ảnh gia',
    titleEn: 'Sunrise SUP Paddleboarding at My Khe with Professional Photographer',
    category: 'Chèo SUP',
    categoryEn: 'Paddleboard',
    location: 'Biển Mỹ Khê',
    duration: '2 giờ',
    durationEn: '2 hours',
    price: 280000,
    rating: 4.98,
    reviewCount: 342,
    imageUrl: 'https://images.unsplash.com/photo-1502680390469-be75c86b636f?auto=format&fit=crop&w=800&q=80',
  },
  {
    id: 'd0000001-0000-0000-0000-000000000011',
    title: 'Tour chèo Kayak xuyên rừng dừa nước & luồn lách vịnh Mân Thái',
    titleEn: 'Sea Kayaking Adventure Through Man Thai Coastal Waters',
    category: 'Kayak biển',
    categoryEn: 'Sea Kayak',
    location: 'Mân Thái, Sơn Trà',
    duration: '2.5 giờ',
    durationEn: '2.5 hours',
    price: 320000,
    rating: 4.95,
    reviewCount: 178,
    imageUrl: 'https://images.unsplash.com/photo-1544551763-8dd44758c2dd?auto=format&fit=crop&w=800&q=80',
  },
  {
    id: 'd0000001-0000-0000-0000-000000000012',
    title: 'Chèo SUP hoàng hôn thư giãn kèm mocktail & tiệc nhẹ bãi biển',
    titleEn: 'Sunset SUP Relaxation with Beach Mocktail & Light Snack',
    category: 'Chèo SUP',
    categoryEn: 'Paddleboard',
    location: 'Bãi biển Non Nước',
    duration: '2 giờ',
    durationEn: '2 hours',
    price: 350000,
    rating: 5.0,
    reviewCount: 215,
    imageUrl: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=800&q=80',
  },
  {
    id: 'd0000001-0000-0000-0000-000000000001',
    title: 'Khóa học làm chủ kỹ thuật chèo SUP cơ bản & an toàn cứu sinh biển',
    titleEn: 'Beginner SUP Coaching & Marine Safety Fundamentals',
    category: 'Khóa học thể thao',
    categoryEn: 'Training Course',
    location: 'Công viên Biển Đông',
    duration: '1.5 giờ',
    durationEn: '1.5 hours',
    price: 250000,
    rating: 4.91,
    reviewCount: 88,
    imageUrl: 'https://images.unsplash.com/photo-1517400508447-f8dd518b86db?auto=format&fit=crop&w=800&q=80',
  },
  {
    id: 'd0000001-0000-0000-0000-000000000011',
    title: 'Kayak đôi khám phá vách đá Mũi Nghê và rạn san hô ngầm nguyên sinh',
    titleEn: 'Tandem Kayak to Mui Nghe Cliffs & Pristine Underwater Reefs',
    category: 'Kayak biển',
    categoryEn: 'Sea Kayak',
    location: 'Mũi Nghê, Sơn Trà',
    duration: '3 giờ',
    durationEn: '3 hours',
    price: 450000,
    rating: 4.97,
    reviewCount: 129,
    imageUrl: 'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?auto=format&fit=crop&w=800&q=80',
  },
  {
    id: 'd0000001-0000-0000-0000-000000000012',
    title: 'Combo SUP Yoga buổi sớm trên mặt biển phẳng lặng Mỹ Khê',
    titleEn: 'Morning Ocean SUP Yoga on Calm Mirrored Waters',
    category: 'SUP Thư giãn',
    categoryEn: 'Wellness SUP',
    location: 'Bãi biển Phạm Văn Đồng',
    duration: '1.5 giờ',
    durationEn: '1.5 hours',
    price: 300000,
    rating: 4.99,
    reviewCount: 164,
    imageUrl: 'https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=800&q=80',
  },
];

// Category B: Lặn biển & San hô Sơn Trà
export const DIVING_EXPERIENCES: ActivityCardData[] = [
  {
    id: 'd0000001-0000-0000-0000-000000000002',
    title: 'Lặn ống thở Snorkeling ngắm san hô Bãi Sụp - Nước trong vắt 10m',
    titleEn: 'Hon Sup Snorkeling Adventure - 10m High Visibility Coral Waters',
    category: 'Lặn ống thở',
    categoryEn: 'Snorkeling',
    location: 'Bãi Sụp, Sơn Trà',
    duration: '3 giờ',
    durationEn: '3 hours',
    price: 450000,
    rating: 4.96,
    reviewCount: 280,
    imageUrl: 'https://images.unsplash.com/photo-1544551763-77ef2d0cfc6c?auto=format&fit=crop&w=800&q=80',
  },
  {
    id: 'd0000001-0000-0000-0000-000000000021',
    title: 'Lặn bình khí Scuba Diving cùng Master Dive PADI - Kèm video Go Pro 4K',
    titleEn: 'PADI Certified Scuba Dive with 4K Underwater GoPro Footage',
    category: 'Lặn bình khí',
    categoryEn: 'Scuba Diving',
    location: 'Mũi Nghê, Sơn Trà',
    duration: '3.5 giờ',
    durationEn: '3.5 hours',
    price: 890000,
    rating: 5.0,
    reviewCount: 156,
    imageUrl: 'https://images.unsplash.com/photo-1682687220063-4742bd7fd538?auto=format&fit=crop&w=800&q=80',
  },
  {
    id: 'd0000001-0000-0000-0000-000000000022',
    title: 'Tour đi bộ dưới đáy biển Sea Walker ngắm cá hề & sinh vật biển',
    titleEn: 'Underwater Sea Walker Tour - Feed Clownfish & Sea Life',
    category: 'Đi bộ dưới biển',
    categoryEn: 'Sea Walker',
    location: 'Bãi Bụt, Sơn Trà',
    duration: '2 giờ',
    durationEn: '2 hours',
    price: 750000,
    rating: 4.93,
    reviewCount: 310,
    imageUrl: 'https://images.unsplash.com/photo-1582967788606-a171c1080cb0?auto=format&fit=crop&w=800&q=80',
  },
  {
    id: 'd0000001-0000-0000-0000-000000000002',
    title: 'Tour lặn tự do Freediving khám phá hang đá ngầm bí ẩn Sơn Trà',
    titleEn: 'Freediving Exploration of Son Tra Underwater Marine Caverns',
    category: 'Lặn tự do',
    categoryEn: 'Freediving',
    location: 'Bãi Rạng',
    duration: '4 giờ',
    durationEn: '4 hours',
    price: 650000,
    rating: 4.97,
    reviewCount: 94,
    imageUrl: 'https://images.unsplash.com/photo-1544551763-46a013bb70d5?auto=format&fit=crop&w=800&q=80',
  },
  {
    id: 'd0000001-0000-0000-0000-000000000021',
    title: 'Lặn ngắm hoàng hôn ngầm & trải nghiệm bắt ốc biển cùng ngư dân',
    titleEn: 'Dusk Reef Snorkeling & Coastal Shell Gathering with Fishermen',
    category: 'Văn hóa bản địa',
    categoryEn: 'Local Culture',
    location: 'Bán đảo Sơn Trà',
    duration: '2.5 giờ',
    durationEn: '2.5 hours',
    price: 390000,
    rating: 4.92,
    reviewCount: 118,
    imageUrl: 'https://images.unsplash.com/photo-1510312305653-8ed496efae75?auto=format&fit=crop&w=800&q=80',
  },
];

// Category C: Thể thao mạo hiểm & Cảm giác mạnh
export const THRILL_EXPERIENCES: ActivityCardData[] = [
  {
    id: 'd0000001-0000-0000-0000-000000000003',
    title: 'Bay dù lượn cano Parasailing đôi ngắm toàn cảnh thành phố biển',
    titleEn: 'Tandem Parasailing Flight with Panoramic Coastal City Views',
    category: 'Dù bay cano',
    categoryEn: 'Parasailing',
    location: 'Biển Mỹ Khê',
    duration: '1.5 giờ',
    durationEn: '1.5 hours',
    price: 650000,
    rating: 4.97,
    reviewCount: 412,
    imageUrl: 'https://images.unsplash.com/photo-1533105079780-92b9be482077?auto=format&fit=crop&w=800&q=80',
  },
  {
    id: 'd0000001-0000-0000-0000-000000000004',
    title: 'Lái mô tô nước Jet Ski Yamaha 1800cc xé sóng tốc độ cao',
    titleEn: 'High-speed Yamaha 1800cc Jet Ski Wave Riding Adventure',
    category: 'Mô tô nước',
    categoryEn: 'Jet Ski',
    location: 'Bãi biển Mỹ Khê',
    duration: '30 phút',
    durationEn: '30 minutes',
    price: 550000,
    rating: 4.94,
    reviewCount: 295,
    imageUrl: 'https://images.unsplash.com/photo-1559827291-72ee739d0d9a?auto=format&fit=crop&w=800&q=80',
  },
  {
    id: 'd0000001-0000-0000-0000-000000000005',
    title: 'Cano kéo phao chuối Banana Boat vui nhộn cho nhóm đông người',
    titleEn: 'Banana Boat Splash Ride for Friends & Corporate Groups',
    category: 'Phao chuối',
    categoryEn: 'Banana Boat',
    location: 'Bãi Non Nước',
    duration: '45 phút',
    durationEn: '45 minutes',
    price: 180000,
    rating: 4.91,
    reviewCount: 388,
    imageUrl: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=800&q=80',
  },
  {
    id: 'd0000001-0000-0000-0000-000000000031',
    title: 'Lướt ván cano Wakeboarding trải nghiệm cảm giác thăng bằng trên sóng',
    titleEn: 'Speedboat Wakeboarding - Thrill Riding on Ocean Waves',
    category: 'Lướt ván',
    categoryEn: 'Wakeboarding',
    location: 'Vịnh Đà Nẵng',
    duration: '1 giờ',
    durationEn: '1 hour',
    price: 590000,
    rating: 4.95,
    reviewCount: 142,
    imageUrl: 'https://images.unsplash.com/photo-1502680390469-be75c86b636f?auto=format&fit=crop&w=800&q=80',
  },
  {
    id: 'd0000001-0000-0000-0000-000000000051',
    title: 'Bay Flyboard lượn trên mặt biển cùng huấn luyện viên quốc tế',
    titleEn: 'Flyboard Hydroflight Session with International Instructor',
    category: 'Flyboard',
    categoryEn: 'Hydroflight',
    location: 'Mân Thái',
    duration: '45 phút',
    durationEn: '45 minutes',
    price: 950000,
    rating: 4.98,
    reviewCount: 76,
    imageUrl: 'https://images.unsplash.com/photo-1569263979104-865ab7cd8d13?auto=format&fit=crop&w=800&q=80',
  },
];

// Category D: Combo & Trải nghiệm Hoàng Hôn
export const COMBO_SUNSET_EXPERIENCES: ActivityCardData[] = [
  {
    id: 'd0000001-0000-0000-0000-000000000006',
    title: 'Combo Cắm trại lều Glamping ngắm hoàng hôn + Tiệc BBQ hải sản bãi biển',
    titleEn: 'Sunset Glamping Tent + Fresh Seafood BBQ Beach Feast',
    category: 'Cắm trại & BBQ',
    categoryEn: 'Camp & Feast',
    location: 'Bãi biển Non Nước',
    duration: '5 giờ',
    durationEn: '5 hours',
    price: 850000,
    rating: 4.99,
    reviewCount: 260,
    imageUrl: 'https://images.unsplash.com/photo-1510312305653-8ed496efae75?auto=format&fit=crop&w=800&q=80',
  },
  {
    id: 'd0000001-0000-0000-0000-000000000061',
    title: 'Tour du thuyền buồm ngắm hoàng hôn vịnh Đà Nẵng kèm rượu vang',
    titleEn: 'Sunset Sailing Yacht Cruise Along Da Nang Bay with Wine',
    category: 'Du thuyền',
    categoryEn: 'Luxury Yacht',
    location: 'Cảng sông Hàn',
    duration: '2.5 giờ',
    durationEn: '2.5 hours',
    price: 950000,
    rating: 4.97,
    reviewCount: 185,
    imageUrl: 'https://images.unsplash.com/photo-1569263979104-865ab7cd8d13?auto=format&fit=crop&w=800&q=80',
  },
  {
    id: 'd0000001-0000-0000-0000-000000000062',
    title: 'Combo 1 ngày: Chèo SUP sớm + Lặn ngắm san hô + Thưởng thức hải sản bãi rạng',
    titleEn: 'Full Marine Day: Sunrise SUP + Coral Reef Dive + Seafood Lunch',
    category: 'Gói trọn gói',
    categoryEn: 'Full-Day Combo',
    location: 'Bán đảo Sơn Trà',
    duration: '6 giờ',
    durationEn: '6 hours',
    price: 1150000,
    rating: 5.0,
    reviewCount: 340,
    imageUrl: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=800&q=80',
  },
  {
    id: 'd0000001-0000-0000-0000-000000000032',
    title: 'Khám phá rạn Nam Ô: Chụp ảnh bãi đá rêu xanh + Chèo thuyền thúng cổ truyền',
    titleEn: 'Nam O Green Moss Reef Tour + Traditional Basket Boat Paddling',
    category: 'Văn hóa & Thể thao',
    categoryEn: 'Heritage & Sport',
    location: 'Rạn Nam Ô, Liên Chiểu',
    duration: '3 giờ',
    durationEn: '3 hours',
    price: 380000,
    rating: 4.92,
    reviewCount: 198,
    imageUrl: 'https://images.unsplash.com/photo-1518837695005-2083093ee35b?auto=format&fit=crop&w=800&q=80',
  },
];

// 3. Coastal Bays Expanding Panels
export const COASTAL_BAYS: CoastalBayData[] = [
  {
    id: 'bay-1',
    name: 'Bãi Biển Mỹ Khê',
    nameEn: 'My Khe Beach',
    tagline: 'Top 6 Bãi Biển Quyến Rũ Nhất Hành Tinh - Forbes',
    taglineEn: 'Top 6 Most Attractive Beaches on Earth - Forbes',
    bestTime: '05:00 - 08:30 & 16:00 - 18:30',
    bestTimeEn: '05:00 - 08:30 & 16:00 - 18:30',
    waterDepth: '1.2m - 3.5m',
    tideCondition: 'Sóng êm, bãi cát thoai thoải, nước ấm',
    tideConditionEn: 'Gentle waves, gradual slope, warm water',
    imageUrl: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=80',
    tourCount: 16,
  },
  {
    id: 'bay-2',
    name: 'Bán Đảo Sơn Trà',
    nameEn: 'Son Tra Peninsula',
    tagline: 'Viên Ngọc Xanh - Rạn San Hô Nguyên Sinh Tuyệt Sắc',
    taglineEn: 'Green Jewel - Pristine Biodiversity & Corals',
    bestTime: '07:30 - 11:30 (Nước trong nhất)',
    bestTimeEn: '07:30 - 11:30 (Highest visibility)',
    waterDepth: '3.0m - 12.0m',
    tideCondition: 'Tầm nhìn 10m - 14m, nhiều vách đá vôi',
    tideConditionEn: '10-14m visibility, limestone reefs',
    imageUrl: 'https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1200&q=80',
    tourCount: 12,
  },
  {
    id: 'bay-3',
    name: 'Bãi Rạng & Hòn Sụp',
    nameEn: 'Bai Rang & Hon Sup',
    tagline: 'Thiên Đường Lặn Biển & Câu Cá Bờ Cực Hot',
    taglineEn: 'Premier Snorkeling & Coastal Reef Fishing',
    bestTime: '08:00 - 14:00',
    bestTimeEn: '08:00 - 14:00',
    waterDepth: '2.0m - 6.0m',
    tideCondition: 'Nước phẳng lặng, quần thể cá hề & sao biển',
    tideConditionEn: 'Calm water, clownfish & starfish colonies',
    imageUrl: 'https://images.unsplash.com/photo-1544551763-77ef2d0cfc6c?auto=format&fit=crop&w=1200&q=80',
    tourCount: 9,
  },
  {
    id: 'bay-4',
    name: 'Bãi Biển Non Nước',
    nameEn: 'Non Nuoc Beach',
    tagline: 'Thơ Mộng Dưới Chân Núi Ngũ Hành Sơn Kì Vĩ',
    taglineEn: 'Serene Haven at the Foot of Marble Mountains',
    bestTime: '15:30 - 18:30 (Hoàng hôn lộng gió)',
    bestTimeEn: '15:30 - 18:30 (Breezy Golden Hour)',
    waterDepth: '1.0m - 2.8m',
    tideCondition: 'Bờ cát trắng mịn trải dài 5km, gió lộng',
    tideConditionEn: '5km white sand coastline, gentle breeze',
    imageUrl: 'https://images.unsplash.com/photo-1519046904884-53103b34b206?auto=format&fit=crop&w=1200&q=80',
    tourCount: 8,
  },
];

// 4. Ocean Sports Showcase (Hover-to-swap)
export const OCEAN_SPORTS_SHOWCASE: OceanSportShowcase[] = [
  {
    id: 'sport-1',
    index: '01',
    name: 'Chèo SUP Đón Bình Minh',
    nameEn: 'Sunrise Paddleboarding',
    level: 'Dễ tiếp cận • Mọi lứa tuổi',
    levelEn: 'Easy • All ages welcome',
    description: 'Thả mình giữa làn nước biển sớm mai, đón tia nắng đầu tiên ló rạng trên đường chân trời Mỹ Khê. Thao tác đơn giản, huấn luyện viên kèm sát 1:1.',
    descriptionEn: 'Glide effortlessly on mirrored morning waters, greeting the first sunrise rays over My Khe horizon. Beginner-friendly with 1:1 certified instructor.',
    highlights: ['Bao gồm trọn bộ ván SUP, mái chèo carbon, áo phao đạt chuẩn', 'Tặng bộ ảnh nhiếp ảnh gia chụp máy cơ chuyên nghiệp', 'Nước uống giải khát & túi chống nước điện thoại'],
    highlightsEn: ['Full gear: Carbon paddle, certified life vest, SUP board', 'Free photoshoot package by professional photographer', 'Refreshment drink & waterproof phone pouch included'],
    startingPrice: 280000,
    imageUrl: 'https://images.unsplash.com/photo-1502680390469-be75c86b636f?auto=format&fit=crop&w=1000&q=80',
  },
  {
    id: 'sport-2',
    index: '02',
    name: 'Lặn Bình Khí Scuba Diving',
    nameEn: 'Scuba Diving Adventure',
    level: 'Thể lực vừa phải • Kèm Master Diver',
    levelEn: 'Moderate fitness • Master Diver guided',
    description: 'Chìm đắm vào lòng đại dương Sơn Trà, ngắm nhìn rạn san hô đĩa, san hô cành hàng trăm năm tuổi và những đàn cá sặc sỡ bơi lội xung quanh.',
    descriptionEn: 'Immerse into Son Tra underwater paradise, marveling at centuries-old coral formations and schools of tropical marine life.',
    highlights: ['Trang bị bình khí nén oxy tinh khiết, kính lặn chuyên dụng', 'Hướng dẫn thở & xử lý áp suất tai chi tiết trước khi xuống nước', 'Bảo hiểm du lịch thể thao mạo hiểm 100%'],
    highlightsEn: ['Full scuba gear: Pure compressed air tank, mask, fins', 'Pre-dive ear pressure & breathing safety briefing', '100% Comprehensive marine adventure insurance'],
    startingPrice: 550000,
    imageUrl: 'https://images.unsplash.com/photo-1544551763-46a013bb70d5?auto=format&fit=crop&w=1000&q=80',
  },
  {
    id: 'sport-3',
    index: '03',
    name: 'Dù Lượn Bay Trên Vịnh Biển',
    nameEn: 'Coastal Bay Parasailing',
    level: 'Cực đã • Không cần kinh nghiệm',
    levelEn: 'Top thrill • No experience required',
    description: 'Cano cao tốc tăng tốc đưa bạn bay vút lên độ cao 70m so với mặt biển. Ngắm trọn vẹn đường cong bán đảo Sơn Trà và cung đường biển Đà Nẵng tuyệt mỹ.',
    descriptionEn: 'Speedboat accelerates to lift you 70 meters high into the sky. Soak in the breathtaking curve of Son Tra Peninsula and the Da Nang skyline.',
    highlights: ['Dù đôi hoặc dù đơn chịu tải 250kg an toàn tuyệt đối', 'Hệ thống tời cuốn tự động êm ái, hạ cánh nhẹ nhàng', 'Trải nghiệm nhúng chân nước biển cực sảng khoái'],
    highlightsEn: ['Tandem/single parachute tested up to 250kg capacity', 'Smooth hydraulic winch system with soft water landing', 'Refreshing optional sea dip during flight'],
    startingPrice: 650000,
    imageUrl: 'https://images.unsplash.com/photo-1533105079780-92b9be482077?auto=format&fit=crop&w=1000&q=80',
  },
  {
    id: 'sport-4',
    index: '04',
    name: 'Mô Tô Nước Jet Ski Cao Tốc',
    nameEn: 'High-speed Jet Ski Waves',
    level: 'Cảm giác mạnh • Có HLV hỗ trợ',
    levelEn: 'Extreme thrill • Instructor supported',
    description: 'Tự tay vít ga cầm lái cỗ máy phản lực 1800cc xé toang bọt sóng trắng xóa. Cảm nhận adrenaline dâng trào giữa biển khơi bao la.',
    descriptionEn: 'Take the handlebars of a 1800cc jet engine machine carving through ocean waves. Feel the pure adrenaline rush amidst the vast turquoise ocean.',
    highlights: ['Mô tô nước Yamaha WaveRunner đời mới 2025/2026', 'Áo phao gia cường chống va đập và còi cứu sinh khẩn cấp', 'Huấn luyện viên kèm lái hoặc tự do cầm lái theo tuyến phao an toàn'],
    highlightsEn: ['Latest Yamaha WaveRunner 2025/2026 edition', 'Impact-resistant reinforced life jacket with whistle', 'Instructor accompaniment or solo ride within marked safe zones'],
    startingPrice: 550000,
    imageUrl: 'https://images.unsplash.com/photo-1559827291-72ee739d0d9a?auto=format&fit=crop&w=1000&q=80',
  },
];

// 5. Guest Memories Gallery Photos (Authentic Portraits & Joyful Traveler Moments)
export const GUEST_GALLERY_ROW_1 = [
  'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1502680390469-be75c86b636f?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1544551763-77ef2d0cfc6c?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&w=600&q=80',
];

export const GUEST_GALLERY_ROW_2 = [
  'https://images.unsplash.com/photo-1529156069898-49953e39b3ac?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1501196354995-cbb51c65aaea?auto=format&fit=crop&w=600&q=80',
  'https://images.unsplash.com/photo-1516483638261-f4dbaf036963?auto=format&fit=crop&w=600&q=80',
];
