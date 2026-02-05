# Sprint 4: Frontend Sayfa ve Feature Haritası

Sprint 4'ün odak alanı: Mevcut koda göre Next.js sayfa listesi ve feature–sayfa eşlemesinin dokümantasyonu. **Sadece doc yazılır; kod değiştirilmez.**

## Dosya Listesi

| Dosya | İçerik |
|-------|--------|
| [01-page-list.md](01-page-list.md) | Tüm sayfalar: path (locale sonrası), layout grubu, auth, dosya yolu. |
| [02-feature-map.md](02-feature-map.md) | Feature → sayfalar; kısa açıklama; ilgili hook (opsiyonel). |
| [03-route-structure.md](03-route-structure.md) | [locale], layout grupları, dinamik segmentler özeti. |

## Kapsam

- **Kaynak:** `sellerx-frontend/app/[locale]/**/page.tsx`, layout grupları; `hooks/queries/` referansı.
- **Güncelleme:** Yeni sayfa veya feature eklenince ilgili doc güncellenir; kod değişikliği yapılmaz.

## Nasıl Kullanılır

- Yeni sayfa eklenince → 01-page-list.md ve 02-feature-map.md (ilgili feature) güncellenir; gerekirse 03-route-structure.md.
- Yeni layout grubu veya dinamik segment eklenince → 03-route-structure.md güncellenir.
