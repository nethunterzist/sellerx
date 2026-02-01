"use client";

import { useState, useMemo } from "react";
import dynamic from "next/dynamic";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { MapPin, TrendingUp, Package, AlertCircle } from "lucide-react";
import { cn } from "@/lib/utils";
import type { CityStats, CityStatsResponse } from "@/types/city-stats";
import { CITY_NAME_TO_CODE } from "@/types/city-stats";
import { useCurrency } from "@/lib/contexts/currency-context";

// Dynamically import TurkeyMap to avoid SSR issues
const TurkeyMap = dynamic(() => import("turkey-map-react").then((mod) => mod.default), {
  ssr: false,
  loading: () => (
    <div className="flex items-center justify-center h-[400px] bg-muted rounded-lg">
      <Skeleton className="w-full h-full" />
    </div>
  ),
});

interface CitiesViewProps {
  cityStats: CityStatsResponse | undefined;
  isLoading: boolean;
}

function formatNumber(value: number): string {
  return new Intl.NumberFormat("tr-TR").format(value);
}

// Color scale based on order count
function getColorForValue(value: number, maxValue: number): string {
  if (maxValue === 0) return "#E3F2FD"; // Light blue for no data

  const ratio = value / maxValue;

  // Color scale from light blue to dark blue
  if (ratio === 0) return "#E3F2FD";
  if (ratio < 0.1) return "#BBDEFB";
  if (ratio < 0.2) return "#90CAF9";
  if (ratio < 0.3) return "#64B5F6";
  if (ratio < 0.4) return "#42A5F5";
  if (ratio < 0.5) return "#2196F3";
  if (ratio < 0.6) return "#1E88E5";
  if (ratio < 0.7) return "#1976D2";
  if (ratio < 0.8) return "#1565C0";
  if (ratio < 0.9) return "#0D47A1";
  return "#0D47A1"; // Darkest blue
}

export function CitiesView({ cityStats, isLoading }: CitiesViewProps) {
  const { formatCurrency } = useCurrency();
  const [hoveredCity, setHoveredCity] = useState<string | null>(null);
  const [showAllCities, setShowAllCities] = useState(false);
  const [mousePosition, setMousePosition] = useState({ x: 0, y: 0 });
  const [isHoveringMap, setIsHoveringMap] = useState(false);

  // Create a map of city name to stats
  const cityStatsMap = useMemo(() => {
    if (!cityStats?.cities) return new Map<string, CityStats>();

    const map = new Map<string, CityStats>();
    cityStats.cities.forEach((city) => {
      // Normalize city name for matching
      const normalizedName = city.cityName?.toLowerCase().trim();
      if (normalizedName) {
        map.set(normalizedName, city);
      }
    });
    return map;
  }, [cityStats]);

  // Get max order count for color scaling
  const maxOrders = useMemo(() => {
    if (!cityStats?.cities || cityStats.cities.length === 0) return 0;
    return Math.max(...cityStats.cities.map((c) => c.totalOrders));
  }, [cityStats]);

  // Get hovered city stats
  const hoveredCityStats = useMemo(() => {
    if (!hoveredCity) return null;
    const normalizedName = hoveredCity.toLowerCase().trim();
    return cityStatsMap.get(normalizedName) || null;
  }, [hoveredCity, cityStatsMap]);

  // Generate city data for the map
  const cityData = useMemo(() => {
    const data: Record<string, { value: number }> = {};

    cityStats?.cities.forEach((city) => {
      // Try to find city code from name
      const cityCode = city.cityCode || CITY_NAME_TO_CODE[city.cityName];
      if (cityCode) {
        // turkey-map-react uses city plate codes as keys
        data[cityCode.toString()] = { value: city.totalOrders };
      }
    });

    return data;
  }, [cityStats]);

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Card>
          <CardHeader>
            <Skeleton className="h-6 w-48" />
          </CardHeader>
          <CardContent>
            <Skeleton className="h-[400px] w-full" />
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <Skeleton className="h-6 w-48" />
          </CardHeader>
          <CardContent>
            <Skeleton className="h-[300px] w-full" />
          </CardContent>
        </Card>
      </div>
    );
  }

  if (!cityStats || cityStats.cities.length === 0) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center justify-center py-12">
          <MapPin className="h-12 w-12 text-muted-foreground/50 mb-4" />
          <h3 className="text-lg font-medium text-foreground">Şehir verisi yok</h3>
          <p className="text-sm text-muted-foreground mt-1">
            Seçili dönemde şehir bilgisi olan sipariş bulunamadı.
          </p>
          {cityStats?.ordersWithoutCity && cityStats.ordersWithoutCity > 0 && (
            <p className="text-xs text-amber-600 mt-2 flex items-center gap-1">
              <AlertCircle className="h-3 w-3" />
              {cityStats.ordersWithoutCity} sipariş şehir bilgisi olmadan kaydedilmiş
            </p>
          )}
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      {/* Map Card */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="text-lg flex items-center gap-2">
            <MapPin className="h-5 w-5 text-[#1D70F1]" />
            Türkiye Sipariş Haritası
          </CardTitle>
          <div className="text-sm text-muted-foreground">
            {cityStats.totalCities} şehirden sipariş
          </div>
        </CardHeader>
        <CardContent>
          <div
            className="relative"
            onMouseEnter={() => setIsHoveringMap(true)}
            onMouseLeave={() => {
              setIsHoveringMap(false);
              setHoveredCity(null);
            }}
            onMouseMove={(e) => {
              const rect = e.currentTarget.getBoundingClientRect();
              setMousePosition({
                x: e.clientX - rect.left,
                y: e.clientY - rect.top
              });
            }}
          >
            {/* Turkey Map */}
            <div className="min-h-[400px]">
              <TurkeyMap
                hoverable
                showTooltip={false}
                customStyle={{ idleColor: "#E3F2FD", hoverColor: "#1D70F1" }}
                cityWrapper={(cityComponent, cityData) => {
                  const cityCode = cityData.plateNumber;
                  const stats = cityStats.cities.find(
                    (c) => c.cityCode === cityCode || CITY_NAME_TO_CODE[c.cityName] === cityCode
                  );
                  const fillColor = stats
                    ? getColorForValue(stats.totalOrders, maxOrders)
                    : "#E3F2FD";

                  return (
                    <g
                      data-plate={cityCode}
                      onMouseEnter={() => setHoveredCity(cityData.name)}
                      onMouseLeave={() => setHoveredCity(null)}
                      style={{ cursor: "pointer" }}
                    >
                      <style>{`[data-plate="${cityCode}"] path { fill: ${fillColor} !important; }`}</style>
                      {cityComponent}
                    </g>
                  );
                }}
              />
            </div>

            {/* Hover Tooltip - follows mouse, only shows when hovering map */}
            {isHoveringMap && hoveredCity && (
              <div
                className="absolute bg-card rounded-lg shadow-lg border border-border p-3 min-w-[180px] z-50 pointer-events-none"
                style={{
                  left: Math.min(mousePosition.x + 15, window.innerWidth - 250),
                  top: mousePosition.y + 15,
                  transform: mousePosition.x > 600 ? 'translateX(-100%)' : 'none'
                }}
              >
                <h4 className="font-semibold text-foreground text-sm">{hoveredCity}</h4>
                {hoveredCityStats ? (
                  <div className="mt-1.5 space-y-0.5 text-xs">
                    <div className="flex justify-between gap-4">
                      <span className="text-muted-foreground">Sipariş:</span>
                      <span className="font-medium text-foreground">
                        {formatNumber(hoveredCityStats.totalOrders)}
                      </span>
                    </div>
                    <div className="flex justify-between gap-4">
                      <span className="text-muted-foreground">Ciro:</span>
                      <span className="font-medium text-foreground">
                        {formatCurrency(hoveredCityStats.totalRevenue)}
                      </span>
                    </div>
                    <div className="flex justify-between gap-4">
                      <span className="text-muted-foreground">Ürün:</span>
                      <span className="font-medium text-foreground">
                        {formatNumber(hoveredCityStats.totalQuantity)} adet
                      </span>
                    </div>
                    <div className="flex justify-between gap-4 border-t border-border pt-1 mt-1">
                      <span className="text-muted-foreground">Ort. Sepet:</span>
                      <span className="font-medium text-foreground">
                        {formatCurrency(hoveredCityStats.averageOrderValue)}
                      </span>
                    </div>
                  </div>
                ) : (
                  <p className="text-xs text-muted-foreground mt-1">Bu şehirden sipariş yok</p>
                )}
              </div>
            )}

            {/* Color Legend */}
            <div className="flex items-center justify-center gap-4 mt-4 text-xs text-muted-foreground">
              <span>Az sipariş</span>
              <div className="flex">
                {["#E3F2FD", "#90CAF9", "#42A5F5", "#1976D2", "#0D47A1"].map((color, i) => (
                  <div
                    key={i}
                    className="w-8 h-4"
                    style={{ backgroundColor: color }}
                  />
                ))}
              </div>
              <span>Çok sipariş</span>
            </div>
          </div>

          {/* Warning for orders without city */}
          {cityStats.ordersWithoutCity > 0 && (
            <div className="mt-4 flex items-center gap-2 text-sm text-amber-600 bg-amber-50 px-3 py-2 rounded-md">
              <AlertCircle className="h-4 w-4" />
              <span>
                {formatNumber(cityStats.ordersWithoutCity)} sipariş şehir bilgisi olmadan
                kaydedilmiş (eski siparişler)
              </span>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Top Cities Table */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <TrendingUp className="h-5 w-5 text-[#1D70F1]" />
            En Çok Sipariş Alan Şehirler
          </CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-12">#</TableHead>
                <TableHead>Şehir</TableHead>
                <TableHead className="text-right">Sipariş</TableHead>
                <TableHead className="text-right">Ciro</TableHead>
                <TableHead className="text-right">Ürün</TableHead>
                <TableHead className="text-right">Ort. Sepet</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {(showAllCities ? cityStats.cities : cityStats.cities.slice(0, 15)).map((city, index) => (
                <TableRow
                  key={city.cityCode || city.cityName}
                  className={cn(
                    "cursor-pointer transition-colors",
                    hoveredCity?.toLowerCase() === city.cityName?.toLowerCase() && "bg-blue-50 dark:bg-blue-900/20"
                  )}
                  onMouseEnter={() => setHoveredCity(city.cityName)}
                  onMouseLeave={() => setHoveredCity(null)}
                >
                  <TableCell className="font-medium text-muted-foreground">{index + 1}</TableCell>
                  <TableCell className="font-medium">
                    <div className="flex items-center gap-2">
                      <div
                        className="w-3 h-3 rounded-full"
                        style={{ backgroundColor: getColorForValue(city.totalOrders, maxOrders) }}
                      />
                      {city.cityName}
                    </div>
                  </TableCell>
                  <TableCell className="text-right">{formatNumber(city.totalOrders)}</TableCell>
                  <TableCell className="text-right">{formatCurrency(city.totalRevenue)}</TableCell>
                  <TableCell className="text-right">{formatNumber(city.totalQuantity)}</TableCell>
                  <TableCell className="text-right">
                    {formatCurrency(city.averageOrderValue)}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          {cityStats.cities.length > 15 && (
            <button
              onClick={() => setShowAllCities(!showAllCities)}
              className="w-full text-sm text-[#1D70F1] hover:text-[#1557c0] font-medium text-center mt-4 py-2 hover:bg-blue-50 rounded-md transition-colors"
            >
              {showAllCities
                ? "Sadece ilk 15 şehri göster"
                : `Tüm ${cityStats.cities.length} şehri göster`}
            </button>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
