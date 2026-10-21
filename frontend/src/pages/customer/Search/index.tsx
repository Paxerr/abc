import React, { useState, useEffect, useMemo, useRef, useCallback } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { CinematicSceneBackdrop } from '../../../components/customer/CinematicSceneBackdrop';
import { ScrollReveal } from '../../../components/common/ScrollReveal';
import { useWishlistStore } from '../../../store/useWishlistStore';
import { useLanguageStore } from '../../../store/useLanguageStore';
import { serviceCatalogService, type ServiceQueryParams } from '../../../services/serviceCatalogService';
import { categoryService } from '../../../services/categoryService';
import type { ServiceSummaryDto } from '../../../types/service';
import type { CategoryDto } from '../../../types/category';
import { FEATURED_SERVICES, CATEGORIES, MOCK_IMAGES } from '../../../mockData';

// Normalized experience item interface for seamless display
interface DisplayExperience {
  id: string;
  name: string;
  nameEn?: string;
  description: string;
  price: number;
  promotionalPrice?: number | null;
  rating: number;
  reviewCount: number;
  location: string;
  imageUrl: string;
  duration?: string;
  badge?: string;
  categoryId?: string;
}

// Category descriptions & slogans for cinematic FPT Play / Netflix-style category showcase
const CATEGORY_META: Record<string, { titleVi: string; titleEn: string; descVi: string; descEn: string; tag: string }> = {
  'cheo-sup-kayak': {
    titleVi: 'CHÈO SUP & KAYAK ĐÀ NẴNG',
    titleEn: 'STAND-UP PADDLEBOARDING & KAYAK',
    descVi: 'Lướt êm trên mặt biển phẳng lặng sớm mai, đón ánh bình minh rạng rỡ và ngắm nhìn trọn vẹn vịnh biển Đà Nẵng.',
    descEn: 'Glide peacefully across calm morning waters, embrace the sunrise and admire the pristine bay of Da Nang.',
    tag: 'Đón Bình Minh',
  },
  'lan-ngam-san-ho': {
    titleVi: 'LẶN NGẮM SAN HÔ BÁN ĐẢO SƠN TRÀ',
    titleEn: 'CORAL REEF SCUBA & SEAWALKER',
    descVi: 'Đắm mình trong làn nước pha lê, khám phá quần thể san hô đa sắc màu cùng huấn luyện viên PADI chuyên nghiệp.',
    descEn: 'Dive into crystal-clear waters, exploring vibrant coral reefs guided by certified PADI master divers.',
    tag: 'Sinh Thái Biển',
  },
  'cano-du-bay': {
    titleVi: 'CANO LƯỚT SÓNG & BAY DÙ LƯỢN BIỂN',
    titleEn: 'SPEEDBOAT & PARASAILING ADVENTURE',
    descVi: 'Bay vút lên không trung ngắm toàn cảnh bờ biển Mỹ Khê và thử thách cano cao tốc lướt sóng cực phấn khích.',
    descEn: 'Soar high above My Khe coastline and feel the rush of high-speed wave carving with maximum safety.',
    tag: 'Cảm Giác Mạnh',
  },
  'mo-to-nuoc-jetski': {
    titleVi: 'MÔ TÔ NƯỚC JETSKI CẢM GIÁC MẠNH',
    titleEn: 'JETSKI HIGH-SPEED WAVE RIDING',
    descVi: 'Tự tay cầm lái mô tô nước Yamaha công suất lớn, chinh phục từng con sóng vịnh biển với sự bảo hộ an toàn tuyệt đối.',
    descEn: 'Take the helm of high-performance Yamaha jetskis, carving dynamic waves under professional marine surveillance.',
    tag: 'Tốc Độ & Sóng',
  },
  'truot-phao-chuoi': {
    titleVi: 'TRƯỢT PHAO CHUỐI TỐC ĐỘ CAO CHO NHÓM',
    titleEn: 'GROUP BANANA BOAT WAVE RIDE',
    descVi: 'Trải nghiệm phấn khích tột độ cùng hội bạn thân: lướt nhanh trên mặt nước, tạo bọt sóng trắng xóa ngập tràn tiếng cười.',
    descEn: 'Exhilarating group ocean action: fast water-skimming turns, splashing foam and unforgettable shared laughter.',
    tag: 'Nhóm & Bạn Bè',
  },
  'du-thuyen-ngam-hoang-hon': {
    titleVi: 'DU THUYỀN HẠNG SANG NGẮM HOÀNG HÔN',
    titleEn: 'LUXURY SUNSET CATAMARAN YACHT',
    descVi: 'Hành trình lãng mạn trên vịnh Đà Nẵng: thưởng thức trà chiều, rượu vang và ngắm hoàng hôn buông xuống chân trời.',
    descEn: 'A breathtaking romantic cruise across Da Nang Bay: afternoon tea, premium wine and golden sunset on the horizon.',
    tag: 'Sang Trọng & Combo',
  },
};

export function Search() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { language, t } = useLanguageStore();
  const { toggleWishlist, isInWishlist } = useWishlistStore();

  const categoryParam = searchParams.get('category');
  const keywordParam = searchParams.get('q') || '';

  // Local state
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [services, setServices] = useState<DisplayExperience[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [keyword, setKeyword] = useState<string>(keywordParam);
  const [selectedCategorySlug, setSelectedCategorySlug] = useState<string | null>(categoryParam);
  const [priceMax, setPriceMax] = useState<number>(1500000);
  const [sortBy, setSortBy] = useState<'rating_desc' | 'price_asc' | 'price_desc' | 'popular'>('popular');

  // Staggered reveal tracker
  const [revealedIds, setRevealedIds] = useState<Set<string>>(new Set());

  // 1. Fetch categories from Backend or Fallback
  useEffect(() => {
    let isMounted = true;
    async function loadCategories() {
      try {
        const remoteCats = await categoryService.getCategories();
        if (isMounted && remoteCats && remoteCats.length > 0) {
          setCategories(remoteCats);
        } else if (isMounted) {
          // Fallback to mock categories
          const fallbackCats: CategoryDto[] = CATEGORIES.map((c, idx) => ({
            id: `a0000001-0000-0000-0000-00000000000${idx + 1}`,
            name: c.name,
            nameEn: c.name,
            slug: c.id === 'cat-1' ? 'cheo-sup-kayak' : c.id === 'cat-2' ? 'cano-du-bay' : c.id === 'cat-3' ? 'lan-ngam-san-ho' : c.id === 'cat-4' ? 'cheo-sup-kayak' : 'du-thuyen-ngam-hoang-hon',
            iconUrl: c.icon,
            isActive: true,
          }));
          setCategories(fallbackCats);
        }
      } catch (err) {
        console.warn('[Search] Lỗi nạp danh mục, sử dụng fallback:', err);
      }
    }
    loadCategories();
    return () => { isMounted = false; };
  }, []);

  // 2. Synchronize URL query parameter when changed
  useEffect(() => {
    setSelectedCategorySlug(categoryParam);
  }, [categoryParam]);

  // Resolve matching category entity from selectedCategorySlug
  const matchedCategory = useMemo(() => {
    if (!selectedCategorySlug) return null;
    const lower = selectedCategorySlug.toLowerCase();

    // Direct match by slug or id
    const direct = categories.find(c => c.slug?.toLowerCase() === lower || c.id === selectedCategorySlug);
    if (direct) return direct;

    // Aliases mapping (sup, diving, thrill, combo, kayak)
    if (lower === 'sup' || lower.includes('kayak')) {
      return categories.find(c => c.slug?.includes('sup') || c.slug?.includes('kayak')) || categories[0];
    }
    if (lower === 'diving' || lower.includes('lan') || lower.includes('san-ho')) {
      return categories.find(c => c.slug?.includes('lan') || c.slug?.includes('san-ho')) || categories[1];
    }
    if (lower === 'thrill' || lower.includes('cano') || lower.includes('jetski') || lower.includes('du-bay')) {
      return categories.find(c => c.slug?.includes('cano') || c.slug?.includes('jetski')) || categories[2];
    }
    if (lower === 'combo' || lower.includes('du-thuyen') || lower.includes('hoang-hon')) {
      return categories.find(c => c.slug?.includes('du-thuyen')) || categories[5];
    }

    return null;
  }, [selectedCategorySlug, categories]);

  // 3. Fetch services from Backend with fallback
  useEffect(() => {
    let isMounted = true;
    async function fetchServices() {
      setIsLoading(true);
      console.groupCollapsed('%c[Search Page] Nạp dữ liệu trải nghiệm biển...', 'color: #0284c7; font-weight: bold;');
      console.log('[Bước 1/3] Khởi tạo bộ lọc tìm kiếm:', {
        categoryId: matchedCategory?.id,
        categorySlug: selectedCategorySlug,
        keyword,
        sortBy,
      });

      try {
        const queryParams: ServiceQueryParams = {
          size: 24,
          ...(matchedCategory?.id ? { categoryId: matchedCategory.id } : {}),
          ...(keyword.trim() ? { keyword: keyword.trim() } : {}),
          ...(sortBy === 'price_asc' || sortBy === 'price_desc' || sortBy === 'rating_desc' ? { sortBy } : {}),
        };

        const response = await serviceCatalogService.getServices(queryParams);

        if (isMounted) {
          if (response && response.content && response.content.length > 0) {
            console.log(`[Bước 2/3] Backend trả về ${response.content.length} dịch vụ trực tiếp`);
            const mapped: DisplayExperience[] = response.content.map((item, idx) => ({
              id: item.id,
              name: item.name,
              description: item.shortDescription || 'Trải nghiệm thể thao biển chuẩn an toàn hàng hải tại vịnh Đà Nẵng.',
              price: item.price,
              promotionalPrice: item.promotionalPrice,
              rating: item.averageRating || 4.95,
              reviewCount: item.reviewCount || 150,
              location: item.address ? item.address.split(',')[0] : 'Bãi biển Mỹ Khê',
              imageUrl: item.primaryImageUrl || 'https://images.unsplash.com/photo-1502680390469-be75c86b636f?auto=format&fit=crop&w=800&q=80',
              duration: '120 phút',
              badge: idx === 0 ? 'Bán chạy nhất' : idx === 1 ? 'Khuyên dùng' : 'Chuẩn PADI',
              categoryId: matchedCategory?.id,
            }));
            setServices(mapped);
          } else {
            console.log('[Bước 2/3] Backend rỗng hoặc đang chạy kiểm thử -> Sử dụng fallback thông minh từ mockData');
            // Intelligent fallback from mockData
            const filteredMock = FEATURED_SERVICES.filter(item => {
              if (matchedCategory && item.categoryId) {
                // If matchedCategory is cat-1, filter
                const matchesMockCat = (matchedCategory.slug?.includes('sup') && item.categoryId === 'cat-1') ||
                  (matchedCategory.slug?.includes('cano') && item.categoryId === 'cat-2') ||
                  (matchedCategory.slug?.includes('lan') && item.categoryId === 'cat-3') ||
                  (matchedCategory.slug?.includes('du-thuyen') && item.categoryId === 'cat-5');
                if (!matchesMockCat && selectedCategorySlug) return false;
              }
              if (keyword.trim()) {
                const kw = keyword.toLowerCase();
                const matchName = item.name.toLowerCase().includes(kw);
                const matchDesc = (item.description || '').toLowerCase().includes(kw);
                if (!matchName && !matchDesc) return false;
              }
              return true;
            });

            const mappedMock: DisplayExperience[] = filteredMock.map((item, idx) => {
              const img = MOCK_IMAGES.find(i => i.serviceId === item.id && i.isPrimary)?.imageUrl ||
                'https://images.unsplash.com/photo-1502680390469-be75c86b636f?auto=format&fit=crop&w=800&q=80';
              return {
                id: item.id,
                name: item.name,
                description: item.description || 'Trải nghiệm thể thao biển cao cấp tại Đà Nẵng.',
                price: item.basePrice,
                rating: item.avgRating || 4.9,
                reviewCount: item.ratingCount || 98,
                location: item.locationName,
                imageUrl: img,
                duration: `${item.durationMinutes || 90} phút`,
                badge: idx === 0 ? 'Phổ biến' : undefined,
                categoryId: item.categoryId,
              };
            });
            setServices(mappedMock);
          }
          console.log('[Bước 3/3] Hoàn tất nạp dữ liệu trải nghiệm cho trang Search');
          console.groupEnd();
        }
      } catch (err) {
        console.groupEnd();
        console.warn('[Search] Lỗi nạp dịch vụ:', err);
      } finally {
        if (isMounted) setIsLoading(false);
      }
    }

    fetchServices();
    return () => { isMounted = false; };
  }, [matchedCategory, selectedCategorySlug, keyword, sortBy]);

  // Client-side filtering by max price & sorting if needed
  const displayServices = useMemo(() => {
    let result = [...services];
    if (priceMax < 1500000) {
      result = result.filter(s => s.price <= priceMax);
    }
    if (sortBy === 'price_asc') {
      result.sort((a, b) => a.price - b.price);
    } else if (sortBy === 'price_desc') {
      result.sort((a, b) => b.price - a.price);
    } else if (sortBy === 'rating_desc') {
      result.sort((a, b) => b.rating - a.rating);
    }
    return result;
  }, [services, priceMax, sortBy]);

  // ScrollReveal observer for smooth staggered card entrance
  useEffect(() => {
    if (isLoading || displayServices.length === 0) return;

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const id = entry.target.getAttribute('data-card-id');
            if (id) {
              setRevealedIds((prev) => new Set([...prev, id]));
            }
          }
        });
      },
      { threshold: 0.15, rootMargin: '0px 0px -50px 0px' }
    );

    const cards = document.querySelectorAll('[data-card-id]');
    cards.forEach((card) => observer.observe(card));

    return () => observer.disconnect();
  }, [isLoading, displayServices]);

  // Category change handler
  const handleSelectCategory = (slug: string | null) => {
    setSelectedCategorySlug(slug);
    if (slug) {
      setSearchParams({ category: slug });
    } else {
      setSearchParams({});
    }
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // Category metadata for showcase header
  const categoryMeta = useMemo(() => {
    if (!matchedCategory && !selectedCategorySlug) return null;
    const key = matchedCategory?.slug || selectedCategorySlug || '';
    if (CATEGORY_META[key]) return CATEGORY_META[key];

    // Fuzzy matching
    for (const [k, v] of Object.entries(CATEGORY_META)) {
      if (key.includes(k) || k.includes(key)) return v;
    }

    return {
      titleVi: matchedCategory?.name.toUpperCase() || 'DANH MỤC TRẢI NGHIỆM BIỂN',
      titleEn: matchedCategory?.nameEn?.toUpperCase() || 'MARINE EXPERIENCE CATEGORY',
      descVi: 'Khám phá tất cả các gói hoạt động thể thao biển được yêu thích nhất tại Đà Nẵng.',
      descEn: 'Explore all popular water sports and ocean adventures in Da Nang.',
      tag: 'Chuẩn An Toàn',
    };
  }, [matchedCategory, selectedCategorySlug]);

  return (
    <main className="w-full text-white min-h-screen relative overflow-x-clip bg-slate-950 pb-24">
      {/* Dynamic Deep Ocean Backdrop matching Homepage */}
      <CinematicSceneBackdrop />

      {/* Optical ambient dark gradient to ensure ultra-sharp readability */}
      <div className="fixed inset-0 bg-gradient-to-b from-slate-950/70 via-slate-950/40 to-slate-950/80 pointer-events-none z-10" />

      <div className="relative z-20 max-w-[1520px] w-full mx-auto px-4 sm:px-6 lg:px-8 pt-24 sm:pt-28">

        {/* =========================================================================
            CINEMATIC CATEGORY SHOWCASE BANNER (FPT PLAY / NETFLIX STYLE)
           ========================================================================= */}
        <section data-scene="bays" className="mb-8 select-none">
          {/* Breadcrumb Navigation */}
          <div className="flex items-center gap-2 text-xs sm:text-sm text-cyan-200/80 mb-3">
            <Link
              to="/"
              className="inline-flex items-center gap-1 hover:text-white transition-colors py-1"
            >
              <span className="material-symbols-outlined text-[16px]">arrow_back</span>
              <span>{language === 'en' ? 'Back to Home' : 'Trang chủ'}</span>
            </Link>
            <span>/</span>
            <button
              onClick={() => handleSelectCategory(null)}
              className={`hover:text-white transition-colors py-1 ${!selectedCategorySlug ? 'text-white font-semibold' : ''}`}
            >
              {language === 'en' ? 'All Marine Experiences' : 'Tất cả trải nghiệm biển'}
            </button>
            {matchedCategory && (
              <>
                <span>/</span>
                <span className="text-white font-bold truncate">
                  {language === 'en' ? matchedCategory.nameEn || matchedCategory.name : matchedCategory.name}
                </span>
              </>
            )}
          </div>

          {/* Large Showcase Title & Subtitle */}
          <div className="bg-gradient-to-r from-slate-900/90 via-slate-900/70 to-slate-900/40 backdrop-blur-xl border border-white/15 rounded-3xl p-6 sm:p-8 lg:p-10 shadow-2xl relative overflow-hidden">
            {/* Ambient water reflection glow */}
            <div className="absolute top-0 right-0 w-96 h-96 bg-cyan-500/10 rounded-full blur-3xl pointer-events-none" />

            <div className="relative z-10 max-w-4xl">
              <div className="flex items-center gap-2.5 mb-2.5">
                <span className="px-3 py-1 rounded-full bg-cyan-500/20 text-cyan-300 border border-cyan-400/30 text-xs font-mono font-bold uppercase tracking-wider">
                  {categoryMeta ? categoryMeta.tag : (language === 'en' ? 'Ocean Catalog' : 'Danh mục biển')}
                </span>
                <span className="text-xs text-slate-300 font-mono">
                  {displayServices.length} {language === 'en' ? 'experiences available' : 'trải nghiệm sẵn sàng'}
                </span>
              </div>

              <h1 className="text-2xl sm:text-4xl lg:text-5xl font-black text-white tracking-tight uppercase drop-shadow-[0_2px_8px_rgba(0,0,0,0.8)] mb-3">
                {categoryMeta
                  ? (language === 'en' ? categoryMeta.titleEn : categoryMeta.titleVi)
                  : (language === 'en' ? 'EXPLORE DA NANG MARINE EXPERIENCES' : 'KHÁM PHÁ TRẢI NGHIỆM BIỂN ĐÀ NẴNG')}
              </h1>

              <p className="text-sm sm:text-base text-slate-200/90 leading-relaxed font-normal max-w-3xl drop-shadow-sm">
                {categoryMeta
                  ? (language === 'en' ? categoryMeta.descEn : categoryMeta.descVi)
                  : (language === 'en'
                    ? 'Handpicked certified marine activities in Da Nang: sunrise SUP, coral scuba diving, speedboats, parasailing and luxury sunset yachts.'
                    : 'Toàn bộ các gói hoạt động thể thao biển được thẩm định an toàn hàng hải: chèo SUP, lặn ngắm san hô Sơn Trà, dù bay, cano cao tốc và du thuyền hoàng hôn.')}
              </p>
            </div>

            {/* Quick Category Switcher Pills */}
            <div className="mt-6 pt-5 border-t border-white/10 flex items-center gap-2 overflow-x-auto no-scrollbar pb-1">
              <button
                type="button"
                onClick={() => handleSelectCategory(null)}
                className={`px-4 py-1.5 rounded-full text-xs font-bold transition-all shrink-0 cursor-pointer ${
                  !selectedCategorySlug
                    ? 'bg-gradient-to-r from-cyan-500 to-teal-500 text-white shadow-lg shadow-cyan-500/25 scale-105'
                    : 'bg-white/10 text-slate-200 hover:bg-white/20 hover:text-white'
                }`}
              >
                {language === 'en' ? 'All Activities' : 'Tất cả'}
              </button>

              {categories.map((cat) => {
                const isSelected = selectedCategorySlug === cat.slug || matchedCategory?.id === cat.id;
                return (
                  <button
                    key={cat.id}
                    type="button"
                    onClick={() => handleSelectCategory(cat.slug || cat.id)}
                    className={`px-4 py-1.5 rounded-full text-xs font-bold transition-all shrink-0 cursor-pointer flex items-center gap-1.5 ${
                      isSelected
                        ? 'bg-gradient-to-r from-cyan-500 to-teal-500 text-white shadow-lg shadow-cyan-500/25 scale-105'
                        : 'bg-white/10 text-slate-200 hover:bg-white/20 hover:text-white'
                    }`}
                  >
                    <span>{language === 'en' ? cat.nameEn || cat.name : cat.name}</span>
                  </button>
                );
              })}
            </div>
          </div>
        </section>

        {/* =========================================================================
            STICKY FILTER & CONTROLS CAPSULE
           ========================================================================= */}
        <section className="sticky top-20 z-30 mb-8">
          <div className="bg-slate-900/85 backdrop-blur-xl border border-white/15 rounded-2xl p-3 sm:p-4 shadow-2xl flex flex-col md:flex-row items-center justify-between gap-4">
            {/* Search Input Box */}
            <div className="w-full md:w-auto flex-1 flex items-center gap-2 bg-slate-950/60 rounded-xl px-3.5 py-2 border border-white/10 focus-within:border-cyan-400 transition-colors">
              <span className="material-symbols-outlined text-cyan-400 text-[20px]">search</span>
              <input
                type="text"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                placeholder={language === 'en' ? 'Search by activity, location (e.g., My Khe, Son Tra)...' : 'Tìm trải nghiệm, bãi biển (Mỹ Khê, Sơn Trà, Hòn Sụp)...'}
                className="bg-transparent text-sm text-white placeholder-slate-400 outline-none w-full"
              />
              {keyword && (
                <button
                  type="button"
                  onClick={() => setKeyword('')}
                  className="text-slate-400 hover:text-white"
                >
                  <span className="material-symbols-outlined text-[16px]">close</span>
                </button>
              )}
            </div>

            {/* Filter Controls: Max Price Slider & Sort Dropdown */}
            <div className="w-full md:w-auto flex flex-wrap items-center justify-between sm:justify-end gap-3 text-xs">
              {/* Price Filter */}
              <div className="flex items-center gap-2 bg-slate-950/60 rounded-xl px-3 py-1.5 border border-white/10">
                <span className="text-slate-300">{language === 'en' ? 'Max Price:' : 'Giá tối đa:'}</span>
                <span className="font-bold text-cyan-300">
                  {priceMax >= 1500000 ? (language === 'en' ? 'All' : 'Tất cả') : `${(priceMax / 1000).toLocaleString('vi-VN')}k`}
                </span>
                <input
                  type="range"
                  min="200000"
                  max="1500000"
                  step="50000"
                  value={priceMax}
                  onChange={(e) => setPriceMax(Number(e.target.value))}
                  className="w-20 sm:w-24 accent-cyan-400 cursor-pointer"
                />
              </div>

              {/* Sort By Dropdown */}
              <div className="flex items-center gap-1.5 bg-slate-950/60 rounded-xl px-3 py-2 border border-white/10">
                <span className="text-slate-300">{language === 'en' ? 'Sort:' : 'Sắp xếp:'}</span>
                <select
                  value={sortBy}
                  onChange={(e) => setSortBy(e.target.value as any)}
                  className="bg-transparent text-white font-semibold outline-none cursor-pointer"
                >
                  <option value="popular" className="bg-slate-900 text-white">
                    {language === 'en' ? 'Most Popular' : 'Phổ biến nhất'}
                  </option>
                  <option value="rating_desc" className="bg-slate-900 text-white">
                    {language === 'en' ? 'Top Rated ★' : 'Đánh giá cao nhất ★'}
                  </option>
                  <option value="price_asc" className="bg-slate-900 text-white">
                    {language === 'en' ? 'Price: Low to High' : 'Giá: Thấp đến Cao'}
                  </option>
                  <option value="price_desc" className="bg-slate-900 text-white">
                    {language === 'en' ? 'Price: High to Low' : 'Giá: Cao đến Thấp'}
                  </option>
                </select>
              </div>

              {/* Reset Filter Button */}
              {(selectedCategorySlug || keyword || priceMax < 1500000 || sortBy !== 'popular') && (
                <button
                  type="button"
                  onClick={() => {
                    handleSelectCategory(null);
                    setKeyword('');
                    setPriceMax(1500000);
                    setSortBy('popular');
                  }}
                  className="text-cyan-300 hover:text-white transition-colors underline underline-offset-2 font-semibold"
                >
                  {language === 'en' ? 'Reset' : 'Đặt lại'}
                </button>
              )}
            </div>
          </div>
        </section>

        {/* =========================================================================
            EXPERIENCES RESULTS GRID WITH SMOOTH SCROLL REVEAL (60 FPS)
           ========================================================================= */}
        <section data-scene="sports">
          {isLoading ? (
            /* Skeleton Loading State */
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
              {[...Array(8)].map((_, i) => (
                <div
                  key={i}
                  className="bg-slate-900/70 border border-white/10 rounded-2xl overflow-hidden p-3 animate-pulse"
                >
                  <div className="aspect-[4/3] rounded-xl bg-slate-800 mb-3" />
                  <div className="h-4 bg-slate-800 rounded w-3/4 mb-2" />
                  <div className="h-3 bg-slate-800 rounded w-1/2 mb-4" />
                  <div className="h-5 bg-slate-800 rounded w-1/3" />
                </div>
              ))}
            </div>
          ) : displayServices.length > 0 ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
              {displayServices.map((service, idx) => {
                const isSaved = isInWishlist(service.id);
                const isRevealed = revealedIds.has(service.id);

                return (
                  <article
                    key={service.id}
                    data-card-id={service.id}
                    style={{
                      transitionDelay: `${(idx % 4) * 75}ms`,
                    }}
                    className={`group flex flex-col bg-white/95 dark:bg-slate-900/90 backdrop-blur-md rounded-2xl overflow-hidden border border-white/20 shadow-xl hover:shadow-[0_20px_35px_rgba(0,0,0,0.4)] hover:-translate-y-2 hover:border-cyan-400/50 transition-all duration-400 ease-out ${
                      isRevealed
                        ? 'opacity-100 translate-y-0'
                        : 'opacity-0 translate-y-8'
                    }`}
                  >
                    {/* Image Box */}
                    <div
                      className="relative aspect-[4/3] overflow-hidden bg-slate-800 cursor-pointer"
                      onClick={() => navigate(`/tour/${service.id}`)}
                    >
                      <img
                        src={service.imageUrl}
                        alt={service.name}
                        loading="lazy"
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-600 ease-out pointer-events-none"
                      />

                      {/* Optical scrim on image bottom */}
                      <div className="absolute inset-0 bg-gradient-to-t from-slate-950/70 via-transparent to-transparent pointer-events-none" />

                      {/* Top Badges */}
                      <div className="absolute top-2.5 left-2.5 flex flex-col gap-1 items-start">
                        {service.badge && (
                          <span className="px-2.5 py-0.5 rounded-full bg-cyan-600 text-white text-[11px] font-bold shadow-md">
                            {service.badge}
                          </span>
                        )}
                      </div>

                      {/* Top Right Wishlist Heart */}
                      <button
                        type="button"
                        onClick={(e) => {
                          e.preventDefault();
                          e.stopPropagation();
                          toggleWishlist(service.id, service.name);
                        }}
                        aria-label="Toggle wishlist"
                        className="absolute top-2.5 right-2.5 w-8 h-8 rounded-full bg-slate-950/60 backdrop-blur-md border border-white/20 flex items-center justify-center hover:scale-110 active:scale-95 transition-transform shadow-md cursor-pointer z-10"
                      >
                        <span className={`text-base ${isSaved ? 'text-rose-500' : 'text-white'}`}>
                          {isSaved ? '♥' : '♡'}
                        </span>
                      </button>

                      {/* Bottom Info on Image */}
                      <div className="absolute bottom-2.5 left-2.5 right-2.5 flex items-center justify-between text-white text-xs font-medium">
                        <span className="flex items-center gap-1 drop-shadow-md">
                          <span className="material-symbols-outlined text-[14px] text-cyan-300">location_on</span>
                          <span className="truncate">{service.location}</span>
                        </span>
                        {service.duration && (
                          <span className="flex items-center gap-1 drop-shadow-md">
                            <span className="material-symbols-outlined text-[14px] text-amber-300">schedule</span>
                            <span>{service.duration}</span>
                          </span>
                        )}
                      </div>
                    </div>

                    {/* Content Box */}
                    <div
                      className="p-4 flex-1 flex flex-col justify-between cursor-pointer text-slate-900 dark:text-white"
                      onClick={() => navigate(`/tour/${service.id}`)}
                    >
                      <div>
                        {/* Rating Row */}
                        <div className="flex items-center justify-between mb-1.5 text-xs">
                          <div className="flex items-center gap-1 text-amber-500 font-bold">
                            <span>★</span>
                            <span className="text-slate-800 dark:text-slate-200">{service.rating.toFixed(2)}</span>
                            <span className="text-slate-500 dark:text-slate-400 font-normal">({service.reviewCount})</span>
                          </div>
                          <span className="text-[11px] text-cyan-600 dark:text-cyan-400 font-semibold uppercase tracking-wider">
                            {language === 'en' ? 'Maritime Verified' : 'Chuẩn An Toàn'}
                          </span>
                        </div>

                        {/* Title */}
                        <h3 className="font-extrabold text-base leading-snug line-clamp-2 text-slate-900 dark:text-white group-hover:text-cyan-600 dark:group-hover:text-cyan-300 transition-colors">
                          {service.name}
                        </h3>

                        {/* Description */}
                        <p className="mt-1.5 text-xs text-slate-600 dark:text-slate-300 line-clamp-2 leading-relaxed">
                          {service.description}
                        </p>
                      </div>

                      {/* Price & Action Row */}
                      <div className="mt-4 pt-3 border-t border-slate-200/80 dark:border-white/10 flex items-center justify-between">
                        <div>
                          <div className="text-xs text-slate-500 dark:text-slate-400">
                            {language === 'en' ? 'From' : 'Đơn giá từ'}
                          </div>
                          <div className="flex items-baseline gap-1">
                            <span className="font-black text-lg text-rose-600 dark:text-rose-400">
                              {service.price.toLocaleString('vi-VN')}₫
                            </span>
                            <span className="text-xs text-slate-500 dark:text-slate-400">
                              /{language === 'en' ? 'guest' : 'khách'}
                            </span>
                          </div>
                        </div>

                        {/* Booking Details Arrow Button */}
                        <span className="w-9 h-9 rounded-full bg-cyan-500/10 dark:bg-cyan-400/20 text-cyan-700 dark:text-cyan-300 flex items-center justify-center group-hover:bg-cyan-500 group-hover:text-white transition-all duration-200">
                          <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
                        </span>
                      </div>
                    </div>
                  </article>
                );
              })}
            </div>
          ) : (
            /* Empty State */
            <div className="bg-slate-900/80 backdrop-blur-xl border border-white/15 rounded-3xl p-12 text-center max-w-xl mx-auto my-12 shadow-2xl">
              <span className="material-symbols-outlined text-cyan-400 text-[64px] mb-3">search_off</span>
              <h2 className="text-xl font-bold text-white mb-2">
                {language === 'en' ? 'No marine experiences found' : 'Không tìm thấy trải nghiệm phù hợp'}
              </h2>
              <p className="text-sm text-slate-300 mb-6 leading-relaxed">
                {language === 'en'
                  ? 'Try expanding your price range or clearing keyword filters to view other exciting ocean adventures.'
                  : 'Hãy thử nới rộng mức giá lọc hoặc xóa từ khóa tìm kiếm để khám phá các hoạt động biển hấp dẫn khác.'}
              </p>
              <button
                type="button"
                onClick={() => {
                  handleSelectCategory(null);
                  setKeyword('');
                  setPriceMax(1500000);
                  setSortBy('popular');
                }}
                className="px-6 py-2.5 rounded-full bg-gradient-to-r from-cyan-500 to-teal-500 text-white font-bold text-sm shadow-lg shadow-cyan-500/30 hover:scale-105 active:scale-95 transition-transform cursor-pointer"
              >
                {language === 'en' ? 'Show All Experiences' : 'Xem tất cả trải nghiệm biển'}
              </button>
            </div>
          )}
        </section>

      </div>
    </main>
  );
}

export default Search;
