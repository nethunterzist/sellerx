/**
 * Trendyol Profit Calculator Utility Functions
 * Matches Excel calculation model exactly
 */

import type {
  CalculatorInput,
  CalculatorResult,
  CostLineItem,
  VatLineItem,
} from '@/types/calculator';

// Standard VAT rate for expenses (nakliyat, paketleme, reklam, komisyon, etc.)
const EXPENSE_VAT_RATE = 0.20;

/**
 * Remove VAT from an amount
 */
export function removeVat(amountIncVat: number, vatRate: number): number {
  return amountIncVat / (1 + vatRate);
}

/**
 * Add VAT to an amount
 */
export function addVat(amountExVat: number, vatRate: number): number {
  return amountExVat * (1 + vatRate);
}

/**
 * Calculate VAT amount from amount excluding VAT
 */
export function calculateVat(amountExVat: number, vatRate: number): number {
  return amountExVat * vatRate;
}

/**
 * Format number to 2 decimal places
 */
export function round2(value: number): number {
  return Math.round(value * 100) / 100;
}

/**
 * Calculate ACOS from CPC and CVR
 * Formula: ACOS = CPC / (SalePrice × CVR)
 */
export function calculateAcosFromCpcCvr(
  salePrice: number,
  cpc: number,
  cvr: number
): number {
  if (salePrice <= 0 || cvr <= 0) return 0;
  return cpc / (salePrice * cvr);
}

/**
 * Calculate suggested sale price based on target margin
 * Reverse calculation to find what sale price gives desired margin
 */
export function calculateSuggestedPrice(
  input: CalculatorInput,
  targetMargin: number
): number {
  if (targetMargin <= 0 || targetMargin >= 1) return 0;

  const {
    vatRate,
    commissionRate,
    otvRate,
    quantity,
    purchaseCost,
    shipping,
    packaging,
    cargo,
    acos,
    cpc,
    cvr,
    useManualAcos,
    platformFee,
    stopaj,
    incomeTaxRate,
  } = input;

  // Fixed costs (excluding VAT)
  const purchaseCostExVat = removeVat(purchaseCost, vatRate);
  const otvAmount = purchaseCostExVat * otvRate;
  const shippingExVat = removeVat(shipping, EXPENSE_VAT_RATE);
  const packagingExVat = removeVat(packaging, EXPENSE_VAT_RATE);
  const cargoExVat = removeVat(cargo, EXPENSE_VAT_RATE);
  const platformFeeExVat = platformFee;
  const stopajExVat = stopaj;

  const fixedCostsExVat =
    (purchaseCostExVat +
      otvAmount +
      shippingExVat +
      packagingExVat +
      cargoExVat +
      platformFeeExVat +
      stopajExVat) *
    quantity;

  // Variable cost rates (commission and ACOS are percentages of sale price)
  // Net Profit = SalePriceExVat - FixedCosts - (CommissionRate × SalePriceExVat) - (ACOS × SalePrice / 1.20)
  // Margin = NetProfit / SalePrice
  // targetMargin × SalePrice = SalePriceExVat - FixedCosts - CommissionRate × SalePriceExVat - ACOS × SalePrice / 1.20
  // SalePrice / (1 + vatRate) = SalePriceExVat
  // Let S = SalePrice
  // targetMargin × S = S/(1+vatRate) - FixedCosts - CommissionRate × S/(1+vatRate) - ACOS × S / 1.20
  // targetMargin × S = S/(1+vatRate) × (1 - CommissionRate) - FixedCosts - ACOS × S / 1.20
  // targetMargin × S + ACOS × S / 1.20 = S/(1+vatRate) × (1 - CommissionRate) - FixedCosts
  // S × (targetMargin + ACOS/1.20 - (1-CommissionRate)/(1+vatRate)) = -FixedCosts
  // S = FixedCosts / ((1-CommissionRate)/(1+vatRate) - targetMargin - ACOS/1.20)

  const effectiveAcos = useManualAcos ? acos : calculateAcosFromCpcCvr(100, cpc, cvr); // Use 100 as placeholder

  // Simplified calculation - iterative approach for accuracy
  let suggestedPrice = purchaseCost * 2; // Initial guess
  for (let i = 0; i < 20; i++) {
    const testAcos = useManualAcos ? acos : calculateAcosFromCpcCvr(suggestedPrice, cpc, cvr);
    const advertisingExVat = removeVat(suggestedPrice * testAcos, EXPENSE_VAT_RATE);
    const salePriceExVat = removeVat(suggestedPrice, vatRate);
    const commissionExVat = salePriceExVat * commissionRate;

    const totalCostExVat =
      (purchaseCostExVat +
        otvAmount +
        shippingExVat +
        packagingExVat +
        advertisingExVat +
        commissionExVat +
        platformFeeExVat +
        stopajExVat +
        cargoExVat) *
      quantity;

    const grossProfit = salePriceExVat * quantity - totalCostExVat;
    const incomeTax = grossProfit > 0 ? grossProfit * incomeTaxRate : 0;
    const netProfit = grossProfit - incomeTax;
    const currentMargin = netProfit / (suggestedPrice * quantity);

    if (Math.abs(currentMargin - targetMargin) < 0.001) break;

    // Adjust price based on margin difference
    suggestedPrice = suggestedPrice * (1 + (targetMargin - currentMargin));
  }

  return suggestedPrice > 0 ? suggestedPrice : 0;
}

/**
 * Main calculation function - matches Excel formulas
 */
export function calculateProfit(input: CalculatorInput): CalculatorResult {
  const {
    vatRate,
    commissionRate,
    otvRate,
    quantity,
    purchaseCost,
    salePrice,
    shipping,
    packaging,
    cargo,
    acos,
    cpc,
    cvr,
    useManualAcos,
    targetMargin,
    platformFee,
    stopaj,
    incomeTaxRate,
  } = input;

  // Calculate effective ACOS (manual or from CPC/CVR)
  const effectiveAcos = useManualAcos ? acos : calculateAcosFromCpcCvr(salePrice, cpc, cvr);

  // ===== 1. BASIC CONVERSIONS =====

  // Sale price without VAT
  const salePriceExVat = removeVat(salePrice, vatRate);

  // Purchase cost without VAT
  const purchaseCostExVat = removeVat(purchaseCost, vatRate);

  // OTV (Special Consumption Tax) - calculated on purchase cost excluding VAT
  const otvAmount = purchaseCostExVat * otvRate;
  const otvIncVat = addVat(otvAmount, EXPENSE_VAT_RATE);

  // Other costs without VAT (standard 20% VAT)
  const shippingExVat = removeVat(shipping, EXPENSE_VAT_RATE);
  const packagingExVat = removeVat(packaging, EXPENSE_VAT_RATE);
  const cargoExVat = removeVat(cargo, EXPENSE_VAT_RATE);

  // Stopaj (Withholding Tax) - input is already excluding VAT
  const stopajExVat = stopaj;
  const stopajIncVat = addVat(stopajExVat, EXPENSE_VAT_RATE);

  // ===== 2. ADVERTISING COST =====
  const advertisingIncVat = salePrice * effectiveAcos;
  const advertisingExVat = removeVat(advertisingIncVat, EXPENSE_VAT_RATE);

  // ===== 3. TRENDYOL DEDUCTIONS =====

  // Commission: calculated on sale price excluding VAT
  const commissionExVat = salePriceExVat * commissionRate;
  const commissionIncVat = addVat(commissionExVat, EXPENSE_VAT_RATE);

  // Platform fee (input is already excluding VAT)
  const platformFeeExVat = platformFee;
  const platformFeeIncVat = addVat(platformFeeExVat, EXPENSE_VAT_RATE);

  // ===== 4. COST BREAKDOWN TABLE =====
  const costs: CostLineItem[] = [
    {
      name: 'Alış Maliyeti',
      taxType: 'KDV',
      rate: vatRate,
      amountIncVat: purchaseCost,
      quantity: quantity,
      totalIncVat: purchaseCost * quantity,
      amountExVat: purchaseCostExVat,
      totalExVat: purchaseCostExVat * quantity,
    },
    // OTV (Special Consumption Tax) - only show if otvRate > 0
    ...(otvRate > 0
      ? [
          {
            name: 'ÖTV (Özel Tüketim Vergisi)',
            taxType: 'KDV',
            rate: otvRate,
            amountIncVat: otvIncVat,
            quantity: quantity,
            totalIncVat: otvIncVat * quantity,
            amountExVat: otvAmount,
            totalExVat: otvAmount * quantity,
          },
        ]
      : []),
    {
      name: 'Nakliyat',
      taxType: 'KDV',
      rate: EXPENSE_VAT_RATE,
      amountIncVat: shipping,
      quantity: quantity,
      totalIncVat: shipping * quantity,
      amountExVat: shippingExVat,
      totalExVat: shippingExVat * quantity,
    },
    {
      name: 'Paketleme Maliyeti',
      taxType: 'KDV',
      rate: EXPENSE_VAT_RATE,
      amountIncVat: packaging,
      quantity: quantity,
      totalIncVat: packaging * quantity,
      amountExVat: packagingExVat,
      totalExVat: packagingExVat * quantity,
    },
    {
      name: 'Reklam Maliyeti',
      taxType: 'KDV',
      rate: EXPENSE_VAT_RATE,
      amountIncVat: advertisingIncVat,
      quantity: quantity,
      totalIncVat: advertisingIncVat * quantity,
      amountExVat: advertisingExVat,
      totalExVat: advertisingExVat * quantity,
    },
    {
      name: 'Trendyol Komisyon',
      taxType: 'KDV',
      rate: commissionRate,
      amountIncVat: commissionIncVat,
      quantity: quantity,
      totalIncVat: commissionIncVat * quantity,
      amountExVat: commissionExVat,
      totalExVat: commissionExVat * quantity,
    },
    {
      name: 'Trendyol Platform Hizmet Bedeli',
      taxType: 'KDV',
      rate: EXPENSE_VAT_RATE,
      amountIncVat: platformFeeIncVat,
      quantity: quantity,
      totalIncVat: platformFeeIncVat * quantity,
      amountExVat: platformFeeExVat,
      totalExVat: platformFeeExVat * quantity,
    },
    // Stopaj (Withholding Tax) - only show if stopaj > 0
    ...(stopaj > 0
      ? [
          {
            name: 'Stopaj',
            taxType: 'KDV',
            rate: EXPENSE_VAT_RATE,
            amountIncVat: stopajIncVat,
            quantity: quantity,
            totalIncVat: stopajIncVat * quantity,
            amountExVat: stopajExVat,
            totalExVat: stopajExVat * quantity,
          },
        ]
      : []),
    {
      name: 'Kargo Masrafı',
      taxType: 'KDV',
      rate: EXPENSE_VAT_RATE,
      amountIncVat: cargo,
      quantity: quantity,
      totalIncVat: cargo * quantity,
      amountExVat: cargoExVat,
      totalExVat: cargoExVat * quantity,
    },
  ];

  // ===== 5. VAT CALCULATION =====

  // Gross VAT expense (from sale)
  const saleVatAmount = calculateVat(salePriceExVat, vatRate);

  const grossExpense: VatLineItem = {
    name: 'Satış KDV Tutarı',
    type: 'gross',
    rate: vatRate,
    amount: saleVatAmount,
    quantity: quantity,
    total: saleVatAmount * quantity,
  };

  // Deductible VAT items (negative - these reduce VAT liability)
  const incomeItems: VatLineItem[] = [
    {
      name: 'Malzeme Maliyeti',
      type: 'income',
      rate: vatRate,
      amount: -calculateVat(purchaseCostExVat, vatRate),
      quantity: quantity,
      total: -calculateVat(purchaseCostExVat, vatRate) * quantity,
    },
    // OTV VAT - only show if otvRate > 0
    ...(otvRate > 0
      ? [
          {
            name: 'ÖTV',
            type: 'income' as const,
            rate: EXPENSE_VAT_RATE,
            amount: -calculateVat(otvAmount, EXPENSE_VAT_RATE),
            quantity: quantity,
            total: -calculateVat(otvAmount, EXPENSE_VAT_RATE) * quantity,
          },
        ]
      : []),
    {
      name: 'Nakliyat',
      type: 'income',
      rate: EXPENSE_VAT_RATE,
      amount: -calculateVat(shippingExVat, EXPENSE_VAT_RATE),
      quantity: quantity,
      total: -calculateVat(shippingExVat, EXPENSE_VAT_RATE) * quantity,
    },
    {
      name: 'Paketleme Maliyeti',
      type: 'income',
      rate: EXPENSE_VAT_RATE,
      amount: -calculateVat(packagingExVat, EXPENSE_VAT_RATE),
      quantity: quantity,
      total: -calculateVat(packagingExVat, EXPENSE_VAT_RATE) * quantity,
    },
    {
      name: 'Reklam Maliyeti',
      type: 'income',
      rate: EXPENSE_VAT_RATE,
      amount: -calculateVat(advertisingExVat, EXPENSE_VAT_RATE),
      quantity: quantity,
      total: -calculateVat(advertisingExVat, EXPENSE_VAT_RATE) * quantity,
    },
    {
      name: 'Trendyol Komisyon',
      type: 'income',
      rate: EXPENSE_VAT_RATE,
      amount: -calculateVat(commissionExVat, EXPENSE_VAT_RATE),
      quantity: quantity,
      total: -calculateVat(commissionExVat, EXPENSE_VAT_RATE) * quantity,
    },
    {
      name: 'Trendyol Hizmet Bedeli',
      type: 'income',
      rate: EXPENSE_VAT_RATE,
      amount: -calculateVat(platformFeeExVat, EXPENSE_VAT_RATE),
      quantity: quantity,
      total: -calculateVat(platformFeeExVat, EXPENSE_VAT_RATE) * quantity,
    },
    // Stopaj VAT - only show if stopaj > 0
    ...(stopaj > 0
      ? [
          {
            name: 'Stopaj',
            type: 'income' as const,
            rate: EXPENSE_VAT_RATE,
            amount: -calculateVat(stopajExVat, EXPENSE_VAT_RATE),
            quantity: quantity,
            total: -calculateVat(stopajExVat, EXPENSE_VAT_RATE) * quantity,
          },
        ]
      : []),
    {
      name: 'Kargo Masrafı',
      type: 'income',
      rate: EXPENSE_VAT_RATE,
      amount: -calculateVat(cargoExVat, EXPENSE_VAT_RATE),
      quantity: quantity,
      total: -calculateVat(cargoExVat, EXPENSE_VAT_RATE) * quantity,
    },
  ];

  // Net VAT expense
  const totalDeductibleVat = incomeItems.reduce((sum, item) => sum + item.total, 0);
  const netVatExpense = grossExpense.total + totalDeductibleVat;

  const netExpense: VatLineItem = {
    name: 'Satış KDV Tutarı',
    type: 'net',
    rate: 0,
    amount: netVatExpense,
    quantity: quantity,
    total: netVatExpense,
  };

  // ===== 6. PROFIT CALCULATION =====

  // Total costs excluding VAT (per unit × quantity)
  const totalCostExVat =
    (purchaseCostExVat +
      otvAmount +
      shippingExVat +
      packagingExVat +
      advertisingExVat +
      commissionExVat +
      platformFeeExVat +
      stopajExVat +
      cargoExVat) *
    quantity;

  // Gross profit
  const grossProfit = salePriceExVat * quantity - totalCostExVat;

  // Income tax
  const incomeTax = grossProfit > 0 ? grossProfit * incomeTaxRate : 0;

  // Net profit
  const netProfit = grossProfit - incomeTax;

  // ===== 7. RESULT METRICS =====

  // Margin: Net Profit / Sale Price (inc VAT) × 100
  const margin = salePrice > 0 ? (netProfit / (salePrice * quantity)) * 100 : 0;

  // ROI: Net Profit / Total Investment × 100
  const totalInvestment =
    (purchaseCost + (otvRate > 0 ? otvIncVat : 0) + shipping + packaging + advertisingIncVat) * quantity;
  const roi = totalInvestment > 0 ? (netProfit / totalInvestment) * 100 : 0;

  // ===== 8. SUGGESTED SALE PRICE =====
  const suggestedSalePrice =
    targetMargin > 0 ? calculateSuggestedPrice(input, targetMargin) : 0;

  return {
    salePriceIncVat: round2(salePrice),
    salePriceExVat: round2(salePriceExVat),
    costs: costs.map((cost) => ({
      ...cost,
      amountIncVat: round2(cost.amountIncVat),
      totalIncVat: round2(cost.totalIncVat),
      amountExVat: round2(cost.amountExVat),
      totalExVat: round2(cost.totalExVat),
    })),
    vatCalculation: {
      grossExpense: {
        ...grossExpense,
        amount: round2(grossExpense.amount),
        total: round2(grossExpense.total),
      },
      incomeItems: incomeItems.map((item) => ({
        ...item,
        amount: round2(item.amount),
        total: round2(item.total),
      })),
      netExpense: {
        ...netExpense,
        amount: round2(netExpense.amount),
        total: round2(netExpense.total),
      },
    },
    grossProfit: round2(grossProfit),
    incomeTax: round2(incomeTax),
    netProfit: round2(netProfit),
    margin: round2(margin),
    roi: round2(roi),
    totalCostExVat: round2(totalCostExVat),
    totalInvestment: round2(totalInvestment),
    suggestedSalePrice: round2(suggestedSalePrice),
  };
}

/**
 * Format currency for display
 */
export function formatCurrency(value: number): string {
  return new Intl.NumberFormat('tr-TR', {
    style: 'currency',
    currency: 'TRY',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

/**
 * Format percentage for display
 */
export function formatPercentage(value: number): string {
  return `%${value.toFixed(2)}`;
}
