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

      {/* Global Floating Wishlist Toast Notification (Nền xanh lá nhạt trong suốt, chữ trắng) */}
      {toastMessage && (
        <div className="fixed top-24 right-5 sm:top-24 sm:right-8 z-[99999] bg-emerald-500/35 backdrop-blur-xl text-white px-4 py-3 rounded-2xl shadow-[0_12px_36px_-4px_rgba(5,150,105,0.35)] flex items-center gap-3 border border-emerald-400/40 animate-fade-in-down">
          <div className="w-8 h-8 rounded-full bg-white/20 border border-white/30 flex items-center justify-center shrink-0">
            <span
              className="material-symbols-outlined text-rose-400 text-[18px]"
              style={{ fontVariationSettings: "'FILL' 1" }}
            >
              favorite
            </span>
          </div>
          <span className="font-medium text-xs sm:text-sm text-white drop-shadow-[0_1px_2px_rgba(0,0,0,0.6)] line-clamp-1 max-w-xs sm:max-w-md pr-1">
            {toastMessage}
          </span>
          <button
            onClick={clearToast}
            className="ml-auto p-1 rounded-full text-white/70 hover:text-white hover:bg-white/20 cursor-pointer transition-colors"
            aria-label="Close toast"
          >
            <span className="material-symbols-outlined text-[18px]">close</span>
          </button>
        </div>
      )}
    </div>
  );
}
