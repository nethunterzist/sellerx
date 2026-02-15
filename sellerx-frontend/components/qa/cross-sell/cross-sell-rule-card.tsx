"use client";

import { useTranslations } from "next-intl";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  GripVertical,
  MoreHorizontal,
  Edit2,
  Trash2,
  Eye,
  BarChart3,
} from "lucide-react";
import { cn } from "@/lib/utils";
import type { CrossSellRule } from "@/types/cross-sell";

interface CrossSellRuleCardProps {
  rule: CrossSellRule;
  onEdit: (rule: CrossSellRule) => void;
  onDelete: (ruleId: string) => void;
  onToggleStatus: (ruleId: string, active: boolean) => void;
  dragHandleProps?: Record<string, unknown>;
}

const STATUS_COLORS: Record<string, string> = {
  ACTIVE: "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400",
  INACTIVE: "bg-gray-100 text-gray-700 dark:bg-gray-900/30 dark:text-gray-400",
  DRAFT: "bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400",
};

const RECOMMENDATION_TYPE_LABELS: Record<string, string> = {
  COMPLEMENTARY: "Tamamlayici",
  UPSELL: "Ust Segment",
  ALTERNATIVE: "Alternatif",
  BUNDLE: "Paket",
};

const RECOMMENDATION_TYPE_COLORS: Record<string, string> = {
  COMPLEMENTARY: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400",
  UPSELL: "bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400",
  ALTERNATIVE: "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400",
  BUNDLE: "bg-teal-100 text-teal-700 dark:bg-teal-900/30 dark:text-teal-400",
};

export function CrossSellRuleCard({
  rule,
  onEdit,
  onDelete,
  onToggleStatus,
  dragHandleProps,
}: CrossSellRuleCardProps) {
  const t = useTranslations("qa.crossSell.ruleCard");

  const isActive = rule.status === "ACTIVE";

  return (
    <Card
      className={cn(
        "transition-all",
        !isActive && "opacity-60"
      )}
    >
      <CardContent className="p-4">
        <div className="flex items-start gap-3">
          {/* Drag handle */}
          {dragHandleProps && (
            <div
              {...dragHandleProps}
              className="mt-1 cursor-grab active:cursor-grabbing text-muted-foreground hover:text-foreground"
            >
              <GripVertical className="h-4 w-4" />
            </div>
          )}

          {/* Content */}
          <div className="flex-1 min-w-0 space-y-2">
            {/* Header row */}
            <div className="flex items-center justify-between gap-2">
              <div className="flex items-center gap-2 min-w-0">
                <h4 className="font-medium text-sm truncate">{rule.name}</h4>
                <Badge
                  variant="outline"
                  className={cn("text-xs shrink-0", STATUS_COLORS[rule.status])}
                >
                  {t(`status.${rule.status.toLowerCase()}`)}
                </Badge>
              </div>

              <div className="flex items-center gap-1 shrink-0">
                <Switch
                  checked={isActive}
                  onCheckedChange={(checked) =>
                    onToggleStatus(rule.id, checked)
                  }
                  className="scale-75"
                />
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="ghost" size="icon" className="h-7 w-7">
                      <MoreHorizontal className="h-4 w-4" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={() => onEdit(rule)}>
                      <Edit2 className="h-4 w-4 mr-2" />
                      {t("edit")}
                    </DropdownMenuItem>
                    <DropdownMenuItem
                      onClick={() => onDelete(rule.id)}
                      className="text-destructive"
                    >
                      <Trash2 className="h-4 w-4 mr-2" />
                      {t("delete")}
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            </div>

            {/* Description */}
            {rule.description && (
              <p className="text-xs text-muted-foreground line-clamp-1">
                {rule.description}
              </p>
            )}

            {/* Tags row */}
            <div className="flex flex-wrap items-center gap-1.5">
              <Badge
                variant="outline"
                className={cn(
                  "text-xs",
                  RECOMMENDATION_TYPE_COLORS[rule.recommendationType]
                )}
              >
                {RECOMMENDATION_TYPE_LABELS[rule.recommendationType] ||
                  rule.recommendationType}
              </Badge>

              {rule.triggerConditions.slice(0, 3).map((tc, i) => (
                <Badge key={i} variant="secondary" className="text-xs">
                  {tc.value}
                </Badge>
              ))}
              {rule.triggerConditions.length > 3 && (
                <Badge variant="secondary" className="text-xs">
                  +{rule.triggerConditions.length - 3}
                </Badge>
              )}
            </div>

            {/* Stats row */}
            <div className="flex items-center gap-4 text-xs text-muted-foreground">
              <span className="flex items-center gap-1">
                <Eye className="h-3 w-3" />
                {rule.impressionCount} {t("impressions")}
              </span>
              <span className="flex items-center gap-1">
                <BarChart3 className="h-3 w-3" />
                %{(rule.conversionRate * 100).toFixed(1)} {t("conversion")}
              </span>
              <span>
                {rule.recommendedProducts.length} {t("products")}
              </span>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
