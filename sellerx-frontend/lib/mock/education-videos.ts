import { EducationVideo, VideoCategory } from "@/types/education";

export const mockEducationVideos: EducationVideo[] = [
  {
    id: "1",
    title: "SellerX'e Hoş Geldiniz",
    description: "SellerX platformunun temel özellikleri ve nasıl kullanacağınızı öğrenin.",
    category: "GETTING_STARTED",
    duration: "2:30",
    thumbnailUrl: "https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
    videoUrl: "https://www.youtube.com/embed/dQw4w9WgXcQ",
    videoType: "YOUTUBE",
    watched: false,
    isActive: true,
    order: 1,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: "2",
    title: "Trendyol Mağazası Nasıl Eklenir",
    description: "Trendyol API bilgilerinizi kullanarak mağazanızı SellerX'e bağlayın.",
    category: "GETTING_STARTED",
    duration: "5:45",
    thumbnailUrl: "https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
    videoUrl: "https://www.youtube.com/embed/dQw4w9WgXcQ",
    videoType: "YOUTUBE",
    watched: false,
    isActive: true,
    order: 2,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: "3",
    title: "Ürün Maliyeti Girme",
    description: "Ürünlerinize maliyet bilgisi ekleyerek kar/zarar analizinizi yapın.",
    category: "PRODUCTS",
    duration: "4:20",
    thumbnailUrl: "https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
    videoUrl: "https://www.youtube.com/embed/dQw4w9WgXcQ",
    videoType: "YOUTUBE",
    watched: false,
    isActive: true,
    order: 3,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: "4",
    title: "Stok Takibi Nasıl Yapılır",
    description: "Stok hareketlerinizi takip edin ve düşük stok uyarıları alın.",
    category: "PRODUCTS",
    duration: "3:15",
    thumbnailUrl: "https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
    videoUrl: "https://www.youtube.com/embed/dQw4w9WgXcQ",
    videoType: "YOUTUBE",
    watched: true,
    isActive: true,
    order: 4,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: "5",
    title: "Sipariş Yönetimi",
    description: "Siparişlerinizi görüntüleyin, durumlarını takip edin ve yönetin.",
    category: "ORDERS",
    duration: "6:00",
    thumbnailUrl: "https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
    videoUrl: "https://www.youtube.com/embed/dQw4w9WgXcQ",
    videoType: "YOUTUBE",
    watched: true,
    isActive: true,
    order: 5,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: "6",
    title: "Dashboard ve Analizler",
    description: "Satış verilerinizi analiz edin ve iş kararlarınızı veriye dayalı alın.",
    category: "ANALYTICS",
    duration: "7:30",
    thumbnailUrl: "https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
    videoUrl: "https://www.youtube.com/embed/dQw4w9WgXcQ",
    videoType: "YOUTUBE",
    watched: false,
    isActive: true,
    order: 6,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: "7",
    title: "Gider Yönetimi",
    description: "Sabit ve değişken giderlerinizi ekleyin, kar hesabınızı netleştirin.",
    category: "ANALYTICS",
    duration: "4:45",
    thumbnailUrl: "https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
    videoUrl: "https://www.youtube.com/embed/dQw4w9WgXcQ",
    videoType: "YOUTUBE",
    watched: false,
    isActive: true,
    order: 7,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: "8",
    title: "Mağaza Ayarları",
    description: "Mağaza bilgilerinizi güncelleyin ve webhook ayarlarını yapılandırın.",
    category: "SETTINGS",
    duration: "3:30",
    thumbnailUrl: "https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
    videoUrl: "https://www.youtube.com/embed/dQw4w9WgXcQ",
    videoType: "YOUTUBE",
    watched: true,
    isActive: true,
    order: 8,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
];

export function getUnwatchedCount(): number {
  return mockEducationVideos.filter((v) => !v.watched).length;
}

export function getRecentVideos(count: number = 3): EducationVideo[] {
  return mockEducationVideos
    .sort((a, b) => a.order - b.order)
    .slice(0, count);
}

export function getVideosByCategory(category: VideoCategory): EducationVideo[] {
  return mockEducationVideos
    .filter((v) => v.category === category)
    .sort((a, b) => a.order - b.order);
}

export function getAllVideos(): EducationVideo[] {
  return mockEducationVideos.sort((a, b) => a.order - b.order);
}

export const videoCategories: { id: VideoCategory | 'all'; labelKey: string }[] = [
  { id: 'all', labelKey: 'education.categories.all' },
  { id: 'GETTING_STARTED', labelKey: 'education.categories.getting-started' },
  { id: 'PRODUCTS', labelKey: 'education.categories.products' },
  { id: 'ORDERS', labelKey: 'education.categories.orders' },
  { id: 'ANALYTICS', labelKey: 'education.categories.analytics' },
  { id: 'SETTINGS', labelKey: 'education.categories.settings' },
];
