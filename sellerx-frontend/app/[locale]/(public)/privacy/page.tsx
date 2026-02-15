export default function PrivacyPage() {
  return (
    <div className="container mx-auto px-4 py-12 max-w-4xl">
      <h1 className="text-3xl font-bold mb-8">Gizlilik Politikası</h1>

      <div className="prose prose-gray max-w-none space-y-6">
        <p className="text-muted-foreground">
          Son güncelleme: {new Date().toLocaleDateString('tr-TR', { year: 'numeric', month: 'long', day: 'numeric' })}
        </p>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-4">1. Giriş</h2>
          <p>
            SellerX olarak, gizliliğinize önem veriyoruz. Bu politika,
            kişisel verilerinizi nasıl topladığımızı, kullandığımızı ve
            koruduğumuzu açıklamaktadır.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-4">2. Toplanan Veriler</h2>
          <p>Aşağıdaki kişisel verileri topluyoruz:</p>
          <ul className="list-disc pl-6 mt-2 space-y-1">
            <li><strong>Hesap Bilgileri:</strong> Ad, e-posta adresi, şifre (şifrelenmiş)</li>
            <li><strong>Mağaza Bilgileri:</strong> Trendyol/Hepsiburada API kimlik bilgileri, mağaza adı</li>
            <li><strong>Ticari Veriler:</strong> Sipariş, ürün, satış verileri (pazaryeri API'lerinden)</li>
            <li><strong>Kullanım Verileri:</strong> Platform kullanım istatistikleri, oturum bilgileri</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-4">3. Veri Kullanım Amaçları</h2>
          <p>Verilerinizi şu amaçlarla kullanıyoruz:</p>
          <ul className="list-disc pl-6 mt-2 space-y-1">
            <li>Platform hizmetlerini sunmak ve geliştirmek</li>
            <li>Analitik raporlar ve öneriler oluşturmak</li>
            <li>Teknik destek sağlamak</li>
            <li>Yasal yükümlülükleri yerine getirmek</li>
            <li>Hizmet güncellemeleri ve duyurular göndermek</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-4">4. Veri Güvenliği</h2>
          <p>Verilerinizi korumak için aldığımız önlemler:</p>
          <ul className="list-disc pl-6 mt-2 space-y-1">
            <li>SSL/TLS şifrelemesi ile güvenli veri iletimi</li>
            <li>Şifrelerin BCrypt ile hash'lenmesi</li>
            <li>API anahtarlarının şifrelenmiş saklanması</li>
            <li>Düzenli güvenlik denetimleri</li>
            <li>Erişim kontrolleri ve yetkilendirme</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-4">5. Veri Paylaşımı</h2>
          <p>
            Verileriniz üçüncü taraflarla yalnızca şu durumlarda paylaşılır:
          </p>
          <ul className="list-disc pl-6 mt-2 space-y-1">
            <li>Yasal zorunluluk halinde</li>
            <li>Açık izniniz olduğunda</li>
            <li>Hizmet sağlayıcılarımızla (hosting, e-posta hizmetleri)</li>
          </ul>
          <p className="mt-2">
            Verilerinizi hiçbir koşulda pazarlama amacıyla satmıyoruz.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-4">6. Çerezler</h2>
          <p>
            Platform, oturum yönetimi ve kullanıcı deneyimini iyileştirmek için
            çerezler kullanmaktadır. Tarayıcı ayarlarından çerezleri kontrol edebilirsiniz.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-4">7. Haklarınız (KVKK)</h2>
          <p>KVKK kapsamında sahip olduğunuz haklar:</p>
          <ul className="list-disc pl-6 mt-2 space-y-1">
            <li>Verilerinize erişim hakkı</li>
            <li>Verilerin düzeltilmesini talep etme hakkı</li>
            <li>Verilerin silinmesini talep etme hakkı</li>
            <li>Veri işlemeyi kısıtlama hakkı</li>
            <li>Veri taşınabilirliği hakkı</li>
            <li>İşlemeye itiraz hakkı</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-4">8. Veri Saklama</h2>
          <p>
            Verileriniz, hesabınız aktif olduğu süre boyunca ve hesap silindikten sonra
            yasal yükümlülükler gereği belirli bir süre daha saklanır.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-4">9. Politika Değişiklikleri</h2>
          <p>
            Bu politika güncellenebilir. Önemli değişiklikler e-posta ile bildirilir.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-4">10. İletişim</h2>
          <p>
            Gizlilik sorularınız için: <a href="mailto:privacy@sellerx.com" className="text-primary hover:underline">privacy@sellerx.com</a>
          </p>
          <p className="mt-2">
            Veri Sorumlusu: SellerX Yazılım ve Teknoloji A.Ş.
          </p>
        </section>
      </div>
    </div>
  );
}
