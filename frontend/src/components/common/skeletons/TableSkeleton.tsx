import React from 'react';

interface TableSkeletonProps {
  rows?: number;
  cols?: number;
  className?: string;
}

export const TableSkeleton: React.FC<TableSkeletonProps> = ({
  rows = 5,
  cols = 5,
  className = '',
}) => {
  return (
    <div className={`w-full overflow-hidden rounded-2xl border border-slate-200/80 dark:border-slate-800 bg-white dark:bg-slate-900 animate-pulse ${className}`}>
      {/* Table Header Skeleton */}
      <div className="bg-slate-50 dark:bg-slate-800/60 px-4 py-3 border-b border-slate-200/80 dark:border-slate-800 flex items-center justify-between gap-4">
        {Array.from({ length: cols }).map((_, colIdx) => (
          <div
            key={`th-skel-${colIdx}`}
            className="h-3.5 bg-slate-200 dark:bg-slate-700 rounded-md flex-1 max-w-[140px]"
          />
        ))}
      </div>

      {/* Table Rows Skeleton */}
      <div className="divide-y divide-slate-100 dark:divide-slate-800/80">
        {Array.from({ length: rows }).map((_, rowIdx) => (
          <div
            key={`tr-skel-${rowIdx}`}
            className="px-4 py-3.5 flex items-center justify-between gap-4"
          >
            {Array.from({ length: cols }).map((_, colIdx) => (
              <div
                key={`td-skel-${rowIdx}-${colIdx}`}
                className={`h-4 bg-slate-200/80 dark:bg-slate-800 rounded flex-1 ${
                  colIdx === 0
                    ? 'max-w-[160px]'
                    : colIdx === cols - 1
                    ? 'max-w-[80px]'
                    : 'max-w-[120px]'
                }`}
              />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
};

export default TableSkeleton;
