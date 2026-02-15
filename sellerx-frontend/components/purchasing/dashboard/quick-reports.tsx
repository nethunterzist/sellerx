"use client";

import { useRouter } from "next/navigation";
import { FileBarChart, Package, Clock } from "lucide-react";
import { cn } from "@/lib/utils";

interface QuickReportsProps {
  storeId: string | undefined;
}

const reports = [
  {
    id: "cost-history",
    label: "Maliyet Gecmisi",
    description: "Urun bazli maliyet degisimlerini inceleyin",
    icon: Clock,
    href: "/purchasing/reports/cost-history",
    color: "text-blue-600 dark:text-blue-400",
    bgColor: "bg-blue-50 dark:bg-blue-900/20",
    borderColor: "border-blue-200 dark:border-blue-800",
  },
  {
    id: "stock-valuation",
    label: "Stok Degerleme",
    description: "Guncel stok degerini ve yaslari gorun",
    icon: Package,
    href: "/purchasing/reports/stock-valuation",
    color: "text-purple-600 dark:text-purple-400",
    bgColor: "bg-purple-50 dark:bg-purple-900/20",
    borderColor: "border-purple-200 dark:border-purple-800",
  },
];

export function QuickReports({ storeId }: QuickReportsProps) {
  const router = useRouter();

  if (!storeId) return null;

  return (
    <div className="space-y-3">
      <h3 className="text-sm font-medium text-muted-foreground px-1">Hizli Raporlar</h3>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {reports.map((report) => {
          const Icon = report.icon;
          return (
            <button
              key={report.id}
              onClick={() => router.push(report.href)}
              className={cn(
                "p-4 rounded-xl border text-left transition-all hover:shadow-md hover:scale-[1.02]",
                report.borderColor,
                report.bgColor
              )}
            >
              <div className="flex items-start gap-3">
                <div className={cn("p-2 rounded-lg bg-white dark:bg-gray-900")}>
                  <Icon className={cn("h-5 w-5", report.color)} />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-foreground">{report.label}</p>
                  <p className="text-xs text-muted-foreground mt-0.5">
                    {report.description}
                  </p>
                </div>
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}
