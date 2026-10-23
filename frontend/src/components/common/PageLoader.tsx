import React, { useEffect, useState, useRef } from 'react';
import { useLoadingStore } from '../../store/useLoadingStore';

export function PageLoader() {
  const { isLoading, durationMs, hideLoader, showLoader } = useLoadingStore();
  const [progress, setProgress] = useState(0);
  const [isExiting, setIsExiting] = useState(false);
  const rafRef = useRef<number | null>(null);
  const startTimeRef = useRef<number | null>(null);

  // 1. Initial Splash Screen on first session visit
  useEffect(() => {
    const hasSeenSplash = sessionStorage.getItem('danasea_initial_splash_shown');
    if (!hasSeenSplash) {
      sessionStorage.setItem('danasea_initial_splash_shown', 'true');
      showLoader('', '', 2400);
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
      className={`fixed inset-0 z-[99999] flex items-center justify-center backdrop-blur-xl bg-slate-900/10 select-none cursor-pointer transition-all duration-300 ${
        isExiting ? 'opacity-0 scale-105 pointer-events-none' : 'opacity-100 scale-100'
      }`}
      role="status"
      aria-label="Đang tải trang"
    >
      {/* =========================================================================
          PURE LIQUID WAVE LOGO (FLOATING SEAMLESSLY OVER FROSTED GLASS BACKGROUND)
         ========================================================================= */}
      <div className="relative w-48 h-48 sm:w-60 sm:h-60 flex items-center justify-center filter drop-shadow-[0_12px_32px_rgba(0,0,0,0.18)]">
        {/* Layer 1: Faint Logo Silhouette (Empty Glass Vessel Waiting to Fill) */}
        <img
          src="/images/danasea-logo.png"
          alt="DANASEA Logo Silhouette"
          className="absolute inset-0 w-full h-full object-contain opacity-25 filter contrast-125 select-none pointer-events-none"
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
            {/* Wave 1: Back Wave (Deep Teal, rolling right to left with big amplitude) */}
            <div className="absolute -top-7 sm:-top-9 -left-full w-[300%] h-12 sm:h-14 overflow-hidden opacity-50 animate-liquid-wave-rev pointer-events-none">
              <svg viewBox="0 0 1200 120" preserveAspectRatio="none" className="w-full h-full fill-teal-400">
                <path d="M 0,60 C 150,10 350,110 500,60 C 650,10 850,110 1000,60 C 1150,10 1350,110 1500,60 V 120 H 0 Z" />
              </svg>
            </div>

            {/* Wave 2: Middle Wave (Vibrant Ocean Cyan, rolling left to right) */}
            <div className="absolute -top-6 sm:-top-8 -left-full w-[300%] h-11 sm:h-13 overflow-hidden opacity-70 animate-liquid-wave-mid pointer-events-none">
              <svg viewBox="0 0 1200 120" preserveAspectRatio="none" className="w-full h-full fill-cyan-500">
                <path d="M 0,60 C 150,110 350,10 500,60 C 650,110 850,10 1000,60 C 1150,110 1350,10 1500,60 V 120 H 0 Z" />
              </svg>
            </div>

            {/* Wave 3: Front Wave (Bright Cyan with White Foam Crest, fast rolling) */}
            <div className="absolute -top-5 sm:-top-7 -left-full w-[300%] h-10 sm:h-12 overflow-hidden opacity-95 animate-liquid-wave pointer-events-none">
              <svg viewBox="0 0 1200 120" preserveAspectRatio="none" className="w-full h-full fill-cyan-300">
                <path d="M 0,60 C 150,15 350,105 500,60 C 650,15 850,105 1000,60 C 1150,15 1350,105 1500,60 V 120 H 0 Z" />
              </svg>
            </div>

            {/* Floating Sea Bubbles */}
            <div className="absolute inset-0 overflow-hidden pointer-events-none">
              <span className="absolute left-[15%] bottom-1 w-2.5 h-2.5 rounded-full bg-white/70 animate-liquid-bubble-1" />
              <span className="absolute left-[40%] bottom-1 w-2 h-2 rounded-full bg-white/80 animate-liquid-bubble-2" />
              <span className="absolute left-[65%] bottom-1 w-3 h-3 rounded-full bg-white/60 animate-liquid-bubble-3" />
              <span className="absolute left-[85%] bottom-1 w-2 h-2 rounded-full bg-white/85 animate-liquid-bubble-4" />
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
    </div>
  );
}

export default PageLoader;
