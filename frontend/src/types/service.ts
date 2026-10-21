export interface ServiceSummaryDto {
  id: string;
  name: string;
  shortDescription?: string;
  price: number;
  promotionalPrice?: number | null;
  averageRating: number;
  reviewCount: number;
  viewCount: number;
  primaryImageUrl?: string;
  address?: string;
}

export interface ServicePageResponse {
  content: ServiceSummaryDto[];
  page: number;
  size: number;
  totalElements: number;
  total: number;
}
