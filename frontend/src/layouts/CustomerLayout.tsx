import { Outlet, useLocation } from "react-router-dom";
import { Header } from "../components/layout/Header";
import { Footer } from "../components/layout/Footer";
import { useEffect, useState } from "react";
import { useWishlistStore } from "../store/useWishlistStore";

// Định nghĩa thứ tự các trang chính để xác định hướng trượt
const PAGE_ORDER: Record<string, number> = {
  "/": 0,
  "/search": 1,
  "/cart": 2,
  "/profile": 3,
};

export function CustomerLayout() {
  const location = useLocation();
  const [prevPath, setPrevPath] = useState(location.pathname);
  const [animationClass, setAnimationClass] = useState("animate-fade-in-up");
  const { toastMessage, clearToast } = useWishlistStore();

  useEffect(() => {
    if (toastMessage) {
      const timer = setTimeout(() => {
        clearToast();
      }, 3000);
      return () => clearTimeout(timer);
    }
  }, [toastMessage, clearToast]);

  if (location.pathname !== prevPath) {
    const prevOrder = PAGE_ORDER[prevPath] ?? 0;
    const currentOrder = PAGE_ORDER[location.pathname] ?? 0;
    setPrevPath(location.pathname);
    if (currentOrder > prevOrder) {
      setAnimationClass("animate-slide-in-right");
    } else if (currentOrder < prevOrder) {
      setAnimationClass("animate-slide-in-left");
    } else {
      setAnimationClass("animate-fade-in-up");
    }
  }

  useEffect(() => {
    if (location.hash) {
      const timer = setTimeout(() => {
        const element = document.querySelector(location.hash);
        if (element) {
          const headerHeight = 80;
          const targetPosition = element.getBoundingClientRect().top + window.scrollY - headerHeight;
          window.scrollTo({ top: targetPosition, behavior: "smooth" });
        }
      }, 100);
      return () => clearTimeout(timer);
    }
  }, [location.hash]);

  const isHomePage = location.pathname === '/';
  const isAuthPage = location.pathname === '/auth';

  return (
    <div className={`${isHomePage ? 'bg-transparent' : isAuthPage ? 'bg-slate-950' : 'bg-surface'} font-body-md text-on-surface min-h-screen flex flex-col overflow-x-hidden`}>
      <Header />
      {/* Bao bọc Outlet bằng một the div chứa key={location.pathname} để ép React render lại khi đổi route */}
      <div
        key={location.pathname}
        onAnimationEnd={() => setAnimationClass("")}
        className={`flex-1 flex flex-col ${animationClass}`}
      >
        <Outlet />
      </div>
      {!isAuthPage && <Footer />}

      {/* Global Floating Wishlist Toast Notification (Phong cách Frosted Glass tươi sáng, cao cấp) */}
      {toastMessage && (
        <div className="fixed top-24 right-5 sm:top-24 sm:right-8 z-[99999] bg-white/95 backdrop-blur-xl text-slate-800 px-4 py-3 rounded-2xl shadow-[0_16px_40px_-6px_rgba(0,0,0,0.12),0_4px_16px_rgba(0,0,0,0.06)] flex items-center gap-3 border border-slate-200/90 animate-fade-in-down">
          <div className="w-9 h-9 rounded-full bg-rose-50 border border-rose-100 flex items-center justify-center shrink-0">
            <span
              className="material-symbols-outlined text-rose-500 text-[20px]"
              style={{ fontVariationSettings: "'FILL' 1" }}
            >
              favorite
            </span>
          </div>
          <div className="flex flex-col min-w-0 pr-1">
            <span className="text-[10px] font-bold uppercase tracking-wider text-rose-500 font-mono">
              Danh sách yêu thích
            </span>
            <span className="font-semibold text-xs sm:text-sm text-slate-800 line-clamp-1 max-w-xs sm:max-w-md">
              {toastMessage}
            </span>
          </div>
          <button
            onClick={clearToast}
            className="ml-auto p-1 rounded-full text-slate-400 hover:text-slate-700 hover:bg-slate-100 cursor-pointer transition-colors"
            aria-label="Close toast"
          >
            <span className="material-symbols-outlined text-[18px]">close</span>
          </button>
        </div>
      )}
    </div>
  );
}
