import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface WishlistState {
  wishlistIds: string[];
  toastMessage: string | null;
  toggleWishlist: (serviceId: string, serviceName?: string) => boolean;
  isInWishlist: (serviceId: string) => boolean;
  removeFromWishlist: (serviceId: string, serviceName?: string) => void;
  clearToast: () => void;
}

export const useWishlistStore = create<WishlistState>()(
  persist(
    (set, get) => ({
      // Initially seed with first 3 popular tours so the wishlist is not completely empty on first load
      wishlistIds: ['serv-01', 'serv-02', 'serv-03'],
      toastMessage: null,

      isInWishlist: (serviceId: string) => {
        return get().wishlistIds.includes(serviceId);
      },

      toggleWishlist: (serviceId: string, serviceName?: string) => {
        const current = get().wishlistIds;
        const exists = current.includes(serviceId);
        if (exists) {
          set({
            wishlistIds: current.filter((id) => id !== serviceId),
            toastMessage: "Đã bỏ lưu khỏi yêu thích!",
          });
          return false;
        } else {
          set({
            wishlistIds: [...current, serviceId],
            toastMessage: "Đã lưu vào yêu thích!",
          });
          return true;
        }
      },

      removeFromWishlist: (serviceId: string, serviceName?: string) => {
        set({
          wishlistIds: get().wishlistIds.filter((id) => id !== serviceId),
          toastMessage: "Đã bỏ lưu khỏi yêu thích!",
        });
      },

      clearToast: () => set({ toastMessage: null }),
    }),
    {
      name: 'danasea_wishlist_storage',
    }
  )
);
