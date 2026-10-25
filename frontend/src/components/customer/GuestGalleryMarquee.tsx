import React from 'react';
import { GUEST_GALLERY_ROW_1, GUEST_GALLERY_ROW_2 } from '../../data/customerHomeData';
import { useLanguageStore } from '../../store/useLanguageStore';

const GUEST_CARDS_ROW_1 = [
  {
    imageUrl: GUEST_GALLERY_ROW_1[0],
    guestName: 'Nguyễn Thảo My',
    guestLocation: 'Hà Nội',
    activity: 'SUP Bình Minh Mỹ Khê',
    activityEn: 'Sunrise SUP My Khe',
    quote: 'Nước biển trong vắt, anh hướng dẫn chụp ảnh có tâm đến từng góc máy!',
    quoteEn: 'Crystal waters and the guide took breathtaking photos for us!',
    rating: 5,
  },
  {
    imageUrl: GUEST_GALLERY_ROW_1[1],
    guestName: 'David & Sarah',
    guestLocation: 'Australia',
    activity: 'Lặn Scuba Sơn Trà',
    activityEn: 'Son Tra Scuba Dive',
    quote: 'The coral reef in Da Nang surprised us completely. World-class gear & safety!',
    quoteEn: 'The coral reef in Da Nang surprised us completely. World-class gear & safety!',
    rating: 5,
  },
  {
    imageUrl: GUEST_GALLERY_ROW_1[2],
    guestName: 'Trần Minh Quân',
    guestLocation: 'TP. Hồ Chí Minh',
    activity: 'Cano & Lặn Bãi Rạng',
    activityEn: 'Speedboat & Snorkel',
    quote: 'Chuyến đi đáng giá từng xu! Cả nhà đều hào hứng nhảy sóng và bắt ốc.',
    quoteEn: 'Worth every penny! My whole family had a blast riding waves and snorkeling.',
    rating: 5,
  },
  {
    imageUrl: GUEST_GALLERY_ROW_1[3],
    guestName: 'Lê Hoàng Phong',
    guestLocation: 'Đà Nẵng',
    activity: 'Bay Dù Lượn Vịnh Biển',
    activityEn: 'Coastal Parasailing',
    quote: 'Cảm giác lơ lửng ngắm toàn cảnh thành phố từ trên không trung cực kỳ đã.',
    quoteEn: 'Floating high above the bay with panoramic views was sensational.',
    rating: 5,
  },
  {
    imageUrl: GUEST_GALLERY_ROW_1[4],
    guestName: 'Kim Min-seo',
    guestLocation: 'South Korea',
    activity: 'Kayak Hoàng Hôn Mũi Nghê',
    activityEn: 'Sunset Kayak Mui Nghe',
    quote: '다낭 바다 노을이 너무 아름다웠어요. DANASEA 최고입니다!',
    quoteEn: 'Da Nang sunset was so breathtaking. DANASEA is truly the best!',
    rating: 5,
  },
];

const GUEST_CARDS_ROW_2 = [
  {
    imageUrl: GUEST_GALLERY_ROW_2[0],
    guestName: 'Phạm Nhật Anh',
    guestLocation: 'Hải Phòng',
    activity: 'Jet Ski Cao Tốc 1800cc',
    activityEn: 'High-speed Jet Ski',
    quote: 'Tay ga bốc, sóng văng tung bọt sảng khoái không tả nổi!',
    quoteEn: 'Pure throttle power, slicing through ocean waves was pure euphoria!',
    rating: 5,
  },
  {
    imageUrl: GUEST_GALLERY_ROW_2[1],
    guestName: 'Emma Watson',
    guestLocation: 'United Kingdom',
    activity: 'Sunset Luxury Yacht',
    activityEn: 'Sunset Luxury Yacht',
    quote: 'Champagne on the deck while sunset painted the bay in pink. 10/10.',
    quoteEn: 'Champagne on the deck while sunset painted the bay in pink. 10/10.',
    rating: 5,
  },
  {
    imageUrl: GUEST_GALLERY_ROW_2[2],
    guestName: 'Vũ Hải Đăng',
    guestLocation: 'Cần Thơ',
    activity: 'SUP Yoga Sáng Sớm',
    activityEn: 'Sunrise Ocean SUP Yoga',
    quote: 'Tâm trí tĩnh lặng tuyệt đối giữa biển khơi bao la lúc 6 giờ sáng.',
    quoteEn: 'Absolute peace of mind floating gently at 6 AM morning tides.',
    rating: 5,
  },
  {
    imageUrl: GUEST_GALLERY_ROW_2[3],
    guestName: 'Bảo Trâm & Huy',
    guestLocation: 'Đà Lạt',
    activity: 'Cắm Trại Bãi Biển Non Nước',
    activityEn: 'Beach Camping Non Nuoc',
    quote: 'Lều trại setup chỉn chu, nướng BBQ ngay mép sóng cực lãng mạn.',
    quoteEn: 'Super aesthetic camp setup with seaside BBQ under the stars.',
    rating: 5,
  },
  {
    imageUrl: GUEST_GALLERY_ROW_2[4],
    guestName: 'Kenji Takahashi',
    guestLocation: 'Japan',
    activity: 'Khám Phá Rạn Nam Ô',
    activityEn: 'Nam O Reef Tour',
    quote: '素晴らしい体験！船頭さんも親切で大満足でした。',
    quoteEn: 'Wonderful experience! The local boat master was so helpful.',
    rating: 5,
  },
];

export const GuestGalleryMarquee: React.FC = () => {
  const { language, t } = useLanguageStore();

  return (
    <div className="py-6 bg-transparent relative overflow-hidden">
      <style>{`
        @keyframes marquee-left {
          0% { transform: translate3d(0, 0, 0); }
          100% { transform: translate3d(-50%, 0, 0); }
        }
        @keyframes marquee-right {
          0% { transform: translate3d(-50%, 0, 0); }
          100% { transform: translate3d(0, 0, 0); }
        }
        .animate-marquee-left {
          display: flex;
          width: max-content;
          will-change: transform;
          animation: marquee-left 45s linear infinite;
        }
        .animate-marquee-right {
          display: flex;
          width: max-content;
          will-change: transform;
          animation: marquee-right 45s linear infinite;
        }
        .animate-marquee-left:hover, .animate-marquee-right:hover {
          animation-play-state: paused;
        }
      `}</style>

      <div className="mb-8 text-center">
        <h2 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-white drop-shadow-[0_2px_4px_rgba(0,0,0,0.95)] drop-shadow-[0_8px_20px_rgba(0,0,0,0.7)]">
          {t.gallery.title}
        </h2>
      </div>

      {/* Mask container: Real CSS mask gradient instead of opaque fake color blocks */}
      <div
        className="space-y-4 overflow-hidden"
        style={{
          maskImage: 'linear-gradient(to right, transparent 0%, black 8%, black 92%, transparent 100%)',
          WebkitMaskImage: 'linear-gradient(to right, transparent 0%, black 8%, black 92%, transparent 100%)',
        }}
      >
        {/* Marquee Row 1 (Moving Left) */}
        <div className="overflow-hidden">
          <div className="animate-marquee-left gap-4">
            {[...GUEST_CARDS_ROW_1, ...GUEST_CARDS_ROW_1].map((card, idx) => (
              <div
                key={`row1-${idx}`}
                className="w-80 sm:w-96 flex-shrink-0 bg-white/15 backdrop-blur-md border border-white/25 rounded-2xl p-3.5 flex gap-3.5 hover:border-cyan-300 hover:bg-white/20 transition-all shadow-lg"
              >
                <img
                  src={card.imageUrl}
                  alt={card.guestName}
                  className="w-20 h-28 rounded-xl object-cover flex-shrink-0"
                  loading="lazy"
                />
                <div className="flex flex-col justify-between overflow-hidden">
                  <div>
                    <div className="flex items-center gap-0.5 text-amber-300 mb-1">
                      {Array.from({ length: card.rating }).map((_, i) => (
                        <svg key={i} className="w-3.5 h-3.5 fill-current text-amber-300" viewBox="0 0 20 20">
                          <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                        </svg>
                      ))}
                    </div>
                    <p className="text-xs text-white/90 italic line-clamp-3 mb-2 leading-relaxed drop-shadow-sm">
                      "{language === 'en' ? card.quoteEn : card.quote}"
                    </p>
                  </div>
                  <div>
                    <div className="text-xs font-bold text-white truncate drop-shadow-sm">
                      {card.guestName} <span className="text-white/60 font-normal">({card.guestLocation})</span>
                    </div>
                    <div className="text-[11px] text-cyan-300 font-semibold truncate drop-shadow-sm">
                      {language === 'en' ? card.activityEn : card.activity}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Marquee Row 2 (Moving Right) */}
        <div className="overflow-hidden">
          <div className="animate-marquee-right gap-4">
            {[...GUEST_CARDS_ROW_2, ...GUEST_CARDS_ROW_2].map((card, idx) => (
              <div
                key={`row2-${idx}`}
                className="w-80 sm:w-96 flex-shrink-0 bg-white/15 backdrop-blur-md border border-white/25 rounded-2xl p-3.5 flex gap-3.5 hover:border-cyan-300 hover:bg-white/20 transition-all shadow-lg"
              >
                <img
                  src={card.imageUrl}
                  alt={card.guestName}
                  className="w-20 h-28 rounded-xl object-cover flex-shrink-0"
                  loading="lazy"
                />
                <div className="flex flex-col justify-between overflow-hidden">
                  <div>
                    <div className="flex items-center gap-0.5 text-amber-300 mb-1">
                      {Array.from({ length: card.rating }).map((_, i) => (
                        <svg key={i} className="w-3.5 h-3.5 fill-current text-amber-300" viewBox="0 0 20 20">
                          <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                        </svg>
                      ))}
                    </div>
                    <p className="text-xs text-white/90 italic line-clamp-3 mb-2 leading-relaxed drop-shadow-sm">
                      "{language === 'en' ? card.quoteEn : card.quote}"
                    </p>
                  </div>
                  <div>
                    <div className="text-xs font-bold text-white truncate drop-shadow-sm">
                      {card.guestName} <span className="text-white/60 font-normal">({card.guestLocation})</span>
                    </div>
                    <div className="text-[11px] text-cyan-300 font-semibold truncate drop-shadow-sm">
                      {language === 'en' ? card.activityEn : card.activity}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default GuestGalleryMarquee;
