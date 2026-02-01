import { Notification } from "@/types/notification";

export const mockNotifications: Notification[] = [
  {
    id: "1",
    type: "ORDER_UPDATE",
    title: "Yeni Sipariş",
    message: "Sipariş #TY-789456 alındı. 3 ürün, toplam ₺1.250",
    read: false,
    link: "/orders",
    createdAt: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
  },
  {
    id: "2",
    type: "STOCK_ALERT",
    title: "Düşük Stok Uyarısı",
    message: "\"Bluetooth Kulaklık\" ürününde stok 5'in altına düştü",
    read: false,
    link: "/products",
    createdAt: new Date(Date.now() - 15 * 60 * 1000).toISOString(),
  },
  {
    id: "3",
    type: "SUCCESS",
    title: "Senkronizasyon Tamamlandı",
    message: "Trendyol ürünleri başarıyla senkronize edildi. 156 ürün güncellendi",
    read: false,
    link: "/products",
    createdAt: new Date(Date.now() - 32 * 60 * 1000).toISOString(),
  },
  {
    id: "4",
    type: "SYSTEM",
    title: "Fiyat Değişikliği",
    message: "Rakip fiyat değişikliği tespit edildi: \"Kablosuz Mouse\" ürününde",
    read: true,
    link: "/products",
    createdAt: new Date(Date.now() - 60 * 60 * 1000).toISOString(),
  },
  {
    id: "5",
    type: "ORDER_UPDATE",
    title: "Sipariş Kargolandı",
    message: "Sipariş #TY-789123 kargoya verildi. Takip no: 1234567890",
    read: true,
    link: "/orders",
    createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
  },
  {
    id: "6",
    type: "WARNING",
    title: "İade Talebi",
    message: "Sipariş #TY-788999 için iade talebi oluşturuldu",
    read: true,
    link: "/orders",
    createdAt: new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString(),
  },
  {
    id: "7",
    type: "SYSTEM",
    title: "Sistem Güncellemesi",
    message: "Yeni özellikler eklendi: Gelişmiş raporlama ve analitik araçları",
    read: true,
    createdAt: new Date(Date.now() - 5 * 60 * 60 * 1000).toISOString(),
  },
  {
    id: "8",
    type: "STOCK_ALERT",
    title: "Stok Güncellendi",
    message: "\"USB-C Kablo\" ürününe 100 adet stok eklendi",
    read: true,
    link: "/products",
    createdAt: new Date(Date.now() - 6 * 60 * 60 * 1000).toISOString(),
  },
  {
    id: "9",
    type: "SUCCESS",
    title: "Haftalık Rapor Hazır",
    message: "Bu haftanın satış raporu hazırlandı. Toplam ciro: ₺45.780",
    read: true,
    link: "/analytics",
    createdAt: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
  },
  {
    id: "10",
    type: "ORDER_UPDATE",
    title: "Toplu Sipariş",
    message: "5 yeni sipariş alındı. Toplam değer: ₺3.450",
    read: true,
    link: "/orders",
    createdAt: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
  },
];

export function getUnreadCount(): number {
  return mockNotifications.filter((n) => !n.read).length;
}

export function getRecentNotifications(count: number = 5): Notification[] {
  return mockNotifications.slice(0, count);
}
