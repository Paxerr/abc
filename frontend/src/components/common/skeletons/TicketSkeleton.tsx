import React from 'react';

interface TicketSkeletonProps {
  count?: number;
  className?: string;
}

export const TicketSkeleton: React.FC<TicketSkeletonProps> = ({
  count = 2,
  className = '',
}) => {
  return (
    <div className={`space-y-4 ${className}`}>
      {Array.from({ length: count }).map((_, idx) => (
        <div
          key={`ticket-skel-${idx}`}
          className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 animate-pulse flex flex-col md:flex-row items-center justify-between gap-6"
        >
          {/* Ticket Information */}
          <div className="flex-1 space-y-3 w-full">
            <div className="flex items-center gap-2">
              <div className="w-20 h-5 rounded-full bg-slate-200 dark:bg-slate-800" />
              <div className="w-28 h-3.5 rounded bg-slate-200 dark:bg-slate-800" />
            </div>
            <div className="w-3/4 h-5 rounded bg-slate-200 dark:bg-slate-800" />
            <div className="flex items-center gap-4">
              <div className="w-32 h-4 rounded bg-slate-200 dark:bg-slate-800" />
              <div className="w-24 h-4 rounded bg-slate-200 dark:bg-slate-800" />
            </div>
          </div>

          {/* QR Code Placeholder Box */}
          <div className="w-28 h-28 rounded-xl bg-slate-200 dark:bg-slate-800 shrink-0 flex items-center justify-center">
            <div className="w-16 h-16 rounded-lg bg-slate-300 dark:bg-slate-700" />
          </div>
        </div>
      ))}
    </div>
  );
};

export default TicketSkeleton;
