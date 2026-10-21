import { useState, useMemo } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { FEATURED_SERVICES, CATEGORIES, MOCK_IMAGES } from '../../../mockData';
import { useWishlistStore } from '../../../store/useWishlistStore';

export function Search() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const categoryParam = searchParams.get('category');

  // Map category slug from URL parameter to CATEGORIES id
  const initialCatId = useMemo(() => {
    if (!categoryParam) return null;
    const lower = categoryParam.toLowerCase();
    if (lower === 'sup') return 'cat-1';
    if (lower === 'thrill' || lower.includes('cano') || lower.includes('jet')) return 'cat-2';
    if (lower === 'diving' || lower.includes('lặn') || lower.includes('san hô')) return 'cat-3';
    if (lower === 'kayak') return 'cat-4';
    if (lower === 'combo' || lower.includes('đảo') || lower.includes('tour')) return 'cat-5';
    const matched = CATEGORIES.find(c => c.id === categoryParam || c.name.toLowerCase().includes(lower));
    return matched ? matched.id : null;
  }, [categoryParam]);

  const { toggleWishlist, isInWishlist } = useWishlistStore();
  const [selectedCategory, setSelectedCategory] = useState<string | null>(initialCatId);
  const [priceMax, setPriceMax] = useState<number>(1500000);

  // Sync selected category when the URL category param changes (adjust state during render)
  const [prevCatId, setPrevCatId] = useState<string | null>(initialCatId);
  if (initialCatId !== prevCatId) {
    setPrevCatId(initialCatId);
    setSelectedCategory(initialCatId);
  }
  
  const displayServices = useMemo(() => {
    return FEATURED_SERVICES.filter(service => {
      if (selectedCategory && service.categoryId !== selectedCategory) return false;
      if (service.basePrice > priceMax) return false;
      return true;
    });
  }, [selectedCategory, priceMax]);

  return (
    <main className="w-full pt-20 bg-surface flex-1"><div className="flex flex-col w-full">
      {/* Top Ambient Banner / Weather Condition Ticker */}
      <section className="w-full bg-surface-container-low py-2.5 px-4 sm:px-8 lg:px-12 shadow-sm">
        <div className="max-w-[1440px] w-full mx-auto flex flex-wrap items-center justify-between gap-space-sm font-body-sm text-body-sm text-on-surface-variant">
          <div className="flex items-center gap-space-sm flex-wrap">
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-emerald-50 text-emerald-700 font-label-sm text-label-sm font-bold border border-emerald-200">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
              Thời tiết biển
            </span>
            <span className="hidden sm:inline-block">Chiều cao sóng: <strong>0.5m</strong></span>
            <span className="hidden md:inline-block">•</span>
            <span className="hidden md:inline-block">Tốc độ gió: <strong>12 km/h</strong></span>
            <span className="hidden lg:inline-block">•</span>
            <span className="hidden lg:inline-block">Lượng mưa: <strong>0.0 mm</strong></span>
          </div>
          <div className="flex items-center gap-space-md">
            <span className="flex items-center gap-1 text-secondary font-label-md text-label-md font-semibold">
              <span className="material-symbols-outlined text-[16px]">schedule</span>
              Cập nhật: 05:00 hôm nay
            </span>
          </div>
        </div>
      </section>
      
      {/* Horizontal Sticky Search Capsule Container (Phương án 1) */}
      <section className="sticky top-20 z-40 bg-surface/98 w-full border-b border-outline-variant/20 shadow-sm transition-all duration-300 transform-gpu will-change-transform">
        <div className="max-w-[1440px] mx-auto px-4 sm:px-8 lg:px-12 py-2 sm:py-2.5">
          <div className="bg-surface-container-lowest rounded-2xl p-2.5 md:p-3 shadow-[0_8px_24px_-4px_rgba(16,47,58,0.08)] border border-outline-variant/30">
            <form className="grid grid-cols-1 md:grid-cols-12 gap-3 items-center">
              {/* Region Dropdown */}
              <div className="md:col-span-4 flex items-center gap-3 px-4 py-3 rounded-full bg-surface-container-low hover:bg-surface-container transition-colors">
                <span className="material-symbols-outlined text-secondary text-[24px]">explore</span>
              <div className="flex flex-col min-w-0 flex-1">
                <label className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">Khu vực biển</label>
                <select className="bg-transparent font-label-lg text-label-lg text-on-surface outline-none cursor-pointer truncate">
                  <option value="all">Bãi biển Mỹ Khê, Sơn Trà, Non Nước</option>
                  <option value="mykhe">Bãi biển Mỹ Khê (SUP & Surf)</option>
                  <option value="sontra">Bán đảo Sơn Trà & Bãi Rạng</option>
                  <option value="nonnuoc">Bãi biển Non Nước & Ngũ Hành Sơn</option>
                  <option value="namo">Rạn Nam Ô hoang sơ</option>
                </select>
              </div>
            </div>
            {/* Date & Time Slot */}
            <div className="md:col-span-3 flex items-center gap-3 px-4 py-3 rounded-full bg-surface-container-low hover:bg-surface-container transition-colors">
              <span className="material-symbols-outlined text-primary-container text-[24px]">wb_twilight</span>
              <div className="flex flex-col min-w-0 flex-1">
                <label className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">Khởi hành</label>
                <div className="font-label-lg text-label-lg text-on-surface truncate flex items-center gap-1.5">
                  <span className="">Ngày mai</span>
                  <span className="px-2 py-0.5 rounded-full bg-primary-fixed text-on-primary-fixed-variant text-[11px] font-bold">05:30 Bình minh</span>
                </div>
              </div>
            </div>
            {/* Guest count */}
            <div className="md:col-span-3 flex items-center gap-3 px-4 py-3 rounded-full bg-surface-container-low hover:bg-surface-container transition-colors">
              <span className="material-symbols-outlined text-secondary text-[24px]">group</span>
              <div className="flex flex-col min-w-0 flex-1">
                <label className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">Số lượng khách</label>
                <div className="flex items-center justify-between pr-2">
                  <span className="font-label-lg text-label-lg text-on-surface">2 người (1 SUP đôi / 2 ván)</span>
                  <span className="material-symbols-outlined text-[18px] text-on-surface-variant">tune</span>
                </div>
              </div>
            </div>
            {/* Submit Button */}
            <div className="md:col-span-2">
              <button className="w-full h-[52px] rounded-full bg-primary-container text-on-primary font-label-lg text-label-lg flex items-center justify-center gap-2 shadow-[0_8px_20px_rgba(255,115,92,0.35)] hover:scale-[1.02] active:scale-[0.98] transition-transform" type="button">
                <span className="material-symbols-outlined text-[20px]">search</span>
                <span className="">Tìm kiếm</span>
              </button>
            </div>
          </form>
        </div>
      </div>
    </section>
      
      {/* Results Control Bar */}
      <section className="w-full max-w-[1440px] mx-auto px-4 sm:px-8 lg:px-12 py-1 sm:py-1.5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-surface-container-lowest px-4 py-2.5 sm:px-5 sm:py-3 rounded-xl shadow-sm">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-full bg-secondary-fixed flex items-center justify-center text-on-secondary-fixed shrink-0">
              <span className="material-symbols-outlined text-[18px]">waves</span>
            </div>
            <div>
              <h1 className="font-headline-sm text-headline-sm text-on-surface tracking-tight font-bold text-base sm:text-lg">
                Tìm thấy {displayServices.length} kết quả phù hợp
              </h1>
            </div>
          </div>
          {/* Controls: List/Map Switch & Sorting Dropdown */}
          <div className="flex flex-wrap items-center gap-space-sm">
            <div className="flex items-center gap-2 bg-surface-container-low px-3.5 py-1.5 rounded-full">
              <span className="font-label-sm text-label-sm text-on-surface-variant">Sắp xếp:</span>
              <select className="bg-transparent font-label-sm text-label-sm text-on-surface font-bold outline-none cursor-pointer">
                <option value="popular">Phổ biến nhất</option>
                <option value="price_asc">Giá thấp đến cao</option>
                <option value="rating_desc">Đánh giá cao nhất</option>
              </select>
            </div>
          </div>
        </div>
      </section>
      
      {/* Main Content Layout (Sidebar Filter + Experience Grid) */}
      <section className="w-full max-w-[1440px] mx-auto px-4 sm:px-8 lg:px-12 pt-2 pb-14">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-space-xl items-start">
          
          {/* Left Sidebar: Filter Column */}
          <aside className="lg:col-span-3 flex flex-col gap-space-lg bg-surface-container-lowest p-space-lg rounded-xl shadow-[0_4px_20px_-2px_rgba(16,47,58,0.05)] sticky top-[172px]">
            <div className="flex items-center justify-between pb-space-sm">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-secondary">tune</span>
                <h2 className="font-headline-sm text-headline-sm text-on-surface">Bộ lọc tìm kiếm</h2>
              </div>
              <button 
                onClick={() => { setSelectedCategory(null); setPriceMax(1500000); }} 
                className="font-label-sm text-label-sm text-primary hover:underline" type="button"
              >
                Xóa tất cả
              </button>
            </div>
            
            {/* 1. Activity Types */}
            <div className="flex flex-col gap-space-xs">
              <label className="font-label-lg text-label-lg text-on-surface flex items-center justify-between">
                <span className="">Loại hoạt động</span>
              </label>
              <div className="flex flex-col gap-2 mt-1">
                {CATEGORIES.map(cat => (
                  <label key={cat.id} className={`flex items-center gap-2.5 cursor-pointer font-body-sm text-body-sm transition-colors ${selectedCategory === cat.id ? 'text-secondary font-semibold' : 'text-on-surface-variant hover:text-on-surface'}`}>
                    <input 
                      checked={selectedCategory === cat.id} 
                      onChange={() => setSelectedCategory(selectedCategory === cat.id ? null : cat.id)}
                      className="w-4 h-4 rounded accent-secondary cursor-pointer" type="checkbox"
                    />
                    <span>{cat.name}</span>
                  </label>
                ))}
              </div>
            </div>
            <div className="w-full h-px bg-surface-container"></div>
            
            {/* 2. Price Range Slider */}
            <div className="flex flex-col gap-space-xs">
              <div className="flex items-center justify-between">
                <label className="font-label-lg text-label-lg text-on-surface">Mức giá tối đa (VND)</label>
                <span className="font-label-sm text-label-sm text-primary-container font-bold">
                  {priceMax >= 1500000 ? "Tất cả mức giá" : `${priceMax.toLocaleString('vi-VN')} đ`}
                </span>
              </div>
              <input 
                value={priceMax}
                onChange={(e) => setPriceMax(Number(e.target.value))}
                className="w-full accent-primary cursor-pointer mt-2" 
                max="1500000" min="200000" step="50000" type="range"
              />
              <div className="flex justify-between font-label-sm text-label-sm text-on-surface-variant">
                <span className="">200.000 đ</span>
                <span className="">1.5M+ đ</span>
              </div>
            </div>
            
          </aside>
          
          {/* Right Column: Results Grid & Interactive Views */}
          <main className="lg:col-span-9 flex flex-col gap-space-xl">
            {/* Default Grid View Container */}
            {displayServices.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-space-lg">
                {displayServices.map(service => {
                  const image = MOCK_IMAGES.find(i => i.serviceId === service.id && i.isPrimary)?.imageUrl || "https://via.placeholder.com/400";
                  return (
                    <article key={service.id} className="flex flex-col bg-surface-container-lowest rounded-lg overflow-hidden shadow-[0_4px_20px_-2px_rgba(16,47,58,0.05)] hover:shadow-[0_12px_32px_-4px_rgba(16,47,58,0.08)] hover:-translate-y-1 transition-all duration-300 group">
                      <div className="relative w-full aspect-[4/3] overflow-hidden bg-surface-container cursor-pointer" onClick={() => navigate(`/tour/${service.id}`)}>
                        <img src={image} alt={service.name} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500" />
                        <div className="absolute top-3 left-3 flex flex-col gap-1.5 items-start">
                          <span className="px-3 py-1 rounded-full bg-primary-container text-on-primary font-label-sm text-label-sm font-bold shadow-sm">
                            {service.status === 'APPROVED' ? 'Đang mở' : 'Hết chỗ'}
                          </span>
                        </div>
                        {(() => {
                          const isSaved = isInWishlist(service.id);
                          return (
                            <button
                              aria-label={isSaved ? "Bỏ lưu khỏi yêu thích" : "Lưu vào yêu thích"}
                              className={`absolute top-3 right-3 w-9 h-9 rounded-full backdrop-blur-md flex items-center justify-center transition-all shadow-md cursor-pointer hover:scale-110 active:scale-95 ${
                                isSaved
                                  ? "bg-white text-red-500 ring-2 ring-red-100"
                                  : "bg-surface-container-lowest/90 text-on-surface-variant hover:text-red-500"
                              }`}
                              onClick={(e) => {
                                e.preventDefault();
                                e.stopPropagation();
                                toggleWishlist(service.id, service.name);
                              }}
                              type="button"
                            >
                              <span
                                className={`material-symbols-outlined text-[20px] transition-transform ${isSaved ? "text-red-500" : ""}`}
                                style={{ fontVariationSettings: isSaved ? "'FILL' 1" : "'FILL' 0" }}
                              >
                                favorite
                              </span>
                            </button>
                          );
                        })()}
                        <div className="absolute bottom-3 left-3 right-3 flex items-center justify-between text-on-primary text-[12px] font-medium bg-gradient-to-t from-on-background/80 to-transparent p-2 rounded-lg">
                          <span className="flex items-center gap-1">
                            <span className="material-symbols-outlined text-[14px]">location_on</span>
                            {service.locationName}
                          </span>
                          <span className="flex items-center gap-1">
                            <span className="material-symbols-outlined text-[14px]">schedule</span>
                            {service.durationMinutes} phút
                          </span>
                        </div>
                      </div>
                      <div className="p-space-md flex flex-col flex-1 justify-between gap-space-sm cursor-pointer" onClick={() => navigate(`/tour/${service.id}`)}>
                        <div>
                          <div className="flex items-center justify-between mb-1.5">
                            <div className="flex items-center gap-1">
                              <span className="material-symbols-outlined text-primary-container text-[18px]" style={{ fontVariationSettings: "'FILL' 1" }}>star</span>
                              <span className="font-label-lg text-label-lg text-on-surface">4.9</span>
                              <span className="font-body-sm text-body-sm text-on-surface-variant">(128 đánh giá)</span>
                            </div>
                          </div>
                          <h3 className="font-headline-sm text-headline-sm text-on-surface group-hover:text-secondary transition-colors line-clamp-2">
                            {service.name}
                          </h3>
                          <p className="font-body-sm text-body-sm text-on-surface-variant mt-1 line-clamp-2">
                            {service.description}
                          </p>
                        </div>
                        <div className="pt-space-xs flex items-center justify-between gap-2 border-t border-surface-container">
                          <div>
                            <span className="font-label-sm text-label-sm text-on-surface-variant block">Đơn giá trọn gói</span>
                            <div className="flex items-baseline gap-1">
                              <span className="font-headline-md text-headline-md text-primary-container font-extrabold">{service.basePrice.toLocaleString('vi-VN')}đ</span>
                              <span className="font-body-sm text-body-sm text-primary-container font-medium">/ người</span>
                            </div>
                          </div>
                          <button onClick={(e) => { e.stopPropagation(); navigate(`/tour/${service.id}`); }} className="px-4 py-2.5 rounded-full bg-primary text-on-primary font-label-md text-label-md hover:scale-[1.03] active:scale-[0.98] transition-all shadow-sm flex items-center gap-1" type="button">
                            <span className="">Xem chi tiết</span>
                            <span className="material-symbols-outlined text-[16px]">arrow_forward</span>
                          </button>
                        </div>
                      </div>
                    </article>
                  )
                })}
              </div>
            ) : (
              <div className="flex flex-col items-center justify-center py-20">
                <span className="material-symbols-outlined text-[64px] text-outline mb-4">search_off</span>
                <h3 className="font-headline-md text-on-surface">Không tìm thấy kết quả nào</h3>
                <p className="font-body-md text-on-surface-variant mt-2">Vui lòng điều chỉnh lại bộ lọc để xem thêm các dịch vụ khác.</p>
                <button onClick={() => { setSelectedCategory(null); setPriceMax(1500000); }} className="mt-6 px-6 py-2 bg-primary text-on-primary rounded-full hover:bg-primary-container transition-colors font-label-lg">Xóa bộ lọc</button>
              </div>
            )}
            
          </main>
        </div>
      </section>
    </div></main>
  );
}
