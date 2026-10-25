import React from 'react';

interface TourCardSkeletonProps {
  count?: number;
  className?: string;
}

export const TourCardSkeleton: React.FC<TourCardSkeletonProps> = ({
  count = 1,
  className = '',
}) => {
  return (
    <>
      {Array.from({ length: count }).map((_, idx) => (
        <div
          key={`tour-skel-${idx}`}
          className={`bg-white dark:bg-slate-900 border border-slate-200/90 dark:border-slate-800 rounded-2xl overflow-hidden p-3 animate-pulse shadow-xs flex flex-col ${className}`}
        >
          {/* Card Media (Image skeleton with 4:3 aspect ratio) */}
          <div className="relative aspect-[4/3] rounded-xl bg-slate-200 dark:bg-slate-800 mb-3 overflow-hidden">
            <div className="absolute top-2.5 left-2.5 w-16 h-5 rounded-full bg-slate-300 dark:bg-slate-700" />
            <div className="absolute top-2.5 right-2.5 w-7 h-7 rounded-full bg-slate-300 dark:bg-slate-700" />
          </div>

          {/* Location & Category Pill */}
          <div className="flex items-center gap-2 mb-2">
            <div className="w-14 h-4 rounded-full bg-slate-200 dark:bg-slate-800" />
            <div className="w-20 h-4 rounded-full bg-slate-200 dark:bg-slate-800" />
          </div>

          {/* Service Title */}
          <div className="h-4 bg-slate-200 dark:bg-slate-800 rounded w-4/5 mb-1.5" />
          <div className="h-4 bg-slate-200 dark:bg-slate-800 rounded w-3/5 mb-3" />

          {/* Price & Rating Footer */}
          <div className="mt-auto pt-2 border-t border-slate-100 dark:border-slate-800/80 flex items-center justify-between">
            <div className="h-5 bg-slate-200 dark:bg-slate-800 rounded w-24" />
            <div className="w-10 h-4 bg-slate-200 dark:bg-slate-800 rounded" />
          </div>
        </div>
      ))}
    </>
  );
};

export default TourCardSkeleton;
