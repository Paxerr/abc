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
        2200
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

    const targetDuration = Math.max(durationMs || 2000, 1200);

    const step = (timestamp: number) => {
      if (!startTimeRef.current) startTimeRef.current = timestamp;
      const elapsed = timestamp - startTimeRef.current;
      const currentProgress = Math.min(100, (elapsed / targetDuration) * 100);

      setProgress(currentProgress);

      if (currentProgress < 100) {
        rafRef.current = requestAnimationFrame(step);
      } else {
        // Hold at 100% for 200ms so user admires the fully filled logo, then smoothly fade out
        setTimeout(() => {
          setIsExiting(true);
          setTimeout(() => {
            hideLoader();
            setIsExiting(false);
            setProgress(0);
          }, 320);
        }, 220);
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
      className={`fixed inset-0 z-[99999] flex flex-col items-center justify-center bg-slate-950/85 backdrop-blur-2xl select-none transition-all duration-300 ${
        isExiting ? 'opacity-0 scale-102 pointer-events-none' : 'opacity-100 scale-100'
      }`}
      role="status"
      aria-label="Đang tải trang"
    >
      {/* Background Ambient Ocean Glow */}
      <div className="absolute w-72 h-72 sm:w-96 sm:h-96 rounded-full bg-gradient-to-tr from-cyan-500/25 via-teal-400/20 to-sky-500/10 blur-3xl pointer-events-none animate-pulse" />

      {/* Main Container */}
      <div className="relative z-10 flex flex-col items-center max-w-sm px-6 text-center">
        {/* =========================================================================
            LIQUID WAVE LOGO VESSEL (MASKS THE LIQUID TO THE DANASEA LOGO)
           ========================================================================= */}
        <div className="relative w-40 h-40 sm:w-48 sm:h-48 mb-6 flex items-center justify-center">
          {/* Subtle Outer Halo Ring */}
          <div className="absolute -inset-3 rounded-full border border-cyan-400/20 shadow-[0_0_30px_rgba(6,182,212,0.15)] animate-pulse pointer-events-none" />

          {/* Layer 1: Faint Logo Outline/Silhouette (Empty Vessel Waiting to Fill) */}
          <img
            src="/images/danasea-logo.png"
            alt="DANASEA Logo Silhouette"
            className="absolute inset-0 w-full h-full object-contain opacity-25 filter brightness-150 contrast-125 select-none pointer-events-none"
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
            {/* Rising Water Column */}
            <div
              className="absolute left-0 right-0 bottom-0 bg-gradient-to-t from-cyan-700 via-teal-500 to-cyan-400 transition-[height] duration-75 ease-out"
              style={{ height: `${progress}%` }}
            >
              {/* Wave 1: Back wave (translucent teal) */}
              <div className="absolute -top-3 sm:-top-4 -left-full w-[300%] h-5 sm:h-6 overflow-hidden opacity-40 animate-liquid-wave-rev pointer-events-none">
                <svg viewBox="0 0 1200 120" preserveAspectRatio="none" className="w-full h-full fill-teal-200">
                  <path d="M 0,60 C 150,20 350,100 500,60 C 650,20 850,100 1000,60 C 1150,20 1350,100 1500,60 V 120 H 0 Z" />
                </svg>
              </div>

              {/* Wave 2: Front wave (cyan with foam crest) */}
              <div className="absolute -top-3 sm:-top-4 -left-full w-[300%] h-5 sm:h-6 overflow-hidden opacity-90 animate-liquid-wave pointer-events-none">
                <svg viewBox="0 0 1200 120" preserveAspectRatio="none" className="w-full h-full fill-cyan-300">
                  <path d="M 0,60 C 150,100 350,20 500,60 C 650,100 850,20 1000,60 C 1150,100 1350,20 1500,60 V 120 H 0 Z" />
                </svg>
              </div>

              {/* Floating Sea Bubbles */}
              <div className="absolute inset-0 overflow-hidden pointer-events-none">
                <span className="absolute left-[20%] bottom-1 w-2 h-2 rounded-full bg-white/70 animate-liquid-bubble-1" />
                <span className="absolute left-[45%] bottom-1 w-1.5 h-1.5 rounded-full bg-white/60 animate-liquid-bubble-2" />
                <span className="absolute left-[70%] bottom-1 w-2.5 h-2.5 rounded-full bg-white/50 animate-liquid-bubble-3" />
                <span className="absolute left-[85%] bottom-1 w-1 h-1 rounded-full bg-white/80 animate-liquid-bubble-4" />
              </div>
            </div>

            {/* Layer 3: Vibrant Color Overlay (Shimmers as water fills up) */}
            <img
              src="/images/danasea-logo.png"
              alt=""
              className="absolute inset-0 w-full h-full object-contain pointer-events-none opacity-90 mix-blend-color-dodge transition-opacity"
              style={{
                clipPath: `inset(${Math.max(0, 100 - progress)}% 0 0 0)`,
                WebkitClipPath: `inset(${Math.max(0, 100 - progress)}% 0 0 0)`,
              }}
            />
          </div>
        </div>

        {/* Percentage Counter */}
        <div className="mb-3">
          <span className="text-2xl sm:text-3xl font-mono font-black text-transparent bg-clip-text bg-gradient-to-r from-cyan-300 via-teal-200 to-cyan-400 drop-shadow-[0_2px_12px_rgba(6,182,212,0.5)]">
            {Math.round(progress)}%
          </span>
        </div>

        {/* Progress Pill Track */}
        <div className="w-48 sm:w-56 h-1.5 rounded-full bg-white/15 overflow-hidden mb-4 p-0.5 border border-white/10">
          <div
            className="h-full rounded-full bg-gradient-to-r from-cyan-400 to-teal-300 transition-all duration-75 ease-out shadow-[0_0_10px_rgba(6,182,212,0.8)]"
            style={{ width: `${progress}%` }}
          />
        </div>

        {/* Main Message */}
        <h3 className="text-base sm:text-lg font-extrabold text-white tracking-wide mb-1 drop-shadow-md">
          {message}
        </h3>

        {/* Subtitle / Nautical Slogan */}
        <p className="text-xs sm:text-sm text-cyan-200/80 font-medium leading-relaxed drop-shadow-sm">
          {subMessage}
        </p>

        {/* Skip Tip */}
        <span className="mt-5 text-[10px] text-white/40 tracking-wider uppercase font-semibold hover:text-white/70 transition-colors cursor-pointer">
          Chạm bất kỳ đâu để bỏ qua
        </span>
      </div>
    </div>
  );
}

export default PageLoader;
