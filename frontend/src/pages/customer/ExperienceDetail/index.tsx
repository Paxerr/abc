import { useState, useEffect, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useCartStore } from '../../../store/useCartStore';
import { useWishlistStore } from '../../../store/useWishlistStore';
import { serviceCatalogService } from '../../../services/serviceCatalogService';
import type { ServiceDetailDto, ServiceOptionDto, StructuredSlotDto } from '../../../types/service';
import { FEATURED_SERVICES, MOCK_SLOTS, MOCK_IMAGES } from '../../../mockData';

export function ExperienceDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [service, setService] = useState<ServiceDetailDto | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [loadError, setLoadError] = useState<boolean>(false);

  // Booking selections
  const [selectedOptionId, setSelectedOptionId] = useState<string | null>(null);
  const [selectedDate, setSelectedDate] = useState<string>('');
  const [selectedSlotId, setSelectedSlotId] = useState<string | null>(null);
  const [qty, setQty] = useState<number>(1);
  const [isWaiverChecked, setIsWaiverChecked] = useState<boolean>(false);
  const [isAdding, setIsAdding] = useState<boolean>(false);
  const [showAddSuccess, setShowAddSuccess] = useState<boolean>(false);

  const addItem = useCartStore((state) => state.addItem);
  const { toggleWishlist, isInWishlist } = useWishlistStore();

  // Fetch Service Detail from Backend API (with graceful fallback to mock if legacy ID)
  useEffect(() => {
    if (!id) {
      setLoadError(true);
      setIsLoading(false);
      return;
    }

    let isMounted = true;
    setIsLoading(true);
    setLoadError(false);

    serviceCatalogService
      .getServiceDetail(id)
      .then((data) => {
        if (!isMounted) return;
        setService(data);

        // Auto-select first option
        if (data.options && data.options.length > 0) {
          setSelectedOptionId(data.options[0].id);
        }

        // Auto-select first available date and slot
        if (data.slots && data.slots.length > 0) {
          const firstSlot = data.slots[0];
          setSelectedDate(firstSlot.date);
          setSelectedSlotId(firstSlot.slotId);
        }
      })
      .catch((err) => {
        if (!isMounted) return;
        console.warn(`[ExperienceDetail] Không thể tải tour ${id} từ Backend, kiểm tra mock data:`, err);
        
        // Fallback for legacy demo ID (e.g. '1', '2'...)
        const fallback = FEATURED_SERVICES.find((s) => s.id === id);
        if (fallback) {
          const fallbackSlots: StructuredSlotDto[] = MOCK_SLOTS
            .filter((ms) => ms.serviceId === fallback.id)
            .map((ms) => ({
              slotId: ms.id,
              date: ms.date,
              startTime: ms.startTime,
              endTime: ms.endTime,
              capacity: ms.capacity,
              availableCapacity: Math.max(0, ms.capacity - ms.bookedCount),
              price: fallback.basePrice,
              bookable: ms.capacity - ms.bookedCount > 0,
            }));

          const fallbackImages: string[] = MOCK_IMAGES
            .filter((mi) => mi.serviceId === fallback.id)
            .map((mi) => mi.imageUrl || mi.url || '')
            .filter((url): url is string => Boolean(url));

          const mappedFallback: ServiceDetailDto = {
            id: fallback.id,
            name: fallback.name,
            description: fallback.description || '',
            price: fallback.basePrice,
            address: fallback.locationName,
            averageRating: 4.95,
            reviewCount: 128,
            viewCount: 1250,
            categoryId: 'a0000001-0000-0000-0000-000000000001',
            categoryName: 'Thể thao biển',
            imageUrls: fallbackImages.length > 0 ? fallbackImages : ['https://images.unsplash.com/photo-1502680390469-be75c86b636f?w=800'],
            options: [
              {
                id: `opt-${fallback.id}`,
                serviceId: fallback.id,
                name: 'Vé Tiêu Chuẩn',
                optionType: 'SHARED',
                pricingUnit: 'PER_PERSON',
                price: fallback.basePrice,
                maxPaxPerPackage: 1,
                benefits: 'Áo phao, người hướng dẫn, nước uống, bảo hiểm du lịch.',
                status: 'ACTIVE',
              },
            ],
            slots: fallbackSlots,
            vendorId: '22222222-2222-2222-2222-222222222221',
            businessName: 'DANASEA Official Partner',
            badgeTier: 'VERIFIED',
            duration: fallback.durationMinutes || 120,
            capacity: fallback.capacityPerSlot || 15,
            waiverRequired: true,
            waiverContent: fallback.waiverContent || 'Khách hàng cam kết đủ sức khỏe tham gia, bắt buộc mặc áo phao và tuân thủ hướng dẫn an toàn biển.',
            cancellationPolicy: 'Hoàn tiền 100% trước 24 giờ. Hoàn tiền 100% nếu biển động theo quy định cảng vụ.',
          };

          setService(mappedFallback);
          if (mappedFallback.options.length > 0) setSelectedOptionId(mappedFallback.options[0].id);
          if (mappedFallback.slots.length > 0) {
            setSelectedDate(mappedFallback.slots[0].date);
            setSelectedSlotId(mappedFallback.slots[0].slotId);
          }
        } else {
          setLoadError(true);
        }
      })
      .finally(() => {
        if (isMounted) setIsLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [id]);

  // Derived selected option
  const selectedOption: ServiceOptionDto | undefined = useMemo(() => {
    if (!service || !service.options) return undefined;
    return service.options.find((opt) => opt.id === selectedOptionId) || service.options[0];
  }, [service, selectedOptionId]);

  // Unique dates from slots
  const availableDates: string[] = useMemo(() => {
    if (!service || !service.slots || service.slots.length === 0) return [];
    const dateSet = new Set<string>();
    service.slots.forEach((s) => dateSet.add(s.date));
    return Array.from(dateSet).sort();
  }, [service]);

  // Slots for the currently chosen date
  const slotsForSelectedDate: StructuredSlotDto[] = useMemo(() => {
    if (!service || !service.slots || !selectedDate) return [];
    return service.slots.filter((s) => s.date === selectedDate);
  }, [service, selectedDate]);

  // Active slot object
  const activeSlot: StructuredSlotDto | undefined = useMemo(() => {
    if (!slotsForSelectedDate) return undefined;
    return slotsForSelectedDate.find((s) => s.slotId === selectedSlotId) || slotsForSelectedDate[0];
  }, [slotsForSelectedDate, selectedSlotId]);

  // Pricing calculation
  const unitPrice = selectedOption ? selectedOption.price : (service?.price || 0);
  const totalPrice = qty * unitPrice;

  // Capacity validation
  const maxCapacity = activeSlot ? activeSlot.availableCapacity : 10;
  const isSoldOut = activeSlot ? activeSlot.availableCapacity <= 0 : false;

  // Pure Vietnamese normalizers for demo/seed data
  const localizedSafetyRules = useMemo(() => {
    const raw = service?.safetyRules || '';
    if (raw.toLowerCase().includes('max wave') || raw.toLowerCase().includes('weather-sensitive')) {
      const wave = raw.match(/max wave:\s*([\d.]+m)/i)?.[1] || '0.8m';
      const wind = raw.match(/max wind:\s*([\d.]+\s*km\/h)/i)?.[1] || '5km/h';
      return `Sóng biển ≤ ${wave}, gió ≤ ${wind}. Bắt buộc mặc áo phao và tuân thủ an toàn hàng hải.`;
    }
    return raw || 'Sóng biển ≤ 1.0m, gió ≤ 15km/h. Bắt buộc mặc áo phao và tuân thủ an toàn hàng hải.';
  }, [service?.safetyRules]);

  const localizedWaiverContent = useMemo(() => {
    const raw = service?.waiverContent || '';
    if (raw.toLowerCase().includes('participants must confirm') || !raw) {
      return 'Khách hàng cam kết đủ sức khỏe tham gia, bắt buộc mặc áo phao trong suốt thời gian trên biển và tuân thủ chỉ dẫn an toàn của huấn luyện viên.';
    }
    return raw;
  }, [service?.waiverContent]);

  const handleAddToCart = () => {
    if (!service) return;
    if (!activeSlot) {
      alert('Vui lòng chọn khung giờ khởi hành.');
      return;
    }
    if (service.waiverRequired && !isWaiverChecked) {
      alert('Vui lòng đồng ý với Điều khoản an toàn & miễn trừ trách nhiệm trước khi đặt vé.');
      return;
    }

    setIsAdding(true);
    setTimeout(() => {
      addItem({
        serviceId: service.id,
        slotId: activeSlot.slotId,
        quantity: qty,
        unitPrice: unitPrice,
        serviceName: `${service.name}${selectedOption ? ` (${selectedOption.name})` : ''}`,
        thumbnailUrl: (service.imageUrls && service.imageUrls.length > 0) ? service.imageUrls[0] : 'https://images.unsplash.com/photo-1502680390469-be75c86b636f?w=800',
        date: activeSlot.date,
        timeSlot: `${activeSlot.startTime} - ${activeSlot.endTime}`,
        selectedDate: activeSlot.date,
      });
      setIsAdding(false);
      setShowAddSuccess(true);
      setTimeout(() => setShowAddSuccess(false), 3000);
    }, 400);
  };

  // 1. Loading State (Transparent Ocean Glass Skeleton)
  if (isLoading) {
    return (
      <main className="w-full pt-28 pb-20 bg-slate-900/10 min-h-screen flex flex-col items-center">
        <div className="max-w-[1440px] w-full px-4 sm:px-8 lg:px-12 animate-pulse space-y-8">
          <div className="h-6 bg-slate-200/70 rounded-md w-1/4"></div>
          <div className="h-10 bg-slate-200/70 rounded-xl w-3/4"></div>
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 h-[420px]">
            <div className="lg:col-span-7 bg-slate-200/70 rounded-2xl"></div>
            <div className="lg:col-span-5 grid grid-cols-2 gap-4">
              <div className="bg-slate-200/70 rounded-2xl"></div>
              <div className="bg-slate-200/70 rounded-2xl"></div>
              <div className="bg-slate-200/70 rounded-2xl"></div>
              <div className="bg-slate-200/70 rounded-2xl"></div>
            </div>
          </div>
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
            <div className="lg:col-span-8 space-y-4">
              <div className="h-40 bg-slate-200/70 rounded-2xl"></div>
              <div className="h-40 bg-slate-200/70 rounded-2xl"></div>
            </div>
            <div className="lg:col-span-4 h-96 bg-slate-200/70 rounded-2xl"></div>
          </div>
        </div>
      </main>
    );
  }

  // 2. Not Found / Error State
  if (loadError || !service) {
    return (
      <main className="w-full pt-32 pb-20 bg-slate-50 flex-1 flex flex-col items-center justify-center min-h-[70vh]">
        <div className="w-20 h-20 rounded-full bg-cyan-50 border border-cyan-100 flex items-center justify-center text-cyan-600 mb-5 shadow-sm">
          <span className="material-symbols-outlined text-[48px]">search_off</span>
        </div>
        <h2 className="text-2xl font-bold text-slate-800">Không tìm thấy trải nghiệm</h2>
        <p className="text-slate-500 mt-2 text-center max-w-md">
          Trải nghiệm bạn đang tìm kiếm không tồn tại, chưa được mở bán hoặc đã bị gỡ khỏi hệ thống.
        </p>
        <button
          onClick={() => navigate('/search')}
          className="mt-6 px-8 py-3 bg-gradient-to-r from-teal-600 to-cyan-600 hover:from-teal-500 hover:to-cyan-500 text-white rounded-full transition-all shadow-md hover:shadow-cyan-500/20 font-semibold cursor-pointer active:scale-95"
        >
          Về trang Khám phá
        </button>
      </main>
    );
  }

  const primaryImage = (service.imageUrls && service.imageUrls.length > 0)
    ? service.imageUrls[0]
    : 'https://images.unsplash.com/photo-1502680390469-be75c86b636f?w=800';
  const secondaryImages = (service.imageUrls && service.imageUrls.length > 1)
    ? service.imageUrls.slice(1, 5)
    : [primaryImage];
  const isSaved = isInWishlist(service.id);

  return (
    <main className="w-full pt-20 bg-slate-50/60 flex-1">
      <div className="flex flex-col w-full">
        {/* Breadcrumb Navigation */}
        <div className="w-full bg-white/70 backdrop-blur-md border-b border-slate-200/60">
          <div className="max-w-[1440px] w-full mx-auto px-4 sm:px-8 lg:px-12 py-3">
            <nav className="flex items-center gap-2 text-xs sm:text-sm text-slate-500 overflow-x-auto whitespace-nowrap">
              <span
                onClick={() => navigate('/')}
                className="hover:text-cyan-700 transition-colors flex items-center gap-1 cursor-pointer"
              >
                <span className="material-symbols-outlined text-[16px]">home</span>
                Trang chủ
              </span>
              <span className="text-slate-300">/</span>
              <span
                onClick={() => navigate('/search')}
                className="hover:text-cyan-700 transition-colors cursor-pointer"
              >
                Khám phá
              </span>
              <span className="text-slate-300">/</span>
              {service.categoryName && (
                <>
                  <span className="text-slate-600 font-medium">{service.categoryName}</span>
                  <span className="text-slate-300">/</span>
                </>
              )}
              <span className="text-slate-900 font-bold truncate max-w-[280px] sm:max-w-md">{service.name}</span>
            </nav>
          </div>
        </div>

        <div className="max-w-[1440px] mx-auto w-full px-4 sm:px-8 lg:px-12 pt-6 pb-20 flex flex-col gap-8">
          
          {/* Title & Quick Actions Header */}
          <section className="flex flex-col lg:flex-row lg:items-end justify-between gap-4">
            <div className="flex flex-col gap-2 max-w-3xl">
              <div className="flex flex-wrap items-center gap-2">
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-teal-50 text-teal-800 border border-teal-200/70 text-xs font-semibold shadow-xs">
                  <span className="material-symbols-outlined text-[14px]">verified</span>
                  {service.businessName || 'Nhà cung cấp đã xác minh'}
                </span>
                {service.badgeTier && (
                  <span className="px-2.5 py-0.5 rounded-full bg-cyan-50 text-cyan-800 border border-cyan-200 text-xs font-bold uppercase tracking-wider">
                    {service.badgeTier}
                  </span>
                )}
              </div>
              <h1 className="text-2xl sm:text-3xl lg:text-4xl font-extrabold text-slate-900 tracking-tight leading-snug">
                {service.name}
              </h1>
              <div className="flex flex-wrap items-center gap-y-2 gap-x-4 text-xs sm:text-sm text-slate-600">
                <div className="flex items-center gap-1 text-slate-900 font-semibold">
                  <span className="text-amber-500">★</span>
                  <span>{service.averageRating ? service.averageRating.toFixed(2) : '5.0'}</span>
                  <span className="text-slate-500 font-normal">({service.reviewCount || 100} đánh giá)</span>
                </div>
                <span className="text-slate-300">•</span>
                <div className="flex items-center gap-1 text-slate-700">
                  <span className="material-symbols-outlined text-[18px] text-cyan-600">schedule</span>
                  <span>{service.duration || 120} phút</span>
                </div>
                <span className="text-slate-300">•</span>
                <div className="flex items-center gap-1 text-slate-700">
                  <span className="material-symbols-outlined text-[18px] text-cyan-600">visibility</span>
                  <span>{service.viewCount || 1500} lượt xem</span>
                </div>
                {service.address && (
                  <>
                    <span className="text-slate-300">•</span>
                    <div className="flex items-center gap-1 text-slate-700 truncate max-w-xs sm:max-w-md">
                      <span className="material-symbols-outlined text-[18px] text-cyan-600 shrink-0">location_on</span>
                      <span className="truncate">{service.address}</span>
                    </div>
                  </>
                )}
              </div>
            </div>

            {/* Wishlist Quick Action */}
            <div className="flex items-center gap-3 self-start lg:self-center shrink-0">
              <button
                type="button"
                onClick={() => toggleWishlist(service.id, service.name)}
                className={`px-4 py-2.5 rounded-full text-xs sm:text-sm font-semibold flex items-center gap-2 border transition-all shadow-xs cursor-pointer hover:scale-105 active:scale-95 ${
                  isSaved
                    ? 'bg-rose-50 border-rose-200 text-rose-600 ring-2 ring-rose-100'
                    : 'bg-white border-slate-200 text-slate-700 hover:border-rose-300 hover:text-rose-600'
                }`}
              >
                <span className={`text-base ${isSaved ? 'text-rose-500' : 'text-slate-400'}`}>
                  {isSaved ? '♥' : '♡'}
                </span>
                <span>{isSaved ? 'Đã lưu yêu thích' : 'Lưu vào yêu thích'}</span>
              </button>
            </div>
          </section>

          {/* Editorial Gallery Grid */}
          <section className="w-full">
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-3 rounded-2xl overflow-hidden shadow-md bg-white border border-slate-200/80 p-2">
              <div className="lg:col-span-7 relative group cursor-pointer overflow-hidden rounded-xl min-h-[300px] sm:min-h-[420px]">
                <img
                  src={primaryImage}
                  alt={service.name}
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700 ease-out"
                />
              </div>
              <div className="lg:col-span-5 grid grid-cols-2 gap-3">
                {secondaryImages.map((imgUrl, idx) => (
                  <div key={idx} className="relative group cursor-pointer overflow-hidden rounded-xl aspect-[4/3] bg-slate-100">
                    <img
                      src={imgUrl}
                      alt={`${service.name} - ${idx + 2}`}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500 ease-out"
                    />
                  </div>
                ))}
              </div>
            </div>
          </section>

          {/* Main Content Layout */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
            
            {/* LEFT COLUMN: Detailed Experience Intel */}
            <main className="lg:col-span-8 flex flex-col gap-5">
              
              {/* 1. Quy định & Lưu ý an toàn biển (Đơn giản 1 dòng chú ý đặt đầu tiên) */}
              <div className="flex items-start sm:items-center gap-2.5 p-3 sm:px-4 sm:py-2.5 rounded-xl bg-amber-50/90 border border-amber-200/80 text-amber-900 text-xs sm:text-sm">
                <span className="material-symbols-outlined text-amber-600 text-[20px] shrink-0 mt-0.5 sm:mt-0">health_and_safety</span>
                <p className="leading-snug">
                  <strong className="font-semibold text-amber-950">Lưu ý an toàn &amp; hoàn hủy:</strong> Bắt buộc mặc áo phao, đủ sức khỏe tham gia và tuân thủ huấn luyện viên. Miễn phí đổi ngày hoặc hoàn tiền 100% nếu hủy trước 24h hoặc do biển động theo lệnh cảng vụ.
                </p>
              </div>

              {/* 2. Mô tả trải nghiệm & Thông tin vận hành (Gộp vào 1 ô nội dung duy nhất) */}
              <section className="bg-white p-6 sm:p-7 rounded-2xl border border-slate-200/80 shadow-xs space-y-5">
                <div>
                  <h2 className="text-xl font-bold text-slate-900 mb-2">Mô tả trải nghiệm</h2>
                  <p className="text-slate-700 leading-relaxed text-[15px] whitespace-pre-line">
                    {service.description}
                  </p>
                </div>

                <div className="pt-4 border-t border-slate-100 space-y-3">
                  <h3 className="text-xs font-bold text-slate-500 uppercase tracking-wider">Thông tin vận hành</h3>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-2.5 text-xs sm:text-sm">
                    <div className="flex items-center gap-2 text-slate-700">
                      <span className="material-symbols-outlined text-cyan-600 text-[18px] shrink-0">schedule</span>
                      <span className="text-slate-500">Thời lượng:</span>
                      <span className="font-semibold text-slate-900">{service.duration || 120} phút / ca</span>
                    </div>
                    <div className="flex items-center gap-2 text-slate-700">
                      <span className="material-symbols-outlined text-cyan-600 text-[18px] shrink-0">group</span>
                      <span className="text-slate-500">Sức chứa:</span>
                      <span className="font-semibold text-slate-900">Tối đa {service.capacity || 15} người / ca</span>
                    </div>
                    <div className="flex items-center gap-2 text-slate-700">
                      <span className="material-symbols-outlined text-cyan-600 text-[18px] shrink-0">waves</span>
                      <span className="text-slate-500">Điều kiện thời tiết:</span>
                      <span className="font-semibold text-slate-900">{localizedSafetyRules}</span>
                    </div>
                    <div className="flex items-center gap-2 text-slate-700 min-w-0">
                      <span className="material-symbols-outlined text-cyan-600 text-[18px] shrink-0">location_on</span>
                      <span className="text-slate-500 shrink-0">Điểm tập trung:</span>
                      <span className="font-semibold text-slate-900 truncate" title={service.address}>{service.address || 'Ven biển Đà Nẵng'}</span>
                    </div>
                  </div>
                </div>
              </section>

              {/* Service Options (Gói dịch vụ) */}
              {service.options && service.options.length > 0 && (
                <section className="bg-white p-6 sm:p-7 rounded-2xl border border-slate-200/80 shadow-xs space-y-4">
                  <div className="flex items-center justify-between">
                    <h2 className="text-xl font-bold text-slate-900">Lựa chọn gói dịch vụ</h2>
                    <span className="text-xs text-cyan-700 font-semibold">{service.options.length} gói có sẵn</span>
                  </div>
                  <div className="grid grid-cols-1 gap-3">
                    {service.options.map((opt) => {
                      const isSelected = selectedOptionId === opt.id;
                      return (
                        <div
                          key={opt.id}
                          onClick={() => setSelectedOptionId(opt.id)}
                          className={`p-4 rounded-xl border-2 transition-all cursor-pointer flex flex-col sm:flex-row sm:items-center justify-between gap-3 ${
                            isSelected
                              ? 'bg-cyan-50/50 border-cyan-500 shadow-sm'
                              : 'bg-slate-50/60 border-slate-200/70 hover:border-slate-300'
                          }`}
                        >
                          <div className="space-y-1">
                            <div className="flex items-center gap-2">
                              <span className="font-bold text-slate-900 text-sm sm:text-base">{opt.name}</span>
                              <span className="px-2 py-0.5 rounded-md bg-white border border-slate-200 text-[11px] font-medium text-slate-600">
                                {opt.optionType === 'PRIVATE' ? 'Bao trọn gói' : 'Vé ghép'}
                              </span>
                            </div>
                            {opt.benefits && (
                              <p className="text-xs text-slate-600 leading-normal">{opt.benefits}</p>
                            )}
                          </div>
                          <div className="text-left sm:text-right shrink-0">
                            <span className="font-extrabold text-base sm:text-lg text-cyan-700 block">
                              {opt.price.toLocaleString('vi-VN')}₫
                            </span>
                            <span className="text-[11px] text-slate-500">
                              {opt.pricingUnit === 'PER_PERSON' ? '/ người' : '/ gói'}
                            </span>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </section>
              )}

              {/* Verified Vendor Card */}
              <section className="bg-white p-6 sm:p-7 rounded-2xl border border-slate-200/80 shadow-xs flex items-center justify-between gap-4">
                <div className="flex items-center gap-4">
                  <div className="w-14 h-14 rounded-2xl bg-teal-50 border border-teal-100 flex items-center justify-center text-teal-700 font-bold text-xl">
                    <span className="material-symbols-outlined text-3xl">sailing</span>
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="font-bold text-slate-900 text-base">{service.businessName || 'DANASEA Official Partner'}</h3>
                      <span className="px-2 py-0.5 rounded-full bg-teal-50 text-teal-700 text-xs font-semibold border border-teal-200">
                        {service.badgeTier || 'Đã xác minh'}
                      </span>
                    </div>
                    <p className="text-xs text-slate-500 mt-1">Đầy đủ giấy phép kinh doanh thể thao biển &amp; chứng chỉ an toàn hàng hải</p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => navigate('/search')}
                  className="px-4 py-2 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold transition-colors shrink-0 cursor-pointer"
                >
                  Xem thêm dịch vụ
                </button>
              </section>
            </main>

            {/* RIGHT COLUMN: Sticky Interactive Booking Box */}
            <aside className="lg:col-span-4 w-full lg:sticky lg:top-24">
              <div className="w-full p-6 rounded-2xl bg-white border border-slate-200/80 shadow-lg flex flex-col gap-5">
                
                {/* Price Display */}
                <div className="border-b border-slate-100 pb-4 flex flex-col gap-2">
                  <div className="flex items-baseline gap-1.5 flex-wrap">
                    <span className="text-3xl font-extrabold text-cyan-700 tracking-tight">
                      {unitPrice.toLocaleString('vi-VN')}₫
                    </span>
                    <span className="text-xs text-slate-500 font-medium whitespace-nowrap">
                      {selectedOption?.pricingUnit === 'PER_PACKAGE' ? '/ gói' : '/ người'}
                    </span>
                  </div>
                  {selectedOption && (
                    <div className="flex items-center gap-1.5 text-xs text-slate-700 bg-slate-50 border border-slate-200/80 px-2.5 py-1.5 rounded-lg w-full">
                      <span className="material-symbols-outlined text-cyan-600 text-[15px] shrink-0">confirmation_number</span>
                      <span className="text-slate-500 shrink-0">Đang chọn:</span>
                      <span className="font-semibold text-slate-900 truncate">{selectedOption.name}</span>
                    </div>
                  )}
                </div>

                {/* Date Selection */}
                <div className="flex flex-col gap-1.5">
                  <label className="text-xs uppercase tracking-wider text-slate-600 font-bold">Ngày khởi hành</label>
                  {availableDates.length > 0 ? (
                    <select
                      value={selectedDate}
                      onChange={(e) => {
                        const newDate = e.target.value;
                        setSelectedDate(newDate);
                        const slotsOnDate = service?.slots?.filter((s) => s.date === newDate) || [];
                        const firstBookable = slotsOnDate.find((s) => s.bookable && s.availableCapacity > 0) || slotsOnDate[0];
                        setSelectedSlotId(firstBookable ? firstBookable.slotId : null);
                      }}
                      className="w-full p-3 rounded-xl bg-slate-50 border border-slate-200 text-slate-800 text-sm font-medium cursor-pointer hover:bg-slate-100/80 transition-colors outline-none focus:ring-2 focus:ring-cyan-500"
                    >
                      {availableDates.map((d) => (
                        <option key={d} value={d}>
                          {d === new Date().toISOString().split('T')[0] ? `Hôm nay (${d})` : d}
                        </option>
                      ))}
                    </select>
                  ) : (
                    <div className="p-3 rounded-xl bg-slate-50 text-xs text-slate-500">
                      Chưa có lịch mở bán cho dịch vụ này
                    </div>
                  )}
                </div>

                {/* Structured Slots Selection */}
                <div className="flex flex-col gap-1.5">
                  <label className="text-xs uppercase tracking-wider text-slate-600 font-bold">Khung giờ khởi hành</label>
                  <div className="flex flex-col gap-2">
                    {slotsForSelectedDate.length === 0 ? (
                      <p className="text-slate-500 text-xs text-center py-2 bg-slate-50 rounded-xl">
                        Không có ca giờ nào trong ngày này.
                      </p>
                    ) : (
                      slotsForSelectedDate.map((slot) => {
                        const isSelected = activeSlot?.slotId === slot.slotId;
                        const remain = slot.availableCapacity;
                        const isFull = remain <= 0 || !slot.bookable;

                        return (
                          <div
                            key={slot.slotId}
                            onClick={() => !isFull && setSelectedSlotId(slot.slotId)}
                            className={`p-3 rounded-xl border-2 transition-all cursor-pointer flex items-center justify-between ${
                              isSelected
                                ? 'bg-cyan-50/70 border-cyan-500 shadow-xs'
                                : isFull
                                ? 'bg-slate-100 border-transparent opacity-50 cursor-not-allowed'
                                : 'bg-slate-50 border-slate-200/80 hover:bg-slate-100/80'
                            }`}
                          >
                            <div className="flex flex-col">
                              <span className={`text-sm font-bold ${isSelected ? 'text-cyan-800' : 'text-slate-800'}`}>
                                {slot.startTime} - {slot.endTime}
                              </span>
                              <span className="text-[11px] text-slate-500 mt-0.5">
                                {isFull ? 'Đã hết chỗ' : `Còn ${remain} chỗ trống (Tổng ${slot.capacity})`}
                              </span>
                            </div>
                            {isSelected && (
                              <span className="material-symbols-outlined text-cyan-600 text-lg">check_circle</span>
                            )}
                          </div>
                        );
                      })
                    )}
                  </div>
                </div>

                {/* Quantity */}
                <div className="flex items-center justify-between p-3 rounded-xl bg-slate-50 border border-slate-200/80">
                  <div className="flex flex-col">
                    <span className="text-sm font-bold text-slate-800">Số lượng người</span>
                    {activeSlot && (
                      <span className="text-[11px] text-slate-500">Tối đa {maxCapacity} người</span>
                    )}
                  </div>
                  <div className="flex items-center gap-3">
                    <button
                      type="button"
                      onClick={() => setQty(Math.max(1, qty - 1))}
                      disabled={qty <= 1}
                      className="w-8 h-8 rounded-full bg-white border border-slate-200 text-slate-700 flex items-center justify-center font-bold hover:bg-slate-100 transition-colors shadow-xs active:scale-95 disabled:opacity-40 cursor-pointer"
                    >
                      -
                    </button>
                    <span className="font-bold text-base text-slate-900 min-w-[20px] text-center">{qty}</span>
                    <button
                      type="button"
                      onClick={() => setQty(Math.min(maxCapacity > 0 ? maxCapacity : 1, qty + 1))}
                      disabled={!activeSlot || isSoldOut || qty >= maxCapacity}
                      className="w-8 h-8 rounded-full bg-white border border-slate-200 text-slate-700 flex items-center justify-center font-bold hover:bg-slate-100 transition-colors shadow-xs active:scale-95 disabled:opacity-40 cursor-pointer"
                    >
                      +
                    </button>
                  </div>
                </div>

                {/* Waiver Checkbox */}
                {service.waiverRequired && (
                  <div className="p-3 rounded-xl bg-slate-50 border border-slate-200/80 flex flex-col gap-2">
                    <label className="flex items-start gap-2.5 cursor-pointer select-none">
                      <input
                        type="checkbox"
                        checked={isWaiverChecked}
                        onChange={(e) => setIsWaiverChecked(e.target.checked)}
                        className="mt-1 w-4 h-4 rounded text-cyan-600 accent-cyan-600 cursor-pointer"
                      />
                      <span className="text-xs text-slate-700 leading-snug">
                        Tôi đã đọc, hiểu rõ và đồng ý với{' '}
                        <strong className="text-slate-900">Điều khoản an toàn &amp; miễn trừ trách nhiệm</strong> khi tham gia hoạt động biển.
                      </span>
                    </label>
                  </div>
                )}

                {/* Total Price */}
                <div className="flex flex-col gap-1 pt-3 border-t border-slate-100">
                  <div className="flex items-baseline justify-between">
                    <span className="text-sm font-semibold text-slate-600">Tổng tạm tính:</span>
                    <span className="text-2xl font-extrabold text-cyan-700">
                      {totalPrice.toLocaleString('vi-VN')}₫
                    </span>
                  </div>
                </div>

                {/* Actions */}
                <div className="flex flex-col gap-2 pt-1">
                  <button
                    type="button"
                    onClick={handleAddToCart}
                    disabled={!activeSlot || isSoldOut || (service.waiverRequired && !isWaiverChecked) || isAdding}
                    className={`w-full py-3.5 px-6 rounded-full font-bold text-sm shadow-md flex items-center justify-center gap-2 transition-all cursor-pointer ${
                      isAdding
                        ? 'bg-cyan-600 text-white scale-98'
                        : (!activeSlot || isSoldOut || (service.waiverRequired && !isWaiverChecked))
                        ? 'bg-slate-200 text-slate-400 cursor-not-allowed shadow-none'
                        : 'bg-gradient-to-r from-teal-600 to-cyan-600 hover:from-teal-500 hover:to-cyan-500 text-white hover:scale-[1.02] active:scale-98 shadow-cyan-600/20'
                    }`}
                  >
                    {isAdding ? (
                      <span className="material-symbols-outlined animate-spin text-[20px]">refresh</span>
                    ) : (
                      <span className="material-symbols-outlined text-[20px]">shopping_bag</span>
                    )}
                    <span>{isAdding ? 'Đang thêm...' : 'Thêm vào giỏ hàng'}</span>
                  </button>

                  {showAddSuccess && (
                    <div className="p-2.5 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-semibold text-center flex items-center justify-center gap-1.5 animate-fade-in">
                      <span className="material-symbols-outlined text-sm">check_circle</span>
                      <span>Đã thêm vé vào giỏ hàng thành công!</span>
                    </div>
                  )}
                </div>

              </div>
            </aside>
          </div>
        </div>
      </div>
    </main>
  );
}
