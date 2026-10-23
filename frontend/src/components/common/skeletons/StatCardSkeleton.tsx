import React from 'react';

interface StatCardSkeletonProps {
  count?: number;
  className?: string;
}

export const StatCardSkeleton: React.FC<StatCardSkeletonProps> = ({
  count = 4,
  className = '',
}) => {
  return (
    <div className={`grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6 ${className}`}>
      {Array.from({ length: count }).map((_, idx) => (
        <div
          key={`stat-skel-${idx}`}
          className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 animate-pulse flex flex-col justify-between"
        >
          <div className="flex items-center justify-between mb-4">
            <div className="w-24 h-3.5 bg-slate-200 dark:bg-slate-800 rounded" />
            <div className="w-9 h-9 rounded-xl bg-slate-200 dark:bg-slate-800" />
          </div>
          <div>
            <div className="w-32 h-7 bg-slate-200 dark:bg-slate-800 rounded mb-2" />
            <div className="w-20 h-3 bg-slate-200 dark:bg-slate-800 rounded" />
          </div>
        </div>
      ))}
    </div>
  );
};

export default StatCardSkeleton;
