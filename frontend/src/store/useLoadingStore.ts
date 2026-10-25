import { create } from 'zustand';

interface LoadingState {
  isLoading: boolean;
  message: string;
  subMessage: string;
  durationMs: number;
  showLoader: (message?: string, subMessage?: string, durationMs?: number) => void;
  hideLoader: () => void;
  triggerDemoLoading: (durationMs?: number, message?: string, subMessage?: string) => void;
}

export const useLoadingStore = create<LoadingState>((set) => ({
  isLoading: false,
  message: 'Đang kết nối sóng biển DANASEA...',
  subMessage: 'Chuẩn bị hải trình và cập nhật dữ liệu trải nghiệm',
  durationMs: 2200,
  showLoader: (
    message = 'Đang kết nối sóng biển DANASEA...',
    subMessage = 'Chuẩn bị hải trình và cập nhật dữ liệu trải nghiệm',
    durationMs = 2200
  ) => set({ isLoading: true, message, subMessage, durationMs }),
  hideLoader: () => set({ isLoading: false }),
  triggerDemoLoading: (
    durationMs = 2400,
    message = 'Đang kết nối sóng biển DANASEA...',
    subMessage = 'Chuẩn bị hải trình và cập nhật dữ liệu trải nghiệm'
  ) => {
    set({ isLoading: true, message, subMessage, durationMs });
  },
}));
