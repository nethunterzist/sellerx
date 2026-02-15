import { describe, it, expect, vi } from "vitest";
import { render, screen } from "../../helpers/test-utils";
import { CrossSellProductSelector } from "@/components/qa/cross-sell/cross-sell-product-selector";
import type { RecommendedProduct } from "@/types/cross-sell";

// Mock next/image
vi.mock("next/image", () => ({
  default: ({ src, alt, ...props }: any) => (
    <img src={src} alt={alt} data-testid="next-image" {...props} />
  ),
}));

// Mock the useProductSearch hook
const mockSearchResults: any[] = [];
vi.mock("@/hooks/queries/use-cross-sell", () => ({
  useProductSearch: () => ({
    data: mockSearchResults,
    isLoading: false,
  }),
}));

function createMockProduct(
  overrides: Partial<RecommendedProduct> = {}
): RecommendedProduct {
  return {
    barcode: "ABC123",
    title: "Test Product",
    image: "https://example.com/img.jpg",
    salePrice: 99.9,
    displayOrder: 0,
    ...overrides,
  };
}

describe("CrossSellProductSelector", () => {
  const defaultProps = {
    storeId: "store-1",
    selectedProducts: [] as RecommendedProduct[],
    onProductsChange: vi.fn(),
    maxProducts: 5,
  };

  it("should render the search input", () => {
    render(<CrossSellProductSelector {...defaultProps} />);

    const input = screen.getByRole("textbox");
    expect(input).toBeInTheDocument();
  });

  it("should render selected products", () => {
    const selectedProducts = [
      createMockProduct({ barcode: "P1", title: "Product One" }),
      createMockProduct({ barcode: "P2", title: "Product Two" }),
    ];

    render(
      <CrossSellProductSelector
        {...defaultProps}
        selectedProducts={selectedProducts}
      />
    );

    expect(screen.getByText("Product One")).toBeInTheDocument();
    expect(screen.getByText("Product Two")).toBeInTheDocument();
  });

  it("should show selected count text", () => {
    const selectedProducts = [
      createMockProduct({ barcode: "P1", title: "Product One" }),
    ];

    render(
      <CrossSellProductSelector
        {...defaultProps}
        selectedProducts={selectedProducts}
      />
    );

    // The text uses i18n key: "selectedCount" with count and max params
    // Since next-intl is mocked to return the key, we check for the key
    expect(screen.getByText(/selectedCount/)).toBeInTheDocument();
  });

  it("should disable search input when at product limit", () => {
    const selectedProducts = Array.from({ length: 5 }, (_, i) =>
      createMockProduct({
        barcode: `P${i}`,
        title: `Product ${i}`,
        displayOrder: i,
      })
    );

    render(
      <CrossSellProductSelector
        {...defaultProps}
        selectedProducts={selectedProducts}
        maxProducts={5}
      />
    );

    const input = screen.getByRole("textbox");
    expect(input).toBeDisabled();
  });

  it("should not disable search input when under limit", () => {
    render(
      <CrossSellProductSelector
        {...defaultProps}
        selectedProducts={[]}
        maxProducts={5}
      />
    );

    const input = screen.getByRole("textbox");
    expect(input).not.toBeDisabled();
  });

  it("should not render selected products section when empty", () => {
    render(
      <CrossSellProductSelector {...defaultProps} selectedProducts={[]} />
    );

    // Should not have selectedCount text
    expect(screen.queryByText(/selectedCount/)).not.toBeInTheDocument();
  });
});
