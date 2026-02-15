import { describe, it, expect, vi } from "vitest";
import { render, screen } from "../../helpers/test-utils";
import { CrossSellRuleCard } from "@/components/qa/cross-sell/cross-sell-rule-card";
import type { CrossSellRule } from "@/types/cross-sell";

// Mock the DropdownMenu components since Radix portals can be complex in tests
vi.mock("@/components/ui/dropdown-menu", () => ({
  DropdownMenu: ({ children }: any) => <div>{children}</div>,
  DropdownMenuTrigger: ({ children }: any) => <div>{children}</div>,
  DropdownMenuContent: ({ children }: any) => <div>{children}</div>,
  DropdownMenuItem: ({ children, onClick }: any) => (
    <button onClick={onClick}>{children}</button>
  ),
}));

function createMockRule(
  overrides: Partial<CrossSellRule> = {}
): CrossSellRule {
  return {
    id: "rule-1",
    storeId: "store-1",
    name: "Test Rule",
    description: "A test cross-sell rule",
    triggerConditions: [
      { type: "KEYWORD", value: "beden" },
      { type: "KEYWORD", value: "renk" },
    ],
    recommendationType: "COMPLEMENTARY",
    recommendedProducts: [
      {
        barcode: "P001",
        title: "Product 1",
        image: null,
        salePrice: 100,
        displayOrder: 0,
      },
    ],
    maxRecommendations: 3,
    messageTemplate: null,
    status: "ACTIVE",
    priority: 1,
    impressionCount: 150,
    clickCount: 30,
    conversionCount: 10,
    conversionRate: 0.067,
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-15T00:00:00Z",
    ...overrides,
  };
}

describe("CrossSellRuleCard", () => {
  const defaultProps = {
    onEdit: vi.fn(),
    onDelete: vi.fn(),
    onToggleStatus: vi.fn(),
  };

  it("should render rule name", () => {
    const rule = createMockRule({ name: "My Cross-Sell Rule" });
    render(<CrossSellRuleCard rule={rule} {...defaultProps} />);

    expect(screen.getByText("My Cross-Sell Rule")).toBeInTheDocument();
  });

  it("should render rule description", () => {
    const rule = createMockRule({ description: "Rule description here" });
    render(<CrossSellRuleCard rule={rule} {...defaultProps} />);

    expect(screen.getByText("Rule description here")).toBeInTheDocument();
  });

  it("should not render description when null", () => {
    const rule = createMockRule({ description: null });
    render(<CrossSellRuleCard rule={rule} {...defaultProps} />);

    expect(screen.queryByText("Rule description here")).not.toBeInTheDocument();
  });

  it("should render trigger condition badges", () => {
    const rule = createMockRule({
      triggerConditions: [
        { type: "KEYWORD", value: "beden" },
        { type: "KEYWORD", value: "renk" },
      ],
    });
    render(<CrossSellRuleCard rule={rule} {...defaultProps} />);

    expect(screen.getByText("beden")).toBeInTheDocument();
    expect(screen.getByText("renk")).toBeInTheDocument();
  });

  it("should show overflow count when more than 3 trigger conditions", () => {
    const rule = createMockRule({
      triggerConditions: [
        { type: "KEYWORD", value: "a" },
        { type: "KEYWORD", value: "b" },
        { type: "KEYWORD", value: "c" },
        { type: "KEYWORD", value: "d" },
        { type: "KEYWORD", value: "e" },
      ],
    });
    render(<CrossSellRuleCard rule={rule} {...defaultProps} />);

    // Should show first 3 + overflow badge
    expect(screen.getByText("a")).toBeInTheDocument();
    expect(screen.getByText("b")).toBeInTheDocument();
    expect(screen.getByText("c")).toBeInTheDocument();
    expect(screen.getByText("+2")).toBeInTheDocument();
  });

  it("should render recommendation type badge", () => {
    const rule = createMockRule({ recommendationType: "COMPLEMENTARY" });
    render(<CrossSellRuleCard rule={rule} {...defaultProps} />);

    expect(screen.getByText("Tamamlayici")).toBeInTheDocument();
  });

  it("should render impression count", () => {
    const rule = createMockRule({ impressionCount: 250 });
    render(<CrossSellRuleCard rule={rule} {...defaultProps} />);

    expect(screen.getByText(/250/)).toBeInTheDocument();
  });

  it("should render conversion rate", () => {
    const rule = createMockRule({ conversionRate: 0.125 });
    render(<CrossSellRuleCard rule={rule} {...defaultProps} />);

    expect(screen.getByText(/%12\.5/)).toBeInTheDocument();
  });

  it("should render product count", () => {
    const rule = createMockRule({
      recommendedProducts: [
        {
          barcode: "P1",
          title: "Product 1",
          image: null,
          salePrice: 50,
          displayOrder: 0,
        },
        {
          barcode: "P2",
          title: "Product 2",
          image: null,
          salePrice: 75,
          displayOrder: 1,
        },
      ],
    });
    render(<CrossSellRuleCard rule={rule} {...defaultProps} />);

    expect(screen.getByText(/2/)).toBeInTheDocument();
  });

  it("should apply reduced opacity when inactive", () => {
    const rule = createMockRule({ status: "INACTIVE" });
    const { container } = render(
      <CrossSellRuleCard rule={rule} {...defaultProps} />
    );

    // Find the Card element (first child)
    const card = container.firstChild as HTMLElement;
    expect(card.className).toContain("opacity-60");
  });

  it("should not apply reduced opacity when active", () => {
    const rule = createMockRule({ status: "ACTIVE" });
    const { container } = render(
      <CrossSellRuleCard rule={rule} {...defaultProps} />
    );

    const card = container.firstChild as HTMLElement;
    expect(card.className).not.toContain("opacity-60");
  });
});
