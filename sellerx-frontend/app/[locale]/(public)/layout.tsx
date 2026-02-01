'use client';

import Link from 'next/link';
import { Button } from '@/components/ui/button';

export default function PublicLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen bg-gradient-to-b from-background to-muted/20">
      {/* Header */}
      <header className="border-b bg-background/80 backdrop-blur-sm sticky top-0 z-50">
        <div className="container mx-auto px-4 h-16 flex items-center justify-between">
          <Link href="/" className="flex items-center gap-2">
            <span className="text-xl font-bold text-primary">SellerX</span>
          </Link>
          <nav className="flex items-center gap-4">
            <Link href="/pricing">
              <Button variant="ghost">Fiyatlandırma</Button>
            </Link>
            <Link href="/sign-in">
              <Button variant="ghost">Giriş Yap</Button>
            </Link>
            <Link href="/register">
              <Button>Ücretsiz Başla</Button>
            </Link>
          </nav>
        </div>
      </header>

      {/* Main Content */}
      <main>{children}</main>

      {/* Footer */}
      <footer className="border-t bg-background mt-auto">
        <div className="container mx-auto px-4 py-8">
          <div className="grid gap-8 md:grid-cols-4">
            <div>
              <h3 className="font-semibold mb-4">SellerX</h3>
              <p className="text-sm text-muted-foreground">
                Türkiye'nin en kapsamlı e-ticaret yönetim platformu.
              </p>
            </div>
            <div>
              <h4 className="font-medium mb-4">Ürün</h4>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li>
                  <Link href="/pricing" className="hover:text-foreground">
                    Fiyatlandırma
                  </Link>
                </li>
                <li>
                  <Link href="#" className="hover:text-foreground">
                    Özellikler
                  </Link>
                </li>
                <li>
                  <Link href="#" className="hover:text-foreground">
                    Entegrasyonlar
                  </Link>
                </li>
              </ul>
            </div>
            <div>
              <h4 className="font-medium mb-4">Destek</h4>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li>
                  <Link href="#" className="hover:text-foreground">
                    Yardım Merkezi
                  </Link>
                </li>
                <li>
                  <Link href="#" className="hover:text-foreground">
                    İletişim
                  </Link>
                </li>
                <li>
                  <Link href="#" className="hover:text-foreground">
                    SSS
                  </Link>
                </li>
              </ul>
            </div>
            <div>
              <h4 className="font-medium mb-4">Yasal</h4>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li>
                  <Link href="#" className="hover:text-foreground">
                    Gizlilik Politikası
                  </Link>
                </li>
                <li>
                  <Link href="#" className="hover:text-foreground">
                    Kullanım Koşulları
                  </Link>
                </li>
                <li>
                  <Link href="#" className="hover:text-foreground">
                    KVKK
                  </Link>
                </li>
              </ul>
            </div>
          </div>
          <div className="border-t mt-8 pt-8 text-center text-sm text-muted-foreground">
            <p>&copy; {new Date().getFullYear()} SellerX. Tüm hakları saklıdır.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}
