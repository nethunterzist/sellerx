"use client";

import { Search, X } from "lucide-react";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import type { ExpenseFrequency } from "@/types/expense";
import { frequencyLabels } from "@/types/expense";

export interface ExpenseFilters {
  category: string;
  frequency: string;
  search: string;
}

interface ExpenseFiltersProps {
  filters: ExpenseFilters;
  onFiltersChange: (filters: ExpenseFilters) => void;
  categories: string[];
}

const ALL_VALUE = "all";

export function ExpenseFiltersComponent({
  filters,
  onFiltersChange,
  categories,
}: ExpenseFiltersProps) {
  const handleCategoryChange = (value: string) => {
    onFiltersChange({ ...filters, category: value });
  };

  const handleFrequencyChange = (value: string) => {
    onFiltersChange({ ...filters, frequency: value });
  };

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onFiltersChange({ ...filters, search: e.target.value });
  };

  const handleClearFilters = () => {
    onFiltersChange({ category: ALL_VALUE, frequency: ALL_VALUE, search: "" });
  };

  const hasActiveFilters =
    filters.category !== ALL_VALUE ||
    filters.frequency !== ALL_VALUE ||
    filters.search !== "";

  return (
    <div className="flex flex-col sm:flex-row gap-3">
        {/* Category Filter */}
        <div className="flex-1 min-w-[140px]">
          <Select value={filters.category} onValueChange={handleCategoryChange}>
            <SelectTrigger className="w-full border border-gray-300 dark:border-gray-600 rounded-lg bg-background">
              <SelectValue placeholder="Kategori" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL_VALUE}>Tüm Kategoriler</SelectItem>
              {categories.map((category) => (
                <SelectItem key={category} value={category}>
                  {category}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* Frequency Filter */}
        <div className="flex-1 min-w-[140px]">
          <Select value={filters.frequency} onValueChange={handleFrequencyChange}>
            <SelectTrigger className="w-full border border-gray-300 dark:border-gray-600 rounded-lg bg-background">
              <SelectValue placeholder="Sıklık" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL_VALUE}>Tüm Sıklıklar</SelectItem>
              {(Object.keys(frequencyLabels) as ExpenseFrequency[]).map((freq) => (
                <SelectItem key={freq} value={freq}>
                  {frequencyLabels[freq]}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* Search Input */}
        <div className="flex-[2] min-w-[200px]">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Gider adı ara..."
              value={filters.search}
              onChange={handleSearchChange}
              className="pl-9 border border-gray-300 dark:border-gray-600 rounded-lg bg-background"
            />
          </div>
        </div>

        {/* Clear Filters Button */}
        {hasActiveFilters && (
          <Button
            variant="ghost"
            size="icon"
            onClick={handleClearFilters}
            className="shrink-0"
            title="Filtreleri Temizle"
          >
            <X className="h-4 w-4" />
          </Button>
        )}
    </div>
  );
}
