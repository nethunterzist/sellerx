"use client";

import { useState } from "react";
import { cn } from "@/lib/utils";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Search, ChevronDown, Download, MoreHorizontal } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";

interface Product {
  id: string;
  name: string;
  sku: string;
  image?: string;
  cogs: number;
  stock: number;
  marketplace: "trendyol" | "hepsiburada";
  unitsSold: number;
  refunds: number;
  sales: number;
  ads: number;
  sellableReturns: string;
  grossProfit: number;
  netProfit: number;
  margin: number;
  roi: number;
  bsr?: number;
}

interface ProductsTableProps {
  products?: Product[];
  isLoading?: boolean;
}

// Demo products data
const demoProducts: Product[] = [
  {
    id: "1",
    name: "Small Gift Box 5.5*5*2.5cm",
    sku: "SKU 41",
    cogs: 11.99,
    stock: 0,
    marketplace: "trendyol",
    unitsSold: 14,
    refunds: 2,
    sales: 161.86,
    ads: -0.81,
    sellableReturns: "0%",
    grossProfit: 76.94,
    netProfit: 76.94,
    margin: 48,
    roi: 450,
  },
  {
    id: "2",
    name: "Jewelry Packaging Gift Box 2.5*2.5*3cm",
    sku: "SKU 2",
    cogs: 11.99,
    stock: 1613,
    marketplace: "trendyol",
    unitsSold: 11,
    refunds: 0,
    sales: 103.89,
    ads: -3.50,
    sellableReturns: "-",
    grossProfit: 44.20,
    netProfit: 44.20,
    margin: 43,
    roi: 309,
  },
  {
    id: "3",
    name: "Paper Jewelry Earring Storage Box",
    sku: "SKU 78",
    cogs: 11.99,
    stock: 0,
    marketplace: "hepsiburada",
    unitsSold: 7,
    refunds: 0,
    sales: 83.93,
    ads: 0,
    sellableReturns: "-",
    grossProfit: 45.71,
    netProfit: 45.71,
    margin: 54,
    roi: 653,
  },
  {
    id: "4",
    name: "Corrugated Box 11*6*4cm",
    sku: "SKU 76",
    cogs: 23.99,
    stock: 0,
    marketplace: "trendyol",
    unitsSold: 7,
    refunds: 1,
    sales: 173.53,
    ads: 0,
    sellableReturns: "0%",
    grossProfit: 56.74,
    netProfit: 56.74,
    margin: 33,
    roi: 135,
  },
  {
    id: "5",
    name: "Carton Black Boxes 2 pieces",
    sku: "SKU 47",
    cogs: 7.99,
    stock: 0,
    marketplace: "hepsiburada",
    unitsSold: 5,
    refunds: 1,
    sales: 46.95,
    ads: 0,
    sellableReturns: "100%",
    grossProfit: -2.26,
    netProfit: -2.26,
    margin: -5,
    roi: -17,
  },
];

function formatCurrency(value: number): string {
  const formatted = new Intl.NumberFormat("tr-TR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Math.abs(value));
  return value < 0 ? `-${formatted}` : formatted;
}

// Skeleton row for loading state
function ProductRowSkeleton() {
  return (
    <TableRow>
      <TableCell>
        <div className="flex items-start gap-3">
          <Skeleton className="h-12 w-12 rounded flex-shrink-0" />
          <div className="min-w-0 space-y-2">
            <Skeleton className="h-4 w-48" />
            <Skeleton className="h-3 w-32" />
            <Skeleton className="h-3 w-20" />
          </div>
        </div>
      </TableCell>
      <TableCell className="text-right"><Skeleton className="h-4 w-8 ml-auto" /></TableCell>
      <TableCell className="text-right"><Skeleton className="h-4 w-6 ml-auto" /></TableCell>
      <TableCell className="text-right"><Skeleton className="h-4 w-20 ml-auto" /></TableCell>
      <TableCell className="text-right"><Skeleton className="h-4 w-16 ml-auto" /></TableCell>
      <TableCell className="text-right"><Skeleton className="h-4 w-10 ml-auto" /></TableCell>
      <TableCell className="text-right"><Skeleton className="h-4 w-20 ml-auto" /></TableCell>
      <TableCell className="text-right"><Skeleton className="h-4 w-20 ml-auto" /></TableCell>
      <TableCell className="text-right"><Skeleton className="h-4 w-10 ml-auto" /></TableCell>
      <TableCell className="text-right"><Skeleton className="h-4 w-12 ml-auto" /></TableCell>
      <TableCell className="text-right"><Skeleton className="h-4 w-6 ml-auto" /></TableCell>
      <TableCell><Skeleton className="h-6 w-12" /></TableCell>
    </TableRow>
  );
}

export function ProductsTable({ products, isLoading }: ProductsTableProps) {
  const [activeTab, setActiveTab] = useState<"today" | "products" | "orders">("today");
  const [searchQuery, setSearchQuery] = useState("");

  // Don't use demo data - only show real data or empty/loading state
  const filteredProducts = products?.filter(
    (p) =>
      p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.sku.toLowerCase().includes(searchQuery.toLowerCase())
  ) || [];

  return (
    <div className="bg-white rounded-lg border border-[#DDDDDD]">
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b border-[#DDDDDD]">
        {/* Tabs */}
        <div className="flex items-center gap-1">
          {(["today", "products", "orders"] as const).map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={cn(
                "px-3 py-1.5 text-sm font-medium rounded transition-colors",
                activeTab === tab
                  ? "bg-[#E8F1FE] text-[#1D70F1]"
                  : "text-gray-600 hover:bg-gray-100"
              )}
            >
              {tab === "today" ? "Bugün" : tab === "products" ? "Ürünler" : "Sipariş Kalemleri"}
            </button>
          ))}
        </div>

        {/* Actions */}
        <div className="flex items-center gap-2">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <Input
              type="search"
              placeholder="Ürün ara..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="h-8 w-48 pl-9 text-sm"
            />
          </div>

          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="outline" size="sm" className="h-8 gap-1.5">
                Grupla
                <ChevronDown className="h-3.5 w-3.5" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem>Ürün</DropdownMenuItem>
              <DropdownMenuItem>SKU</DropdownMenuItem>
              <DropdownMenuItem>Kategori</DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>

          <Button variant="outline" size="icon" className="h-8 w-8">
            <Download className="h-4 w-4" />
          </Button>
        </div>
      </div>

      {/* Table */}
      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow className="hover:bg-transparent">
              <TableHead className="w-[300px]">Ürün</TableHead>
              <TableHead className="text-right">Satılan</TableHead>
              <TableHead className="text-right">İade</TableHead>
              <TableHead className="text-right">Satış</TableHead>
              <TableHead className="text-right">Reklam</TableHead>
              <TableHead className="text-right">Satılabilir İade</TableHead>
              <TableHead className="text-right">Brüt Kâr</TableHead>
              <TableHead className="text-right">Net Kâr</TableHead>
              <TableHead className="text-right">Marj</TableHead>
              <TableHead className="text-right">ROI</TableHead>
              <TableHead className="text-right">BSR</TableHead>
              <TableHead className="w-[50px]"></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <>
                <ProductRowSkeleton />
                <ProductRowSkeleton />
                <ProductRowSkeleton />
                <ProductRowSkeleton />
                <ProductRowSkeleton />
              </>
            ) : filteredProducts.length === 0 ? (
              <TableRow>
                <TableCell colSpan={12} className="h-24 text-center text-gray-500">
                  {searchQuery ? "Arama sonucu bulunamadı" : "Henüz ürün verisi yok"}
                </TableCell>
              </TableRow>
            ) : (
              filteredProducts.map((product) => (
                <TableRow key={product.id} className="hover:bg-gray-50">
                  <TableCell>
                    <div className="flex items-start gap-3">
                      {/* Product Image */}
                      {product.image ? (
                        <img
                          src={product.image}
                          alt={product.name}
                          className="h-12 w-12 rounded object-cover flex-shrink-0 border border-gray-200"
                          onError={(e) => {
                            (e.target as HTMLImageElement).src = "https://via.placeholder.com/48?text=No+Image";
                          }}
                        />
                      ) : (
                        <div
                          className={cn(
                            "h-12 w-12 rounded flex items-center justify-center text-xs font-bold text-white flex-shrink-0",
                            product.marketplace === "trendyol"
                              ? "bg-[#F27A1A]"
                              : "bg-[#FF6000]"
                          )}
                        >
                          {product.marketplace === "trendyol" ? "T" : "H"}
                        </div>
                      )}
                      <div className="min-w-0">
                        <p className="font-medium text-sm text-gray-900 truncate">
                          {product.name}
                        </p>
                        <p className="text-xs text-gray-500">
                          {product.sku} - Maliyet: {formatCurrency(product.cogs)} TL
                        </p>
                        <p className="text-xs text-gray-400">
                          Stok: {product.stock}
                        </p>
                      </div>
                    </div>
                  </TableCell>
                  <TableCell className="text-right font-medium">
                    {product.unitsSold}
                  </TableCell>
                  <TableCell className="text-right">
                    <span className={product.refunds > 0 ? "text-[#1D70F1]" : ""}>
                      {product.refunds}
                    </span>
                  </TableCell>
                  <TableCell className="text-right font-medium">
                    {formatCurrency(product.sales)} TL
                  </TableCell>
                  <TableCell className="text-right">
                    <span className={product.ads < 0 ? "text-red-600" : ""}>
                      {formatCurrency(product.ads)} TL
                    </span>
                  </TableCell>
                  <TableCell className="text-right text-gray-500">
                    {product.sellableReturns}
                  </TableCell>
                  <TableCell className="text-right">
                    <span className={product.grossProfit >= 0 ? "text-green-600" : "text-red-600"}>
                      {formatCurrency(product.grossProfit)} TL
                    </span>
                  </TableCell>
                  <TableCell className="text-right">
                    <span className={product.netProfit >= 0 ? "text-green-600" : "text-red-600"}>
                      {formatCurrency(product.netProfit)} TL
                    </span>
                  </TableCell>
                  <TableCell className="text-right">
                    <span className={product.margin >= 0 ? "text-green-600" : "text-red-600"}>
                      {product.margin}%
                    </span>
                  </TableCell>
                  <TableCell className="text-right">
                    <span className={product.roi >= 0 ? "text-green-600" : "text-red-600"}>
                      {product.roi}%
                    </span>
                  </TableCell>
                  <TableCell className="text-right text-gray-500">
                    {product.bsr || "-"}
                  </TableCell>
                  <TableCell>
                    <Button variant="ghost" size="sm" className="h-8 text-[#1D70F1]">
                      Detay
                    </Button>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {/* Footer */}
      {filteredProducts.length > 5 && (
        <div className="p-4 border-t border-[#DDDDDD] text-center">
          <Button variant="link" className="text-[#1D70F1]">
            Tüm {filteredProducts.length} ürünü görüntüle
          </Button>
        </div>
      )}
    </div>
  );
}
