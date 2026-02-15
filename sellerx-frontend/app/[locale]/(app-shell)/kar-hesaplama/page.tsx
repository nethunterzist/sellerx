'use client';

import { useState, useMemo } from 'react';

import {
  CalculatorForm,
  CalculatorPartnership,
  CalculatorResults,
  CalculatorSummary,
} from '@/components/calculator';
import { calculateProfit } from '@/lib/utils/calculator';
import { DEFAULT_CALCULATOR_INPUT } from '@/types/calculator';
import type { CalculatorInput } from '@/types/calculator';

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
    <div className="space-y-6">
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
    </div>
  );
}
