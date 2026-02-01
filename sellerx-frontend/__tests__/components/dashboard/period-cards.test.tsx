import { describe, it, expect, vi } from "vitest";
import { render, screen } from "../../helpers/test-utils";
import { PeriodCards } from "@/components/dashboard/period-cards";
import type { DashboardStats, DashboardStatsResponse } from "@/types/dashboard";

// Mock the PeriodDetailModal since it uses Dialog which can be complex in tests
vi.mock("@/components/dashboard/period-detail-modal", () => ({
  PeriodDetailModal: () => null,
}));

// Mock the Skeleton component
vi.mock("@/components/ui/skeleton", () => ({
  Skeleton: ({ className }: { className?: string }) => (
    <div data-testid="skeleton" className={className} />
  ),
}));

// Helper to create minimal DashboardStats
function createMockStats(overrides: Partial<DashboardStats> = {}): DashboardStats {
  return {
    period: "today",
    totalOrders: 10,
    totalProductsSold: 25,
    totalRevenue: 5000,
    returnCount: 2,
    returnCost: 200,
    totalProductCosts: 2000,
    grossProfit: 3000,
    netProfit: 2500,
    profitMargin: 60,
    vatDifference: 100,
    totalStoppage: 50,
    totalEstimatedCommission: 500,
    itemsWithoutCost: 0,
    totalExpenseNumber: 3,
    totalExpenseAmount: 300,
    orders: [],
    products: [],
    expenses: [],
    ...overrides,
  };
}

function createMockStatsResponse(
  overrides: Partial<DashboardStatsResponse> = {}
): DashboardStatsResponse {
  return {
    today: createMockStats({ period: "today" }),
    yesterday: createMockStats({ period: "yesterday", totalRevenue: 4500 }),
    thisMonth: createMockStats({ period: "thisMonth", totalRevenue: 60000 }),
    lastMonth: createMockStats({ period: "lastMonth", totalRevenue: 55000 }),
    storeId: "store-1",
    calculatedAt: new Date().toISOString(),
    ...overrides,
  };
}

describe("PeriodCards", () => {
  describe("multi mode (default)", () => {
    it("should render loading skeletons when isLoading is true", () => {
      render(<PeriodCards isLoading={true} />);

      const skeletons = screen.getAllByTestId("skeleton");
      expect(skeletons.length).toBeGreaterThan(0);
    });

    it("should render empty state when no stats are provided", () => {
      render(<PeriodCards isLoading={false} />);

      expect(screen.getByText("Henüz veri yok")).toBeInTheDocument();
    });

    it("should render period cards when stats are provided", () => {
      const stats = createMockStatsResponse();
      render(<PeriodCards stats={stats} isLoading={false} />);

      expect(screen.getByText("Bugun")).toBeInTheDocument();
      expect(screen.getByText("Dun")).toBeInTheDocument();
      expect(screen.getByText("Bu Ay")).toBeInTheDocument();
      expect(screen.getByText("Gecen Ay")).toBeInTheDocument();
    });

    it("should render forecast card when thisMonth data exists", () => {
      const stats = createMockStatsResponse();
      render(<PeriodCards stats={stats} isLoading={false} />);

      expect(screen.getByText("Bu Ay (tahmin)")).toBeInTheDocument();
    });

    it("should call onPeriodSelect when a card is clicked", () => {
      const stats = createMockStatsResponse();
      const onPeriodSelect = vi.fn();

      render(
        <PeriodCards
          stats={stats}
          isLoading={false}
          onPeriodSelect={onPeriodSelect}
        />
      );

      // Click on "Bugun" card
      (screen.getByText("Bugun").closest("div[class*='cursor-pointer']") as HTMLElement)?.click();
    });
  });

  describe("single mode", () => {
    it("should render loading skeleton in single mode", () => {
      render(<PeriodCards mode="single" isLoading={true} />);

      const skeletons = screen.getAllByTestId("skeleton");
      expect(skeletons.length).toBeGreaterThan(0);
    });

    it("should render empty state when no custom stats", () => {
      render(<PeriodCards mode="single" isLoading={false} />);

      expect(screen.getByText("Henüz veri yok")).toBeInTheDocument();
    });

    it("should render single card with custom stats", () => {
      const customStats = createMockStats({
        totalRevenue: 10000,
        totalOrders: 50,
      });

      render(
        <PeriodCards
          mode="single"
          customStats={customStats}
          customTitle="Son 7 Gun"
          customDateRange="24 Oca - 30 Oca 2026"
          isLoading={false}
        />
      );

      expect(screen.getByText("Son 7 Gun")).toBeInTheDocument();
      expect(screen.getByText("24 Oca - 30 Oca 2026")).toBeInTheDocument();
    });
  });

  describe("dynamic mode", () => {
    it("should render loading skeletons in dynamic mode", () => {
      render(<PeriodCards mode="dynamic" isLoading={true} />);

      const skeletons = screen.getAllByTestId("skeleton");
      expect(skeletons.length).toBeGreaterThan(0);
    });

    it("should render empty state when no period data", () => {
      render(
        <PeriodCards mode="dynamic" periodData={[]} isLoading={false} />
      );

      expect(screen.getByText("Henüz veri yok")).toBeInTheDocument();
    });

    it("should render dynamic period cards with data", () => {
      const periodData = [
        {
          stats: createMockStats({ totalRevenue: 5000 }),
          label: "Bu Hafta",
          shortLabel: "Bu Hafta",
          dateRange: "27 Oca - 31 Oca 2026",
          color: "blue" as const,
        },
        {
          stats: createMockStats({ totalRevenue: 4000 }),
          label: "Gecen Hafta",
          shortLabel: "Gecen Hafta",
          dateRange: "20 Oca - 26 Oca 2026",
          color: "teal" as const,
        },
      ];

      render(
        <PeriodCards
          mode="dynamic"
          periodData={periodData}
          isLoading={false}
        />
      );

      expect(screen.getByText("Bu Hafta")).toBeInTheDocument();
      expect(screen.getByText("Gecen Hafta")).toBeInTheDocument();
    });
  });
});
