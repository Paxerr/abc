import { Link } from 'react-router-dom';

export function Footer() {
  return (
    <footer className="w-full bg-white/75 backdrop-blur-2xl border-t border-white/60 shadow-[0_-8px_32px_rgba(0,104,116,0.06)] relative z-10">
      <div className="max-w-[1440px] w-full mx-auto px-4 sm:px-8 lg:px-12 pt-12 pb-24 sm:pb-20">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8 sm:gap-10">
          {/* Cột 1: Thông tin thương hiệu DANASEA */}
          <div className="flex flex-col gap-4">
            <div className="flex items-center gap-2">
              <Link
                to="/"
                className="font-black text-2xl tracking-tight text-cyan-950 hover:text-cyan-700 transition-colors"
              >
                DANASEA
              </Link>
            </div>
            <p className="text-slate-600 text-xs sm:text-sm leading-relaxed">
              Nền tảng đặt trước trải nghiệm biển cảm xúc cao đầu tiên tại Đà Nẵng. Kết nối trọn vẹn nhịp sống biển với công nghệ trợ lý lịch trình AI thông minh.
            </p>
            <div className="flex items-center gap-2 mt-1">
              <span className="px-3 py-1 rounded-full bg-cyan-50 text-cyan-800 font-semibold text-xs border border-cyan-200/60 shadow-sm">
                Bảo trợ an toàn biển
              </span>
              <span className="px-3 py-1 rounded-full bg-emerald-50 text-emerald-800 font-semibold text-xs border border-emerald-200/60 shadow-sm">
                100% HDV Bản Địa
              </span>
            </div>
          </div>

          {/* Cột 2: Vùng biển trải nghiệm (không dùng dấu chấm) */}
          <div className="flex flex-col gap-3">
            <h3 className="font-bold text-sm sm:text-base text-slate-900 tracking-tight">
              Vùng biển trải nghiệm
            </h3>
            <ul className="flex flex-col gap-2.5 text-xs sm:text-sm text-slate-600 list-none p-0 m-0">
              <li className="hover:text-cyan-700 hover:translate-x-1 transition-all cursor-pointer">
                Bãi biển Mỹ Khê (Lướt sóng &amp; SUP bình minh)
              </li>
              <li className="hover:text-cyan-700 hover:translate-x-1 transition-all cursor-pointer">
                Bán đảo Sơn Trà (Lặn ngắm san hô &amp; chèo kayak)
              </li>
              <li className="hover:text-cyan-700 hover:translate-x-1 transition-all cursor-pointer">
                Bãi biển Non Nước (Dù lượn &amp; Jetski cano)
              </li>
              <li className="hover:text-cyan-700 hover:translate-x-1 transition-all cursor-pointer">
                Rạn Nam Ô (Trải nghiệm rêu xanh hoang sơ)
              </li>
            </ul>
          </div>

          {/* Cột 3: Chính sách & Trợ giúp */}
          <div className="flex flex-col gap-3">
            <h3 className="font-bold text-sm sm:text-base text-slate-900 tracking-tight">
              Chính sách &amp; Trợ giúp
            </h3>
            <ul className="flex flex-col gap-2.5 text-xs sm:text-sm text-slate-600 list-none p-0 m-0">
              <li className="hover:text-cyan-700 hover:translate-x-1 transition-all cursor-pointer">
                Chính sách hoàn tiền &amp; đổi lịch do thời tiết
              </li>
              <li className="hover:text-cyan-700 hover:translate-x-1 transition-all cursor-pointer">
                Hướng dẫn quét vé QR tại điểm tập kết
              </li>
              <li className="hover:text-cyan-700 hover:translate-x-1 transition-all cursor-pointer">
                Câu hỏi thường gặp (FAQ biển mùa khô/mưa)
              </li>
              <li className="hover:text-cyan-700 hover:translate-x-1 transition-all cursor-pointer">
                Điều khoản dịch vụ &amp; Bảo hiểm hoạt động
              </li>
              <li className="hover:text-cyan-700 hover:translate-x-1 transition-all cursor-pointer">
                Quy chuẩn bảo vệ san hô và môi trường biển
              </li>
            </ul>
          </div>

          {/* Cột 4: Kết nối & Ngôn ngữ */}
          <div className="flex flex-col gap-3">
            <h3 className="font-bold text-sm sm:text-base text-slate-900 tracking-tight">
              Kết nối &amp; Ngôn ngữ
            </h3>
            <p className="text-xs sm:text-sm text-slate-600">
              Nhận thông báo lịch thủy triều đẹp và ưu đãi trải nghiệm biển mỗi tuần:
            </p>
            <div className="flex items-center rounded-full bg-white/90 border border-slate-200/90 p-1.5 focus-within:border-cyan-500 focus-within:ring-2 focus-within:ring-cyan-500/20 shadow-sm transition-all">
              <input
                className="w-full bg-transparent px-3 py-1 text-xs sm:text-sm text-slate-800 placeholder:text-slate-400 outline-none"
                placeholder="Email của bạn..."
                type="email"
              />
              <button
                className="px-4 py-2 rounded-full bg-cyan-600 hover:bg-cyan-500 text-white font-bold text-xs shadow-md transition-all shrink-0 hover:scale-[1.02] cursor-pointer"
                type="button"
              >
                Gửi
              </button>
            </div>
            <div className="flex items-center gap-5 mt-2 text-xs sm:text-sm text-slate-600 font-medium">
              <span className="flex items-center gap-1.5">
                <span className="material-symbols-outlined text-[18px] text-cyan-600">support_agent</span>
                Hotline: 1900 8468
              </span>
              <span className="flex items-center gap-1.5">
                <span className="material-symbols-outlined text-[18px] text-cyan-600">waves</span>
                Đà Nẵng, VN
              </span>
            </div>
          </div>
        </div>

        {/* Thanh cuối trang: Copyright & Khoảng trống an toàn tránh bị che bởi thanh phân hệ */}
        <div className="mt-12 pt-6 border-t border-slate-200/60 flex flex-col md:flex-row items-center justify-between gap-4 text-xs text-slate-500 pl-0 sm:pl-36 md:pl-0">
          <p>© 2026 DANASEA Platforms Ltd. Đã đăng ký bản quyền.</p>
          <div className="flex items-center gap-4 text-slate-500">
            <span className="hover:text-cyan-700 cursor-pointer transition-colors">Bảo mật thông tin</span>
            <span>•</span>
            <span className="hover:text-cyan-700 cursor-pointer transition-colors">Tiêu chuẩn an toàn biển</span>
          </div>
        </div>
      </div>
    </footer>
  );
}
