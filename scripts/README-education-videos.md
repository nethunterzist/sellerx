# Mock Eğitim Videoları Ekleme

Bu script SellerX platformu için mock eğitim videolarını backend'e ekler.

## Kullanım

### 1. Gereksinimler
- Backend'in çalışıyor olması gerekiyor
- Admin kullanıcısının access token'ına ihtiyaç var

### 2. Access Token Alma

Admin kullanıcısı ile giriş yapın ve browser'ın Developer Tools > Application > Cookies bölümünden `access_token` cookie'sini kopyalayın.

Veya API'den token alın:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"password"}'
```

### 3. Script Çalıştırma

#### Yöntem 1: Environment Variables ile
```bash
ACCESS_TOKEN="your-token-here" \
API_BASE_URL="http://localhost:8080" \
node scripts/add-mock-education-videos.js
```

#### Yöntem 2: .env dosyası ile
`.env` dosyasına ekleyin:
```
ACCESS_TOKEN=your-token-here
API_BASE_URL=http://localhost:8080
```

Sonra çalıştırın:
```bash
node scripts/add-mock-education-videos.js
```

### 4. Alternatif: Admin Panelden Manuel Ekleme

Script kullanmak istemiyorsanız, admin panelden manuel olarak ekleyebilirsiniz:

1. Admin paneline giriş yapın: `/admin/education`
2. "Video Ekle" butonuna tıklayın
3. Aşağıdaki videoları tek tek ekleyin:

## Eklenecek Videolar

1. **SellerX'e Hoş Geldiniz** - GETTING_STARTED - 3:00
2. **Trendyol Mağazası Nasıl Eklenir** - GETTING_STARTED - 5:30
3. **Ürün Maliyeti Girme** - PRODUCTS - 4:15
4. **Stok Takibi ve Senkronizasyon** - PRODUCTS - 6:00
5. **Sipariş Yönetimi ve Filtreleme** - ORDERS - 7:20
6. **Dashboard ve KPI Kartları** - ANALYTICS - 5:45
7. **Gider Yönetimi** - ANALYTICS - 4:30
8. **Mağaza Ayarları ve Webhook** - SETTINGS - 6:15
9. **Komisyon Hesaplamaları ve Takibi** - ANALYTICS - 5:20
10. **İade Yönetimi ve İade Takibi** - ORDERS - 4:45

## Video Formatı

Her video için gerekli bilgiler:
- **Başlık**: Video başlığı
- **Açıklama**: Video açıklaması
- **Kategori**: GETTING_STARTED, PRODUCTS, ORDERS, ANALYTICS, SETTINGS
- **Süre**: MM:SS formatında (örn: "5:30")
- **Video URL**: YouTube embed URL (örn: `https://www.youtube.com/embed/dQw4w9WgXcQ`)
- **Thumbnail URL**: YouTube thumbnail URL (opsiyonel)
- **Video Tipi**: YOUTUBE
- **Sıra**: 1-8 arası numara
- **Aktif**: true

## Notlar

- Script tüm videoları sırayla ekler
- Her video arasında 500ms bekleme yapılır (rate limiting için)
- Başarılı ve başarısız işlemler özetlenir
- Aynı videoyu tekrar eklemek hata verebilir (backend kontrol eder)
