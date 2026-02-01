"use client";

import {
  User,
  Shield,
  Palette,
  Bell,
  Store,
  Sparkles,
  Keyboard,
  CreditCard,
  Receipt,
  Gift,
  type LucideIcon,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

export interface SettingsSection {
  id: string;
  label: string;
  icon: string;
  badge?: string;
}

interface SettingsGroup {
  id: string;
  label: string;
  sections: SettingsSection[];
}

const iconMap: Record<string, LucideIcon> = {
  User,
  Shield,
  Palette,
  Bell,
  Store,
  Sparkles,
  Keyboard,
  CreditCard,
  Receipt,
  Gift,
};

const settingsGroups: SettingsGroup[] = [
  {
    id: "account",
    label: "HESAP",
    sections: [
      { id: "profile", label: "Profil", icon: "User" },
      { id: "security", label: "Güvenlik", icon: "Shield" },
    ],
  },
  {
    id: "billing",
    label: "FATURALAMA",
    sections: [
      { id: "subscription", label: "Abonelik", icon: "CreditCard" },
      { id: "invoices", label: "Faturalar", icon: "Receipt" },
      { id: "referral", label: "Davet Et", icon: "Gift" },
    ],
  },
  {
    id: "preferences",
    label: "TERCİHLER",
    sections: [
      { id: "appearance", label: "Görünüm", icon: "Palette" },
      { id: "notifications", label: "Bildirimler", icon: "Bell" },
      { id: "activity", label: "Kısayollar & Aktivite", icon: "Keyboard" },
    ],
  },
  {
    id: "integrations",
    label: "ENTEGRASYONLAR",
    sections: [
      { id: "stores", label: "Mağazalar", icon: "Store" },
      { id: "ai", label: "AI Cevap", icon: "Sparkles", badge: "Yeni" },
    ],
  },
];

interface SettingsNavProps {
  activeSection: string;
  onSectionChange: (section: string) => void;
}

export function SettingsNav({ activeSection, onSectionChange }: SettingsNavProps) {
  return (
    <nav className="space-y-6">
      {settingsGroups.map((group) => (
        <div key={group.id}>
          <h3 className="px-3 text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-2">
            {group.label}
          </h3>
          <div className="space-y-1">
            {group.sections.map((section) => {
              const Icon = iconMap[section.icon];
              const isActive = activeSection === section.id;

              return (
                <button
                  key={section.id}
                  onClick={() => onSectionChange(section.id)}
                  className={cn(
                    "w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-all duration-200",
                    isActive
                      ? "bg-primary/10 dark:bg-primary/20 text-primary font-medium shadow-sm"
                      : "text-muted-foreground hover:bg-accent hover:text-accent-foreground"
                  )}
                >
                  <Icon
                    className={cn(
                      "h-4 w-4 shrink-0",
                      isActive ? "text-primary" : "text-muted-foreground"
                    )}
                  />
                  <span className="flex-1 text-left">{section.label}</span>
                  {section.badge && (
                    <Badge
                      variant="outline"
                      className={cn(
                        "text-[10px] px-1.5 py-0",
                        section.badge === "Yeni"
                          ? "bg-green-50 dark:bg-green-900/30 text-green-600 dark:text-green-400 border-green-200 dark:border-green-800"
                          : "bg-muted text-muted-foreground border-border"
                      )}
                    >
                      {section.badge}
                    </Badge>
                  )}
                </button>
              );
            })}
          </div>
        </div>
      ))}
    </nav>
  );
}

export { settingsGroups };
