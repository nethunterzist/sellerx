export default function TermsPage() {
  return (
    <div className="container mx-auto px-4 py-12 max-w-4xl">
      <h1 className="text-3xl font-bold mb-8">Kullanım Koşulları</h1>

      <div className="prose prose-gray max-w-none space-y-6">
        <p className="text-muted-foreground">
          Son güncelleme: {new Date().toLocaleDateString('tr-TR', { year: 'numeric', month: 'long', day: 'numeric' })}
        </p>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-4">1. Genel Koşullar</h2>
          <p>
            SellerX platformunu kullanarak bu kullanım koşullarını kabul etmiş sayılırsınız.
            Platform, e-ticaret satıcılarına yönelik analitik ve yönetim hizmetleri sunmaktadır.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-4">2. Hesap Oluşturma ve Güvenlik</h2>
          <p>
            Hesap oluşturmak için doğru ve güncel bilgiler vermeniz gerekmektedir.
            Hesap güvenliğinizden ve şifrenizin gizliliğinden siz sorumlusunuz.
          </p>
          <ul className="list-disc pl-6 mt-2 space-y-1">
            <li>Hesabınızı başkalarıyla paylaşmayın</li>
            <li>Şüpheli aktiviteleri derhal bildirin</li>
            <li>Güçlü şifre kullanın ve düzenli olarak değiştirin</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-4">3. Hizmet Kullanımı</h2>
          <p>
            Platformumuzu yasalara uygun şekilde ve yalnızca ticari amaçlarla kullanabilirsiniz.
          </p>
          <ul className="list-disc pl-6 mt-2 space-y-1">
            <li>Platformu kötüye kullanmak yasaktır</li>
            <li>Başkalarının haklarına saygı gösterin</li>
            <li>Yanıltıcı bilgi paylaşmayın</li>
          </ul>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-4">4. Ödeme ve Abonelik</h2>
          <p>
            Ücretli planlar için geçerli ödeme yöntemi sağlamanız gerekmektedir.
            Abonelikler, iptal edilmediği takdirde otomatik olarak yenilenir.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-4">5. Fikri Mülkiyet</h2>
          <p>
            Platform içeriği ve yazılımı SellerX'e aittir.
            İzinsiz kopyalama, dağıtma veya tersine mühendislik yasaktır.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-4">6. Sorumluluk Reddi</h2>
          <p>
            Platform "olduğu gibi" sunulmaktadır. Verilerin doğruluğu,
            kesintisiz hizmet veya belirli sonuçlar garanti edilmez.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-4">7. Değişiklikler</h2>
          <p>
            Bu koşullar önceden haber vermeksizin değiştirilebilir.
            Güncel koşulları takip etmek kullanıcının sorumluluğundadır.
          </p>
        </section>

        <section>
          <h2 className="text-xl font-semibold mt-8 mb-4">8. İletişim</h2>
          <p>
            Sorularınız için: <a href="mailto:legal@sellerx.com" className="text-primary hover:underline">legal@sellerx.com</a>
          </p>
        </section>
      </div>
    </div>
  );
}
