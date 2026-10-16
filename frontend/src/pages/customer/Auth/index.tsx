import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../../../store/useAuthStore";
import { authService } from "../../../services/authService";
import { AuthCinematicBackdrop } from "../../../components/customer/AuthCinematicBackdrop";
import type { User } from "../../../types";

export function Auth() {
  const [activeTab, setActiveTab] = useState("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [banner, setBanner] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [showSuccessToast, setShowSuccessToast] = useState(false);
  
  const navigate = useNavigate();
  const setAuth = useAuthStore(state => state.setAuth);

  const executeLogin = async (emailInput: string, passwordInput: string) => {
    const trimmedEmail = emailInput.trim();
    if (!trimmedEmail || !passwordInput) {
      setBanner('wrong_creds');
      setErrorMessage(null);
      return;
    }

    setIsLoading(true);
    setBanner(null);
    setErrorMessage(null);

    console.groupCollapsed(`%c[Auth Flow] Đang đăng nhập: ${trimmedEmail}`, 'color: #0284c7; font-weight: bold;');
    console.log('[Bước 1/5] Kiểm tra tham số gửi đi:', { email: trimmedEmail, password: '***' });
    console.log('[Bước 2/5] Đang gửi HTTP POST /api/auth/login tới Backend...');

    try {
      const authRes = await authService.login({
        email: trimmedEmail,
        password: passwordInput,
      });

      console.log('[Bước 3/5] Backend trả về kết quả thành công (200 OK):', authRes);

      // Construct user model from AuthenticationResponse
      const role = authRes.role;
      const avatarUrl = role === 'ADMIN'
        ? 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150'
        : role === 'VENDOR'
        ? 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150'
        : 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150';

      const userModel: User = {
        id: authRes.userId,
        email: authRes.email,
        role: role,
        fullName: trimmedEmail.split('@')[0],
        avatarUrl: avatarUrl,
        isEmailVerified: true,
        isLocked: false,
        phone: '',
        passwordHash: '',
        locale: 'vi',
      };

      console.log('[Bước 4/5] Lưu trữ Token và User vào Zustand Store (danasea_auth_storage):', {
        userId: userModel.id,
        role: userModel.role,
        tokenLength: authRes.accessToken?.length,
      });
      setAuth(userModel, authRes.accessToken);

      // Determine redirection route based on role
      const destination = role === 'ADMIN' ? '/admin' : role === 'VENDOR' ? '/vendor' : '/';

      // Trigger Slide-in Success Toast at top-right
      setShowSuccessToast(true);

      console.log(`[Bước 5/5] Hoàn tất đăng nhập! Chuẩn bị điều hướng tới: ${destination}`);
      console.groupEnd();

      setTimeout(() => {
        navigate(destination);
      }, 1200);
    } catch (err: any) {
      console.groupEnd();
      console.error('[Auth Flow] Lỗi đăng nhập thất bại:', err);

      const status = err.response?.status;
      const serverMessage = err.response?.data?.message || err.message;

      if (status === 401) {
        console.warn('[Auth Flow] Sai tài khoản hoặc mật khẩu (401 Unauthorized)');
        setBanner('wrong_creds');
      } else if (status === 403) {
        console.warn('[Auth Flow] Tài khoản bị khóa (403 Forbidden)');
        setBanner('locked');
      } else if (!err.response || err.code === 'ERR_NETWORK') {
        console.error('[Auth Flow] Không thể kết nối tới Backend (Network Error)');
        setBanner('network_error');
        setErrorMessage('Không thể kết nối đến máy chủ Backend (http://localhost:8080). Hãy đảm bảo Backend đã khởi động.');
      } else {
        console.error('[Auth Flow] Lỗi từ máy chủ:', serverMessage);
        setBanner('custom_error');
        setErrorMessage(serverMessage || 'Đăng nhập không thành công. Vui lòng thử lại.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const handleQuickLogin = (roleKey: 'customer' | 'vendor' | 'admin') => {
    const quickEmail = `${roleKey}@danasea.vn`;
    const quickPass = '123456';
    setEmail(quickEmail);
    setPassword(quickPass);
    executeLogin(quickEmail, quickPass);
  };

  const handleLoginSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    executeLogin(email, password);
  };

  const handleRegisterSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    // Simulate register for now (Branch 2: feat/auth-register will implement this)
    setBanner('register_error');
  };

  return (
    <main className="w-full min-h-screen pt-20 bg-slate-950 flex flex-col justify-center items-center relative overflow-x-hidden">
      {/* Dynamic Auto-Rotating Coastal Scene Backdrop (Same scenic imagery as Home page) */}
      <AuthCinematicBackdrop autoRotateInterval={5500} />

      {/* Toast thông báo đăng nhập thành công trượt từ bên phải sang trái */}
      {showSuccessToast && (
        <aside
          role="status"
          aria-live="polite"
          className="fixed top-6 right-6 z-[9999] px-6 py-3.5 rounded-2xl bg-green-600 text-white shadow-[0_12px_32px_rgba(22,163,74,0.45)] animate-toast-slide-in flex items-center gap-2.5 pointer-events-auto border border-white/20"
          style={{ backgroundColor: '#16a34a' }}
        >
          <span className="material-symbols-outlined text-[22px] text-white" style={{ fontVariationSettings: "'FILL' 1" }}>
            check_circle
          </span>
          <span className="font-headline-sm text-sm font-semibold tracking-wide text-white">
            Đăng nhập thành công
          </span>
        </aside>
      )}

      <div className="flex flex-col w-full relative z-10">
        <div className="relative w-full py-space-2xl px-space-lg lg:px-margin flex justify-center items-center">
          <div className="relative w-full max-w-[1280px] grid grid-cols-1 lg:grid-cols-12 gap-gutter-lg items-center">
            {/* Left Column: Coastal Brand Messaging */}
            <div className="lg:col-span-5 flex flex-col space-y-space-xl">
              <div className="inline-flex items-center gap-space-xs px-space-md py-1.5 rounded-full bg-slate-900/70 backdrop-blur-md border border-white/20 text-cyan-300 w-fit shadow-md">
                <span className="material-symbols-outlined text-headline-sm text-cyan-400" style={{ fontVariationSettings: "'FILL' 1" }}>sailing</span>
                <span className="font-label-sm text-label-sm uppercase tracking-wider font-semibold">Nền tảng trải nghiệm &amp; thể thao biển Đà Nẵng</span>
              </div>
              <div className="space-y-space-sm">
                <h1 className="font-headline-xl text-headline-xl text-white leading-tight drop-shadow-[0_4px_16px_rgba(0,0,0,0.8)]">
                  Khám phá đại dương cùng <span className="text-cyan-400">DANASEA</span>
                </h1>
                <p className="font-body-lg text-body-lg text-slate-200 max-w-md drop-shadow-[0_2px_8px_rgba(0,0,0,0.8)]">
                  Cổng xác thực tập trung dành cho du khách trải nghiệm cano, lặn biển ngắm san hô Bán đảo Sơn Trà và nền tảng trải nghiệm &amp; thể thao biển Đà Nẵng.
                </p>
              </div>
              
              <div className="space-y-space-md">
                <div className="flex items-start gap-space-md p-space-md rounded-2xl bg-slate-900/60 backdrop-blur-md border border-white/15 text-white transition-all duration-300 hover:bg-slate-900/80 hover:border-cyan-400/40 shadow-lg">
                  <div className="flex-shrink-0 w-11 h-11 rounded-xl bg-cyan-500/20 flex items-center justify-center text-cyan-300 border border-cyan-400/30">
                    <span className="material-symbols-outlined" style={{ fontVariationSettings: "'FILL' 1" }}>scuba_diving</span>
                  </div>
                  <div className="space-y-0.5">
                    <h2 className="font-headline-sm text-headline-sm text-white font-semibold">Đặt trải nghiệm biển chính thống</h2>
                    <p className="font-body-sm text-body-sm text-slate-300">Lặn ngắm san hô Bãi Bụt, bay dù lượn Mỹ Khê, tour cano cao cấp đạt chuẩn cứu hộ quốc tế.</p>
                  </div>
                </div>
              </div>
            </div>
            
            {/* Right Column: Auth Form Card */}
            <div className="lg:col-span-7 flex justify-center">
              <div className="w-full max-w-xl bg-surface-container-lowest/95 backdrop-blur-2xl rounded-[28px] shadow-[0_25px_60px_rgba(0,0,0,0.35)] border border-white/30 p-space-xl sm:p-space-2xl space-y-space-lg">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div className="w-9 h-9 rounded-xl bg-primary-container flex items-center justify-center text-on-primary shadow-sm">
                      <span className="material-symbols-outlined" style={{ fontVariationSettings: "'FILL' 1" }}>waves</span>
                    </div>
                    <span className="font-headline-md text-headline-md text-on-surface tracking-tight">DANA<span className="text-primary-container">SEA</span></span>
                  </div>
                  <span className="font-label-sm text-label-sm text-on-surface-variant bg-surface-container-high px-space-sm py-1 rounded-full uppercase">
                    Cổng Tài Khoản
                  </span>
                </div>
                
                <div className="relative p-1 bg-surface-container rounded-2xl flex items-center">
                  <button className={`relative z-10 w-1/2 py-2.5 rounded-xl font-label-lg text-label-lg transition-all duration-200 flex items-center justify-center gap-2 ${activeTab === "login" ? "text-primary-container bg-surface-container-lowest shadow-sm" : "text-on-surface-variant hover:text-on-surface bg-transparent"}`} onClick={() => setActiveTab("login")}>
                    <span className="material-symbols-outlined text-body-lg">login</span>
                    Đăng nhập
                  </button>
                  <button className={`relative z-10 w-1/2 py-2.5 rounded-xl font-label-lg text-label-lg transition-all duration-200 flex items-center justify-center gap-2 ${activeTab === "register" ? "text-primary-container bg-surface-container-lowest shadow-sm" : "text-on-surface-variant hover:text-on-surface bg-transparent"}`} onClick={() => setActiveTab("register")}>
                    <span className="material-symbols-outlined text-body-lg">person_add</span>
                    Tạo tài khoản mới
                  </button>
                </div>
                
                {activeTab === "login" && (
                  <div className="space-y-space-lg block">
                    <div className="space-y-space-sm">
                      {banner === 'locked' && (
                        <div className="p-space-md rounded-2xl bg-error-container text-on-error-container flex items-start gap-2">
                          <span className="material-symbols-outlined text-[20px] text-error flex-shrink-0 mt-0.5">lock_clock</span>
                          <div>
                            <p className="font-label-lg text-label-lg font-bold">Tài khoản tạm thời bị khóa</p>
                            <p className="font-body-sm text-body-sm mt-0.5">Vui lòng liên hệ ban quản trị để được hỗ trợ mở khóa tài khoản.</p>
                          </div>
                        </div>
                      )}
                      {banner === 'wrong_creds' && (
                        <div className="p-space-md rounded-2xl bg-error-container text-on-error-container flex items-start gap-2">
                          <span className="material-symbols-outlined text-[20px] text-error flex-shrink-0 mt-0.5">error</span>
                          <div>
                            <p className="font-body-sm text-body-sm font-medium">Email hoặc mật khẩu không chính xác. Vui lòng kiểm tra lại thông tin.</p>
                          </div>
                        </div>
                      )}
                      {banner === 'network_error' && (
                        <div className="p-space-md rounded-2xl bg-error-container text-on-error-container flex items-start gap-2">
                          <span className="material-symbols-outlined text-[20px] text-error flex-shrink-0 mt-0.5">cloud_off</span>
                          <div>
                            <p className="font-label-lg text-label-lg font-bold">Không thể kết nối đến máy chủ Backend</p>
                            <p className="font-body-sm text-body-sm mt-0.5">{errorMessage || 'Vui lòng kiểm tra Docker và Backend Spring Boot (cổng 8080) đã được khởi động chưa.'}</p>
                          </div>
                        </div>
                      )}
                      {banner === 'custom_error' && (
                        <div className="p-space-md rounded-2xl bg-error-container text-on-error-container flex items-start gap-2">
                          <span className="material-symbols-outlined text-[20px] text-error flex-shrink-0 mt-0.5">warning</span>
                          <p className="font-body-sm text-body-sm font-medium">{errorMessage}</p>
                        </div>
                      )}
                    </div>

                    {/* Quick Demo Role Selector */}
                    <div className="p-4 rounded-2xl bg-surface-container-low border border-outline-variant/30 space-y-2.5">
                      <div className="flex items-center justify-between">
                        <span className="font-label-sm text-label-sm font-bold text-on-surface uppercase tracking-wider flex items-center gap-1.5">
                          <span className="material-symbols-outlined text-[16px] text-secondary">flash_on</span>
                          Tài khoản thử nghiệm (1-Click Login)
                        </span>
                        <span className="text-[11px] font-mono text-outline">Pass: 123456</span>
                      </div>
                      <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                        <button
                          type="button"
                          disabled={isLoading}
                          onClick={() => handleQuickLogin('customer')}
                          className="p-2.5 rounded-xl bg-surface-container-lowest border border-outline-variant/30 hover:border-primary text-left transition-all hover:shadow-md cursor-pointer group disabled:opacity-50"
                        >
                          <div className="flex items-center gap-1.5 text-primary font-bold font-label-sm text-label-sm">
                            <span className="material-symbols-outlined text-[16px]">person</span>
                            Khách hàng
                          </div>
                          <p className="font-label-sm text-[11px] text-on-surface-variant truncate mt-0.5">customer@danasea.vn</p>
                        </button>

                        <button
                          type="button"
                          disabled={isLoading}
                          onClick={() => handleQuickLogin('vendor')}
                          className="p-2.5 rounded-xl bg-surface-container-lowest border border-outline-variant/30 hover:border-secondary text-left transition-all hover:shadow-md cursor-pointer group disabled:opacity-50"
                        >
                          <div className="flex items-center gap-1.5 text-secondary font-bold font-label-sm text-label-sm">
                            <span className="material-symbols-outlined text-[16px]">sailing</span>
                            Đối tác Vendor
                          </div>
                          <p className="font-label-sm text-[11px] text-on-surface-variant truncate mt-0.5">vendor@danasea.vn</p>
                        </button>

                        <button
                          type="button"
                          disabled={isLoading}
                          onClick={() => handleQuickLogin('admin')}
                          className="p-2.5 rounded-xl bg-surface-container-lowest border border-outline-variant/30 hover:border-primary-container text-left transition-all hover:shadow-md cursor-pointer group disabled:opacity-50"
                        >
                          <div className="flex items-center gap-1.5 text-primary-container font-bold font-label-sm text-label-sm">
                            <span className="material-symbols-outlined text-[16px]">admin_panel_settings</span>
                            Quản trị viên
                          </div>
                          <p className="font-label-sm text-[11px] text-on-surface-variant truncate mt-0.5">admin@danasea.vn</p>
                        </button>
                      </div>
                    </div>
                    
                    <form className="space-y-space-md" onSubmit={handleLoginSubmit}>
                      <div className="space-y-1.5">
                        <label className="font-label-md text-label-md text-on-surface flex items-center justify-between">
                          <span>Địa chỉ Email hoặc Tên tài khoản</span>
                          <span className="text-secondary font-label-sm text-label-sm">*Bắt buộc</span>
                        </label>
                        <div className="relative flex items-center">
                          <span className="material-symbols-outlined absolute left-4 text-outline text-body-lg pointer-events-none">mail</span>
                          <input 
                            value={email} 
                            onChange={e => setEmail(e.target.value)} 
                            disabled={isLoading}
                            className="w-full h-[52px] pl-11 pr-4 rounded-xl bg-surface-container-lowest text-on-surface font-body-md text-body-md shadow-sm outline-none focus:bg-surface-container-lowest focus:shadow-md transition-all placeholder:text-outline-variant disabled:opacity-60" 
                            placeholder="Nhập email (vd: customer@danasea.vn)" 
                            required 
                            type="text" 
                          />
                        </div>
                      </div>
                      
                      <div className="space-y-1.5">
                        <div className="flex items-center justify-between">
                          <label className="font-label-md text-label-md text-on-surface">Mật khẩu</label>
                          <a className="font-label-sm text-label-sm text-primary-container hover:underline cursor-pointer" onClick={() => navigate('/auth/forgot-password')}>
                            Quên mật khẩu?
                          </a>
                        </div>
                        <div className="relative flex items-center">
                          <span className="material-symbols-outlined absolute left-4 text-outline text-body-lg pointer-events-none">lock</span>
                          <input 
                            value={password} 
                            onChange={e => setPassword(e.target.value)} 
                            disabled={isLoading}
                            className="w-full h-[52px] pl-11 pr-12 rounded-xl bg-surface-container-lowest text-on-surface font-body-md text-body-md shadow-sm outline-none focus:shadow-md transition-all placeholder:text-outline-variant disabled:opacity-60" 
                            placeholder="Nhập mật khẩu của bạn" 
                            required 
                            type="password" 
                          />
                        </div>
                      </div>
                      
                      <button 
                        className="w-full h-[52px] rounded-xl bg-primary-container text-on-primary font-label-lg text-label-lg font-semibold flex items-center justify-center gap-2 shadow-md hover:bg-primary transition-all duration-200 cursor-pointer disabled:opacity-60 disabled:cursor-not-allowed" 
                        type="submit"
                        disabled={isLoading}
                      >
                        {isLoading ? (
                          <>
                            <span className="material-symbols-outlined animate-spin text-body-lg">progress_activity</span>
                            <span>Đang xác thực thông tin...</span>
                          </>
                        ) : (
                          <>
                            <span>Đăng nhập hệ sinh thái DANASEA</span>
                            <span className="material-symbols-outlined text-body-lg">arrow_forward</span>
                          </>
                        )}
                      </button>
                    </form>
                    
                    <div className="pt-space-xs text-center">
                      <p className="font-body-sm text-body-sm text-on-surface-variant">
                        Chưa có tài khoản tham gia trải nghiệm? 
                        <button type="button" className="text-primary-container font-semibold hover:underline bg-transparent border-0 cursor-pointer ml-1" onClick={() => setActiveTab("register")}>Đăng ký thành viên ngay</button>
                      </p>
                    </div>
                  </div>
                )}

                {activeTab === "register" && (
                  <div className="space-y-space-md">
                    <div className="space-y-1">
                      <h3 className="font-headline-sm text-headline-sm text-on-surface">Đăng ký tài khoản du khách &amp; đối tác</h3>
                      <p className="font-body-sm text-body-sm text-on-surface-variant">Điền đầy đủ thông tin chuẩn định danh để nhận vé QR chính chủ tại cảng biển Đà Nẵng.</p>
                    </div>
                    {banner === 'register_error' && (
                      <div className="p-space-md rounded-2xl bg-error-container text-on-error-container">
                        <p className="font-body-sm text-body-sm font-semibold">Địa chỉ email này đã được đăng ký trong hệ thống DANASEA.</p>
                      </div>
                    )}
                    <form className="space-y-space-md" onSubmit={handleRegisterSubmit}>
                      <div className="space-y-1">
                        <label className="font-label-md text-label-md text-on-surface flex items-center justify-between">
                          <span>Họ và tên</span>
                          <span className="text-secondary font-label-sm text-label-sm">*Bắt buộc</span>
                        </label>
                        <div className="relative flex items-center">
                          <span className="material-symbols-outlined absolute left-4 text-outline text-body-lg pointer-events-none">badge</span>
                          <input className="w-full h-[50px] pl-11 pr-4 rounded-xl bg-surface-container-lowest text-on-surface font-body-md text-body-md shadow-sm outline-none focus:shadow-md transition-all placeholder:text-outline-variant" placeholder="Ví dụ: Nguyễn Hải Đăng" required type="text" />
                        </div>
                      </div>
                      <div className="space-y-1">
                        <label className="font-label-md text-label-md text-on-surface flex items-center justify-between">
                          <span>Địa chỉ Email</span>
                          <span className="text-secondary font-label-sm text-label-sm">*Bắt buộc</span>
                        </label>
                        <div className="relative flex items-center">
                          <span className="material-symbols-outlined absolute left-4 text-outline text-body-lg pointer-events-none">mail</span>
                          <input className="w-full h-[50px] pl-11 pr-4 rounded-xl bg-surface-container-lowest text-on-surface font-body-md text-body-md shadow-sm outline-none focus:shadow-md transition-all placeholder:text-outline-variant" placeholder="ban@email.com" required type="email" />
                        </div>
                      </div>
                      <div className="space-y-1">
                        <label className="font-label-md text-label-md text-on-surface flex items-center justify-between">
                          <span>Mật khẩu</span>
                          <span className="text-secondary font-label-sm text-label-sm">*Bắt buộc</span>
                        </label>
                        <div className="relative flex items-center">
                          <span className="material-symbols-outlined absolute left-4 text-outline text-body-lg pointer-events-none">lock</span>
                          <input className="w-full h-[50px] pl-11 pr-4 rounded-xl bg-surface-container-lowest text-on-surface font-body-md text-body-md shadow-sm outline-none focus:shadow-md transition-all placeholder:text-outline-variant" placeholder="Nhập mật khẩu" required type="password" />
                        </div>
                      </div>
                      <button className="w-full h-[52px] rounded-xl bg-surface-container-high text-on-surface font-label-lg text-label-lg font-semibold flex items-center justify-center gap-2 hover:bg-surface-dim transition-all duration-200 cursor-pointer" type="submit">
                        Tạo tài khoản miễn phí
                      </button>
                    </form>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}
