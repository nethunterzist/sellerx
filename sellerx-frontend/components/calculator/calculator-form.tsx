'use client';

import { ChevronDown, Megaphone, Package, Percent, Settings, ShoppingCart, Target } from 'lucide-react';
import { useState } from 'react';

import { Card, CardContent } from '@/components/ui/card';
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from '@/components/ui/collapsible';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Separator } from '@/components/ui/separator';
import { Switch } from '@/components/ui/switch';
import type { CalculatorInput } from '@/types/calculator';
import { VAT_RATE_OPTIONS } from '@/types/calculator';

interface CalculatorFormProps {
  inputs: CalculatorInput;
  onChange: (inputs: CalculatorInput) => void;
}

function SectionHeader({ icon: Icon, title }: { icon: React.ElementType; title: string }) {
  return (
    <div className="flex items-center gap-2">
      <Icon className="h-4 w-4 text-primary" />
      <span className="text-sm font-semibold">{title}</span>
    </div>
  );
}

export function CalculatorForm({ inputs, onChange }: CalculatorFormProps) {
  const [advancedOpen, setAdvancedOpen] = useState(false);
  const [useCustomVat, setUseCustomVat] = useState(false);

  const handleChange = (field: keyof CalculatorInput, value: string) => {
    const numValue = parseFloat(value) || 0;
    onChange({
      ...inputs,
      [field]: numValue,
    });
  };

  const handleVatSelect = (value: number) => {
    setUseCustomVat(false);
    onChange({
      ...inputs,
      vatRate: value,
    });
  };

  const handleCustomVatChange = (value: string) => {
    const numValue = (parseFloat(value) || 0) / 100;
    onChange({
      ...inputs,
      vatRate: numValue,
    });
  };

  const handlePercentChange = (field: keyof CalculatorInput, value: string) => {
    const numValue = (parseFloat(value) || 0) / 100;
    onChange({
      ...inputs,
      [field]: numValue,
    });
  };

  const handleBooleanChange = (field: keyof CalculatorInput, value: boolean) => {
    onChange({
      ...inputs,
      [field]: value,
    });
  };

  const isVatSelected = (rate: number) => {
    return !useCustomVat && Math.abs(inputs.vatRate - rate) < 0.001;
  };

  return (
    <Card>
      <CardContent className="pt-5 pb-5 space-y-5">
        {/* Section 1: Alış & Satış */}
        <div className="space-y-3">
          <SectionHeader icon={ShoppingCart} title="Alış & Satış" />

          {/* Satış Fiyatı - Hero Input */}
          <div className="space-y-1.5">
            <Label htmlFor="salePrice" className="text-sm font-medium">
              Satış Fiyatı (KDV Dahil)
            </Label>
            <div className="relative">
              <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground font-medium">
                ₺
              </span>
              <Input
                id="salePrice"
                type="number"
                min="0"
                step="0.01"
                value={inputs.salePrice || ''}
                onChange={(e) => handleChange('salePrice', e.target.value)}
                className="pl-8 text-lg font-semibold h-12 border-primary/30 focus:border-primary"
                placeholder="0.00"
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="purchaseCost" className="text-sm">
                Alış Fiyatı (KDV Dahil)
              </Label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm">
                  ₺
                </span>
                <Input
                  id="purchaseCost"
                  type="number"
                  min="0"
                  step="0.01"
                  value={inputs.purchaseCost || ''}
                  onChange={(e) => handleChange('purchaseCost', e.target.value)}
                  className="pl-8"
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="quantity" className="text-sm">Miktar</Label>
              <Input
                id="quantity"
                type="number"
                min="1"
                step="1"
                value={inputs.quantity || ''}
                onChange={(e) => handleChange('quantity', e.target.value)}
              />
            </div>
          </div>
        </div>

        <Separator />

        {/* Section 2: Kesinti Oranları */}
        <div className="space-y-3">
          <SectionHeader icon={Percent} title="Kesinti Oranları" />

          {/* KDV Seçimi */}
          <div className="space-y-2">
            <Label className="text-sm">Ürün KDV Oranı</Label>
            <div className="grid grid-cols-5 gap-1.5">
              {VAT_RATE_OPTIONS.map((option) => {
                const rateValue = parseFloat(option.value);
                const selected = isVatSelected(rateValue);
                return (
                  <button
                    key={option.value}
                    type="button"
                    onClick={() => handleVatSelect(rateValue)}
                    className={`py-2 px-2 rounded-md text-center text-sm font-medium transition-colors border ${
                      selected
                        ? 'bg-primary text-primary-foreground border-primary'
                        : 'bg-background border-input hover:bg-accent hover:text-accent-foreground'
                    }`}
                  >
                    {option.label}
                  </button>
                );
              })}
              <button
                type="button"
                onClick={() => setUseCustomVat(true)}
                className={`py-2 px-2 rounded-md text-center text-sm font-medium transition-colors border ${
                  useCustomVat
                    ? 'bg-primary text-primary-foreground border-primary'
                    : 'bg-background border-input hover:bg-accent hover:text-accent-foreground'
                }`}
              >
                Özel
              </button>
            </div>

            {useCustomVat && (
              <div className="pt-1">
                <div className="relative">
                  <Input
                    id="customVat"
                    type="number"
                    min="0"
                    max="100"
                    step="1"
                    value={(inputs.vatRate * 100).toFixed(0)}
                    onChange={(e) => handleCustomVatChange(e.target.value)}
                    className="pr-8"
                    placeholder="Örn: 8"
                    autoFocus
                  />
                  <span className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm">
                    %
                  </span>
                </div>
              </div>
            )}
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="commissionRate" className="text-sm">Komisyon Oranı</Label>
              <div className="relative">
                <Input
                  id="commissionRate"
                  type="number"
                  min="0"
                  max="50"
                  step="0.1"
                  value={(inputs.commissionRate * 100).toFixed(1)}
                  onChange={(e) =>
                    handlePercentChange('commissionRate', e.target.value)
                  }
                  className="pr-8"
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm">
                  %
                </span>
              </div>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="otvRate" className="text-sm">ÖTV Oranı</Label>
              <div className="relative">
                <Input
                  id="otvRate"
                  type="number"
                  min="0"
                  max="100"
                  step="1"
                  value={(inputs.otvRate * 100).toFixed(0)}
                  onChange={(e) =>
                    handlePercentChange('otvRate', e.target.value)
                  }
                  className="pr-8"
                  placeholder="0"
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm">
                  %
                </span>
              </div>
            </div>
          </div>
        </div>

        <Separator />

        {/* Section 3: Ek Maliyetler */}
        <div className="space-y-3">
          <SectionHeader icon={Package} title="Ek Maliyetler" />
          <div className="grid grid-cols-3 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="shipping" className="text-sm">Nakliyat</Label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm">
                  ₺
                </span>
                <Input
                  id="shipping"
                  type="number"
                  min="0"
                  step="0.01"
                  value={inputs.shipping || ''}
                  onChange={(e) => handleChange('shipping', e.target.value)}
                  className="pl-8"
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="packaging" className="text-sm">Paketleme</Label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm">
                  ₺
                </span>
                <Input
                  id="packaging"
                  type="number"
                  min="0"
                  step="0.01"
                  value={inputs.packaging || ''}
                  onChange={(e) => handleChange('packaging', e.target.value)}
                  className="pl-8"
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="cargo" className="text-sm">Kargo</Label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm">
                  ₺
                </span>
                <Input
                  id="cargo"
                  type="number"
                  min="0"
                  step="0.01"
                  value={inputs.cargo || ''}
                  onChange={(e) => handleChange('cargo', e.target.value)}
                  className="pl-8"
                />
              </div>
            </div>
          </div>
        </div>

        <Separator />

        {/* Section 4: Reklam */}
        <div className="space-y-3">
          <SectionHeader icon={Megaphone} title="Reklam (Opsiyonel)" />
          <div className="flex items-center gap-3">
            <Switch
              id="useManualAcos"
              checked={inputs.useManualAcos}
              onCheckedChange={(checked) => handleBooleanChange('useManualAcos', checked)}
            />
            <Label htmlFor="useManualAcos" className="text-sm cursor-pointer">
              {inputs.useManualAcos ? 'Manuel ACOS' : 'CPC/CVR ile Hesapla'}
            </Label>
          </div>

          {inputs.useManualAcos ? (
            <div className="space-y-1.5">
              <Label htmlFor="acos" className="text-sm">ACOS</Label>
              <div className="relative">
                <Input
                  id="acos"
                  type="number"
                  min="0"
                  max="100"
                  step="0.1"
                  value={(inputs.acos * 100).toFixed(1)}
                  onChange={(e) => handlePercentChange('acos', e.target.value)}
                  className="pr-8"
                  placeholder="Reklam harcama oranı"
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm">
                  %
                </span>
              </div>
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label htmlFor="cpc" className="text-sm">CPC</Label>
                <div className="relative">
                  <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm">
                    ₺
                  </span>
                  <Input
                    id="cpc"
                    type="number"
                    min="0"
                    step="0.01"
                    value={inputs.cpc || ''}
                    onChange={(e) => handleChange('cpc', e.target.value)}
                    className="pl-8"
                    placeholder="0.00"
                  />
                </div>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="cvr" className="text-sm">CVR</Label>
                <div className="relative">
                  <Input
                    id="cvr"
                    type="number"
                    min="0"
                    max="100"
                    step="0.1"
                    value={(inputs.cvr * 100).toFixed(1)}
                    onChange={(e) => handlePercentChange('cvr', e.target.value)}
                    className="pr-8"
                    placeholder="0"
                  />
                  <span className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm">
                    %
                  </span>
                </div>
              </div>
            </div>
          )}
        </div>

        <Separator />

        {/* Section 5: Hedef Kâr Marjı */}
        <div className="space-y-3">
          <SectionHeader icon={Target} title="Hedef Kâr Marjı (Opsiyonel)" />
          <div className="space-y-1.5">
            <Label htmlFor="targetMargin" className="text-sm">Hedeflenen Kâr Marjı</Label>
            <div className="relative">
              <Input
                id="targetMargin"
                type="number"
                min="0"
                max="100"
                step="1"
                value={(inputs.targetMargin * 100).toFixed(0)}
                onChange={(e) => handlePercentChange('targetMargin', e.target.value)}
                className="pr-8"
                placeholder="0"
              />
              <span className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm">
                %
              </span>
            </div>
            <p className="text-xs text-muted-foreground">
              Bu marjı elde etmek için önerilen satış fiyatı hesaplanır
            </p>
          </div>
        </div>

        <Separator />

        {/* Section 6: Gelişmiş Ayarlar */}
        <Collapsible open={advancedOpen} onOpenChange={setAdvancedOpen}>
          <CollapsibleTrigger className="flex items-center gap-2 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors w-full">
            <Settings className="h-4 w-4 text-primary" />
            <span className="text-sm font-semibold">Gelişmiş Ayarlar</span>
            <ChevronDown
              className={`h-4 w-4 ml-auto transition-transform ${advancedOpen ? 'rotate-180' : ''}`}
            />
          </CollapsibleTrigger>
          <CollapsibleContent className="mt-3 space-y-3">
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label htmlFor="platformFee" className="text-sm">Platform Hizmet Bedeli</Label>
                <div className="relative">
                  <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm">
                    ₺
                  </span>
                  <Input
                    id="platformFee"
                    type="number"
                    min="0"
                    step="0.01"
                    value={inputs.platformFee || ''}
                    onChange={(e) => handleChange('platformFee', e.target.value)}
                    className="pl-8"
                  />
                </div>
              </div>

              <div className="space-y-1.5">
                <Label htmlFor="stopaj" className="text-sm">Stopaj</Label>
                <div className="relative">
                  <span className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm">
                    ₺
                  </span>
                  <Input
                    id="stopaj"
                    type="number"
                    min="0"
                    step="0.01"
                    value={inputs.stopaj || ''}
                    onChange={(e) => handleChange('stopaj', e.target.value)}
                    className="pl-8"
                    placeholder="0"
                  />
                </div>
              </div>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="incomeTaxRate" className="text-sm">Gelir Vergisi Oranı</Label>
              <div className="relative">
                <Input
                  id="incomeTaxRate"
                  type="number"
                  min="0"
                  max="100"
                  step="1"
                  value={(inputs.incomeTaxRate * 100).toFixed(0)}
                  onChange={(e) =>
                    handlePercentChange('incomeTaxRate', e.target.value)
                  }
                  className="pr-8"
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground text-sm">
                  %
                </span>
              </div>
            </div>
          </CollapsibleContent>
        </Collapsible>
      </CardContent>
    </Card>
  );
}
