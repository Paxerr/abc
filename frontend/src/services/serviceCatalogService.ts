import { apiClient } from './apiClient';
import type { ServicePageResponse, ServiceSummaryDto } from '../types/service';

export interface ServiceQueryParams {
  page?: number;
  size?: number;
  categoryId?: string;
  keyword?: string;
  sortBy?: 'price_asc' | 'price_desc' | 'rating_desc' | 'views_desc';
  minPrice?: number;
  maxPrice?: number;
}

export const serviceCatalogService = {
  /**
   * Fetch paginated published services from Backend
   */
  async getServices(params: ServiceQueryParams = {}): Promise<ServicePageResponse> {
    const query = {
      page: params.page ?? 0,
      size: params.size ?? 12,
      ...(params.categoryId ? { categoryId: params.categoryId } : {}),
      ...(params.keyword ? { keyword: params.keyword } : {}),
      ...(params.sortBy ? { sortBy: params.sortBy } : {}),
      ...(params.minPrice !== undefined ? { minPrice: params.minPrice } : {}),
      ...(params.maxPrice !== undefined ? { maxPrice: params.maxPrice } : {}),
    };

    console.groupCollapsed('%c[Services API] Lấy danh sách trải nghiệm biển...', 'color: #00646f; font-weight: bold;');
    console.log('[Bước 1/3] Gửi yêu cầu GET /api/services với bộ lọc:', query);

    try {
      const response = await apiClient.get<ServicePageResponse>('/services', { params: query });
      const data = response.data;
      console.log(`[Bước 2/3] Nhận dữ liệu thành công: ${data.totalElements} dịch vụ (Trang ${data.page + 1})`, data.content);
      console.log('[Bước 3/3] Xác thực và nạp dữ liệu trải nghiệm biển thành công');
      console.groupEnd();
      return data;
    } catch (err) {
      console.groupEnd();
      console.warn('[serviceCatalogService] Không thể tải dữ liệu dịch vụ từ Backend, kích hoạt fallback:', err);
      return {
        content: [],
        page: 0,
        size: query.size,
        totalElements: 0,
        total: 0,
      };
    }
  },
};
