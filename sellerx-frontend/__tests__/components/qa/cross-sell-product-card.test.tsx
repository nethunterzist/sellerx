import { describe, it, expect, vi } from "vitest";
import { render, screen } from "../../helpers/test-utils";
import { CrossSellProductCard } from "@/components/qa/cross-sell/cross-sell-product-card";
import type { RecommendedProduct } from "@/types/cross-sell";

// Mock next/image
vi.mock("next/image", () => ({
  default: ({ src, alt, ...props }: any) => (
    <img src={src} alt={alt} data-testid="next-image" {...props} />
  ),
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

describe("CrossSellProductCard", () => {
  it("should render product title and barcode", () => {
    const product = createMockProduct();
    render(<CrossSellProductCard product={product} />);

    expect(screen.getByText("Test Product")).toBeInTheDocument();
    expect(screen.getByText(/ABC123/)).toBeInTheDocument();
  });

  it("should render product price formatted with TL", () => {
    const product = createMockProduct({ salePrice: 149.5 });
    render(<CrossSellProductCard product={product} />);

    expect(screen.getByText(/149\.50 TL/)).toBeInTheDocument();
  });

  it("should render product image when provided", () => {
    const product = createMockProduct({
      image: "https://example.com/product.jpg",
    });
    render(<CrossSellProductCard product={product} />);

    const img = screen.getByTestId("next-image");
    expect(img).toHaveAttribute("src", "https://example.com/product.jpg");
  });

  it("should render placeholder when image is null", () => {
    const product = createMockProduct({ image: null });
    render(<CrossSellProductCard product={product} />);

    expect(screen.getByText("?")).toBeInTheDocument();
  });

  it("should render remove button when onRemove is provided", () => {
    const onRemove = vi.fn();
    const product = createMockProduct();
    render(<CrossSellProductCard product={product} onRemove={onRemove} />);

    const removeButton = screen.getByRole("button");
    expect(removeButton).toBeInTheDocument();
  });

  it("should call onRemove when remove button is clicked", async () => {
    const onRemove = vi.fn();
    const product = createMockProduct();
    render(<CrossSellProductCard product={product} onRemove={onRemove} />);

    const removeButton = screen.getByRole("button");
    removeButton.click();

    expect(onRemove).toHaveBeenCalledOnce();
  });

  it("should NOT render remove button when readonly is true", () => {
    const onRemove = vi.fn();
    const product = createMockProduct();
    render(
      <CrossSellProductCard
        product={product}
        onRemove={onRemove}
        readonly={true}
      />
    );

    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });

  it("should NOT render remove button when onRemove is not provided", () => {
    const product = createMockProduct();
    render(<CrossSellProductCard product={product} />);

    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });

  it("should apply compact class when compact is true", () => {
    const product = createMockProduct();
    const { container } = render(
      <CrossSellProductCard product={product} compact={true} />
    );

    const card = container.firstChild as HTMLElement;
    expect(card.className).toContain("p-1.5");
  });
});
