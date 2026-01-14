## Getting Started

## 1 Github ve zip

- githubdan projeyi clone'la

- zipteki env dosyalarını projenin ana dizinine yükle

#

## 2

### Node yoksa

- https://nodejs.org/en/download

- buradan indir

### terminalde komutu çalıştır (uzun sürebilir)

- npm install

#

## 3 Docker Desktop yoksa

- Docker Desktop indir kur uygulamayı aç

### terminalde komutu çalıştır 1

- docker compose up -d

### terminalde komutu çalıştır 2

- npx drizzle-kit push

#

## Sunucuyu başlatmak için

- npm run dev

### Not

- Bu development modunda çalıştırır. Her sayfa yenilendiğinde frontend, backend dataları yeniden yüklenir, cache revalidasyonu için önemli

#

## Notlar ve kullanılan paketler

- proje genel olarak react, nextjs, typescript, tailwind, postgres, drizzle orm, zod ile geliştirildi

### 💾 Veri Tabanı ve ORM

- **drizzle-orm**: Tip güvenli modern SQL ORM.
- **pg**: PostgreSQL istemcisi.
- **@auth/pg-adapter / @auth/drizzle-adapter**: Auth.js için PostgreSQL ve Drizzle uyumluluk adaptörleri.

### 🔐 Kimlik Doğrulama

- **next-auth**: Next.js için oturum yönetimi ve kimlik doğrulama kütüphanesi.
- **jsonwebtoken**: JWT tabanlı kimlik doğrulama için token üretimi ve doğrulama.

### 📅 Tarih ve Saat

- **date-fns / @date-fns/tz**: Tarih işlemleri ve zaman dilimi desteği.

### 🧾 Formlar ve Validasyon

- **react-hook-form**: Performanslı form yönetimi.
- **@hookform/resolvers**: `zod` gibi şema doğrulayıcılar için entegrasyon.
- **zod**: Tip güvenli veri validasyonu.

### 🧩 UI Bileşenleri ve Araçlar

- **@radix-ui/react-\***: UI bileşenleri. aslında shadcn kullanıyoruz biz, https://v4.shadcn.com/, https://ui.shadcn.com/docs/installation bu linklerden tailwind 4 için uygun ui bileşenlerini ekleyip düzenleyebilirsin.
- **lucide-react**: Modern ve özelleştirilebilir ikon seti.
- **react-icons**: Popüler ikon kütüphanelerini barındıran bir bileşen. sadece bunu kullansak yeter lucide-react'i de barındırıyor diye biliyorum
- **tw-animate-css**: Tailwind ile CSS animasyonları eklemek için yardımcı.
- **next-themes**: Tema (örneğin dark/light mode) yönetimi.
- **next-intl**: Çok dilli uygulamalar için i18n desteği.
- **clsx / class-variance-authority / tailwind-merge**: Dinamik className yönetimi ve Tailwind ile uyumlu class birleştirme.

### 🛠️ Yardımcı Araçlar

- **swr**: React için veri alma ve önbellekleme kütüphanesi. Genel api call'larını buradan yapmaya çalışalım (TanStack veya React 19 ile use() ile de yapılabilir ama şu anda bunun ile başladım)
- **nodemailer**: E-posta gönderimi
- **npm-check-updates**: Paket güncellemelerini kontrol etmek için CLI aracı.

### 🧪 Tipler ve Geliştirici Deneyimi (DevDependencies)

- **@types/**: TypeScript desteği olmayan kütüphaneler için tip tanımlamaları.
- **prettier-plugin-tailwindcss**: Tailwind class'larını otomatik sıralamak için Prettier eklentisi. vscode'dan indirebilirsin, projede konfigurasyonu yapılmış olması lazım. eğer çalışmazsa yardımcı olurum

## Proje geliştirirken

- typescript kullandığımız için tip belirlemeliyiz, tipleri "any" olarak belirtmemek daha iyi. (build time error'e karşı)
- type, interface ve zod validator gibi tip, tür belirlemelerini oluştururken /types, /lib/validators içindeki dosyalar incelenmeli benzeri veya aynısı varsa onun üzerinden gidilmeli.

## Commit öncesi

- npm run build komutu ile build alınmalı ve build time hataları ve linter hataları çözülmeli
