"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { MessageSquare, Clock, CheckCircle } from "lucide-react";
import type { QaStats } from "@/types/qa";

interface QaStatsCardsProps {
  stats: QaStats | undefined;
  isLoading: boolean;
}

export function QaStatsCards({ stats, isLoading }: QaStatsCardsProps) {
  const cards = [
    {
      title: "Toplam Soru",
      value: stats?.totalQuestions ?? 0,
      icon: MessageSquare,
      color: "text-blue-600",
      bgColor: "bg-blue-50",
    },
    {
      title: "Bekleyen",
      value: stats?.pendingQuestions ?? 0,
      icon: Clock,
      color: "text-orange-600",
      bgColor: "bg-orange-50",
    },
    {
      title: "Yanıtlanan",
      value: stats?.answeredQuestions ?? 0,
      icon: CheckCircle,
      color: "text-green-600",
      bgColor: "bg-green-50",
    },
  ];

  return (
    <div className="grid gap-4 md:grid-cols-3">
      {cards.map((card) => (
        <Card key={card.title}>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">{card.title}</CardTitle>
            <div className={`p-2 rounded-full ${card.bgColor}`}>
              <card.icon className={`h-4 w-4 ${card.color}`} />
            </div>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {isLoading ? (
                <span className="animate-pulse">...</span>
              ) : (
                card.value.toLocaleString("tr-TR")
              )}
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
