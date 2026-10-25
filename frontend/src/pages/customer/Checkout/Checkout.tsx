import { useNavigate } from "react-router-dom";
import { useCartStore } from "../../../store/useCartStore";
import { useLoadingStore } from "../../../store/useLoadingStore";
import { FEATURED_SERVICES, MOCK_IMAGES } from "../../../mockData";
import { useState, useEffect } from "react";

export function Checkout() {
  const navigate = useNavigate();
  const { items, getTotalPrice, clearCart } = useCartStore();
  const [countdown, setCountdown] = useState(600); // 10 minutes
  const [sessionCode] = useState(() => Math.floor(10000 + Math.random() * 90000));
  const [promoInput, setPromoInput] = useState("");
  const [appliedPromo, setAppliedPromo] = useState<{ code: string; discount: number } | null>(null);
  const [promoError, setPromoError] = useState("");

  const handleApplyPromo = () => {
    setPromoError("");
    const code = promoInput.trim().toUpperCase();
    if (!code) return;
    if (code === "DANASEA10" || code === "HE2024") {
      const discount = Math.round(getTotalPrice() * 0.1);
      setAppliedPromo({ code, discount });
    } else if (code === "BIENXANH") {
      setAppliedPromo({ code, discount: 50000 });
    } else {
      setPromoError("Mã khuyến mãi không hợp lệ hoặc đã hết lượt dùng");
    }
  };

  const handleRemovePromo = () => {
    setAppliedPromo(null);
    setPromoInput("");
    setPromoError("");
  };

  const finalTotal = Math.max(0, getTotalPrice() - (appliedPromo?.discount || 0));

  useEffect(() => {
    if (items.length === 0) {
      navigate('/cart');
    }
  }, [items, navigate]);

  useEffect(() => {
    const timer = setInterval(() => {
      setCountdown(prev => {
        if (prev <= 1) {
          clearInterval(timer);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const formatTime = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  const { triggerDemoLoading } = useLoadingStore();

  const handlePayment = () => {
    // Seamless liquid wave transition for payment processing & QR ticket issuance
    triggerDemoLoading(2000, "Đang xử lý giao dịch vé tour...", "Hệ thống đang khóa chỗ cano và sinh mã vé QR");
    setTimeout(() => {
      clearCart();
      navigate('/checkout/result');
    }, 1800);
  };

  if (items.length === 0) return null;

  return (
    <main className="w-full pt-20 bg-surface flex-1"><div className="flex flex-col w-full">
      {/* Sub-Header Progress Stepper Bar */}
      <section className="w-full bg-surface-container-lowest/80 backdrop-blur-md shadow-sm">
        <div className="max-w-[1280px] mx-auto px-margin-desktop py-space-md">
          {/* Breadcrumb & Status Overline */}
          <div className="flex items-center justify-between gap-space-md flex-wrap mb-space-sm">
            <div className="flex items-center gap-space-xs font-label-md text-label-md text-on-surface-variant">
              <span className="hover:text-secondary cursor-pointer" onClick={() => navigate('/')}>DANASEA</span>
              <span className="material-symbols-outlined text-[14px]">chevron_right</span>
              <span className="hover:text-secondary cursor-pointer" onClick={() => navigate('/cart')}>Giỏ trải nghiệm biển</span>
              <span className="material-symbols-outlined text-[14px]">chevron_right</span>
              <span className="text-secondary font-bold">Thông tin & Thanh toán</span>
            </div>
            <div className="flex items-center gap-space-xs px-space-sm py-0.5 rounded-full bg-surface-container text-tertiary font-label-sm text-label-sm">
              <span className="w-2 h-2 rounded-full bg-secondary animate-ping"></span>
              <span className="">Phiên giao dịch mã: <strong>DNS-{sessionCode}</strong></span>
            </div>
          </div>
          {/* Stepper Component */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-space-sm items-center">
            <div className="flex items-center gap-space-sm p-space-xs rounded-lg bg-surface-container-low">
              <div className="w-8 h-8 rounded-full bg-secondary text-on-secondary flex items-center justify-center flex-shrink-0 font-label-lg shadow-sm">
                <span className="material-symbols-outlined text-[18px]">check</span>
              </div>
              <div className="min-w-0">
                <p className="font-label-sm text-label-sm text-secondary uppercase tracking-wider">Bước 1</p>
                <p className="font-headline-sm text-[15px] leading-tight text-on-surface truncate">Giỏ trải nghiệm ({items.length} mục)</p>
              </div>
            </div>
            <div className="flex items-center gap-space-sm p-space-xs rounded-lg bg-secondary/10 shadow-sm">
              <div className="w-8 h-8 rounded-full bg-secondary text-on-secondary flex items-center justify-center flex-shrink-0 font-label-lg shadow-sm">
                <span className="font-bold">2</span>
              </div>
              <div className="min-w-0">
                <p className="font-label-sm text-label-sm text-secondary font-bold uppercase tracking-wider">Bước 2 • Đang thực hiện</p>
                <p className="font-headline-sm text-[15px] leading-tight text-secondary font-bold truncate">Thông tin & Thanh toán</p>
              </div>
            </div>
            <div className="flex items-center gap-space-sm p-space-xs rounded-lg bg-surface-container-low/60 opacity-60">
              <div className="w-8 h-8 rounded-full bg-surface-container-highest text-on-surface-variant flex items-center justify-center flex-shrink-0 font-label-lg">
                <span className="font-bold">3</span>
              </div>
              <div className="min-w-0">
                <p className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">Bước 3</p>
                <p className="font-headline-sm text-[15px] leading-tight text-on-surface-variant truncate">Nhận vé điện tử QR</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Main Checkout Workflow Area */}
      <main className="w-full max-w-[1280px] mx-auto px-margin-desktop py-space-xl">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-space-2xl items-start">
          
          {/* LEFT COLUMN: Checkout Forms (7 Cols on Desktop) */}
          <div className="lg:col-span-7 flex flex-col gap-space-xl">
            {/* Countdown Sticky/Accent Banner */}
            <div className="w-full rounded-lg bg-primary-fixed p-space-md shadow-md flex items-start sm:items-center justify-between gap-space-md flex-col sm:flex-row">
              <div className="flex items-center gap-space-sm">
                <div className="w-10 h-10 rounded-full bg-primary text-on-primary flex items-center justify-center flex-shrink-0 shadow-sm">
                  <span className="material-symbols-outlined text-[22px]">timer</span>
                </div>
                <div>
                  <p className="font-headline-sm text-[17px] text-on-primary-fixed leading-snug">
                    Giữ slot trải nghiệm trong: <span className="font-extrabold text-primary font-display-hero text-[22px]" id="checkout-timer">{formatTime(countdown)}</span> phút
                  </p>
                  <p className="font-body-sm text-body-sm text-on-primary-fixed-variant leading-tight">
                    Slot trải nghiệm đang được khóa riêng. Hết thời gian, hệ thống tự động nhường cho du khách khác.
                  </p>
                </div>
              </div>

            </div>

            {/* Section 1: Customer Information */}
            <section className="w-full rounded-lg bg-surface-container-lowest p-space-xl shadow-sm flex flex-col gap-space-lg">
              <div className="flex items-center justify-between border-b pb-space-xs border-surface-container">
                <div className="flex items-center gap-space-sm">
                  <div className="w-7 h-7 rounded-full bg-secondary-fixed text-on-secondary-fixed flex items-center justify-center font-bold text-label-md">1</div>
                  <h2 className="font-headline-sm text-headline-sm text-on-surface">Thông tin người đặt</h2>
                </div>
                <span className="font-label-sm text-label-sm text-secondary flex items-center gap-1"><span className="material-symbols-outlined text-[16px]">verified_user</span> Đồng bộ tài khoản</span>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-space-md">
                <div className="flex flex-col gap-1.5">
                  <label className="font-label-md text-label-md text-on-surface">Họ và tên *</label>
                  <div className="relative flex items-center">
                    <span className="material-symbols-outlined absolute left-4 text-on-surface-variant text-[20px]">person</span>
                    <input className="w-full pl-11 pr-4 py-3 rounded-full bg-surface-container-low font-body-md text-on-surface outline-none" readOnly type="text" defaultValue="Khách Hàng DANASEA"/>
                  </div>
                </div>
                <div className="flex flex-col gap-1.5">
                  <label className="font-label-md text-label-md text-on-surface">Số điện thoại *</label>
                  <div className="relative flex items-center">
                    <span className="material-symbols-outlined absolute left-4 text-on-surface-variant text-[20px]">call</span>
                    <input className="w-full pl-11 pr-4 py-3 rounded-full bg-surface-container-low font-body-md text-on-surface outline-none" readOnly type="tel" defaultValue="0912 345 678"/>
                  </div>
                </div>
                <div className="flex flex-col gap-1.5 md:col-span-2">
                  <label className="font-label-md text-label-md text-on-surface">Địa chỉ Email *</label>
                  <div className="relative flex items-center">
                    <span className="material-symbols-outlined absolute left-4 text-on-surface-variant text-[20px]">mail</span>
                    <input className="w-full pl-11 pr-4 py-3 rounded-full bg-surface-container-low font-body-md text-on-surface outline-none" readOnly type="email" defaultValue="khachhang@example.com"/>
                  </div>
                </div>
              </div>
            </section>

            {/* Section 3: Payment Methods */}
            <section className="w-full rounded-lg bg-surface-container-lowest p-space-xl shadow-sm flex flex-col gap-space-lg">
              <div className="flex items-center justify-between border-b pb-space-xs border-surface-container">
                <div className="flex items-center gap-space-sm">
                  <div className="w-7 h-7 rounded-full bg-secondary-fixed text-on-secondary-fixed flex items-center justify-center font-bold text-label-md">2</div>
                  <h2 className="font-headline-sm text-headline-sm text-on-surface">Phương thức thanh toán</h2>
                </div>
              </div>
              <div className="flex flex-col gap-space-sm">
                <label className="p-space-md rounded-lg bg-surface-container-low hover:bg-surface-container cursor-pointer transition-all flex items-center justify-between">
                  <div className="flex items-center gap-space-sm">
                    <input defaultChecked className="w-5 h-5 accent-secondary cursor-pointer" name="payment_method" type="radio" value="VNPAY"/>
                    <div className="flex items-center gap-2">
                      <span className="font-label-lg text-label-lg text-on-surface">Cổng VNPAY (QR Pay / Thẻ ATM & Quốc tế)</span>
                      <span className="px-space-sm py-0.5 rounded-full bg-primary-fixed text-on-primary-fixed-variant font-label-sm text-[11px] font-bold">Khuyên dùng</span>
                    </div>
                  </div>
                  <span className="material-symbols-outlined text-secondary">qr_code_scanner</span>
                </label>
                <label className="p-space-md rounded-lg bg-surface-container-low hover:bg-surface-container cursor-pointer transition-all flex items-center justify-between">
                  <div className="flex items-center gap-space-sm">
                    <input className="w-5 h-5 accent-secondary cursor-pointer" name="payment_method" type="radio" value="MOMO"/>
                    <div className="flex items-center gap-2">
                      <span className="font-label-lg text-label-lg text-on-surface">Ví điện tử MoMo</span>
                    </div>
                  </div>
                  <span className="material-symbols-outlined text-secondary">account_balance_wallet</span>
                </label>
              </div>
            </section>

            {/* Section 4: Ocean Safety & Marine Regulations Acknowledgment */}
            <section className="w-full rounded-lg bg-surface-container-low p-space-xl shadow-sm flex flex-col gap-space-md">
              <div className="flex items-center gap-space-sm">
                <div className="w-9 h-9 rounded-full bg-secondary text-on-secondary flex items-center justify-center flex-shrink-0 shadow-sm">
                  <span className="material-symbols-outlined text-[22px]">assignment_turned_in</span>
                </div>
                <div>
                  <h2 className="font-headline-sm text-headline-sm text-on-surface">Cam kết miễn trừ trách nhiệm (Waiver)</h2>
                  <p className="font-body-sm text-body-sm text-on-surface-variant">Nội dung miễn trừ trách nhiệm theo từng dịch vụ biển bắt buộc xác nhận</p>
                </div>
              </div>
              <div className="flex flex-col gap-space-md">
                {items.map((item, i) => {
                  const service = FEATURED_SERVICES.find(s => s.id === item.serviceId);
                  if (!service) return null;
                  return (
                    <div key={item.id} className="p-space-md rounded-lg bg-surface-container-lowest border border-surface-container flex flex-col gap-space-xs">
                      <div className="flex items-center gap-2 font-label-lg text-label-lg text-on-surface font-bold text-primary">
                        <span className="material-symbols-outlined text-[18px]">verified_user</span> 
                        {i+1}. Miễn trừ trách nhiệm: {service.name}
                      </div>
                      <p className="font-body-sm text-body-sm text-on-surface-variant leading-relaxed bg-surface-container-low p-space-sm rounded-lg">
                        Tôi xác nhận đã có đủ sức khỏe thể chất, đồng ý tuân thủ tuyệt đối mọi hướng dẫn an toàn của huấn luyện viên DANASEA tại điểm trải nghiệm {service.locationName}.
                      </p>
                      <label className="flex items-start gap-space-xs cursor-pointer pt-space-2xs">
                        <input className="mt-1 w-4 h-4 accent-secondary cursor-pointer rounded" type="checkbox" defaultChecked/>
                        <span className="font-label-md text-label-md text-on-surface">Tôi đã đọc, hiểu rõ và đồng ý với điều khoản miễn trừ trách nhiệm *</span>
                      </label>
                    </div>
                  )
                })}
              </div>
            </section>
          </div>

          {/* RIGHT COLUMN: Sticky Order Summary & Price Breakdown (5 Cols on Desktop) */}
          <div className="lg:col-span-5 flex flex-col gap-space-lg sticky top-24">
            {/* Booking Summary Card */}
            <div className="w-full rounded-xl bg-surface-container-lowest p-space-xl shadow-md flex flex-col gap-space-lg">
              <div className="flex items-center justify-between border-b pb-space-sm border-surface-container">
                <h3 className="font-headline-sm text-headline-sm text-on-surface">Tóm tắt trải nghiệm biển</h3>
                <span className="px-space-xs py-0.5 rounded-full bg-surface-container text-tertiary font-label-sm text-label-sm">{items.length} Dịch vụ</span>
              </div>
              <div className="flex flex-col gap-space-md">
                {items.map(item => {
                  const service = FEATURED_SERVICES.find(s => s.id === item.serviceId);
                  const image = MOCK_IMAGES.find(i => i.serviceId === item.serviceId && i.isPrimary)?.imageUrl || "https://via.placeholder.com/400";
                  if (!service) return null;
                  return (
                    <div key={item.id} className="flex gap-space-sm items-start border-b pb-space-md border-surface-container last:border-0 last:pb-0">
                      <div className="w-20 h-20 rounded-lg overflow-hidden flex-shrink-0 bg-surface-container">
                        <img className="w-full h-full object-cover" alt={service.name} src={image}/>
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center justify-between gap-1">
                          <span className="px-2 py-0.5 rounded-full bg-secondary/10 text-secondary font-label-sm text-[10px] font-bold">{service.locationName}</span>
                          <span className="font-label-lg text-label-lg text-on-surface font-bold">{(item.unitPrice * item.quantity).toLocaleString('vi-VN')}đ</span>
                        </div>
                        <h4 className="font-headline-sm text-[15px] text-on-surface font-bold leading-tight mt-1 truncate">{service.name}</h4>
                        <p className="font-body-sm text-[12px] text-on-surface-variant">{item.selectedDate}</p>
                        <div className="flex items-center justify-between mt-1 text-on-surface-variant font-body-sm text-[11px]">
                          <span className="">{item.unitPrice.toLocaleString('vi-VN')}đ × {item.quantity} khách</span>
                        </div>
                      </div>
                    </div>
                  )
                })}
              </div>
              
              {/* Promo Code Input */}
              <div className="pt-2 border-t border-surface-container">
                <label className="text-xs font-semibold text-slate-700 block mb-1.5">Mã ưu đãi / Khuyến mãi</label>
                {appliedPromo ? (
                  <div className="p-2.5 rounded-lg bg-emerald-50 border border-emerald-200 flex items-center justify-between text-xs">
                    <div className="flex items-center gap-2">
                      <span className="material-symbols-outlined text-emerald-600 text-sm">check_circle</span>
                      <span className="font-bold text-emerald-800">{appliedPromo.code}</span>
                      <span className="text-emerald-700">(-{appliedPromo.discount.toLocaleString('vi-VN')}đ)</span>
                    </div>
                    <button
                      onClick={handleRemovePromo}
                      className="text-red-500 hover:text-red-700 font-medium cursor-pointer"
                      type="button"
                    >
                      Gỡ bỏ
                    </button>
                  </div>
                ) : (
                  <div className="space-y-1">
                    <div className="flex gap-2">
                      <input
                        type="text"
                        value={promoInput}
                        onChange={(e) => setPromoInput(e.target.value)}
                        placeholder="Nhập DANASEA10 hoặc HE2024"
                        className="flex-1 px-3 py-2 rounded-lg border border-slate-200 text-xs text-slate-800 uppercase focus:outline-none focus:ring-1 focus:ring-primary"
                      />
                      <button
                        onClick={handleApplyPromo}
                        type="button"
                        className="px-3 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-lg text-xs font-semibold cursor-pointer transition-colors"
                      >
                        Áp dụng
                      </button>
                    </div>
                    {promoError && <p className="text-[11px] text-red-500">{promoError}</p>}
                  </div>
                )}
              </div>
              
              <div className="flex flex-col gap-space-xs pt-space-xs border-t border-surface-container font-body-md text-body-md text-on-surface-variant">
                <div className="flex justify-between items-center text-sm">
                  <span>Tạm tính</span>
                  <span className="text-on-surface font-medium">{getTotalPrice().toLocaleString('vi-VN')}đ</span>
                </div>
                {appliedPromo && (
                  <div className="flex justify-between items-center text-sm text-emerald-600 font-medium">
                    <span>Khuyến mãi ({appliedPromo.code})</span>
                    <span>-{appliedPromo.discount.toLocaleString('vi-VN')}đ</span>
                  </div>
                )}
                <div className="flex justify-between items-baseline pt-space-md border-t border-surface-container mt-space-xs">
                  <div>
                    <span className="font-headline-sm text-[18px] text-on-surface font-bold">Tổng thanh toán:</span>
                  </div>
                  <span className="font-display-hero text-[28px] leading-tight text-primary font-extrabold tracking-tight">{finalTotal.toLocaleString('vi-VN')}đ</span>
                </div>
              </div>
              <button onClick={handlePayment} className="w-full py-4 rounded-full bg-primary-container text-on-primary font-label-lg text-label-lg shadow-[0_8px_20px_rgba(255,115,92,0.35)] hover:scale-[1.02] active:scale-[0.98] transition-all flex items-center justify-center gap-space-sm font-bold tracking-wide cursor-pointer" type="button">
                <span className="material-symbols-outlined text-[20px]">lock</span>
                <span>Xác nhận &amp; Thanh toán {finalTotal.toLocaleString('vi-VN')}đ</span>
              </button>
            </div>
          </div>
        </div>
      </main>
    </div></main>
  );
}
