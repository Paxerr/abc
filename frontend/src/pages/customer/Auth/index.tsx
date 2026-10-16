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
    <main className="w-full flex-1 flex flex-col justify-center items-center relative overflow-hidden px-4 py-3 sm:py-6">
      {/* Dynamic Auto-Rotating Coastal Scene Backdrop (Same scenic imagery as Home page) */}
      <AuthCinematicBackdrop autoRotateInterval={5500} />

      {/* Toast thông báo đăng nhập thành công trượt từ bên phải sang trái */}
      {showSuccessToast && (
        <aside
          role="status"
          aria-live="polite"
          className="fixed top-6 right-6 z-[9999] px-6 py-3 rounded-xl bg-green-600 text-white shadow-[0_12px_32px_rgba(22,163,74,0.45)] animate-toast-slide-in flex items-center gap-2.5 pointer-events-auto border border-white/20"
          style={{ backgroundColor: '#16a34a' }}
        >
          <span className="material-symbols-outlined text-[20px] text-white" style={{ fontVariationSettings: "'FILL' 1" }}>
            check_circle
          </span>
          <span className="text-sm font-semibold tracking-wide text-white">
            Đăng nhập thành công
          </span>
        </aside>
      )}

      <div className="w-full max-w-[1180px] grid grid-cols-1 lg:grid-cols-12 gap-6 lg:gap-10 items-center relative z-10 -translate-y-6 sm:-translate-y-9 translate-x-3 sm:translate-x-8 lg:translate-x-14 my-auto">
        {/* Left Column: Coastal Brand Messaging */}
        <div className="lg:col-span-5 hidden lg:flex flex-col space-y-3">
          <h1 className="text-3xl xl:text-4xl font-extrabold text-white leading-tight drop-shadow-[0_4px_16px_rgba(0,0,0,0.8)]">
            Khám phá đại dương cùng <span className="text-cyan-400">DANASEA</span>
          </h1>
          <p className="text-sm xl:text-base text-slate-200 leading-relaxed max-w-md drop-shadow-[0_2px_8px_rgba(0,0,0,0.8)]">
            Cổng xác thực tập trung dành cho du khách trải nghiệm cano, lặn biển ngắm san hô Bán đảo Sơn Trà và nền tảng trải nghiệm &amp; thể thao biển Đà Nẵng.
          </p>
        </div>
        
        {/* Right Column: Auth Form Card */}
        <div className="lg:col-span-7 flex justify-center lg:justify-end w-full lg:pr-4">
          <div className="w-full max-w-md bg-surface-container-lowest/95 backdrop-blur-2xl rounded-2xl shadow-[0_20px_50px_rgba(0,0,0,0.35)] border border-white/30 p-5 sm:p-6 space-y-3.5">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-lg bg-primary-container flex items-center justify-center text-on-primary shadow-sm">
                <span className="material-symbols-outlined text-[18px]" style={{ fontVariationSettings: "'FILL' 1" }}>waves</span>
              </div>
              <span className="text-lg text-on-surface tracking-tight font-bold">DANA<span className="text-primary-container">SEA</span></span>
            </div>
            
            <div className="relative p-1 bg-surface-container rounded-xl flex items-center">
              <button 
                type="button"
                className={`relative z-10 w-1/2 py-1.5 rounded-lg text-xs sm:text-sm font-semibold transition-all duration-200 flex items-center justify-center gap-1.5 ${activeTab === "login" ? "text-primary-container bg-surface-container-lowest shadow-sm" : "text-on-surface-variant hover:text-on-surface bg-transparent"}`} 
                onClick={() => setActiveTab("login")}
              >
                <span className="material-symbols-outlined text-[17px]">login</span>
                Đăng nhập
              </button>
              <button 
                type="button"
                className={`relative z-10 w-1/2 py-1.5 rounded-lg text-xs sm:text-sm font-semibold transition-all duration-200 flex items-center justify-center gap-1.5 ${activeTab === "register" ? "text-primary-container bg-surface-container-lowest shadow-sm" : "text-on-surface-variant hover:text-on-surface bg-transparent"}`} 
                onClick={() => setActiveTab("register")}
              >
                <span className="material-symbols-outlined text-[17px]">person_add</span>
                Tạo tài khoản mới
              </button>
            </div>
            
            {activeTab === "login" && (
              <div className="space-y-3 block">
                {banner && (
                  <div className="space-y-2">
                    {banner === 'locked' && (
                      <div className="p-2.5 rounded-xl bg-error-container text-on-error-container flex items-start gap-2">
                        <span className="material-symbols-outlined text-[18px] text-error flex-shrink-0 mt-0.5">lock_clock</span>
                        <div>
                          <p className="text-xs font-bold">Tài khoản tạm thời bị khóa</p>
                          <p className="text-[11px] mt-0.5">Vui lòng liên hệ ban quản trị để được hỗ trợ mở khóa tài khoản.</p>
                        </div>
                      </div>
                    )}
                    {banner === 'wrong_creds' && (
                      <div className="p-2.5 rounded-xl bg-error-container text-on-error-container flex items-start gap-2">
                        <span className="material-symbols-outlined text-[18px] text-error flex-shrink-0 mt-0.5">error</span>
                        <p className="text-xs font-medium">Email hoặc mật khẩu không chính xác. Vui lòng kiểm tra lại thông tin.</p>
                      </div>
                    )}
                    {banner === 'network_error' && (
                      <div className="p-2.5 rounded-xl bg-error-container text-on-error-container flex items-start gap-2">
                        <span className="material-symbols-outlined text-[18px] text-error flex-shrink-0 mt-0.5">cloud_off</span>
                        <div>
                          <p className="text-xs font-bold">Không thể kết nối đến máy chủ Backend</p>
                          <p className="text-[11px] mt-0.5">{errorMessage || 'Vui lòng kiểm tra Docker và Backend Spring Boot (cổng 8080) đã được khởi động chưa.'}</p>
                        </div>
                      </div>
                    )}
                    {banner === 'custom_error' && (
                      <div className="p-2.5 rounded-xl bg-error-container text-on-error-container flex items-start gap-2">
                        <span className="material-symbols-outlined text-[18px] text-error flex-shrink-0 mt-0.5">warning</span>
                        <p className="text-xs font-medium">{errorMessage}</p>
                      </div>
                    )}
                  </div>
                )}

                {/* Quick Demo Role Selector */}
                <div className="p-2.5 rounded-xl bg-surface-container-low border border-outline-variant/30 space-y-1.5">
                  <div className="flex items-center justify-between text-xs">
                    <span className="font-bold text-on-surface uppercase tracking-wider flex items-center gap-1 text-[11px]">
                      <span className="material-symbols-outlined text-[14px] text-secondary">flash_on</span>
                      1-Click Login
                    </span>
                    <span className="text-[11px] font-mono text-outline">Pass: 123456</span>
                  </div>
                  <div className="grid grid-cols-3 gap-1.5">
                    <button
                      type="button"
                      disabled={isLoading}
                      onClick={() => handleQuickLogin('customer')}
                      className="p-1.5 rounded-lg bg-surface-container-lowest border border-outline-variant/30 hover:border-primary text-left transition-all hover:shadow-sm cursor-pointer group disabled:opacity-50"
                    >
                      <div className="flex items-center gap-1 text-primary font-bold text-xs">
                        <span className="material-symbols-outlined text-[14px]">person</span>
                        <span>Khách</span>
                      </div>
                      <p className="text-[10px] text-on-surface-variant truncate mt-0.5">customer</p>
                    </button>

                    <button
                      type="button"
                      disabled={isLoading}
                      onClick={() => handleQuickLogin('vendor')}
                      className="p-1.5 rounded-lg bg-surface-container-lowest border border-outline-variant/30 hover:border-secondary text-left transition-all hover:shadow-sm cursor-pointer group disabled:opacity-50"
                    >
                      <div className="flex items-center gap-1 text-secondary font-bold text-xs">
                        <span className="material-symbols-outlined text-[14px]">sailing</span>
                        <span>Vendor</span>
                      </div>
                      <p className="text-[10px] text-on-surface-variant truncate mt-0.5">vendor</p>
                    </button>

                    <button
                      type="button"
                      disabled={isLoading}
                      onClick={() => handleQuickLogin('admin')}
                      className="p-1.5 rounded-lg bg-surface-container-lowest border border-outline-variant/30 hover:border-primary-container text-left transition-all hover:shadow-sm cursor-pointer group disabled:opacity-50"
                    >
                      <div className="flex items-center gap-1 text-primary-container font-bold text-xs">
                        <span className="material-symbols-outlined text-[14px]">admin_panel_settings</span>
                        <span>Admin</span>
                      </div>
                      <p className="text-[10px] text-on-surface-variant truncate mt-0.5">admin</p>
                    </button>
                  </div>
                </div>
                
                <form className="space-y-2.5" onSubmit={handleLoginSubmit}>
                  <div className="space-y-1">
                    <label className="text-xs font-medium text-on-surface flex items-center justify-between">
                      <span>Địa chỉ Email hoặc Tên tài khoản</span>
                      <span className="text-secondary text-[11px]">*Bắt buộc</span>
                    </label>
                    <div className="relative flex items-center">
                      <span className="material-symbols-outlined absolute left-3.5 text-outline text-[18px] pointer-events-none">mail</span>
                      <input 
                        value={email} 
                        onChange={e => setEmail(e.target.value)} 
                        disabled={isLoading}
                        className="w-full h-[42px] pl-10 pr-3 rounded-xl bg-surface-container-lowest text-on-surface text-sm shadow-sm outline-none border border-outline-variant/30 focus:border-primary focus:shadow-md transition-all placeholder:text-outline-variant disabled:opacity-60" 
                        placeholder="Nhập email (vd: customer@danasea.vn)" 
                        required 
                        type="text" 
                      />
                    </div>
                  </div>
                  
                  <div className="space-y-1">
                    <div className="flex items-center justify-between">
                      <label className="text-xs font-medium text-on-surface">Mật khẩu</label>
                      <a className="text-[11px] text-primary-container hover:underline cursor-pointer" onClick={() => navigate('/auth/forgot-password')}>
                        Quên mật khẩu?
                      </a>
                    </div>
                    <div className="relative flex items-center">
                      <span className="material-symbols-outlined absolute left-3.5 text-outline text-[18px] pointer-events-none">lock</span>
                      <input 
                        value={password} 
                        onChange={e => setPassword(e.target.value)} 
                        disabled={isLoading}
                        className="w-full h-[42px] pl-10 pr-10 rounded-xl bg-surface-container-lowest text-on-surface text-sm shadow-sm outline-none border border-outline-variant/30 focus:border-primary focus:shadow-md transition-all placeholder:text-outline-variant disabled:opacity-60" 
                        placeholder="Nhập mật khẩu" 
                        required 
                        type="password" 
                      />
                    </div>
                  </div>
                  
                  <button 
                    className="w-full h-[44px] rounded-xl bg-primary-container text-on-primary text-sm font-semibold flex items-center justify-center gap-2 shadow-md hover:bg-primary transition-all duration-200 cursor-pointer disabled:opacity-60 disabled:cursor-not-allowed mt-1" 
                    type="submit"
                    disabled={isLoading}
                  >
                    {isLoading ? (
                      <>
                        <span className="material-symbols-outlined animate-spin text-[18px]">progress_activity</span>
                        <span>Đang xác thực thông tin...</span>
                      </>
                    ) : (
                      <>
                        <span>Đăng nhập hệ sinh thái DANASEA</span>
                        <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
                      </>
                    )}
                  </button>
                </form>
                
                <div className="pt-1 text-center">
                  <p className="text-xs text-on-surface-variant">
                    Chưa có tài khoản tham gia trải nghiệm? 
                    <button type="button" className="text-primary-container font-semibold hover:underline bg-transparent border-0 cursor-pointer ml-1" onClick={() => setActiveTab("register")}>Đăng ký thành viên ngay</button>
                  </p>
                </div>
              </div>
            )}

                {activeTab === "register" && (
                  <div className="space-y-3">
                    <div className="space-y-0.5">
                      <h3 className="text-sm font-bold text-on-surface">Đăng ký tài khoản du khách &amp; đối tác</h3>
                      <p className="text-xs text-on-surface-variant">Điền thông tin để nhận mã QR vé điện tử chính chủ tại cảng Đà Nẵng.</p>
                    </div>
                    {banner === 'register_error' && (
                      <div className="p-2.5 rounded-xl bg-error-container text-on-error-container">
                        <p className="text-xs font-semibold">Địa chỉ email này đã được đăng ký trong hệ thống DANASEA.</p>
                      </div>
                    )}
                    <form className="space-y-2.5" onSubmit={handleRegisterSubmit}>
                      <div className="space-y-1">
                        <label className="text-xs font-medium text-on-surface flex items-center justify-between">
                          <span>Họ và tên</span>
                          <span className="text-secondary text-[11px]">*Bắt buộc</span>
                        </label>
                        <div className="relative flex items-center">
                          <span className="material-symbols-outlined absolute left-3.5 text-outline text-[18px] pointer-events-none">badge</span>
                          <input className="w-full h-[42px] pl-10 pr-3 rounded-xl bg-surface-container-lowest text-on-surface text-sm shadow-sm outline-none border border-outline-variant/30 focus:border-primary focus:shadow-md transition-all placeholder:text-outline-variant" placeholder="Ví dụ: Nguyễn Hải Đăng" required type="text" />
                        </div>
                      </div>
                      <div className="space-y-1">
                        <label className="text-xs font-medium text-on-surface flex items-center justify-between">
                          <span>Địa chỉ Email</span>
                          <span className="text-secondary text-[11px]">*Bắt buộc</span>
                        </label>
                        <div className="relative flex items-center">
                          <span className="material-symbols-outlined absolute left-3.5 text-outline text-[18px] pointer-events-none">mail</span>
                          <input className="w-full h-[42px] pl-10 pr-3 rounded-xl bg-surface-container-lowest text-on-surface text-sm shadow-sm outline-none border border-outline-variant/30 focus:border-primary focus:shadow-md transition-all placeholder:text-outline-variant" placeholder="ban@email.com" required type="email" />
                        </div>
                      </div>
                      <div className="space-y-1">
                        <label className="text-xs font-medium text-on-surface flex items-center justify-between">
                          <span>Mật khẩu</span>
                          <span className="text-secondary text-[11px]">*Bắt buộc</span>
                        </label>
                        <div className="relative flex items-center">
                          <span className="material-symbols-outlined absolute left-3.5 text-outline text-[18px] pointer-events-none">lock</span>
                          <input className="w-full h-[42px] pl-10 pr-3 rounded-xl bg-surface-container-lowest text-on-surface text-sm shadow-sm outline-none border border-outline-variant/30 focus:border-primary focus:shadow-md transition-all placeholder:text-outline-variant" placeholder="Nhập mật khẩu" required type="password" />
                        </div>
                      </div>
                      <button className="w-full h-[44px] rounded-xl bg-primary-container text-on-primary text-sm font-semibold flex items-center justify-center gap-2 hover:bg-primary transition-all duration-200 cursor-pointer shadow-md mt-1" type="submit">
                        Tạo tài khoản miễn phí
                      </button>
                    </form>
                  </div>
                )}
              </div>
            </div>
          </div>
    </main>
  );
}
