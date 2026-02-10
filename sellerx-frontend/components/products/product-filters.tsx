"use client";

import { useState, useEffect } from "react";
import { Filter, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import type { ProductFilters as ProductFiltersType } from "@/types/product";

interface ProductFiltersProps {
  filters: ProductFiltersType;
  onFiltersChange: (filters: ProductFiltersType) => void;
}

// Count active filters (excluding search which is handled separately)
function countActiveFilters(filters: ProductFiltersType): number {
  let count = 0;
  if (filters.minStock !== undefined) count++;
  if (filters.maxStock !== undefined) count++;
  if (filters.minPrice !== undefined) count++;
  if (filters.maxPrice !== undefined) count++;
  if (filters.minCommission !== undefined) count++;
  if (filters.maxCommission !== undefined) count++;
  if (filters.minCost !== undefined) count++;
  if (filters.maxCost !== undefined) count++;
  return count;
}

// Check if any min > max validation error exists
function hasValidationError(filters: ProductFiltersType): boolean {
  if (filters.minPrice !== undefined && filters.maxPrice !== undefined) {
    if (filters.minPrice > filters.maxPrice) return true;
  }
  if (filters.minStock !== undefined && filters.maxStock !== undefined) {
    if (filters.minStock > filters.maxStock) return true;
  }
  if (filters.minCommission !== undefined && filters.maxCommission !== undefined) {
    if (filters.minCommission > filters.maxCommission) return true;
  }
  if (filters.minCost !== undefined && filters.maxCost !== undefined) {
    if (filters.minCost > filters.maxCost) return true;
  }
  return false;
}

export function ProductFiltersPopover({ filters, onFiltersChange }: ProductFiltersProps) {
  const [open, setOpen] = useState(false);

  // Local state for form inputs
  const [localFilters, setLocalFilters] = useState<ProductFiltersType>(filters);

  // Sync local state when external filters change
  useEffect(() => {
    setLocalFilters(filters);
  }, [filters]);

  const activeFilterCount = countActiveFilters(filters);
  const hasError = hasValidationError(localFilters);

  const handleInputChange = (field: keyof ProductFiltersType, value: string) => {
    const numValue = value === "" ? undefined : parseFloat(value);
    setLocalFilters(prev => ({
      ...prev,
      [field]: numValue,
    }));
  };

  const handleApply = () => {
    onFiltersChange(localFilters);
    setOpen(false);
  };

  const handleClear = () => {
    const clearedFilters: ProductFiltersType = {
      search: filters.search, // Preserve search
    };
    setLocalFilters(clearedFilters);
    onFiltersChange(clearedFilters);
    setOpen(false);
  };

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild>
        <Button
          variant="outline"
          className="gap-2 border-gray-300 dark:border-gray-600 dark:bg-gray-800/50 dark:hover:bg-gray-700/50"
        >
          <Filter className="h-4 w-4" />
          Filtrele
          {activeFilterCount > 0 && (
            <span className="ml-1 flex h-5 w-5 items-center justify-center rounded-full bg-primary text-[10px] font-medium text-primary-foreground">
              {activeFilterCount}
            </span>
          )}
        </Button>
      </PopoverTrigger>
      <PopoverContent className="w-80" align="end">
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h4 className="font-medium text-sm">Filtreler</h4>
            {activeFilterCount > 0 && (
              <button
                onClick={handleClear}
                className="text-xs text-muted-foreground hover:text-foreground flex items-center gap-1"
              >
                <X className="h-3 w-3" />
                Temizle
              </button>
            )}
          </div>

          {/* Price Filter */}
          <div className="space-y-2">
            <Label className="text-xs text-muted-foreground">Fiyat (TL)</Label>
            <div className="flex items-center gap-2">
              <Input
                type="number"
                placeholder="Min"
                value={localFilters.minPrice ?? ""}
                onChange={(e) => handleInputChange("minPrice", e.target.value)}
                className="h-8 text-sm"
              />
              <span className="text-muted-foreground">-</span>
              <Input
                type="number"
                placeholder="Max"
                value={localFilters.maxPrice ?? ""}
                onChange={(e) => handleInputChange("maxPrice", e.target.value)}
                className="h-8 text-sm"
              />
            </div>
          </div>

          {/* Stock Filter */}
          <div className="space-y-2">
            <Label className="text-xs text-muted-foreground">Stok (adet)</Label>
            <div className="flex items-center gap-2">
              <Input
                type="number"
                placeholder="Min"
                value={localFilters.minStock ?? ""}
                onChange={(e) => handleInputChange("minStock", e.target.value)}
                className="h-8 text-sm"
              />
              <span className="text-muted-foreground">-</span>
              <Input
                type="number"
                placeholder="Max"
                value={localFilters.maxStock ?? ""}
                onChange={(e) => handleInputChange("maxStock", e.target.value)}
                className="h-8 text-sm"
              />
            </div>
          </div>

          {/* Commission Filter */}
          <div className="space-y-2">
            <Label className="text-xs text-muted-foreground">Komisyon (%)</Label>
            <div className="flex items-center gap-2">
              <Input
                type="number"
                placeholder="Min"
                value={localFilters.minCommission ?? ""}
                onChange={(e) => handleInputChange("minCommission", e.target.value)}
                className="h-8 text-sm"
              />
              <span className="text-muted-foreground">-</span>
              <Input
                type="number"
                placeholder="Max"
                value={localFilters.maxCommission ?? ""}
                onChange={(e) => handleInputChange("maxCommission", e.target.value)}
                className="h-8 text-sm"
              />
            </div>
          </div>

          {/* Cost Filter */}
          <div className="space-y-2">
            <Label className="text-xs text-muted-foreground">Maliyet (TL)</Label>
            <div className="flex items-center gap-2">
              <Input
                type="number"
                placeholder="Min"
                value={localFilters.minCost ?? ""}
                onChange={(e) => handleInputChange("minCost", e.target.value)}
                className="h-8 text-sm"
              />
              <span className="text-muted-foreground">-</span>
              <Input
                type="number"
                placeholder="Max"
                value={localFilters.maxCost ?? ""}
                onChange={(e) => handleInputChange("maxCost", e.target.value)}
                className="h-8 text-sm"
              />
            </div>
          </div>

          {/* Validation Error Message */}
          {hasError && (
            <p className="text-xs text-destructive">
              Min değeri max değerinden büyük olamaz
            </p>
          )}

          {/* Action Buttons */}
          <div className="flex items-center gap-2 pt-2">
            <Button
              variant="outline"
              size="sm"
              onClick={handleClear}
              className="flex-1"
            >
              Temizle
            </Button>
            <Button
              size="sm"
              onClick={handleApply}
              className="flex-1"
              disabled={hasError}
            >
              Uygula
            </Button>
          </div>
        </div>
      </PopoverContent>
    </Popover>
  );
}
