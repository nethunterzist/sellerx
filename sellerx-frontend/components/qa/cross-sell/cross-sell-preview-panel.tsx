"use client";

import { useTranslations } from "next-intl";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { CrossSellProductCard } from "./cross-sell-product-card";
import { Eye, ShoppingBag } from "lucide-react";
import type { CrossSellRule, CrossSellSettings } from "@/types/cross-sell";

interface CrossSellPreviewPanelProps {
  rule?: Partial<CrossSellRule>;
  settings?: CrossSellSettings;
  /** Sample question to display preview for */
  sampleQuestion?: string;
}

const RECOMMENDATION_TYPE_LABELS: Record<string, string> = {
  COMPLEMENTARY: "Tamamlayici",
  UPSELL: "Ust Segment",
  ALTERNATIVE: "Alternatif",
  BUNDLE: "Paket",
};

export function CrossSellPreviewPanel({
  rule,
  settings,
  sampleQuestion = "Bu urun ile uyumlu aksesuar var mi?",
}: CrossSellPreviewPanelProps) {
  const t = useTranslations("qa.crossSell.preview");

  const products = rule?.recommendedProducts || [];
  const messageTemplate =
    rule?.messageTemplate ||
    settings?.defaultMessageTemplate ||
    t("defaultTemplate");

  return (
    <Card className="glass-card sticky top-4">
      <CardHeader className="pb-3">
        <CardTitle className="flex items-center gap-2 text-base">
          <Eye className="h-4 w-4 text-blue-500" />
          {t("title")}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Sample customer question */}
        <div className="space-y-1">
          <p className="text-xs font-medium text-muted-foreground">
            {t("customerQuestion")}
          </p>
          <div className="p-3 bg-muted/50 rounded-lg text-sm">
            {sampleQuestion}
          </div>
        </div>

        {/* Simulated AI Answer with recommendations */}
        <div className="space-y-2">
          <p className="text-xs font-medium text-muted-foreground">
            {t("aiAnswer")}
          </p>
          <div className="p-3 bg-blue-50/50 dark:bg-blue-950/20 rounded-lg border border-blue-200/50 dark:border-blue-800/50 space-y-3">
            {/* Base answer text */}
            <p className="text-sm text-muted-foreground italic">
              {t("sampleAnswer")}
            </p>

            {/* Recommendation block */}
            {products.length > 0 && (
              <div className="border-t border-blue-200/50 dark:border-blue-800/50 pt-3 space-y-2">
                <div className="flex items-center gap-2">
                  <ShoppingBag className="h-3.5 w-3.5 text-blue-600" />
                  <p className="text-xs font-medium text-blue-700 dark:text-blue-400">
                    {messageTemplate}
                  </p>
                </div>

                <div className="space-y-1.5">
                  {products.slice(0, rule?.maxRecommendations || 3).map((product) => (
                    <CrossSellProductCard
                      key={product.barcode}
                      product={product}
                      readonly
                      compact
                    />
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Rule info */}
        {rule && (
          <div className="flex flex-wrap gap-1.5 pt-2 border-t">
            {rule.recommendationType && (
              <Badge variant="outline" className="text-xs">
                {RECOMMENDATION_TYPE_LABELS[rule.recommendationType] ||
                  rule.recommendationType}
              </Badge>
            )}
            {rule.triggerConditions?.map((tc, i) => (
              <Badge key={i} variant="secondary" className="text-xs">
                {tc.type}: {tc.value}
              </Badge>
            ))}
          </div>
        )}

        {/* Empty state */}
        {products.length === 0 && (
          <div className="text-center py-4 text-sm text-muted-foreground">
            <ShoppingBag className="h-8 w-8 mx-auto mb-2 opacity-30" />
            <p>{t("emptyProducts")}</p>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
