import { apiClient } from './apiClient';
import type { CategoryDto } from '../types/category';

let cachedCategories: CategoryDto[] | null = null;
let cacheTimestamp = 0;
const CACHE_TTL_MS = 10 * 60 * 1000; // 10 minutes

export const categoryService = {
  /**
   * Fetch active sea sports categories from Backend
   */
  async getCategories(forceRefresh: boolean = false): Promise<CategoryDto[]> {
    const now = Date.now();
    if (!forceRefresh && cachedCategories && now - cacheTimestamp < CACHE_TTL_MS) {
      console.log('[categoryService] Trả về danh mục từ in-memory cache:', cachedCategories.length, 'danh mục');
      return cachedCategories;
    }

    console.groupCollapsed('%c[Category API] Lấy danh mục thể thao biển...', 'color: #00646f; font-weight: bold;');
    console.log('[Bước 1/3] Gửi yêu cầu GET /api/categories...');

    try {
      const response = await apiClient.get<CategoryDto[]>('/categories');
      const data = response.data || [];
      console.log('[Bước 2/3] Nhận danh mục thành công từ Backend:', data.length, 'danh mục');
      
      cachedCategories = data;
      cacheTimestamp = now;
      console.log('[Bước 3/3] Cache danh mục thành công (TTL: 10 phút)');
      console.groupEnd();
      return data;
    } catch (err) {
      console.groupEnd();
      console.error('[categoryService] Lỗi khi nạp danh mục:', err);
      // Fallback to empty array or cached data
      return cachedCategories || [];
    }
  },
};
