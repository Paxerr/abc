import React, { useEffect, useState, useRef } from 'react';
import { useLoadingStore } from '../../store/useLoadingStore';

export function PageLoader() {
  const { isLoading, message, subMessage, durationMs, hideLoader, showLoader } = useLoadingStore();
  const [progress, setProgress] = useState(0);
  const [isExiting, setIsExiting] = useState(false);
  const rafRef = useRef<number | null>(null);
  const startTimeRef = useRef<number | null>(null);

  // 1. Initial Splash Screen on first session visit
  useEffect(() => {
    const hasSeenSplash = sessionStorage.getItem('danasea_initial_splash_shown');
    if (!hasSeenSplash) {
      sessionStorage.setItem('danasea_initial_splash_shown', 'true');
      showLoader(
        'Chào mừng bạn đến với DANASEA',
        'Chạm sóng biển, mở chuyến đi riêng',
        2400
      );
    }
  }, [showLoader]);

  // 2. Liquid Wave Progress Animation
  useEffect(() => {
    if (!isLoading) {
      setProgress(0);
      setIsExiting(false);
      if (rafRef.current) cancelAnimationFrame(rafRef.current);
      return;
    }

    setProgress(0);
    setIsExiting(false);
    startTimeRef.current = null;

    const targetDuration = Math.max(durationMs || 2200, 1400);

    const step = (timestamp: number) => {
      if (!startTimeRef.current) startTimeRef.current = timestamp;
      const elapsed = timestamp - startTimeRef.current;
      const currentProgress = Math.min(100, (elapsed / targetDuration) * 100);

      setProgress(currentProgress);

      if (currentProgress < 100) {
        rafRef.current = requestAnimationFrame(step);
      } else {
        // Hold at 100% for 250ms so user admires the fully filled logo, then smoothly fade out
        setTimeout(() => {
          setIsExiting(true);
          setTimeout(() => {
            hideLoader();
            setIsExiting(false);
            setProgress(0);
          }, 320);
        }, 250);
      }
    };

    rafRef.current = requestAnimationFrame(step);

    return () => {
      if (rafRef.current) cancelAnimationFrame(rafRef.current);
    };
  }, [isLoading, durationMs, hideLoader]);

  if (!isLoading) return null;

  return (
    <div
      onClick={hideLoader}
      className={`fixed inset-0 z-[99999] flex flex-col items-center justify-center backdrop-blur-xl bg-white/70 dark:bg-slate-950/60 select-none transition-all duration-300 ${
        isExiting ? 'opacity-0 scale-102 pointer-events-none' : 'opacity-100 scale-100'
      }`}
      role="status"
      aria-label="Đang tải trang"
    >
      {/* Main Glassmorphic Center Card */}
      <div className="relative z-10 flex flex-col items-center max-w-sm px-6 py-8 rounded-3xl bg-white/80 dark:bg-slate-900/80 backdrop-blur-2xl border border-white/80 dark:border-white/10 shadow-[0_20px_60px_-15px_rgba(0,0,0,0.15)] text-center">
        
        {/* =========================================================================
            LIQUID WAVE LOGO VESSEL (MASKS THE LIQUID TO THE DANASEA LOGO)
           ========================================================================= */}
        <div className="relative w-44 h-44 sm:w-52 sm:h-52 mb-5 flex items-center justify-center filter drop-shadow-[0_8px_20px_rgba(6,182,212,0.2)]">
          {/* Layer 1: Faint Logo Silhouette (Empty Glass Vessel Waiting to Fill) */}
          <img
            src="/images/danasea-logo.png"
            alt="DANASEA Logo Silhouette"
            className="absolute inset-0 w-full h-full object-contain opacity-20 filter contrast-125 select-none pointer-events-none"
          />

          {/* Layer 2: Liquid Water Wave (Clipped precisely to the DANASEA Logo geometry) */}
          <div
            className="absolute inset-0 w-full h-full select-none pointer-events-none overflow-hidden"
            style={{
              maskImage: 'url(/images/danasea-logo.png)',
              WebkitMaskImage: 'url(/images/danasea-logo.png)',
              maskSize: 'contain',
              WebkitMaskSize: 'contain',
              maskPosition: 'center',
              WebkitMaskPosition: 'center',
              maskRepeat: 'no-repeat',
              WebkitMaskRepeat: 'no-repeat',
            }}
          >
            {/* Rising Water Column with dramatic wave crests */}
            <div
              className="absolute left-0 right-0 bottom-0 bg-gradient-to-t from-cyan-700 via-teal-500 to-cyan-400 transition-[height] duration-75 ease-out"
              style={{ height: `${progress}%` }}
            >
              {/* Wave 1: Back Wave (Deep Teal, rolling right to left with big 45px amplitude) */}
              <div className="absolute -top-7 sm:-top-8 -left-full w-[300%] h-11 sm:h-12 overflow-hidden opacity-50 animate-liquid-wave-rev pointer-events-none">
                <svg viewBox="0 0 1200 120" preserveAspectRatio="none" className="w-full h-full fill-teal-400">
                  <path d="M 0,60 C 150,10 350,110 500,60 C 650,10 850,110 1000,60 C 1150,10 1350,110 1500,60 V 120 H 0 Z" />
                </svg>
              </div>

              {/* Wave 2: Middle Wave (Vibrant Ocean Cyan, rolling left to right) */}
              <div className="absolute -top-6 sm:-top-7 -left-full w-[300%] h-10 sm:h-11 overflow-hidden opacity-70 animate-liquid-wave-mid pointer-events-none">
                <svg viewBox="0 0 1200 120" preserveAspectRatio="none" className="w-full h-full fill-cyan-500">
                  <path d="M 0,60 C 150,110 350,10 500,60 C 650,110 850,10 1000,60 C 1150,110 1350,10 1500,60 V 120 H 0 Z" />
                </svg>
              </div>

              {/* Wave 3: Front Wave (Bright Cyan with White Foam Crest, fast rolling) */}
              <div className="absolute -top-5 sm:-top-6 -left-full w-[300%] h-9 sm:h-10 overflow-hidden opacity-95 animate-liquid-wave pointer-events-none">
                <svg viewBox="0 0 1200 120" preserveAspectRatio="none" className="w-full h-full fill-cyan-300">
                  <path d="M 0,60 C 150,15 350,105 500,60 C 650,15 850,105 1000,60 C 1150,15 1350,105 1500,60 V 120 H 0 Z" />
                </svg>
              </div>

              {/* Floating Sea Bubbles */}
              <div className="absolute inset-0 overflow-hidden pointer-events-none">
                <span className="absolute left-[15%] bottom-1 w-2 h-2 rounded-full bg-white/70 animate-liquid-bubble-1" />
                <span className="absolute left-[40%] bottom-1 w-1.5 h-1.5 rounded-full bg-white/80 animate-liquid-bubble-2" />
                <span className="absolute left-[65%] bottom-1 w-2.5 h-2.5 rounded-full bg-white/60 animate-liquid-bubble-3" />
                <span className="absolute left-[85%] bottom-1 w-1.5 h-1.5 rounded-full bg-white/85 animate-liquid-bubble-4" />
              </div>
            </div>

            {/* Layer 3: Vibrant Color Overlay (Revealed dynamically with the water waves) */}
            <img
              src="/images/danasea-logo.png"
              alt=""
              className="absolute inset-0 w-full h-full object-contain pointer-events-none opacity-90 mix-blend-color-dodge transition-opacity"
              style={{
                clipPath: `inset(${Math.max(0, 100 - progress - 4)}% 0 0 0)`,
                WebkitClipPath: `inset(${Math.max(0, 100 - progress - 4)}% 0 0 0)`,
              }}
            />
          </div>
        </div>

        {/* Percentage Counter */}
        <div className="mb-2">
          <span className="text-3xl sm:text-4xl font-mono font-black text-cyan-800 dark:text-cyan-300 tracking-wider drop-shadow-sm">
            {Math.round(progress)}%
          </span>
        </div>

        {/* Progress Pill Track */}
        <div className="w-48 sm:w-56 h-1.5 rounded-full bg-slate-200 dark:bg-white/15 overflow-hidden mb-3.5 p-0.5 border border-slate-300/40 dark:border-white/10">
          <div
            className="h-full rounded-full bg-gradient-to-r from-cyan-500 via-teal-400 to-cyan-400 transition-all duration-75 ease-out shadow-[0_0_8px_rgba(6,182,212,0.6)]"
            style={{ width: `${progress}%` }}
          />
        </div>

        {/* Main Message */}
        <h3 className="text-base sm:text-lg font-extrabold text-slate-900 dark:text-white tracking-wide mb-1">
          {message}
        </h3>

        {/* Subtitle / Slogan */}
        <p className="text-xs sm:text-sm text-slate-600 dark:text-cyan-200/80 font-medium leading-relaxed">
          {subMessage}
        </p>

        {/* Skip Tip */}
        <span className="mt-4 text-[10px] text-slate-400 dark:text-white/40 tracking-wider uppercase font-semibold hover:text-slate-600 dark:hover:text-white/70 transition-colors cursor-pointer">
          Chạm bất kỳ đâu để bỏ qua
        </span>
      </div>
    </div>
  );
}

export default PageLoader;
