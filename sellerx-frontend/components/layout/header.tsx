"use client";

import { useState } from "react";
import Link from "next/link";
import { useAuth } from "@/hooks/useAuth";
import { useMyStores, useSelectedStore, useSetSelectedStore } from "@/hooks/queries/use-stores";
import { cn } from "@/lib/utils";
import {
  Search,
  Bell,
  HelpCircle,
  ChevronDown,
  Filter,
  LayoutGrid,
  LineChart,
  FileText,
  Map,
  TrendingUp,
  LogOut,
  User,
  Settings,
} from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

interface HeaderTab {
  icon: React.ElementType;
  label: string;
  value: string;
}

const tabs: HeaderTab[] = [
  { icon: LayoutGrid, label: "Kutucuklar", value: "tiles" },
  { icon: LineChart, label: "Grafik", value: "chart" },
  { icon: FileText, label: "Kâr/Zarar", value: "pnl" },
  { icon: Map, label: "Harita", value: "map" },
  { icon: TrendingUp, label: "Trendler", value: "trends" },
];

export function Header() {
  const { user, logout } = useAuth();
  const { data: stores } = useMyStores();
  const { data: selectedStoreData } = useSelectedStore();
  const { mutate: setSelectedStore } = useSetSelectedStore();
  const [activeTab, setActiveTab] = useState("tiles");
  const [searchQuery, setSearchQuery] = useState("");

  const selectedStore = stores?.find(
    (s) => s.id === selectedStoreData?.selectedStoreId
  );

  return (
    <header className="fixed top-0 right-0 left-[220px] z-30 flex h-14 items-center justify-between border-b border-[#DDDDDD] bg-white px-4">
      {/* Left Section - Tabs */}
      <div className="flex items-center gap-1">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.value;
          return (
            <button
              key={tab.value}
              onClick={() => setActiveTab(tab.value)}
              className={cn(
                "flex items-center gap-1.5 rounded px-3 py-1.5 text-sm transition-colors",
                isActive
                  ? "bg-[#E8F1FE] text-[#1D70F1]"
                  : "text-gray-600 hover:bg-gray-100"
              )}
            >
              <Icon className="h-4 w-4" />
              <span className="hidden sm:inline">{tab.label}</span>
            </button>
          );
        })}
      </div>

      {/* Right Section - Search, Filters, User */}
      <div className="flex items-center gap-3">
        {/* Search */}
        <div className="relative hidden md:block">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
          <Input
            type="search"
            placeholder="Ara..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="h-8 w-48 pl-9 text-sm"
          />
        </div>

        {/* Period Selector */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="outline" size="sm" className="h-8 gap-1.5 text-sm">
              Dönem
              <ChevronDown className="h-3.5 w-3.5" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem>Bugün</DropdownMenuItem>
            <DropdownMenuItem>Dün</DropdownMenuItem>
            <DropdownMenuItem>Son 7 gün</DropdownMenuItem>
            <DropdownMenuItem>Son 30 gün</DropdownMenuItem>
            <DropdownMenuItem>Bu ay</DropdownMenuItem>
            <DropdownMenuItem>Geçen ay</DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem>Özel tarih aralığı...</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>

        {/* Store Selector */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="outline" size="sm" className="h-8 gap-1.5 text-sm">
              {selectedStore?.storeName || "Tüm mağazalar"}
              <ChevronDown className="h-3.5 w-3.5" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onClick={() => setSelectedStore("")}>
              Tüm mağazalar
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            {stores?.map((store) => (
              <DropdownMenuItem
                key={store.id}
                onClick={() => setSelectedStore(store.id)}
              >
                <div className="flex items-center gap-2">
                  <div
                    className={cn(
                      "h-4 w-4 rounded text-[8px] font-bold text-white flex items-center justify-center",
                      store.marketplace === "trendyol"
                        ? "bg-[#F27A1A]"
                        : "bg-[#FF6000]"
                    )}
                  >
                    {store.marketplace === "trendyol" ? "T" : "H"}
                  </div>
                  {store.storeName}
                </div>
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>

        {/* Currency */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="outline" size="sm" className="h-8 gap-1.5 text-sm">
              TRY
              <ChevronDown className="h-3.5 w-3.5" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem>TRY - Türk Lirası</DropdownMenuItem>
            <DropdownMenuItem>USD - Amerikan Doları</DropdownMenuItem>
            <DropdownMenuItem>EUR - Euro</DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>

        {/* Filter */}
        <Button variant="outline" size="sm" className="h-8 gap-1.5 text-sm">
          <Filter className="h-3.5 w-3.5" />
          <span className="hidden sm:inline">Filtre</span>
        </Button>

        {/* Help */}
        <Button
          variant="ghost"
          size="icon"
          className="h-8 w-8 rounded-full bg-[#E8F1FE] text-[#1D70F1] hover:bg-[#d0e3fc]"
        >
          <HelpCircle className="h-4 w-4" />
        </Button>

        {/* Notifications */}
        <Button
          variant="ghost"
          size="icon"
          className="relative h-8 w-8 text-[#1D70F1]"
        >
          <Bell className="h-4 w-4" />
          <span className="absolute -right-1 -top-1 flex h-4 w-4 items-center justify-center rounded-full bg-[#FEB72B] text-[10px] font-medium text-black">
            3
          </span>
        </Button>

        {/* User Menu */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant="ghost"
              size="sm"
              className="h-8 gap-2 text-sm font-normal"
            >
              <div className="flex h-6 w-6 items-center justify-center rounded-full bg-[#1D70F1] text-white text-xs">
                {user?.name?.charAt(0).toUpperCase() || "U"}
              </div>
              <span className="hidden md:inline">
                {user?.name || "Kullanıcı"}
              </span>
              <ChevronDown className="h-3.5 w-3.5" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-48">
            <DropdownMenuItem asChild>
              <Link href="/profile" className="flex items-center gap-2">
                <User className="h-4 w-4" />
                Profil
              </Link>
            </DropdownMenuItem>
            <DropdownMenuItem asChild>
              <Link href="/settings" className="flex items-center gap-2">
                <Settings className="h-4 w-4" />
                Ayarlar
              </Link>
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem
              onClick={() => logout()}
              className="text-[#F34E1B] focus:text-[#F34E1B]"
            >
              <LogOut className="h-4 w-4 mr-2" />
              Çıkış Yap
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>

        {/* CTA Button */}
        <Button
          size="sm"
          className="h-8 bg-[#FEB72B] text-black hover:bg-[#e5a526] font-medium"
        >
          Ücretsiz Dene
        </Button>
      </div>
    </header>
  );
}
