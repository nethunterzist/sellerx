"use client";

import { ReactNode } from "react";
import { SettingsNav, SettingsSection } from "./settings-nav";
import { ScrollArea } from "@/components/ui/scroll-area";
import { cn } from "@/lib/utils";

interface SettingsLayoutProps {
  children: ReactNode;
  activeSection: string;
  onSectionChange: (section: string) => void;
}

export function SettingsLayout({
  children,
  activeSection,
  onSectionChange,
}: SettingsLayoutProps) {
  return (
    <div className="p-6 max-w-7xl mx-auto">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-2xl font-semibold text-foreground">Ayarlar</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Hesap ve tercihlerinizi yönetin
        </p>
      </div>

      {/* Mobile Navigation */}
      <div className="lg:hidden mb-6">
        <MobileSettingsNav
          activeSection={activeSection}
          onSectionChange={onSectionChange}
        />
      </div>

      {/* Two-column layout */}
      <div className="flex gap-8">
        {/* Sidebar Navigation - Desktop */}
        <aside className="hidden lg:block w-64 shrink-0">
          <div className="sticky top-24">
            <SettingsNav
              activeSection={activeSection}
              onSectionChange={onSectionChange}
            />
          </div>
        </aside>

        {/* Content Area */}
        <main className="flex-1 min-w-0">{children}</main>
      </div>
    </div>
  );
}

// Mobile navigation - horizontal scroll
function MobileSettingsNav({
  activeSection,
  onSectionChange,
}: {
  activeSection: string;
  onSectionChange: (section: string) => void;
}) {
  const sections: SettingsSection[] = [
    { id: "profile", label: "Profil", icon: "User" },
    { id: "security", label: "Güvenlik", icon: "Shield" },
    { id: "subscription", label: "Abonelik", icon: "CreditCard" },
    { id: "invoices", label: "Faturalar", icon: "Receipt" },
    { id: "referral", label: "Davet Et", icon: "Gift" },
    { id: "appearance", label: "Görünüm", icon: "Palette" },
    { id: "notifications", label: "Bildirimler", icon: "Bell" },
    { id: "stores", label: "Mağazalar", icon: "Store" },
    { id: "ai", label: "AI Cevap", icon: "Sparkles" },
    { id: "activity", label: "Aktivite", icon: "History" },
  ];

  return (
    <ScrollArea className="w-full">
      <div className="flex gap-2 pb-2">
        {sections.map((section) => (
          <button
            key={section.id}
            onClick={() => onSectionChange(section.id)}
            className={cn(
              "px-4 py-2 rounded-lg text-sm font-medium whitespace-nowrap transition-colors",
              activeSection === section.id
                ? "bg-primary text-primary-foreground"
                : "bg-muted text-muted-foreground hover:bg-accent hover:text-accent-foreground"
            )}
          >
            {section.label}
          </button>
        ))}
      </div>
    </ScrollArea>
  );
}
