import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useLanguageStore } from '../../store/useLanguageStore';

interface TimeSlotData {
  time: string;
  label: string;
  labelEn: string;
  sunCondition: string;
  sunConditionEn: string;
  location: string;
  locationEn: string;
  tourTitle: string;
  tourTitleEn: string;
  tourCategory: string;
  tourCategoryEn: string;
  price: number;
  imageUrl: string;
}

const TIME_SLOTS: TimeSlotData[] = [
  {
    time: '05:30',
    label: 'Bình Minh Biển Sớm',
    labelEn: 'Early Sunrise',
    sunCondition: 'Mặt trời mọc êm đềm, mặt biển phẳng lặng như gương soi, nhiệt độ mát mẻ phù hợp nhất cho các hoạt động mặt nước.',
    sunConditionEn: 'Gentle sunrise, glass-calm water surface with cool morning sea breeze, ideal for paddle sports.',
    location: 'Bãi biển Mân Thái & Mỹ Khê',
    locationEn: 'Man Thai & My Khe Beach',
    tourTitle: 'Chèo SUP Đón Bình Minh Biển Mỹ Khê & Mân Thái',
    tourTitleEn: 'Sunrise SUP Paddleboarding at My Khe & Man Thai',
    tourCategory: 'Chèo SUP & Kayak',
    tourCategoryEn: 'Stand-Up Paddleboard',
    price: 280000,
    imageUrl: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=800&q=80',
  },
  {
    time: '09:00',
    label: 'Biển Nắng Pha Lê',
    labelEn: 'Crystal Waters',
    sunCondition: 'Ánh nắng xuyên thấu nước biển sâu, tầm nhìn san hô tự nhiên đạt độ trong và rõ nét nhất trong ngày.',
    sunConditionEn: 'Direct sunlight penetration providing maximum underwater clarity and coral reef visibility.',
    location: 'Hòn Sụp, Bán đảo Sơn Trà',
    locationEn: 'Hon Sup, Son Tra Peninsula',
    tourTitle: 'Lặn Bình Khí Khám Phá Rạn San Hô Hòn Sụp Bán Đảo Sơn Trà',
    tourTitleEn: 'Scuba Diving at Hon Sup Coral Reefs - Son Tra Peninsula',
    tourCategory: 'Lặn Biển Ngắm San Hô',
    tourCategoryEn: 'Scuba Diving',
    price: 550000,
    imageUrl: 'https://images.unsplash.com/photo-1544551763-46a013bb70d5?auto=format&fit=crop&w=800&q=80',
  },
  {
    time: '14:30',
    label: 'Gió Biển & Tốc Độ',
    labelEn: 'Ocean Breeze & Speed',
    sunCondition: 'Gió biển Đông Nam thổi lộng, nhịp sóng dồi dào thích hợp cho các hoạt động thể thao nước tốc độ cao.',
    sunConditionEn: 'Energizing onshore sea breeze and active waves perfect for high-speed coastal adventures.',
    location: 'Bãi biển Mỹ Khê',
    locationEn: 'My Khe Beach',
    tourTitle: 'Bay Dù Lượn Cano Kéo Bãi Biển Mỹ Khê',
    tourTitleEn: 'Tandem Parasailing Flight at My Khe Beach',
    tourCategory: 'Cano & Dù Bay',
    tourCategoryEn: 'Speedboat & Parasailing',
    price: 500000,
    imageUrl: 'https://images.unsplash.com/photo-1533105079780-92b9be482077?auto=format&fit=crop&w=800&q=80',
  },
  {
    time: '17:00',
    label: 'Hoàng Hôn Vịnh Biển',
    labelEn: 'Sunset Horizon',
    sunCondition: 'Nắng dịu chuyển sắc vàng cam rực rỡ, gió chiều mát rượi, không gian biển lãng mạn và thư thái.',
    sunConditionEn: 'Soft golden hour glow, soothing sea breeze, peaceful coastal twilight atmosphere.',
    location: 'Vịnh Đà Nẵng & Bán Đảo Sơn Trà',
    locationEn: 'Da Nang Bay & Son Tra',
    tourTitle: 'Du Thuyền Hạng Sang Ngắm Hoàng Hôn Vịnh Đà Nẵng',
    tourTitleEn: 'Luxury Sunset Yacht Tour in Da Nang Bay',
    tourCategory: 'Du Thuyền Hoàng Hôn',
    tourCategoryEn: 'Sunset Cruise',
    price: 450000,
    imageUrl: 'https://images.unsplash.com/photo-1518837695005-2083093ee35b?auto=format&fit=crop&w=800&q=80',
  },
];

export const SmartOceanPlanner: React.FC = () => {
  const { language } = useLanguageStore();
  const [selectedSlotIndex, setSelectedSlotIndex] = useState<number>(0);
  const currentSlot = TIME_SLOTS[selectedSlotIndex];

  return (
    <section className="pt-2 pb-6 sm:pt-4 sm:pb-8 bg-transparent relative overflow-hidden">
      <div className="w-full relative z-10 max-w-[1360px] mx-auto px-4 sm:px-6 lg:px-8">
        {/* Header */}
        <div className="text-center max-w-3xl mx-auto mb-6">
          <h2 className="text-2xl sm:text-3xl lg:text-4xl font-extrabold tracking-tight text-white drop-shadow-[0_2px_4px_rgba(0,0,0,0.95)] drop-shadow-[0_8px_20px_rgba(0,0,0,0.7)]">
            {language === 'en' ? 'Experiences by Ocean Hours' : 'Trải Nghiệm Theo Khung Giờ Biển'}
          </h2>
        </div>

        {/* Time Preset Buttons - Transparent Glass (No Icons) */}
        <div className="flex flex-wrap items-center justify-center gap-2.5 sm:gap-3 mb-6">
          {TIME_SLOTS.map((slot: TimeSlotData, index: number) => {
            const isActive = index === selectedSlotIndex;
            return (
              <button
                key={slot.time}
                type="button"
                onClick={() => setSelectedSlotIndex(index)}
                className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-xs sm:text-sm font-semibold transition-all duration-300 cursor-pointer backdrop-blur-md ${
                  isActive
                    ? 'bg-cyan-600 text-white border border-cyan-400 shadow-lg scale-105 ring-2 ring-cyan-400/30'
                    : 'bg-white/10 hover:bg-white/20 text-white border border-white/20 shadow-sm'
                }`}
              >
                <span className={`font-mono font-bold ${isActive ? 'text-white' : 'text-cyan-300'}`}>
                  {slot.time}
                </span>
                <span className="drop-shadow-sm">{language === 'en' ? slot.labelEn : slot.label}</span>
              </button>
            );
          })}
        </div>

        {/* Dynamic Card Container - 100% Transparent Glass (Reveals Ocean Backdrop Completely) */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 sm:gap-8 items-center bg-white/10 backdrop-blur-xl rounded-3xl p-6 sm:p-8 lg:p-10 shadow-2xl border border-white/20">
          {/* Left Column: Context & Moment Atmosphere (Text White, No Murkiness, No Clutter) */}
          <div className="lg:col-span-7 flex flex-col justify-center space-y-4">
            <div className="inline-flex items-center self-start gap-2 px-3.5 py-1.5 rounded-full text-xs font-bold bg-cyan-400/20 backdrop-blur-md text-cyan-200 border border-cyan-300/40 shadow-sm">
              <span>{currentSlot.time}</span>
              <span>•</span>
              <span>{language === 'en' ? currentSlot.labelEn : currentSlot.label}</span>
            </div>

            <h3 className="text-xl sm:text-2xl lg:text-3xl font-extrabold text-white leading-snug tracking-tight drop-shadow-[0_2px_8px_rgba(0,0,0,0.85)]">
              {language === 'en' ? currentSlot.sunConditionEn : currentSlot.sunCondition}
            </h3>

            <div className="pt-2 flex items-center gap-2 text-xs sm:text-sm font-medium text-white/90 drop-shadow-sm">
              <span className="font-bold text-cyan-200">
                {language === 'en' ? 'Area:' : 'Địa điểm:'}
              </span>
              <span>{language === 'en' ? currentSlot.locationEn : currentSlot.location}</span>
            </div>
          </div>

          {/* Right Column: Experience Card - Transparent Glass, Matching Ocean Backdrop */}
          <div className="lg:col-span-5">
            <div className="rounded-2xl overflow-hidden bg-white/15 backdrop-blur-xl border border-white/25 shadow-2xl group transition-all duration-300 hover:border-white/40">
              <div className="relative aspect-[16/10] overflow-hidden bg-slate-900/30">
                <img
                  src={currentSlot.imageUrl}
                  alt={currentSlot.tourTitle}
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                />
              </div>

              <div className="p-5 sm:p-6 bg-white/10 backdrop-blur-md">
                <span className="text-xs font-bold text-cyan-300 uppercase tracking-wider block mb-1.5 drop-shadow-sm">
                  {language === 'en' ? currentSlot.tourCategoryEn : currentSlot.tourCategory}
                </span>
                <h4 className="text-base font-bold text-white group-hover:text-cyan-200 transition-colors line-clamp-2 mb-4 leading-snug drop-shadow-sm">
                  {language === 'en' ? currentSlot.tourTitleEn : currentSlot.tourTitle}
                </h4>

                <div className="flex items-center justify-between pt-4 border-t border-white/20">
                  <div>
                    <span className="text-[11px] text-white/70 uppercase tracking-wider block">
                      {language === 'en' ? 'Ticket Price' : 'Giá vé'}
                    </span>
                    <span className="text-lg font-black text-cyan-300 drop-shadow-sm">
                      {currentSlot.price.toLocaleString('vi-VN')}₫
                    </span>
                  </div>
                  <Link
                    to="/search"
                    className="px-5 py-2.5 rounded-xl bg-cyan-600 hover:bg-cyan-500 text-white font-bold text-xs transition-all shadow-lg hover:shadow-cyan-500/25"
                  >
                    {language === 'en' ? 'Book Now' : 'Đặt ngay'}
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};
