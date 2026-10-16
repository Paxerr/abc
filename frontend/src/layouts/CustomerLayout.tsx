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

      {/* Global Floating Wishlist Toast Notification (Chuyển lên góc phải trên, tone màu nền cyan chủ đạo) */}
      {toastMessage && (
        <div className="fixed top-24 right-6 sm:top-24 sm:right-8 z-[99999] bg-gradient-to-r from-cyan-600 via-cyan-700 to-teal-700 text-white px-5 py-3.5 rounded-2xl shadow-[0_12px_32px_-4px_rgba(6,182,212,0.4),0_4px_12px_rgba(0,0,0,0.1)] flex items-center gap-3 backdrop-blur-md border border-cyan-300/40 animate-fade-in-down">
          <div className="w-8 h-8 rounded-full bg-white/20 flex items-center justify-center shrink-0">
            <span
              className="material-symbols-outlined text-rose-300 text-[20px]"
              style={{ fontVariationSettings: "'FILL' 1" }}
            >
              favorite
            </span>
          </div>
          <span className="font-medium text-sm text-white max-w-xs sm:max-w-md drop-shadow-sm">
            {toastMessage}
          </span>
          <button
            onClick={clearToast}
            className="ml-2 p-1 rounded-full hover:bg-white/20 text-white/80 hover:text-white cursor-pointer transition-colors"
            aria-label="Close toast"
          >
            <span className="material-symbols-outlined text-[16px]">close</span>
          </button>
        </div>
      )}
    </div>
  );
}
