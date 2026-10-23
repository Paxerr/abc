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
          {/* Rising Water Column with GIGANTIC SURGING & CHURNING WAVES (No flat cut!) */}
          <div
            className="absolute left-0 right-0 w-full h-[200%] pointer-events-none transition-[top] duration-75 ease-out"
            style={{
              top: `${100 - progress * 1.5}%`,
            }}
          >
            {/* Wave 1: Back Wave (Deep Sea Teal, rolling right-to-left with 110-unit amplitude) */}
            <div className="absolute top-0 left-0 w-[200%] h-full opacity-60 animate-liquid-wave-rev pointer-events-none">
              <svg viewBox="0 0 1200 500" preserveAspectRatio="none" className="w-full h-full">
                <defs>
                  <linearGradient id="danaseaWaveGrad1" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#0d9488" />
                    <stop offset="60%" stopColor="#08515a" />
                    <stop offset="100%" stopColor="#042f2e" />
                  </linearGradient>
                </defs>
                <path
                  d="M 0 70 Q 150 -40 300 70 T 600 70 Q 750 -40 900 70 T 1200 70 V 500 H 0 Z"
                  fill="url(#danaseaWaveGrad1)"
                />
              </svg>
            </div>

            {/* Wave 2: Middle Wave (Ocean Cyan, rolling left-to-right in counter-phase with 110-unit amplitude) */}
            <div className="absolute top-0 left-0 w-[200%] h-full opacity-75 animate-liquid-wave-mid pointer-events-none">
              <svg viewBox="0 0 1200 500" preserveAspectRatio="none" className="w-full h-full">
                <defs>
                  <linearGradient id="danaseaWaveGrad2" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#06b6d4" />
                    <stop offset="50%" stopColor="#0284c7" />
                    <stop offset="100%" stopColor="#08515a" />
                  </linearGradient>
                </defs>
                <path
                  d="M 0 70 Q 150 180 300 70 T 600 70 Q 750 180 900 70 T 1200 70 V 500 H 0 Z"
                  fill="url(#danaseaWaveGrad2)"
                />
              </svg>
            </div>

            {/* Wave 3: Front Wave (Vibrant Aqua with Glowing White Foam Crest & Spray, fast rolling) */}
            <div className="absolute top-0 left-0 w-[200%] h-full opacity-95 animate-liquid-wave pointer-events-none">
              <svg viewBox="0 0 1200 500" preserveAspectRatio="none" className="w-full h-full">
                <defs>
                  <linearGradient id="danaseaWaveGrad3" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#22d3ee" />
                    <stop offset="40%" stopColor="#0891b2" />
                    <stop offset="100%" stopColor="#0369a1" />
                  </linearGradient>
                </defs>
                {/* Aqua Water Body */}
                <path
                  d="M 0 70 Q 75 -30 150 70 T 300 70 Q 375 -30 450 70 T 600 70 Q 675 -30 750 70 T 900 70 Q 975 -30 1050 70 T 1200 70 V 500 H 0 Z"
                  fill="url(#danaseaWaveGrad3)"
                />
                {/* Soft Cyan Water Spray Glow */}
                <path
                  d="M 0 70 Q 75 -30 150 70 T 300 70 Q 375 -30 450 70 T 600 70 Q 675 -30 750 70 T 900 70 Q 975 -30 1050 70 T 1200 70"
                  fill="none"
                  stroke="#a5f3fc"
                  strokeWidth="12"
                  strokeLinecap="round"
                  opacity="0.45"
                />
                {/* Dazzling White Foam Crest along the wave peaks */}
                <path
                  d="M 0 70 Q 75 -30 150 70 T 300 70 Q 375 -30 450 70 T 600 70 Q 675 -30 750 70 T 900 70 Q 975 -30 1050 70 T 1200 70"
                  fill="none"
                  stroke="white"
                  strokeWidth="6"
                  strokeLinecap="round"
                  opacity="0.95"
                />
              </svg>
            </div>

            {/* Rising Sea Bubbles */}
            <div className="absolute inset-0 overflow-hidden pointer-events-none">
              <span className="absolute left-[18%] top-[25%] w-3 h-3 rounded-full bg-white/80 animate-liquid-bubble-1 filter drop-shadow-[0_0_4px_rgba(255,255,255,0.8)]" />
              <span className="absolute left-[38%] top-[35%] w-2 h-2 rounded-full bg-white/90 animate-liquid-bubble-2 filter drop-shadow-[0_0_4px_rgba(255,255,255,0.8)]" />
              <span className="absolute left-[62%] top-[28%] w-3.5 h-3.5 rounded-full bg-white/70 animate-liquid-bubble-3 filter drop-shadow-[0_0_4px_rgba(255,255,255,0.8)]" />
              <span className="absolute left-[82%] top-[40%] w-2.5 h-2.5 rounded-full bg-white/85 animate-liquid-bubble-4 filter drop-shadow-[0_0_4px_rgba(255,255,255,0.8)]" />
            </div>
          </div>
        </div>

        {/* Layer 3: Authentic Brand Logo detail - cleanly reveals as liquid reaches top */}
        <img
          src="/images/danasea-logo.png"
          alt="DANASEA"
          className="absolute inset-0 w-full h-full object-contain select-none pointer-events-none transition-opacity duration-300"
          style={{
            opacity: progress >= 95 ? 1 : Math.max(0, (progress - 25) / 75) * 0.35,
          }}
        />
      </div>
    </div>
  );
}

export default PageLoader;
