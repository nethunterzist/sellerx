/**
 * Trendyol Profit Calculator Types
 * Based on Excel calculation model
 */

// VAT rate options (now supports custom rates)
export type VatRate = number;

// Calculator input values
export interface CalculatorInput {
  // Deduction rates
  vatRate: number;               // Product VAT rate (0-1, e.g., 0.20 for 20%)
  commissionRate: number;        // Trendyol commission rate (0-0.50)
  otvRate: number;               // Special Consumption Tax rate (0-1, optional)

  // Purchase
  quantity: number;              // Purchase quantity
  purchaseCost: number;          // Purchase cost including VAT (TL)

  // Sale
  salePrice: number;             // Trendyol sale price including VAT (TL)

  // Other costs (all including VAT)
  shipping: number;              // Shipping to warehouse (TL)
  packaging: number;             // Packaging cost per unit (TL)
  cargo: number;                 // Delivery to customer - desi based (TL)

  // Advertising (optional) - can use ACOS directly or calculate from CPC/CVR
  acos: number;                  // Advertising Cost of Sale (0-1)
  cpc: number;                   // Cost Per Click (TL) - alternative ACOS calculation
  cvr: number;                   // Conversion Rate (0-1) - alternative ACOS calculation
  useManualAcos: boolean;        // true = manual ACOS, false = calculate from CPC/CVR

  // Target profit margin for reverse calculation
  targetMargin: number;          // Target profit margin (0-1, e.g., 0.12 for 12%)

  // Advanced settings
  platformFee: number;           // Platform service fee excluding VAT (default: 2.99 TL)
  stopaj: number;                // Withholding tax amount (TL, excluding VAT)
  incomeTaxRate: number;         // Income tax rate (0-1, default: 0)
}

// Individual cost line item
export interface CostLineItem {
  name: string;                  // Display name
  taxType: string;               // Tax type (KDV, etc.)
  rate: number;                  // Tax rate
  amountIncVat: number;          // Amount including VAT
  quantity: number;              // Quantity
  totalIncVat: number;           // Total including VAT
  amountExVat: number;           // Amount excluding VAT
  totalExVat: number;            // Total excluding VAT
}

// VAT calculation line item
export interface VatLineItem {
  name: string;                  // Display name
  type: 'gross' | 'income' | 'net';  // Brüt Gider / Gelir / Net Gider
  rate: number;                  // VAT rate
  amount: number;                // VAT amount
  quantity: number;              // Quantity
  total: number;                 // Total VAT
}

// Complete calculation result
export interface CalculatorResult {
  // Sale price breakdown
  salePriceIncVat: number;       // Sale price including VAT
  salePriceExVat: number;        // Sale price excluding VAT

  // Cost breakdown table (Excel rows 33-40)
  costs: CostLineItem[];

  // VAT calculation table (Excel rows 42-50)
  vatCalculation: {
    grossExpense: VatLineItem;   // Satış KDV Tutarı (Brüt Gider)
    incomeItems: VatLineItem[];  // All deductible VAT items (Gelir)
    netExpense: VatLineItem;     // Net KDV Gider
  };

  // Profit results (Excel rows 52-59)
  grossProfit: number;           // Toplam Kâr (Brüt)
  incomeTax: number;             // Gelir Vergisi
  netProfit: number;             // Toplam Kâr (Net)
  margin: number;                // Kâr Oranı - Margin (%)
  roi: number;                   // Kâr Oranı - ROI (%)

  // Additional metrics
  totalCostExVat: number;        // Total costs excluding VAT
  totalInvestment: number;       // Total investment for ROI calculation

  // Suggested sale price based on target margin
  suggestedSalePrice: number;    // Recommended sale price to achieve target margin
}

// Default input values
export const DEFAULT_CALCULATOR_INPUT: CalculatorInput = {
  vatRate: 0.20,
  commissionRate: 0.19,
  otvRate: 0,
  quantity: 1,
  purchaseCost: 0,
  salePrice: 0,
  shipping: 0,
  packaging: 0,
  cargo: 0,
  acos: 0,
  cpc: 0,
  cvr: 0,
  useManualAcos: true,
  targetMargin: 0,
  platformFee: 2.99,
  stopaj: 0,
  incomeTaxRate: 0,
};

// VAT rate options for dropdown
export const VAT_RATE_OPTIONS = [
  { value: '0.01', label: '%1' },
  { value: '0.10', label: '%10' },
  { value: '0.20', label: '%20' },
] as const;
