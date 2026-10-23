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
      className={`fixed inset-0 z-[99999] flex items-center justify-center backdrop-blur-md bg-white/[0.02] dark:bg-black/10 select-none cursor-pointer transition-all duration-300 ${
        isExiting ? 'opacity-0 scale-105 pointer-events-none' : 'opacity-100 scale-100'
      }`}
      role="status"
      aria-label="Đang tải trang"
    >
      {/* =========================================================================
          PURE LIQUID WAVE LOGO (FLOATING SEAMLESSLY OVER FROSTED GLASS BACKGROUND)
         ========================================================================= */}
      <div className="relative w-56 h-56 sm:w-64 sm:h-64 flex items-center justify-center filter drop-shadow-[0_16px_36px_rgba(0,0,0,0.18)]">
        {/* Layer 1: Faint Logo Silhouette (Empty Glass Vessel Waiting to Fill) */}
        <img
          src="/images/danasea-logo.png"
          alt="DANASEA Logo Silhouette"
          className="absolute inset-0 w-full h-full object-contain opacity-20 filter contrast-125 select-none pointer-events-none"
        />

        {/* Layer 2: Liquid Water Waves (Clipped precisely to the DANASEA Logo geometry) */}
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
          {/* Rising Water Column with DRAMATIC SURGING & CHURNING WAVES */}
          <div
            className="absolute left-0 right-0 bottom-0 pointer-events-none transition-[height] duration-75 ease-out"
            style={{
              height: `${Math.min(108, progress * 1.08)}%`,
              opacity: progress > 0.5 ? 1 : 0,
            }}
          >
            {/* Solid Water Body beneath waves (Starts below wave troughs to eliminate ANY flat horizontal line) */}
            <div className="absolute inset-x-0 bottom-0 top-6 sm:top-8 bg-gradient-to-t from-cyan-950 via-teal-700 to-cyan-600" />

            {/* Wave 1: Back Wave (Deep Teal, rolling right to left with massive amplitude) */}
            <div className="absolute -top-12 sm:-top-16 left-0 w-[200%] h-20 sm:h-24 overflow-visible opacity-65 animate-liquid-wave-rev pointer-events-none">
              <svg viewBox="0 0 1000 100" preserveAspectRatio="none" className="w-full h-full fill-teal-700">
                <path d="M 0 50 Q 125 -10 250 50 T 500 50 Q 625 -10 750 50 T 1000 50 V 100 H 0 Z" />
              </svg>
            </div>

            {/* Wave 2: Middle Wave (Ocean Cyan, rolling left to right in counter-phase) */}
            <div className="absolute -top-10 sm:-top-14 left-0 w-[200%] h-18 sm:h-22 overflow-visible opacity-80 animate-liquid-wave-mid pointer-events-none">
              <svg viewBox="0 0 1000 100" preserveAspectRatio="none" className="w-full h-full fill-cyan-600">
                <path d="M 0 50 Q 125 110 250 50 T 500 50 Q 625 110 750 50 T 1000 50 V 100 H 0 Z" />
              </svg>
            </div>

            {/* Wave 3: Front Wave (Bright Aqua with Dazzling White Foam Crest, fast rolling) */}
            <div className="absolute -top-9 sm:-top-12 left-0 w-[200%] h-16 sm:h-20 overflow-visible opacity-95 animate-liquid-wave pointer-events-none">
              <svg viewBox="0 0 1000 100" preserveAspectRatio="none" className="w-full h-full fill-cyan-400">
                <path d="M 0 50 Q 62.5 -5 125 50 T 250 50 Q 312.5 -5 375 50 T 500 50 Q 562.5 -5 625 50 T 750 50 Q 812.5 -5 875 50 T 1000 50 V 100 H 0 Z" />
                {/* Soft Cyan Water Spray Glow */}
                <path
                  d="M 0 50 Q 62.5 -5 125 50 T 250 50 Q 312.5 -5 375 50 T 500 50 Q 562.5 -5 625 50 T 750 50 Q 812.5 -5 875 50 T 1000 50"
                  fill="none"
                  stroke="#a5f3fc"
                  strokeWidth="8"
                  strokeLinecap="round"
                  opacity="0.5"
                />
                {/* Crisp White Foam Crest */}
                <path
                  d="M 0 50 Q 62.5 -5 125 50 T 250 50 Q 312.5 -5 375 50 T 500 50 Q 562.5 -5 625 50 T 750 50 Q 812.5 -5 875 50 T 1000 50"
                  fill="none"
                  stroke="#ffffff"
                  strokeWidth="4"
                  strokeLinecap="round"
                  opacity="0.95"
                />
              </svg>
            </div>

            {/* Rising Sea Bubbles */}
            <div className="absolute inset-0 overflow-hidden pointer-events-none">
              <span className="absolute left-[18%] bottom-2 w-3 h-3 rounded-full bg-white/80 animate-liquid-bubble-1 filter drop-shadow-[0_0_4px_rgba(255,255,255,0.8)]" />
              <span className="absolute left-[38%] bottom-3 w-2 h-2 rounded-full bg-white/90 animate-liquid-bubble-2 filter drop-shadow-[0_0_4px_rgba(255,255,255,0.8)]" />
              <span className="absolute left-[62%] bottom-2 w-3.5 h-3.5 rounded-full bg-white/70 animate-liquid-bubble-3 filter drop-shadow-[0_0_4px_rgba(255,255,255,0.8)]" />
              <span className="absolute left-[82%] bottom-4 w-2.5 h-2.5 rounded-full bg-white/85 animate-liquid-bubble-4 filter drop-shadow-[0_0_4px_rgba(255,255,255,0.8)]" />
            </div>
          </div>
        </div>

        {/* Layer 3: Authentic Brand Logo detail - seamlessly revealed as water fills */}
        <img
          src="/images/danasea-logo.png"
          alt="DANASEA"
          className="absolute inset-0 w-full h-full object-contain select-none pointer-events-none transition-opacity duration-300"
          style={{
            opacity: progress >= 95 ? 1 : Math.max(0, (progress - 20) / 80) * 0.45,
          }}
        />
      </div>
    </div>
  );
}

export default PageLoader;
