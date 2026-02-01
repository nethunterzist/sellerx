import { useQuery } from "@tanstack/react-query";
import { apiRequest } from "@/lib/api/client";
import type { CityStatsResponse } from "@/types/city-stats";

interface UseCityStatsParams {
  storeId: string | undefined;
  startDate: string | undefined;
  endDate: string | undefined;
  productBarcode?: string;
}

// City Stats Query Keys
export const cityStatsKeys = {
  all: ["city-stats"] as const,
  byStore: (storeId: string, startDate: string, endDate: string, productBarcode?: string) =>
    [...cityStatsKeys.all, storeId, startDate, endDate, productBarcode] as const,
};

export function useCityStats({ storeId, startDate, endDate, productBarcode }: UseCityStatsParams) {
  return useQuery<CityStatsResponse>({
    queryKey: cityStatsKeys.byStore(storeId || "", startDate || "", endDate || "", productBarcode),
    queryFn: async () => {
      const params = new URLSearchParams();
      if (startDate) params.set("startDate", startDate);
      if (endDate) params.set("endDate", endDate);
      if (productBarcode) params.set("productBarcode", productBarcode);

      return apiRequest<CityStatsResponse>(
        `/dashboard/stats/${storeId}/cities?${params.toString()}`
      );
    },
    enabled: !!storeId && !!startDate && !!endDate,
    staleTime: 5 * 60 * 1000, // 5 dakika
    gcTime: 15 * 60 * 1000, // 15 dakika
    refetchOnWindowFocus: false,
  });
}
