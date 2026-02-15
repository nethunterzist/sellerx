'use client';

import Link from 'next/link';
import { Rocket, ArrowRight } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';

export function CalculatorCTA() {
  return (
    <Card className="bg-gradient-to-r from-primary/10 via-primary/5 to-transparent border-primary/20">
      <CardContent className="py-8">
        <div className="flex flex-col md:flex-row items-center justify-between gap-6">
          <div className="flex items-start gap-4">
            <div className="p-3 rounded-full bg-primary/10">
              <Rocket className="h-6 w-6 text-primary" />
            </div>
            <div>
              <h3 className="text-xl font-semibold mb-2">
                Tüm Mağazalarınızı Tek Panelden Yönetin
              </h3>
              <p className="text-muted-foreground max-w-lg">
                SellerX ile siparişlerinizi, ürünlerinizi ve finansal verilerinizi
                otomatik senkronize edin. Gerçek zamanlı kâr analizi yapın.
              </p>
            </div>
          </div>

          <div className="flex flex-col sm:flex-row gap-3 w-full md:w-auto">
            <Button asChild size="lg" className="w-full sm:w-auto">
              <Link href="/register">
                Ücretsiz Deneyin
                <ArrowRight className="ml-2 h-4 w-4" />
              </Link>
            </Button>
            <Button asChild variant="outline" size="lg" className="w-full sm:w-auto">
              <Link href="/pricing">Daha Fazla Bilgi</Link>
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
