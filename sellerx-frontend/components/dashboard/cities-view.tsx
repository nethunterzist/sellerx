"use client";

import { useState, useMemo } from "react";
import { FadeIn } from "@/components/motion";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { Calendar } from "@/components/ui/calendar";
import { Button } from "@/components/ui/button";
import { MapPin, TrendingUp, AlertCircle, CalendarIcon } from "lucide-react";
import { cn } from "@/lib/utils";
import { format, subDays, subMonths, startOfMonth, endOfMonth } from "date-fns";
import { tr } from "date-fns/locale";
import type { CityStats, CityStatsResponse } from "@/types/city-stats";
import { CITY_NAME_TO_CODE, TURKEY_CITY_CODES } from "@/types/city-stats";
import { useCurrency } from "@/lib/contexts/currency-context";
import { useCityStats } from "@/hooks/queries/use-city-stats";
import type { DateRange } from "react-day-picker";

// Dynamically import TurkeyMap to avoid SSR issues
const TurkeyMap = dynamic(() => import("turkey-map-react").then((mod) => mod.default), {
  ssr: false,
  loading: () => (
    <div className="flex items-center justify-center h-[400px] bg-muted rounded-lg">
      <Skeleton className="w-full h-full" />
    </div>
  ),
});

// Dönem seçenekleri
const CITIES_PERIOD_PRESETS = [
  { id: "today", label: "Bugün" },
  { id: "yesterday", label: "Dün" },
  { id: "last7days", label: "Son 7 gün" },
  { id: "thisMonth", label: "Bu ay" },
  { id: "lastMonth", label: "Geçen ay" },
  { id: "last3months", label: "Son 3 ay" },
  { id: "last12months", label: "Son 12 ay" },
  { id: "custom", label: "Özel tarih" },
] as const;

type CitiesPeriodPreset = typeof CITIES_PERIOD_PRESETS[number]["id"];

// Dönem preset'ine göre tarih aralığı hesapla
function calculateDateRange(preset: CitiesPeriodPreset): { startDate: string; endDate: string } {
  const today = new Date();
  const formatDate = (d: Date) => format(d, "yyyy-MM-dd");

  switch (preset) {
    case "today":
      return { startDate: formatDate(today), endDate: formatDate(today) };
    case "yesterday":
      const yesterday = subDays(today, 1);
      return { startDate: formatDate(yesterday), endDate: formatDate(yesterday) };
    case "last7days":
      return { startDate: formatDate(subDays(today, 6)), endDate: formatDate(today) };
    case "thisMonth":
      return { startDate: formatDate(startOfMonth(today)), endDate: formatDate(today) };
    case "lastMonth":
      const lastMonth = subMonths(today, 1);
      return { startDate: formatDate(startOfMonth(lastMonth)), endDate: formatDate(endOfMonth(lastMonth)) };
    case "last3months":
      return { startDate: formatDate(subMonths(today, 3)), endDate: formatDate(today) };
    case "last12months":
      return { startDate: formatDate(subMonths(today, 12)), endDate: formatDate(today) };
    default:
      return { startDate: formatDate(subDays(today, 29)), endDate: formatDate(today) };
  }
}

interface CitiesViewProps {
  storeId: string | undefined;
}

function formatNumber(value: number): string {
  return new Intl.NumberFormat("tr-TR").format(value);
}

/**
 * Şehir isimlerini normalize eder (karşılaştırma için)
 * - Boşlukları temizler
 * - Küçük harfe çevirir
 * - Türkçe karakterleri normalize eder
 */
function normalizeCityName(name: string | null | undefined): string {
  if (!name) return "";
  return name
    .trim()
    .toLowerCase()
    .replace(/İ/g, "i")  // Türkçe büyük İ
    .replace(/I/g, "ı")  // ASCII büyük I → Türkçe ı
    .replace(/Ş/g, "ş")
    .replace(/Ğ/g, "ğ")
    .replace(/Ü/g, "ü")
    .replace(/Ö/g, "ö")
    .replace(/Ç/g, "ç");
}

/**
 * cityCode'dan doğru şehir ismini döndürür
 */
function getCanonicalCityName(cityCode: number | null, fallbackName: string): string {
  if (cityCode && TURKEY_CITY_CODES[cityCode]) {
    return TURKEY_CITY_CODES[cityCode];
  }
  // Fallback: ilk harfi büyük yap
  return fallbackName.charAt(0).toUpperCase() + fallbackName.slice(1).toLowerCase();
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

export function CitiesView({ storeId }: CitiesViewProps) {
  const { formatCurrency } = useCurrency();
  const [hoveredCity, setHoveredCity] = useState<string | null>(null);
  const [showAllCities, setShowAllCities] = useState(false);
  const [mousePosition, setMousePosition] = useState({ x: 0, y: 0 });
  const [isHoveringMap, setIsHoveringMap] = useState(false);

  // Dönem state'i
  const [selectedPeriod, setSelectedPeriod] = useState<CitiesPeriodPreset>("last7days");
  const [customDateRange, setCustomDateRange] = useState<DateRange | undefined>(undefined);
  const [isCalendarOpen, setIsCalendarOpen] = useState(false);

  // Tarih aralığını hesapla
  const dateRange = useMemo(() => {
    if (selectedPeriod === "custom" && customDateRange?.from && customDateRange?.to) {
      return {
        startDate: format(customDateRange.from, "yyyy-MM-dd"),
        endDate: format(customDateRange.to, "yyyy-MM-dd"),
      };
    }
    return calculateDateRange(selectedPeriod);
  }, [selectedPeriod, customDateRange]);

  // Hook'u içeride çağır
  const { data: cityStats, isLoading } = useCityStats({
    storeId,
    startDate: dateRange.startDate,
    endDate: dateRange.endDate,
  });

  // Dönem seçimi değiştiğinde
  const handlePeriodChange = (value: string) => {
    setSelectedPeriod(value as CitiesPeriodPreset);
    if (value !== "custom") {
      setCustomDateRange(undefined);
    }
  };

  // Özel tarih seçimi değiştiğinde
  const handleCustomDateSelect = (range: DateRange | undefined) => {
    setCustomDateRange(range);
    if (range?.from && range?.to) {
      setIsCalendarOpen(false);
    }
  };

  // Seçili dönemin label'ını al
  const getSelectedPeriodLabel = () => {
    if (selectedPeriod === "custom" && customDateRange?.from && customDateRange?.to) {
      return `${format(customDateRange.from, "d MMM", { locale: tr })} - ${format(customDateRange.to, "d MMM yyyy", { locale: tr })}`;
    }
    const preset = CITIES_PERIOD_PRESETS.find(p => p.id === selectedPeriod);
    return preset?.label || "Son 7 gün";
  };

  // Şehirleri normalize edip birleştir (tekrar eden şehirleri merge et)
  const normalizedCities = useMemo(() => {
    if (!cityStats?.cities) return [];

    const cityMap = new Map<string, CityStats>();

    cityStats.cities.forEach((city) => {
      const normalizedKey = normalizeCityName(city.cityName);
      if (!normalizedKey) return;

      const existing = cityMap.get(normalizedKey);

      if (existing) {
        // Mevcut şehre değerleri ekle (birleştir)
        cityMap.set(normalizedKey, {
          cityName: existing.cityName, // İlk gelen canonical ismi kullan
          cityCode: existing.cityCode || city.cityCode, // cityCode'u al
          totalOrders: existing.totalOrders + city.totalOrders,
          totalRevenue: existing.totalRevenue + city.totalRevenue,
          totalQuantity: existing.totalQuantity + city.totalQuantity,
          averageOrderValue: 0, // Sonra hesaplanacak
        });
      } else {
        // Yeni şehir ekle
        const canonicalName = getCanonicalCityName(city.cityCode, city.cityName);
        cityMap.set(normalizedKey, {
          ...city,
          cityName: canonicalName,
        });
      }
    });

    // averageOrderValue'ları hesapla ve sırala
    const result = Array.from(cityMap.values())
      .map((city) => ({
        ...city,
        averageOrderValue: city.totalOrders > 0 ? city.totalRevenue / city.totalOrders : 0,
      }))
      .sort((a, b) => b.totalOrders - a.totalOrders);

    return result;
  }, [cityStats?.cities]);

  // Normalize edilmiş şehir sayısı
  const normalizedTotalCities = normalizedCities.length;

  // Create a map of city name to stats (for hover matching)
  const cityStatsMap = useMemo(() => {
    const map = new Map<string, CityStats>();
    normalizedCities.forEach((city) => {
      const normalizedName = normalizeCityName(city.cityName);
      if (normalizedName) {
        map.set(normalizedName, city);
      }
    });
    return map;
  }, [normalizedCities]);

  // Get max order count for color scaling
  const maxOrders = useMemo(() => {
    if (normalizedCities.length === 0) return 0;
    return Math.max(...normalizedCities.map((c) => c.totalOrders));
  }, [normalizedCities]);

  // Get hovered city stats
  const hoveredCityStats = useMemo(() => {
    if (!hoveredCity) return null;
    const normalizedName = hoveredCity.toLowerCase().trim();
    return cityStatsMap.get(normalizedName) || null;
  }, [hoveredCity, cityStatsMap]);

  // Generate city data for the map
  const cityData = useMemo(() => {
    const data: Record<string, { value: number }> = {};

    normalizedCities.forEach((city) => {
      // Try to find city code from name
      const cityCode = city.cityCode || CITY_NAME_TO_CODE[city.cityName];
      if (cityCode) {
        // turkey-map-react uses city plate codes as keys
        data[cityCode.toString()] = { value: city.totalOrders };
      }
    });

    return data;
  }, [normalizedCities]);

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
    <FadeIn>
    <div className="space-y-6">
      {/* Map Card */}
      <Card>
        <CardHeader className="flex flex-row items-center justify-between flex-wrap gap-4">
          <CardTitle className="text-lg flex items-center gap-2">
            <MapPin className="h-5 w-5 text-[#1D70F1]" />
            Türkiye Sipariş Haritası
          </CardTitle>
          <div className="flex items-center gap-3">
            {/* Dönem Seçici */}
            <div className="flex items-center gap-2">
              <Select value={selectedPeriod} onValueChange={handlePeriodChange}>
                <SelectTrigger className="w-[160px] h-9">
                  <SelectValue placeholder="Dönem seçin" />
                </SelectTrigger>
                <SelectContent>
                  {CITIES_PERIOD_PRESETS.map((preset) => (
                    <SelectItem key={preset.id} value={preset.id}>
                      {preset.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>

              {/* Özel Tarih Seçici */}
              {selectedPeriod === "custom" && (
                <Popover open={isCalendarOpen} onOpenChange={setIsCalendarOpen}>
                  <PopoverTrigger asChild>
                    <Button
                      variant="outline"
                      className={cn(
                        "w-[220px] h-9 justify-start text-left font-normal",
                        !customDateRange && "text-muted-foreground"
                      )}
                    >
                      <CalendarIcon className="mr-2 h-4 w-4" />
                      {customDateRange?.from && customDateRange?.to ? (
                        `${format(customDateRange.from, "d MMM", { locale: tr })} - ${format(customDateRange.to, "d MMM", { locale: tr })}`
                      ) : (
                        "Tarih seçin"
                      )}
                    </Button>
                  </PopoverTrigger>
                  <PopoverContent className="w-auto p-0" align="end">
                    <Calendar
                      mode="range"
                      selected={customDateRange}
                      onSelect={handleCustomDateSelect}
                      numberOfMonths={2}
                      locale="tr"
                    />
                  </PopoverContent>
                </Popover>
              )}
            </div>

            <div className="text-sm text-muted-foreground">
              {normalizedTotalCities} şehirden sipariş
            </div>
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
                  const stats = normalizedCities.find(
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
              {(showAllCities ? normalizedCities : normalizedCities.slice(0, 15)).map((city, index) => (
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
          {normalizedCities.length > 15 && (
            <button
              onClick={() => setShowAllCities(!showAllCities)}
              className="w-full text-sm text-[#1D70F1] hover:text-[#1557c0] font-medium text-center mt-4 py-2 hover:bg-blue-50 rounded-md transition-colors"
            >
              {showAllCities
                ? "Sadece ilk 15 şehri göster"
                : `Tüm ${normalizedCities.length} şehri göster`}
            </button>
          )}
        </CardContent>
      </Card>
    </div>
    </FadeIn>
  );
}
