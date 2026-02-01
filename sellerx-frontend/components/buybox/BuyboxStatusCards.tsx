"use client";

import { Trophy, XCircle, AlertTriangle, Users } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { BuyboxDashboard } from "@/types/buybox";

interface BuyboxStatusCardsProps {
  dashboard: BuyboxDashboard | undefined;
  isLoading: boolean;
}

export function BuyboxStatusCards({ dashboard, isLoading }: BuyboxStatusCardsProps) {
  const cards = [
    {
      title: "Toplam Takip",
      value: dashboard?.totalTrackedProducts || 0,
      icon: Users,
      color: "text-blue-600",
      bgColor: "bg-blue-50",
    },
    {
      title: "Kazanılan",
      value: dashboard?.wonCount || 0,
      icon: Trophy,
      color: "text-green-600",
      bgColor: "bg-green-50",
    },
    {
      title: "Kaybedilen",
      value: dashboard?.lostCount || 0,
      icon: XCircle,
      color: "text-red-600",
      bgColor: "bg-red-50",
    },
    {
      title: "Riskli",
      value: dashboard?.riskCount || 0,
      icon: AlertTriangle,
      color: "text-yellow-600",
      bgColor: "bg-yellow-50",
    },
  ];

  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
      {cards.map((card) => (
        <Card key={card.title}>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-muted-foreground">{card.title}</p>
                <p className="text-2xl font-bold mt-1">
                  {isLoading ? "-" : card.value}
                </p>
              </div>
              <div className={`p-3 rounded-full ${card.bgColor}`}>
                <card.icon className={`h-5 w-5 ${card.color}`} />
              </div>
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
