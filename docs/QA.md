# Müşteri Soruları (Q&A) Modülü

## Genel Bakış

Bu modül, Trendyol'daki müşteri sorularını ve satıcı cevaplarını SellerX platformu üzerinden görüntüleme ve yönetme imkanı sağlar.

**Önemli Not:** Cevap gönderme özelliği şu an **devre dışıdır**. Bu özellik ileride AI otomatik cevaplama sistemi ile birlikte aktif edilecektir. Manuel cevap yazma ve Trendyol'a gönderme işlemleri güvenlik nedeniyle engellenmektedir.

---

## Özellikler

### Mevcut Özellikler
- Trendyol'dan müşteri sorularını senkronize etme
- **Satıcı cevaplarını da otomatik çekme**
- Soruları listeleme ve filtreleme (Tümü / Bekleyen / Yanıtlanan)
- Soru istatistikleri (Toplam, Bekleyen, Yanıtlanan)
- Ürün bilgisi ile bağlantılı soru görüntüleme
- Sayfalandırma desteği
- Türkçe/İngilizce çoklu dil desteği

### Gelecek Özellikler (AI Entegrasyonu)
- AI ile otomatik cevap oluşturma
- Cevapları Trendyol'a gönderme
- Cevap şablonları
- Cevap süresi takibi (12 saat kuralı)

---

## Trendyol API Entegrasyonu

### Doğru Endpoint

> **ÖNEMLİ:** Trendyol Q&A API'si için doğru endpoint aşağıdaki gibidir. Eski endpoint'ler (`/sapigw/integration/suppliers/...`) artık çalışmamaktadır.

```
GET https://apigw.trendyol.com/integration/qna/sellers/{sellerId}/questions/filter
```

### Parametreler

| Parametre | Tip | Zorunlu | Açıklama |
|-----------|-----|---------|----------|
| `supplierId` | long | ✅ Evet | Satıcı ID |
| `page` | int | Hayır | Sayfa numarası (0'dan başlar) |
| `size` | int | Hayır | Sayfa başı kayıt (max 50) |
| `startDate` | long | ✅ Evet | Başlangıç tarihi (ms timestamp) |
| `endDate` | long | ✅ Evet | Bitiş tarihi (ms timestamp) |
| `status` | string | Hayır | Filtre: WAITING_FOR_ANSWER, ANSWERED, vb. |
| `barcode` | long | Hayır | Ürün barkodu ile filtre |
| `orderByField` | string | Hayır | LastModifiedDate veya CreatedDate |
| `orderByDirection` | string | Hayır | ASC veya DESC |

### Kısıtlamalar

- **Max tarih aralığı:** 2 hafta (14 gün)
- **Max sayfa boyutu:** 50
- **Base URL:** `https://apigw.trendyol.com` (api.trendyol.com değil!)

### Status Değerleri

| Trendyol Status | Açıklama | Internal Status |
|-----------------|----------|-----------------|
| `WAITING_FOR_ANSWER` | Cevap bekliyor | PENDING |
| `WAITING_FOR_APPROVE` | Onay bekliyor | ANSWERED |
| `ANSWERED` | Cevaplandı | ANSWERED |
| `REPORTED` | Raporlandı | REJECTED |
| `REJECTED` | Reddedildi | REJECTED |

### API Response Yapısı

```json
{
  "content": [
    {
      "id": 380907206,
      "text": "Bu ürün ne zaman kargoya verilir?",
      "customerId": 12345,
      "userName": "M***",
      "productMainId": "1234567",
      "productName": "Ürün Adı",
      "status": "ANSWERED",
      "creationDate": 1705312800000,
      "public": true,
      "imageUrl": "https://...",
      "webUrl": "https://...",
      "answer": {
        "id": 98765,
        "text": "Merhabalar, siparişiniz 1-2 iş günü içinde kargoya verilecektir.",
        "creationDate": 1705316400000
      }
    }
  ],
  "page": 0,
  "size": 50,
  "totalElements": 370,
  "totalPages": 8
}
```

### Örnek API Çağrısı

```bash
# Son 2 haftanın sorularını çek
START_DATE=$(($(date +%s) * 1000 - 14 * 24 * 60 * 60 * 1000))
END_DATE=$(($(date +%s) * 1000))

curl -X GET "https://apigw.trendyol.com/integration/qna/sellers/1080066/questions/filter?supplierId=1080066&page=0&size=50&startDate=$START_DATE&endDate=$END_DATE" \
  -H "Authorization: Basic $(echo -n 'API_KEY:API_SECRET' | base64)" \
  -H "User-Agent: 1080066 - SelfIntegration" \
  -H "Content-Type: application/json"
```

---

## Sync Akışı

### Nasıl Çalışır?

1. **Tarih Aralığı Hesaplama:**
   - `endDate`: Şu anki zaman (ms)
   - `startDate`: 14 gün öncesi (ms)
   - API max 2 hafta desteklediği için bu aralık kullanılır

2. **Sayfalı Çekme:**
   - Her sayfada max 50 soru çekilir
   - `totalPages` değerine göre tüm sayfalar gezilir

3. **Soru İşleme:**
   - Yeni soru → Veritabanına kaydedilir
   - Mevcut soru → Status ve diğer alanlar güncellenir

4. **Cevap İşleme:**
   - API response'unda `answer` objesi varsa
   - Cevap metni ve Trendyol answer ID kaydedilir
   - Duplicate kontrolü yapılır (trendyol_answer_id ile)

### Kod Akışı

```java
// TrendyolQaService.java - syncQuestions()

// 1. Tarih aralığı hesapla (son 14 gün)
long endDate = System.currentTimeMillis();
long startDate = endDate - (14L * 24 * 60 * 60 * 1000);

// 2. API'ye istek at
String url = String.format(
    "%s/integration/qna/sellers/%s/questions/filter?supplierId=%s&page=%d&size=%d&startDate=%d&endDate=%d",
    TRENDYOL_QA_BASE_URL, sellerId, sellerId, page, size, startDate, endDate
);

// 3. Response'u işle
for (JsonNode questionNode : content) {
    // Soru kaydet/güncelle
    TrendyolQuestion question = createOrUpdateQuestion(questionNode);

    // Cevap varsa kaydet
    saveAnswerFromJson(question, questionNode);
}
```

### Status Mapping

```java
private String mapTrendyolStatus(String trendyolStatus) {
    return switch (trendyolStatus) {
        case "ANSWERED", "WAITING_FOR_APPROVE" -> "ANSWERED";
        case "REJECTED", "REPORTED" -> "REJECTED";
        default -> "PENDING"; // WAITING_FOR_ANSWER ve diğerleri
    };
}
```

---

## Yaşanan Sorun ve Çözüm

### Problem

Q&A sync işlemi **556 Service Unavailable** hatası veriyordu.

### Sebep

**Yanlış endpoint** kullanılıyordu:

```
❌ YANLIŞ: /sapigw/integration/suppliers/{sellerId}/questions
✅ DOĞRU:  /integration/qna/sellers/{sellerId}/questions/filter
```

Ayrıca:
- `api.trendyol.com` domain'i Cloudflare tarafından bloklanmış
- Doğru domain: `apigw.trendyol.com`
- `startDate` ve `endDate` parametreleri zorunlu (eski endpoint'te yoktu)

### Çözüm

1. **Base URL değiştirildi:**
   ```java
   // Eski
   private static final String BASE_URL = "https://api.trendyol.com";

   // Yeni
   private static final String BASE_URL = "https://apigw.trendyol.com";
   ```

2. **Endpoint path değiştirildi:**
   ```java
   // Eski
   /sapigw/integration/suppliers/{sellerId}/questions

   // Yeni
   /integration/qna/sellers/{sellerId}/questions/filter
   ```

3. **Tarih parametreleri eklendi:**
   ```java
   long endDate = System.currentTimeMillis();
   long startDate = endDate - (14L * 24 * 60 * 60 * 1000);
   ```

4. **JSON field mapping güncellendi:**
   ```java
   // Eski
   .productId(node.path("productId").asText(null))

   // Yeni
   .productId(node.path("productMainId").asText(null))
   ```

---

## Veritabanı Şeması

### Migration Dosyası
`sellerx-backend/src/main/resources/db/migration/V24__create_qa_tables.sql`

### Tablolar

#### trendyol_questions
```sql
CREATE TABLE trendyol_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    question_id VARCHAR(255) NOT NULL,       -- Trendyol question ID
    product_id VARCHAR(255),                 -- productMainId from API
    barcode VARCHAR(100),
    product_title VARCHAR(500),
    customer_question TEXT NOT NULL,
    question_date TIMESTAMP NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',    -- PENDING, ANSWERED, REJECTED
    is_public BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(store_id, question_id)
);
```

#### trendyol_answers
```sql
CREATE TABLE trendyol_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES trendyol_questions(id) ON DELETE CASCADE,
    answer_text TEXT NOT NULL,
    is_submitted BOOLEAN DEFAULT false,      -- true = Trendyol'dan çekildi
    trendyol_answer_id VARCHAR(255),         -- Trendyol'dan gelen answer ID
    submitted_at TIMESTAMP,
    submitted_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW()
);
```

### İndeksler
```sql
CREATE INDEX idx_questions_store_status ON trendyol_questions(store_id, status);
CREATE INDEX idx_questions_store_date ON trendyol_questions(store_id, question_date DESC);
CREATE INDEX idx_questions_product ON trendyol_questions(product_id);
CREATE INDEX idx_answers_question ON trendyol_answers(question_id);
CREATE INDEX idx_answers_trendyol_id ON trendyol_answers(trendyol_answer_id);
```

---

## Backend Yapısı

### Paket Yapısı
```
sellerx-backend/src/main/java/com/ecommerce/sellerx/qa/
├── TrendyolQuestion.java           # Entity
├── TrendyolAnswer.java             # Entity
├── TrendyolQuestionRepository.java # Repository
├── TrendyolAnswerRepository.java   # Repository (+ findByTrendyolAnswerId)
├── TrendyolQaService.java          # Trendyol API entegrasyonu
├── QaController.java               # REST API
└── dto/
    ├── QuestionDto.java
    ├── AnswerDto.java
    ├── QaStatsDto.java
    ├── SubmitAnswerRequest.java
    └── QaSyncResponse.java
```

### Önemli Metodlar (TrendyolQaService.java)

| Metod | Açıklama |
|-------|----------|
| `syncQuestions(storeId)` | Trendyol'dan soruları ve cevapları çeker |
| `saveAnswerFromJson(question, node)` | API response'undan cevabı parse edip kaydeder |
| `mapTrendyolStatus(status)` | Trendyol status'unu internal status'a çevirir |
| `createQuestionFromJson(store, node)` | JSON'dan Question entity oluşturur |

### REST API Endpoint'leri

| Method | Endpoint | Açıklama |
|--------|----------|----------|
| GET | `/api/qa/stores/{storeId}/questions` | Soruları listele (paginated) |
| GET | `/api/qa/questions/{questionId}` | Tek soru detayı (cevaplarla birlikte) |
| POST | `/api/qa/stores/{storeId}/questions/sync` | Trendyol'dan senkronize et |
| GET | `/api/qa/stores/{storeId}/stats` | İstatistikler |

---

## Frontend Yapısı

### Dosya Yapısı
```
sellerx-frontend/
├── app/[locale]/(app-shell)/qa/
│   └── page.tsx                          # Ana dashboard sayfası
├── app/api/qa/
│   ├── stores/[storeId]/
│   │   ├── questions/
│   │   │   ├── route.ts                  # GET questions (paginated)
│   │   │   └── sync/route.ts             # POST sync
│   │   └── stats/route.ts                # GET stats
│   └── questions/[questionId]/
│       └── route.ts                      # GET single question
├── components/qa/
│   ├── qa-stats-cards.tsx                # İstatistik kartları
│   └── question-list.tsx                 # Soru listesi (cevaplarla)
├── hooks/queries/
│   └── use-qa.ts                         # React Query hooks
├── types/
│   └── qa.ts                             # TypeScript tipleri
└── messages/
    ├── tr.json                           # Türkçe çeviriler (qa section)
    └── en.json                           # İngilizce çeviriler (qa section)
```

---

## Kullanım Kılavuzu

### Soruları Görüntüleme

1. Sidebar'dan "Müşteri Soruları" menüsüne tıklayın
2. Sayfada mağazanıza ait sorular listelenecek
3. Her sorunun altında **satıcı cevabı** da görüntülenir
4. "Bekleyen", "Yanıtlanan" veya "Tümü" sekmeleri ile filtreleme yapabilirsiniz

### Senkronizasyon

1. "Senkronize Et" butonuna tıklayın
2. Son 2 haftanın soruları ve cevapları Trendyol'dan çekilecek
3. Sonuç mesajında kaç yeni ve güncellenen soru olduğu gösterilir

### İstatistikler

Sayfa üstündeki kartlarda şu bilgiler gösterilir:
- **Toplam Soru**: Tüm soruların sayısı
- **Bekleyen**: Henüz yanıtlanmamış sorular
- **Yanıtlanan**: Yanıtlanmış sorular

---

## Test Etme

### Backend Test
```bash
# Login
TOKEN=$(curl -s -X POST "http://localhost:8080/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"123456"}' | jq -r '.token')

# Q&A Sync
curl -X POST "http://localhost:8080/qa/stores/{STORE_ID}/questions/sync" \
  -H "Cookie: access_token=$TOKEN"

# Beklenen: {"totalFetched":370,"newQuestions":370,"updatedQuestions":0,"message":"Sync completed successfully"}
```

### Veritabanı Kontrolü
```bash
# Soru sayısı
psql -d sellerx_db -c "SELECT COUNT(*) FROM trendyol_questions WHERE store_id = '{STORE_ID}';"

# Cevap sayısı
psql -d sellerx_db -c "SELECT COUNT(*) FROM trendyol_answers WHERE question_id IN (SELECT id FROM trendyol_questions WHERE store_id = '{STORE_ID}');"

# Örnek soru-cevap
psql -d sellerx_db -c "
SELECT q.product_title, LEFT(q.customer_question, 40) as soru, LEFT(a.answer_text, 50) as cevap
FROM trendyol_questions q
JOIN trendyol_answers a ON a.question_id = q.id
LIMIT 5;
"
```

---

## Güvenlik Önlemleri

### Cevap Gönderme Devre Dışı

1. **QaController**: Manuel cevap endpoint'i kaldırıldı
2. **TrendyolQaService.submitAnswerToTrendyol()**: Gerçek API çağrısı yapılmıyor, sadece log yazıyor
3. **Frontend**: Cevap yazma UI'ı yok, sadece görüntüleme var

### Neden?
- Test Trendyol hesabına yanlışlıkla cevap gönderilmesini önlemek
- AI otomatik cevaplama sistemi hazır olana kadar beklemek
- Kullanıcı hatalarını önlemek

---

## Gelecek Geliştirmeler

### Faz 2: AI Otomatik Cevaplama
- [ ] LLM entegrasyonu (OpenAI/Anthropic)
- [ ] Ürün bilgisine göre cevap üretme
- [ ] Cevap kalitesi kontrolü
- [ ] Trendyol'a otomatik gönderim

### Faz 3: Gelişmiş Özellikler
- [ ] Cevap şablonları
- [ ] Toplu cevaplama
- [ ] Cevap süresi uyarıları (12 saat kuralı)
- [ ] Ürün bazlı soru istatistikleri
- [ ] Soru arama ve filtreleme
- [ ] 2 haftadan eski soruları çekmek için tarih aralığı seçimi

---

## İlgili Kaynaklar

- [Trendyol Q&A Integration Docs](https://developers.trendyol.com/docs/marketplace/soru-cevap-entegrasyonu/musteri-sorularini-cekme)
- [Trendyol Müşteri Soruları Kuralları](https://akademi.trendyol.com/satici-bilgi-merkezi/temel-kavramlar/platform-kurallari/trendyol-musteri-sorulari-ozelligi-kurallari)

---

## Değişiklik Geçmişi

| Tarih | Değişiklik |
|-------|------------|
| 2026-01-18 | API endpoint düzeltildi: `/sapigw/...` → `/integration/qna/...` |
| 2026-01-18 | Cevapların otomatik çekilmesi eklendi |
| 2026-01-18 | Status mapping eklendi (Trendyol → Internal) |
| 2026-01-18 | Base URL düzeltildi: `api.trendyol.com` → `apigw.trendyol.com` |
