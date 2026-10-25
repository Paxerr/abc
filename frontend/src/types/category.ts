export interface CategoryDto {
  id: string;
  name: string;
  nameEn: string;
  slug: string;
  parentId?: string | null;
  iconUrl?: string;
  isActive?: boolean;
  children?: CategoryDto[];
}
