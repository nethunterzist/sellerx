"use client";

import { useTranslations } from "next-intl";
import type { SeniorityStats } from "@/types/qa";

interface SeniorityStatsCardsProps {
  stats: SeniorityStats;
}

export function SeniorityStatsCards({ stats }: SeniorityStatsCardsProps) {
  const t = useTranslations("qa.seniority");

  const cards = [
    {
      level: "JUNIOR",
      label: t("junior"),
      description: t("juniorDesc"),
      count: stats.juniorCount,
      icon: "○",
      color: "text-gray-500 bg-gray-100 dark:bg-gray-800",
    },
    {
      level: "LEARNING",
      label: t("learning"),
      description: t("learningDesc"),
      count: stats.learningCount,
      icon: "◐",
      color: "text-blue-500 bg-blue-100 dark:bg-blue-900/30",
    },
    {
      level: "SENIOR",
      label: t("senior"),
      description: t("seniorDesc"),
      count: stats.seniorCount,
      icon: "●",
      color: "text-green-500 bg-green-100 dark:bg-green-900/30",
    },
    {
      level: "EXPERT",
      label: t("expert"),
      description: t("expertDesc"),
      count: stats.expertCount,
      icon: "★",
      color: "text-yellow-500 bg-yellow-100 dark:bg-yellow-900/30",
    },
  ];

  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
      {cards.map((card) => (
        <div
          key={card.level}
          className={`rounded-lg p-4 ${card.color} transition-all hover:scale-105`}
        >
          <div className="flex items-center justify-between mb-2">
            <span className="text-2xl">{card.icon}</span>
            <span className="text-2xl font-bold">{card.count}</span>
          </div>
          <p className="font-semibold text-sm">{card.label}</p>
          <p className="text-xs opacity-75">{card.description}</p>
        </div>
      ))}
    </div>
  );
}
