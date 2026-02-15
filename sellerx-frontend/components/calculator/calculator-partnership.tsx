'use client';

import { ChevronDown, Users } from 'lucide-react';
import { useState } from 'react';

import { Card, CardContent } from '@/components/ui/card';
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '@/components/ui/collapsible';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Slider } from '@/components/ui/slider';
import { formatCurrency } from '@/lib/utils/calculator';

interface PartnershipResults {
  myProfit: number;
  partnerProfit: number;
  myInvestment: number;
  partnerInvestment: number;
}

interface CalculatorPartnershipProps {
  partnershipSplit: number;
  partnershipResults: PartnershipResults;
  onPartnershipSplitChange: (value: number) => void;
}

export function CalculatorPartnership({
  partnershipSplit,
  partnershipResults,
  onPartnershipSplitChange,
}: CalculatorPartnershipProps) {
  const [open, setOpen] = useState(false);

  return (
    <Card>
      <CardContent className="pt-3 pb-3">
        <Collapsible open={open} onOpenChange={setOpen}>
          <CollapsibleTrigger className="flex items-center gap-2 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors w-full">
            <Users className="h-4 w-4 text-indigo-500" />
            <span>Ortaklık Paylaşımı</span>
            <ChevronDown
              className={`h-4 w-4 ml-auto transition-transform ${open ? 'rotate-180' : ''}`}
            />
          </CollapsibleTrigger>
          <CollapsibleContent className="mt-3 space-y-3">
            <div className="flex items-center gap-3">
              <Label className="text-sm text-muted-foreground min-w-fit">
                Benim Payım
              </Label>
              <Slider
                value={[partnershipSplit]}
                onValueChange={([value]) => onPartnershipSplitChange(value)}
                min={0}
                max={100}
                step={5}
                className="flex-1"
              />
              <div className="flex items-center gap-1">
                <Input
                  type="number"
                  min={0}
                  max={100}
                  value={partnershipSplit}
                  onChange={(e) =>
                    onPartnershipSplitChange(
                      Math.min(100, Math.max(0, parseInt(e.target.value) || 0))
                    )
                  }
                  className="w-16 text-center h-8 text-sm [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                />
                <span className="text-sm text-muted-foreground">%</span>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3 pt-2 border-t">
              <div className="space-y-1">
                <p className="text-xs font-medium text-indigo-600 dark:text-indigo-400 uppercase">
                  Benim (%{partnershipSplit})
                </p>
                <div className="flex justify-between items-center">
                  <span className="text-xs text-muted-foreground">Kâr:</span>
                  <span
                    className={`text-sm font-semibold ${
                      partnershipResults.myProfit >= 0
                        ? 'text-green-600 dark:text-green-400'
                        : 'text-red-600 dark:text-red-400'
                    }`}
                  >
                    {formatCurrency(partnershipResults.myProfit)}
                  </span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-xs text-muted-foreground">Yatırım:</span>
                  <span className="text-sm font-medium">
                    {formatCurrency(partnershipResults.myInvestment)}
                  </span>
                </div>
              </div>

              <div className="space-y-1">
                <p className="text-xs font-medium text-indigo-600 dark:text-indigo-400 uppercase">
                  Ortak (%{100 - partnershipSplit})
                </p>
                <div className="flex justify-between items-center">
                  <span className="text-xs text-muted-foreground">Kâr:</span>
                  <span
                    className={`text-sm font-semibold ${
                      partnershipResults.partnerProfit >= 0
                        ? 'text-green-600 dark:text-green-400'
                        : 'text-red-600 dark:text-red-400'
                    }`}
                  >
                    {formatCurrency(partnershipResults.partnerProfit)}
                  </span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-xs text-muted-foreground">Yatırım:</span>
                  <span className="text-sm font-medium">
                    {formatCurrency(partnershipResults.partnerInvestment)}
                  </span>
                </div>
              </div>
            </div>
          </CollapsibleContent>
        </Collapsible>
      </CardContent>
    </Card>
  );
}
