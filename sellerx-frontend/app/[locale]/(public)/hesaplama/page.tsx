'use client';

import { useState, useMemo } from 'react';
import { Calculator } from 'lucide-react';

import {
  CalculatorForm,
  CalculatorPartnership,
  CalculatorResults,
  CalculatorSummary,
  CalculatorCTA,
} from '@/components/calculator';
import { calculateProfit } from '@/lib/utils/calculator';
import { DEFAULT_CALCULATOR_INPUT } from '@/types/calculator';
import type { CalculatorInput } from '@/types/calculator';
import { FadeIn } from '@/components/motion';

export default function HesaplamaPage() {
  const [inputs, setInputs] = useState<CalculatorInput>(DEFAULT_CALCULATOR_INPUT);
  const [partnershipSplit, setPartnershipSplit] = useState(50);

  const results = useMemo(() => calculateProfit(inputs), [inputs]);

  const partnershipResults = useMemo(() => {
    const myShare = partnershipSplit / 100;
    const partnerShare = 1 - myShare;
    return {
      myProfit: results.netProfit * myShare,
      partnerProfit: results.netProfit * partnerShare,
      myInvestment: results.totalInvestment * myShare,
      partnerInvestment: results.totalInvestment * partnerShare,
    };
  }, [results, partnershipSplit]);

  return (
    <div className="container mx-auto px-4 py-8 max-w-7xl space-y-8">
      {/* Header - Public sayfada korunuyor */}
      <FadeIn direction="up">
        <div className="text-center">
          <div className="inline-flex items-center justify-center p-3 rounded-full bg-primary/10 mb-4">
            <Calculator className="h-8 w-8 text-primary" />
          </div>
          <h1 className="text-3xl md:text-4xl font-bold mb-3">
            Trendyol Kâr Hesaplama Aracı
          </h1>
          <p className="text-muted-foreground max-w-2xl mx-auto">
            Ürün maliyetlerinizi girin, komisyon ve KDV dahil tüm kesintileri hesaplayarak
            net kârınızı ve kâr marjınızı anında görün.
          </p>
        </div>
      </FadeIn>

      {/* İki kolonlu layout */}
      <div className="grid grid-cols-1 lg:grid-cols-[480px_1fr] gap-6">
        {/* Sol Kolon - Form */}
        <div>
          <CalculatorForm inputs={inputs} onChange={setInputs} />
        </div>

        {/* Sağ Kolon - Sonuçlar (her zaman görünür) */}
        <div className="lg:sticky lg:top-20 lg:self-start space-y-4">
          <CalculatorSummary
            results={results}
            quantity={inputs.quantity}
            targetMargin={inputs.targetMargin}
          />
          <CalculatorResults
            results={results}
            quantity={inputs.quantity}
            vatRate={inputs.vatRate}
          />
          <CalculatorPartnership
            partnershipSplit={partnershipSplit}
            partnershipResults={partnershipResults}
            onPartnershipSplitChange={setPartnershipSplit}
          />
        </div>
      </div>

      {/* CTA Banner - Full width */}
      <CalculatorCTA />
    </div>
  );
}
