# Stil Özeti (Mevcut Kod)

Tailwind v4, globals.css, theme.css ve kullanım özeti. Kaynak: [sellerx-frontend/app/globals.css](sellerx-frontend/app/globals.css), [sellerx-frontend/app/theme.css](sellerx-frontend/app/theme.css).

## Dosyalar

| Dosya | İçerik |
|-------|--------|
| app/globals.css | Tailwind import, tw-animate-css, dark variant, theme.css import; body tipografi; @theme inline (Tailwind v4 theme değişkenleri). |
| app/theme.css | Sellerboard renk sistemi: :root ve .dark CSS değişkenleri; --sb-primary, --sidebar-width, Shadcn değişkenleri (--background, --primary, --chart-1 vb.). |

## Tailwind v4

- **Import:** `@import "tailwindcss";` (globals.css).
- **Dark mode:** `@custom-variant dark (&:is(.dark *));` — `.dark` sınıfı ile dark tema.
- **Theme:** `@theme inline { ... }` ile --color-background, --color-primary vb. Tailwind renkleri CSS değişkenlerine bağlanır.

## theme.css Özeti

- **Sellerboard:** --sb-primary (#1D70F1), --sb-cta, --sb-danger, --sb-sidebar, --sidebar-width, --sidebar-collapsed-width, --header-height, --content-padding.
- **Shadcn eşlemesi:** --background, --foreground, --primary, --secondary, --muted, --accent, --destructive, --border, --ring, --chart-1..5, --sidebar*, --font-sans, --font-mono.
- **Dark:** .dark altında aynı değişkenler koyu tema değerleriyle override edilir.

## Kullanım

- Bileşenler: `className` ile Tailwind sınıfları; renkler için `bg-primary`, `text-muted-foreground` vb. (theme değişkenleri üzerinden).
- Layout: --sidebar-width, --header-height, --content-padding layout bileşenlerinde kullanılır.
- Charts: --chart-1 .. --chart-5 recharts vb. grafiklerde kullanılır.
