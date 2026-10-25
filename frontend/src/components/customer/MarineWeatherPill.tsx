import React, { useEffect, useState } from 'react';
import { useLanguageStore } from '../../store/useLanguageStore';
import { weatherService } from '../../services/weatherService';
import type { WeatherInfoDto, MarineSafetySummary } from '../../types/weather';

interface MarineWeatherPillProps {
  className?: string;
}

export const MarineWeatherPill: React.FC<MarineWeatherPillProps> = ({ className = '' }) => {
  const { language } = useLanguageStore();
  const [data, setData] = useState<WeatherInfoDto | null>(null);
  const [safety, setSafety] = useState<MarineSafetySummary | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [hasError, setHasError] = useState<boolean>(false);

  useEffect(() => {
    let isMounted = true;

    const fetchWeather = async () => {
      try {
        setIsLoading(true);
        setHasError(false);
        const result = await weatherService.getCurrentWeather();
        if (isMounted) {
          setData(result);
          setSafety(weatherService.evaluateSafety(result));
        }
      } catch (err) {
        if (isMounted) {
          console.warn('[MarineWeatherPill] Không thể nạp dữ liệu thời tiết biển, ẩn an toàn:', err);
          setHasError(true);
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    };

    fetchWeather();

    return () => {
      isMounted = false;
    };
  }, []);

  // Gracefully hide if error occurs to avoid breaking layout
  if (hasError) {
    return null;
  }

  // Loading Skeleton Pill
  if (isLoading || !data) {
    return (
      <div
        className={`inline-flex items-center gap-3 px-3.5 py-1.5 sm:px-4 sm:py-2 rounded-full bg-white/70 dark:bg-slate-900/70 backdrop-blur-md border border-white/40 dark:border-slate-800 shadow-sm animate-pulse ${className}`}
      >
        <div className="w-2.5 h-2.5 rounded-full bg-slate-300 dark:bg-slate-600" />
        <span className="w-px h-3 bg-slate-200 dark:bg-slate-700" />
        <div className="w-10 h-3 rounded bg-slate-300 dark:bg-slate-700" />
        <span className="w-px h-3 bg-slate-200 dark:bg-slate-700" />
        <div className="w-10 h-3 rounded bg-slate-300 dark:bg-slate-700" />
        <span className="w-px h-3 bg-slate-200 dark:bg-slate-700" />
        <div className="w-12 h-3 rounded bg-slate-300 dark:bg-slate-700" />
      </div>
    );
  }

  const temp = Math.round(data.weather?.temperature ?? 0);
  const wave = (data.marine?.waveHeight ?? 0).toFixed(1);
  const wind = (data.weather?.windSpeed ?? 0).toFixed(1);

  // Status dot styling based on safety level
  const statusColor =
    safety?.level === 'RED'
      ? 'bg-rose-500'
      : safety?.level === 'YELLOW'
      ? 'bg-amber-500'
      : 'bg-emerald-500';

  const statusLabel = language === 'en' ? safety?.statusLabelEn : safety?.statusLabel;

  return (
    <div
      className={`inline-flex items-center flex-wrap sm:flex-nowrap justify-center gap-2.5 sm:gap-3 px-3.5 sm:px-4 py-1.5 sm:py-2 rounded-full bg-white/90 dark:bg-slate-900/80 backdrop-blur-md border border-white/60 dark:border-slate-700/60 shadow-[0_4px_20px_rgba(0,0,0,0.12)] hover:-translate-y-0.5 hover:shadow-[0_8px_24px_rgba(0,0,0,0.16)] text-xs sm:text-[13px] text-slate-800 dark:text-slate-100 transition-all duration-300 select-none cursor-default ${className}`}
    >
      {/* 1. Status Indicator Dot (Color pulse only, no text) */}
      <span
        title={statusLabel}
        className={`w-2.5 h-2.5 rounded-full ${statusColor} animate-pulse shrink-0`}
      />

      <span className="w-px h-3 bg-slate-200 dark:bg-slate-700" />

      {/* 2. Temperature (Thermometer Icon) */}
      <div className="flex items-center gap-1 text-slate-700 dark:text-slate-300 font-medium">
        <span className="material-symbols-outlined text-[16px] text-amber-500">thermostat</span>
        <span>{temp}°C</span>
      </div>

      <span className="w-px h-3 bg-slate-200 dark:bg-slate-700" />

      {/* 3. Wave Height (Waves Icon) */}
      <div className="flex items-center gap-1 text-slate-700 dark:text-slate-300 font-medium">
        <span className="material-symbols-outlined text-[16px] text-cyan-500">waves</span>
        <span>{wave} m</span>
      </div>

      <span className="w-px h-3 bg-slate-200 dark:bg-slate-700" />

      {/* 4. Wind Speed (Air Icon) */}
      <div className="flex items-center gap-1 text-slate-700 dark:text-slate-300 font-medium">
        <span className="material-symbols-outlined text-[16px] text-teal-500">air</span>
        <span>{wind} km/h</span>
      </div>
    </div>
  );
};
