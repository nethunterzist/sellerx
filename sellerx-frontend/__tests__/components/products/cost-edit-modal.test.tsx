import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "../../helpers/test-utils";
import { CostEditModal } from "@/components/products/cost-edit-modal";
import type { TrendyolProduct } from "@/types/product";

// Mock the useUpdateProductCostAndStock hook
const mockMutate = vi.fn();
vi.mock("@/hooks/queries/use-products", () => ({
  useUpdateProductCostAndStock: () => ({
    mutate: mockMutate,
    isPending: false,
  }),
}));

// Mock CostHistoryTimeline since it may have its own dependencies
vi.mock("@/components/products/cost-history-timeline", () => ({
  CostHistoryTimeline: () => (
    <div data-testid="cost-history-timeline">Cost History</div>
  ),
}));

// Create a minimal mock product matching TrendyolProduct shape
function createMockProduct(overrides: Partial<TrendyolProduct> = {}): TrendyolProduct {
  return {
    id: "product-1",
    storeId: "store-1",
    productId: "12345",
    barcode: "8680001234567",
    title: "Test Urun - Siyah T-Shirt",
    brand: "TestBrand",
    categoryName: "Giyim",
    salePrice: 299.99,
    listPrice: 399.99,
    trendyolQuantity: 50,
    onSale: true,
    commissionRate: 15.0,
    vatRate: 20,
    costAndStockInfo: [],
    lastCommissionRate: 15.0,
    ...overrides,
  } as TrendyolProduct;
}

describe("CostEditModal", () => {
  beforeEach(() => {
    mockMutate.mockClear();
  });

  it("should render modal with product title when open", () => {
    const product = createMockProduct();

    render(
      <CostEditModal product={product} open={true} onOpenChange={vi.fn()} />
    );

    expect(screen.getByText("Maliyet & Stok Yönetimi")).toBeInTheDocument();
    expect(screen.getByText("Test Urun - Siyah T-Shirt")).toBeInTheDocument();
  });

  it("should render product info summary section", () => {
    const product = createMockProduct({
      salePrice: 299.99,
      trendyolQuantity: 50,
      commissionRate: 15.0,
    });

    render(
      <CostEditModal product={product} open={true} onOpenChange={vi.fn()} />
    );

    expect(screen.getByText("Satış Fiyatı")).toBeInTheDocument();
    expect(screen.getByText("Trendyol Stok")).toBeInTheDocument();
    expect(screen.getByText("Komisyon")).toBeInTheDocument();
    expect(screen.getByText("50")).toBeInTheDocument();
    expect(screen.getByText("%15")).toBeInTheDocument();
  });

  it("should render the add cost form fields", () => {
    const product = createMockProduct();

    render(
      <CostEditModal product={product} open={true} onOpenChange={vi.fn()} />
    );

    expect(screen.getByText("Yeni Maliyet Ekle")).toBeInTheDocument();
    expect(screen.getByText("Adet")).toBeInTheDocument();
    expect(screen.getByText("Birim Maliyet (TL)")).toBeInTheDocument();
    expect(screen.getByText("KDV Oranı")).toBeInTheDocument();
    expect(screen.getByText("Stok Tarihi")).toBeInTheDocument();
  });

  it("should render submit and cancel buttons", () => {
    const product = createMockProduct();

    render(
      <CostEditModal product={product} open={true} onOpenChange={vi.fn()} />
    );

    expect(screen.getByText("Kaydet")).toBeInTheDocument();
    expect(screen.getByText("İptal")).toBeInTheDocument();
  });

  it("should render cost history timeline component", () => {
    const product = createMockProduct();

    render(
      <CostEditModal product={product} open={true} onOpenChange={vi.fn()} />
    );

    expect(screen.getByTestId("cost-history-timeline")).toBeInTheDocument();
  });

  it("should not render anything when modal is closed", () => {
    const product = createMockProduct();

    render(
      <CostEditModal product={product} open={false} onOpenChange={vi.fn()} />
    );

    expect(screen.queryByText("Maliyet & Stok Yönetimi")).not.toBeInTheDocument();
  });
});
