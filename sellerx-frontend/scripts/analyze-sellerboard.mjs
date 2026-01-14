import { chromium } from 'playwright';
import fs from 'fs';

async function analyzeSellerboard() {
  console.log('🚀 Tarayıcı açılıyor...');

  const browser = await chromium.launch({
    headless: false, // Görünür tarayıcı
    slowMo: 1000 // Yavaşlatılmış (izlemek için)
  });

  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 }
  });

  const page = await context.newPage();

  console.log('📍 Demo sayfasına gidiliyor...');
  await page.goto('https://app.sellerboard.com/en/auth/demolinkConfirm', {
    waitUntil: 'networkidle',
    timeout: 60000
  });

  // Dashboard yüklenene kadar bekle
  console.log('⏳ Dashboard yükleniyor...');
  await page.waitForTimeout(10000); // 10 saniye bekle

  // Screenshot al
  console.log('📸 Screenshot alınıyor...');
  await page.screenshot({
    path: 'sellerboard-dashboard.png',
    fullPage: true
  });

  // CSS değişkenlerini çıkar
  console.log('🎨 Stil bilgileri çıkarılıyor...');
  const styles = await page.evaluate(() => {
    const computedStyle = getComputedStyle(document.documentElement);
    const body = getComputedStyle(document.body);

    return {
      // Body styles
      bodyBg: body.backgroundColor,
      bodyColor: body.color,
      bodyFont: body.fontFamily,

      // Tüm elementlerin renklerini topla
      allColors: Array.from(document.querySelectorAll('*')).slice(0, 500).map(el => {
        const style = getComputedStyle(el);
        return {
          tag: el.tagName,
          class: el.className?.toString()?.slice(0, 50),
          bg: style.backgroundColor,
          color: style.color,
          borderColor: style.borderColor,
          fontSize: style.fontSize,
          fontWeight: style.fontWeight
        };
      }).filter(s => s.bg !== 'rgba(0, 0, 0, 0)' || s.color !== 'rgb(0, 0, 0)')
    };
  });

  // HTML yapısını al
  console.log('📄 HTML yapısı alınıyor...');
  const html = await page.content();

  // Sonuçları kaydet
  fs.writeFileSync('sellerboard-styles.json', JSON.stringify(styles, null, 2));
  fs.writeFileSync('sellerboard-page.html', html);

  console.log('✅ Analiz tamamlandı!');
  console.log('📁 Dosyalar:');
  console.log('   - sellerboard-dashboard.png');
  console.log('   - sellerboard-styles.json');
  console.log('   - sellerboard-page.html');

  // Tarayıcıyı açık bırak (inceleme için)
  console.log('\n👀 Tarayıcı açık bırakıldı. İncelemek için kullanabilirsin.');
  console.log('   Kapatmak için terminalde Ctrl+C bas.');

  // 5 dakika bekle sonra kapat
  await page.waitForTimeout(300000);
  await browser.close();
}

analyzeSellerboard().catch(console.error);
