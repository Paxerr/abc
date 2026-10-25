import { Link, useNavigate } from "react-router-dom";
import { useCartStore } from "../../../store/useCartStore";
import { FEATURED_SERVICES, MOCK_IMAGES, MOCK_SLOTS } from "../../../mockData";

export function Cart() {
  const navigate = useNavigate();
  const { items, removeItem, getTotalPrice } = useCartStore();

  const handleCheckout = () => {
    if (items.length > 0) {
      navigate('/checkout');
    }
  };

  return (
    <main className="w-full pt-20 bg-surface flex-1"><div className="flex flex-col w-full">
      <section className="max-w-[1280px] w-full mx-auto px-margin-mobile md:px-margin-desktop py-space-xl">
        {/* Breadcrumb & Booking Stepper */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-space-lg mb-space-2xl">
          <div className="flex flex-col gap-space-2xs">
            <div className="flex items-center gap-2 text-on-surface-variant font-label-sm text-label-sm uppercase tracking-wider">
              <Link className="hover:text-primary transition-colors" to="/">Trang chủ</Link>
              <span className="material-symbols-outlined text-[14px]">chevron_right</span>
              <span className="text-secondary font-bold">Giỏ hàng của bạn</span>
            </div>
            <div className="flex items-baseline gap-space-sm">
              <h1 className="font-headline-lg text-headline-lg text-on-surface tracking-tight">Hành Trình Của Bạn</h1>
              <span className="px-space-sm py-0.5 rounded-full bg-secondary/10 text-secondary font-label-sm text-label-sm font-bold">{items.length} trải nghiệm đã chọn</span>
            </div>
          </div>
          {/* Stepper Pills */}
          <div className="flex items-center gap-space-xs bg-surface-container-low p-1.5 rounded-full shadow-sm overflow-x-auto">
            <div className="flex items-center gap-2 px-space-md py-1.5 rounded-full bg-secondary text-on-secondary font-label-md text-label-md shadow-sm whitespace-nowrap">
              <span className="w-5 h-5 rounded-full bg-on-secondary/20 flex items-center justify-center text-[11px] font-bold">1</span>
              <span className="">Giỏ trải nghiệm</span>
            </div>
            <div className="w-6 h-0.5 bg-surface-container-high rounded-full"></div>
            <div className="flex items-center gap-2 px-space-md py-1.5 rounded-full text-on-surface-variant font-label-md text-label-md opacity-60 whitespace-nowrap">
              <span className="w-5 h-5 rounded-full bg-surface-container-highest flex items-center justify-center text-[11px] font-bold">2</span>
              <span className="">Thông tin & Thanh toán</span>
            </div>
            <div className="w-6 h-0.5 bg-surface-container-high rounded-full"></div>
            <div className="flex items-center gap-2 px-space-md py-1.5 rounded-full text-on-surface-variant font-label-md text-label-md opacity-60 whitespace-nowrap">
              <span className="w-5 h-5 rounded-full bg-surface-container-highest flex items-center justify-center text-[11px] font-bold">3</span>
              <span className="">Vé điện tử QR</span>
            </div>
          </div>
        </div>

        {/* Main Content Layout */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-space-xl items-start">
          {/* Left Column: Experiences List */}
          <div className="lg:col-span-8 flex flex-col gap-space-lg">
{items.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-10 bg-surface-container-lowest rounded-lg">
                <span className="material-symbols-outlined text-[64px] text-outline mb-4">shopping_cart</span>
                <h2 className="font-headline-sm">Giỏ hàng của bạn đang trống</h2>
                <p className="text-on-surface-variant mb-6">Hãy thêm trải nghiệm để bắt đầu hành trình của bạn.</p>
                <Link to="/search" className="px-6 py-2 bg-primary text-on-primary rounded-full hover:bg-primary-container transition-colors">
                  Khám phá trải nghiệm
                </Link>
              </div>
            ) : (
              items.map(item => {
                const service = FEATURED_SERVICES.find(s => s.id === item.serviceId);
                const slot = MOCK_SLOTS.find(s => s.id === item.slotId);
                const image = MOCK_IMAGES.find(i => i.serviceId === item.serviceId && i.isPrimary)?.imageUrl || "https://via.placeholder.com/400";
                
                if (!service) return null;

                return (
                  <article key={item.id} className="p-space-lg rounded-lg bg-surface-container-lowest shadow-sm hover:shadow-md transition-shadow relative overflow-hidden flex flex-col md:flex-row gap-space-lg">
                    <div className="w-full md:w-56 h-44 rounded-DEFAULT overflow-hidden flex-shrink-0 relative">
                      <img className="w-full h-full object-cover" alt={service.name} src={image}/>
                      <span className="absolute bottom-2 left-2 px-space-xs py-0.5 rounded-full bg-surface-container-lowest/90 backdrop-blur text-on-surface font-label-sm text-[11px] font-semibold">
                        {service.locationName}
                      </span>
                    </div>
                    <div className="flex-1 flex flex-col justify-between gap-space-sm min-w-0">
                      <div className="flex flex-col gap-1">
                        <div className="flex items-center justify-between gap-space-sm">
                          <div className="flex items-center gap-1.5">
                            <span className="material-symbols-outlined text-[16px] text-secondary">verified</span>
                            <span className="font-label-sm text-label-sm text-on-surface-variant font-semibold">Danang Ocean Club</span>
                            <span className="w-1 h-1 rounded-full bg-surface-dim"></span>
                            <span className="text-secondary font-label-sm text-label-sm font-bold">4.9 ★</span>
                          </div>
                          <button onClick={() => removeItem(item.id)} aria-label="Xoá mục trải nghiệm" className="text-on-surface-variant hover:text-primary transition-colors p-1" type="button">
                            <span className="material-symbols-outlined text-[20px]">delete</span>
                          </button>
                        </div>
                        <h2 className="font-headline-sm text-headline-sm text-on-surface tracking-tight leading-snug">
                          {service.name}
                        </h2>
                      </div>
                      
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-space-md gap-y-1.5 py-space-xs font-body-sm text-body-sm text-on-surface-variant">
                        <div className="flex items-center gap-2 truncate"><span className="material-symbols-outlined text-[18px] text-secondary">schedule</span><span className="">{slot?.startTime.slice(0,5)} - {slot?.endTime.slice(0,5)}, {item.selectedDate}</span></div>
                        <div className="flex items-center gap-2 truncate"><span className="material-symbols-outlined text-[18px] text-secondary">location_on</span><span className="">{service.locationName}</span></div>
                        <div className="flex items-center gap-2"><span className="material-symbols-outlined text-[18px] text-secondary">group</span><span className="">{item.quantity} khách ({item.unitPrice.toLocaleString('vi-VN')}đ/người)</span></div>
                        <div className="flex items-center gap-2 text-on-surface-variant"><span className="material-symbols-outlined text-[18px] text-secondary">check_circle</span><span className="">Đã bao gồm trang bị và HDV</span></div>
                      </div>
                      
                      <div className="pt-space-xs flex items-center justify-between gap-space-sm flex-wrap">
                        <div className="flex items-center gap-2">
                          <span className="px-space-xs py-0.5 rounded-full bg-emerald-50 text-emerald-700 font-label-sm text-[11px] font-bold">
                            Chắc chắn có chỗ
                          </span>
                          <Link to={`/tour/${service.id}`} className="font-label-sm text-label-sm text-secondary hover:underline font-semibold flex items-center gap-0.5">
                            <span className="material-symbols-outlined text-[14px]">tune</span> Xem chi tiết
                          </Link>
                        </div>
                        <div className="flex items-baseline gap-1 text-right"><span className="font-headline-sm text-headline-sm text-primary font-bold">{(item.quantity * item.unitPrice).toLocaleString('vi-VN')}đ</span></div>
                      </div>
                    </div>
                  </article>
                )
              })
            )}

            {/* Upsell & Cross-sell Section */}
            <div className="mt-space-lg flex flex-col gap-space-md">
              <div className="flex items-baseline justify-between">
                <h2 className="font-headline-sm text-headline-sm text-on-surface">Gợi ý tiện ích biển bổ sung</h2>
                <span className="font-label-sm text-label-sm text-secondary font-bold">Ưu đãi giảm 15% kèm giỏ</span>
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-space-md">
                <div className="p-space-md rounded-lg bg-surface-container-low flex items-center gap-space-md hover:bg-surface-container transition-colors">
                  <div className="w-20 h-20 rounded-DEFAULT overflow-hidden flex-shrink-0">
                    <img className="w-full h-full object-cover" src="https://lh3.googleusercontent.com/aida-public/AB6AXuA4AMOKaaKBGjcISuAt-XoZ69bw4Ew8cOvMFOZWZiJmWtD3lMDWdWdsMCXDYim138aPB_K0YtIt4neMWClVMAUCJ96u2FQNzmHzH87RUZeBkmgQ-1NyRFOd4m5CoZFEcYCTZw6IMJcOkE08WZJR6v2CAqYwEFVD9u83nG0WVzYlXMbDGq-9Rmaam7pEGi_JURvMBnCUsjcApmlWd3Hxgzgr8VtyDySBMJFW1QlDs6HSINlKJYjo7zPasg"/>
                  </div>
                  <div className="flex-1 min-w-0 flex flex-col justify-between h-full">
                    <div>
                      <h3 className="font-label-lg text-label-lg text-on-surface truncate">Thuê máy quay GoPro Hero 11</h3>
                      <p className="font-body-sm text-body-sm text-on-surface-variant">Kèm phao nổi & thẻ 64GB</p>
                    </div>
                    <div className="flex items-center justify-between gap-2 pt-1">
                      <span className="font-label-lg text-label-lg text-secondary font-bold">150.000đ<span className="font-body-sm text-body-sm text-on-surface-variant font-normal">/ngày</span></span>
                      <button className="px-space-sm py-1 rounded-full bg-secondary-fixed text-on-secondary-fixed font-label-sm text-label-sm hover:bg-secondary-fixed-dim transition-colors flex items-center gap-1" type="button">
                        <span className="material-symbols-outlined text-[14px]">add</span> Thêm
                      </button>
                    </div>
                  </div>
                </div>
                <div className="p-space-md rounded-lg bg-surface-container-low flex items-center gap-space-md hover:bg-surface-container transition-colors">
                  <div className="w-20 h-20 rounded-DEFAULT overflow-hidden flex-shrink-0">
                    <img className="w-full h-full object-cover" src="https://lh3.googleusercontent.com/aida-public/AB6AXuAfWUW3Hh31WzoPA-Ojkb6aUebp-mM7tEo4pz2N0htIIzxEJ5IOmPvCU7QgD2Nqp5bYk_pcSGQVFoJxDYiXIPDDQvtdq9fKjbUwnxn9PXaaJbjYqX4JyYS5K2xcxgOV3vYVMhQhilqeGanR8EZwBQxQukU0W-MwF5SHTdZLz7Uu0c7lH04dE7kauPd_ofeNpDcg61Ddj0wRdGT_MYtg7GP22iXKcqFA-EfvTjHF3Cpwsi6oZV7nDMXPIw"/>
                  </div>
                  <div className="flex-1 min-w-0 flex flex-col justify-between h-full">
                    <div>
                      <h3 className="font-label-lg text-label-lg text-on-surface truncate">Xe đón khách sạn ra bến cano</h3>
                      <p className="font-body-sm text-body-sm text-on-surface-variant">Đón trả tận nơi tại Sơn Trà</p>
                    </div>
                    <div className="flex items-center justify-between gap-2 pt-1">
                      <span className="font-label-lg text-label-lg text-secondary font-bold">80.000đ<span className="font-body-sm text-body-sm text-on-surface-variant font-normal">/chuyến</span></span>
                      <button className="px-space-sm py-1 rounded-full bg-secondary-fixed text-on-secondary-fixed font-label-sm text-label-sm hover:bg-secondary-fixed-dim transition-colors flex items-center gap-1" type="button">
                        <span className="material-symbols-outlined text-[14px]">add</span> Thêm
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Right Column: Sticky Order Summary */}
          <aside className="lg:col-span-4 flex flex-col gap-space-md lg:sticky lg:top-28">
            <div className="p-space-md rounded-lg bg-primary/10 flex items-center gap-space-sm">
              <span className="material-symbols-outlined text-primary text-[24px] flex-shrink-0 animate-spin" style={{ animationDuration: "4s" }}>timelapse</span>
              <div className="flex-1">
                <p className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider">Thời gian giữ chỗ ưu tiên</p>
                <p className="font-label-lg text-label-lg text-primary font-bold flex items-center gap-1"><span className="">14:30</span></p>
                <p className="font-body-sm text-body-sm text-on-primary-fixed-variant mt-0.5">Vui lòng chuyển sang bước thanh toán trước khi slot được giải phóng.</p>
              </div>
            </div>

            <div className="p-space-lg rounded-lg bg-surface-container-lowest shadow-md flex flex-col gap-space-md">
              <h2 className="font-headline-sm text-headline-sm text-on-surface pb-space-xs">Tóm tắt đơn hàng</h2>
              <div className="flex flex-col gap-space-xs font-body-md text-body-md text-on-surface-variant">
                <div className="flex items-center justify-between"><span className="">Tạm tính ({items.length} trải nghiệm)</span><span className="font-semibold text-on-surface">{getTotalPrice().toLocaleString('vi-VN')}đ</span></div>
                <div className="flex items-center justify-between text-secondary"><span className="flex items-center gap-1"><span className="material-symbols-outlined text-[16px]">local_offer</span>Voucher (Nếu có)</span><span className="font-bold">0đ</span></div>
                <div className="flex items-center justify-between pt-1"><span className="">Phí nền tảng DANASEA</span><span className="font-semibold text-secondary">0đ</span></div>
              </div>
              
              <div className="pt-space-md flex items-end justify-between gap-space-sm">
                <div>
                  <p className="font-label-sm text-label-sm text-on-surface-variant uppercase tracking-wider font-bold">Tổng thanh toán</p>
                  <p className="font-body-sm text-body-sm text-on-surface-variant">Đã bao gồm thuế & phí</p>
                </div>
                <div className="text-right">
                  <p className="font-headline-lg text-headline-lg text-primary font-bold leading-none">{getTotalPrice().toLocaleString('vi-VN')}đ</p>
                </div>
              </div>
              
              <div className="flex items-center gap-space-xs bg-surface-container-low p-1 rounded-full mt-2">
                <span className="material-symbols-outlined text-outline pl-3 text-[18px]">redeem</span>
                <input className="bg-transparent border-none outline-none font-body-sm text-body-sm flex-1 min-w-0 placeholder-on-surface-variant/50" placeholder="Mã giảm giá (ví dụ: SEA10)" type="text"/>
                <button className="px-4 py-1.5 rounded-full bg-secondary text-on-secondary font-label-sm text-label-sm hover:scale-105 transition-transform" type="button">Áp dụng</button>
              </div>
            </div>

            <div className="flex flex-col gap-space-xs">
              <button 
                onClick={handleCheckout} 
                disabled={items.length === 0}
                className={`w-full py-4 rounded-full font-label-lg text-label-lg flex items-center justify-center gap-2 shadow-sm transition-transform ${items.length > 0 ? 'bg-primary text-on-primary hover:scale-[1.02] active:scale-[0.98]' : 'bg-surface-variant text-on-surface-variant cursor-not-allowed opacity-50'}`} 
                type="button"
              >
                <span className="material-symbols-outlined text-[20px]">credit_card</span>
                <span>Tiến hành thanh toán</span>
              </button>
              <p className="text-center font-body-sm text-on-surface-variant text-[11px] mt-1 flex items-center justify-center gap-1">
                <span className="material-symbols-outlined text-[14px]">lock</span>
                Thanh toán bảo mật SSL 256-bit
              </p>
            </div>
            
            <div className="mt-space-sm bg-surface-container-low p-space-md rounded-lg flex flex-col gap-2">
              <h3 className="font-label-md text-label-md text-on-surface">Chính sách hủy linh hoạt</h3>
              <ul className="font-body-sm text-body-sm text-on-surface-variant flex flex-col gap-1 list-disc pl-4">
                <li>Miễn phí hủy trước 48h khởi hành.</li>
                <li>Hoàn 100% nếu điều kiện biển không an toàn (có xác nhận từ cảng vụ).</li>
              </ul>
            </div>
          </aside>
        </div>
      </section>
    </div></main>
  );
}
