"use client";

import type { LucideIcon } from "lucide-react";

interface StatCardMiniProps {
  label: string;
  value: number;
  icon: LucideIcon;
  color: "blue" | "green" | "red" | "yellow" | "purple" | "amber" | "cyan" | "gray";
  alert?: boolean;
}

const colorClasses = {
  blue: {
    bg: "bg-blue-50 dark:bg-blue-900/20",
    border: "border-blue-200 dark:border-blue-800",
    icon: "text-blue-600 dark:text-blue-400",
    iconBg: "bg-blue-100 dark:bg-blue-900/40",
  },
  green: {
    bg: "bg-green-50 dark:bg-green-900/20",
    border: "border-green-200 dark:border-green-800",
    icon: "text-green-600 dark:text-green-400",
    iconBg: "bg-green-100 dark:bg-green-900/40",
  },
  red: {
    bg: "bg-red-50 dark:bg-red-900/20",
    border: "border-red-200 dark:border-red-800",
    icon: "text-red-600 dark:text-red-400",
    iconBg: "bg-red-100 dark:bg-red-900/40",
  },
  yellow: {
    bg: "bg-yellow-50 dark:bg-yellow-900/20",
    border: "border-yellow-200 dark:border-yellow-800",
    icon: "text-yellow-600 dark:text-yellow-400",
    iconBg: "bg-yellow-100 dark:bg-yellow-900/40",
  },
  purple: {
    bg: "bg-purple-50 dark:bg-purple-900/20",
    border: "border-purple-200 dark:border-purple-800",
    icon: "text-purple-600 dark:text-purple-400",
    iconBg: "bg-purple-100 dark:bg-purple-900/40",
  },
  amber: {
    bg: "bg-amber-50 dark:bg-amber-900/20",
    border: "border-amber-200 dark:border-amber-800",
    icon: "text-amber-600 dark:text-amber-400",
    iconBg: "bg-amber-100 dark:bg-amber-900/40",
  },
  cyan: {
    bg: "bg-cyan-50 dark:bg-cyan-900/20",
    border: "border-cyan-200 dark:border-cyan-800",
    icon: "text-cyan-600 dark:text-cyan-400",
    iconBg: "bg-cyan-100 dark:bg-cyan-900/40",
  },
  gray: {
    bg: "bg-gray-50 dark:bg-gray-900/20",
    border: "border-gray-200 dark:border-gray-800",
    icon: "text-gray-600 dark:text-gray-400",
    iconBg: "bg-gray-100 dark:bg-gray-900/40",
  },
};

export function StatCardMini({ label, value, icon: Icon, color, alert }: StatCardMiniProps) {
  const colors = colorClasses[color];
  const alertClasses = alert ? "ring-2 ring-red-500 ring-offset-2 dark:ring-offset-gray-900" : "";

  return (
    <div
      className={`rounded-lg border p-3 ${colors.bg} ${colors.border} ${alertClasses} transition-all hover:shadow-md`}
    >
      <div className="flex items-center gap-3">
        <div className={`p-2 rounded-md ${colors.iconBg}`}>
          <Icon className={`h-4 w-4 ${colors.icon}`} />
        </div>
        <div className="min-w-0 flex-1">
          <p className="text-xs text-muted-foreground truncate">{label}</p>
          <p className="text-lg font-semibold text-foreground">
            {value.toLocaleString("tr-TR")}
          </p>
        </div>
      </div>
    </div>
  );
}
