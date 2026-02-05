/**
 * Mock Eğitim Videoları Ekleme Scripti
 * 
 * Bu script SellerX platformu için mock eğitim videolarını backend'e ekler.
 * 
 * Kullanım:
 * 1. Backend'in çalıştığından emin olun
 * 2. Admin kullanıcısının access token'ını alın
 * 3. API_BASE_URL ve ACCESS_TOKEN değişkenlerini ayarlayın
 * 4. node scripts/add-mock-education-videos.js komutunu çalıştırın
 */

const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8080';
const ACCESS_TOKEN = process.env.ACCESS_TOKEN || ''; // Admin kullanıcısının token'ı

const mockVideos = [
  {
    title: "SellerX'e Hoş Geldiniz",
    description: "SellerX platformunun temel özellikleri ve nasıl kullanacağınızı öğrenin. Dashboard, menü yapısı ve genel kullanım hakkında bilgi edinin.",
    category: "GETTING_STARTED",
    duration: "3:00",
    videoUrl: "https://www.youtube.com/embed/dQw4w9WgXcQ",
    thumbnailUrl: "https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
    videoType: "YOUTUBE",
    order: 1,
    isActive: true
  },
  {
    title: "Trendyol Mağazası Nasıl Eklenir",
    description: "Trendyol API bilgilerinizi kullanarak mağazanızı SellerX'e bağlayın. API key ve supplier ID'nizi nasıl bulacağınızı ve sisteme nasıl ekleyeceğinizi öğrenin.",
    category: "GETTING_STARTED",
    duration: "5:30",
    videoUrl: "https://www.youtube.com/embed/dQw4w9WgXcQ",
    thumbnailUrl: "https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
    videoType: "YOUTUBE",
    order: 2,
    isActive: true
  },
  {
    title: "Ürün Maliyeti Girme",
    description: "Ürünlerinize maliyet bilgisi ekleyerek kar/zarar analizinizi yapın. Toplu maliyet girişi ve maliyet güncelleme işlemlerini öğrenin.",
    category: "PRODUCTS",
    duration: "4:15",
    videoUrl: "https://www.youtube.com/embed/dQw4w9WgXcQ",
    thumbnailUrl: "https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
    videoType: "YOUTUBE",
    order: 3,
    isActive: true
  },
  {
    title: "Stok Takibi ve Senkronizasyon",
    description: "Stok hareketlerinizi takip edin ve düşük stok uyarıları alın. Otomatik stok senkronizasyonu nasıl çalışır ve manuel senkronizasyon nasıl yapılır.",
    category: "PRODUCTS",
    duration: "6:00",
    videoUrl: "https://www.youtube.com/embed/dQw4w9WgXcQ",
    thumbnailUrl: "https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
    videoType: "YOUTUBE",
    order: 4,
    isActive: true
  },
  {
    title: "Sipariş Yönetimi ve Filtreleme",
    description: "Siparişlerinizi görüntüleyin, durumlarını takip edin ve yönetin. Sipariş filtreleme, arama ve detaylı sipariş bilgilerini inceleme.",
    category: "ORDERS",
    duration: "7:20",
    videoUrl: "https://www.youtube.com/embed/dQw4w9WgXcQ",
    thumbnailUrl: "https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
    videoType: "YOUTUBE",
    order: 5,
    isActive: true
  },
  {
    title: "Dashboard ve KPI Kartları",
    description: "Satış verilerinizi analiz edin ve iş kararlarınızı veriye dayalı alın. Dashboard'daki KPI kartları, grafikler ve trend analizlerini öğrenin.",
    category: "ANALYTICS",
    duration: "5:45",
    videoUrl: "https://www.youtube.com/embed/dQw4w9WgXcQ",
    thumbnailUrl: "https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
    videoType: "YOUTUBE",
    order: 6,
    isActive: true
  },
  {
    title: "Gider Yönetimi",
    description: "Sabit ve değişken giderlerinizi ekleyin, kar hesabınızı netleştirin. Gider kategorileri, tekrarlayan giderler ve gider raporlarını öğrenin.",
    category: "ANALYTICS",
    duration: "4:30",
    videoUrl: "https://www.youtube.com/embed/dQw4w9WgXcQ",
    thumbnailUrl: "https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
    videoType: "YOUTUBE",
    order: 7,
    isActive: true
  },
  {
    title: "Mağaza Ayarları ve Webhook",
    description: "Mağaza bilgilerinizi güncelleyin ve webhook ayarlarını yapılandırın. Webhook URL'leri, güvenlik ayarları ve API entegrasyonlarını öğrenin.",
    category: "SETTINGS",
    duration: "6:15",
    videoUrl: "https://www.youtube.com/embed/dQw4w9WgXcQ",
    thumbnailUrl: "https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
    videoType: "YOUTUBE",
    order: 8,
    isActive: true
  },
  {
    title: "Komisyon Hesaplamaları ve Takibi",
    description: "Trendyol komisyon oranlarınızı takip edin ve komisyon hesaplamalarınızı yapın. Komisyon raporları, komisyon detayları ve komisyon analizlerini öğrenin.",
    category: "ANALYTICS",
    duration: "5:20",
    videoUrl: "https://www.youtube.com/embed/dQw4w9WgXcQ",
    thumbnailUrl: "https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
    videoType: "YOUTUBE",
    order: 9,
    isActive: true
  },
  {
    title: "İade Yönetimi ve İade Takibi",
    description: "İade edilen siparişlerinizi yönetin ve takip edin. İade durumları, iade sebepleri, iade süreçleri ve iade raporlarını öğrenin.",
    category: "ORDERS",
    duration: "4:45",
    videoUrl: "https://www.youtube.com/embed/dQw4w9WgXcQ",
    thumbnailUrl: "https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
    videoType: "YOUTUBE",
    order: 10,
    isActive: true
  }
];

async function addVideo(video) {
  try {
    const response = await fetch(`${API_BASE_URL}/api/education/videos`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${ACCESS_TOKEN}`
      },
      body: JSON.stringify(video)
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(`HTTP ${response.status}: ${JSON.stringify(errorData)}`);
    }

    const data = await response.json();
    return data;
  } catch (error) {
    console.error(`Video eklenirken hata: ${video.title}`, error.message);
    throw error;
  }
}

async function addAllVideos() {
  if (!ACCESS_TOKEN) {
    console.error('HATA: ACCESS_TOKEN ayarlanmamış!');
    console.log('\nKullanım:');
    console.log('  ACCESS_TOKEN="your-token" API_BASE_URL="http://localhost:8080" node scripts/add-mock-education-videos.js');
    console.log('\nveya .env dosyasına ekleyin:');
    console.log('  ACCESS_TOKEN=your-token');
    console.log('  API_BASE_URL=http://localhost:8080');
    process.exit(1);
  }

  console.log(`\n${mockVideos.length} adet mock video ekleniyor...\n`);
  console.log(`API Base URL: ${API_BASE_URL}\n`);

  const results = {
    success: [],
    failed: []
  };

  for (let i = 0; i < mockVideos.length; i++) {
    const video = mockVideos[i];
    console.log(`[${i + 1}/${mockVideos.length}] "${video.title}" ekleniyor...`);
    
    try {
      const result = await addVideo(video);
      results.success.push({ title: video.title, id: result.id });
      console.log(`  ✓ Başarıyla eklendi (ID: ${result.id})\n`);
    } catch (error) {
      results.failed.push({ title: video.title, error: error.message });
      console.log(`  ✗ Hata: ${error.message}\n`);
    }

    // Rate limiting için kısa bir bekleme
    if (i < mockVideos.length - 1) {
      await new Promise(resolve => setTimeout(resolve, 500));
    }
  }

  // Özet
  console.log('\n' + '='.repeat(50));
  console.log('ÖZET');
  console.log('='.repeat(50));
  console.log(`Başarılı: ${results.success.length}`);
  console.log(`Başarısız: ${results.failed.length}`);
  
  if (results.failed.length > 0) {
    console.log('\nBaşarısız videolar:');
    results.failed.forEach(item => {
      console.log(`  - ${item.title}: ${item.error}`);
    });
  }
  
  if (results.success.length > 0) {
    console.log('\nBaşarıyla eklenen videolar:');
    results.success.forEach(item => {
      console.log(`  - ${item.title} (ID: ${item.id})`);
    });
  }
  console.log('\n');
}

// Script çalıştır
addAllVideos().catch(error => {
  console.error('Beklenmeyen hata:', error);
  process.exit(1);
});
