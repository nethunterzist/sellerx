"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";
import { ConfidenceGauge } from "./confidence-gauge";
import { useGenerateAiAnswer, useApproveAiAnswer } from "@/hooks/queries/use-ai";
import {
  Sparkles,
  Check,
  Edit2,
  X,
  Package,
  Clock,
  Loader2,
  MessageSquare,
  BookOpen,
  ShoppingBag,
} from "lucide-react";
import { toast } from "sonner";
import { formatDistanceToNow } from "date-fns";
import { tr } from "date-fns/locale";
import type { Question } from "@/types/qa";
import type { AiGenerateResponse } from "@/types/ai";

interface QuestionDetailPanelProps {
  question: Question;
  storeId: string;
  onRefresh?: () => void;
}

type GenerateState = "idle" | "generating" | "generated";

const SOURCE_COLORS: Record<string, string> = {
  product: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400",
  historical_qa: "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400",
  knowledge_base: "bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400",
  template: "bg-gray-100 text-gray-700 dark:bg-gray-900/30 dark:text-gray-400",
};

const SOURCE_LABELS: Record<string, string> = {
  product: "Ürün",
  historical_qa: "Geçmiş S&S",
  knowledge_base: "Bilgi Tabanı",
  template: "Şablon",
};

export function QuestionDetailPanel({
  question,
  storeId,
  onRefresh,
}: QuestionDetailPanelProps) {
  const t = useTranslations("qa.answerFlow");

  const [generateState, setGenerateState] = useState<GenerateState>("idle");
  const [aiResponse, setAiResponse] = useState<AiGenerateResponse | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [editedText, setEditedText] = useState("");

  const generateMutation = useGenerateAiAnswer();
  const approveMutation = useApproveAiAnswer();

  const relativeTime = formatDistanceToNow(new Date(question.questionDate), {
    addSuffix: true,
    locale: tr,
  });

  const handleGenerate = async () => {
    setGenerateState("generating");
    try {
      const response = await generateMutation.mutateAsync(question.id);
      setAiResponse(response);
      setEditedText(response.generatedAnswer);
      setGenerateState("generated");
    } catch {
      toast.error(t("generateError"));
      setGenerateState("idle");
    }
  };

  const handleApprove = async () => {
    if (!aiResponse) return;
    try {
      await approveMutation.mutateAsync({
        questionId: question.id,
        data: {
          logId: aiResponse.logId,
          finalAnswer: editedText,
        },
      });
      toast.success(t("approveSuccess"));
      onRefresh?.();
    } catch {
      toast.error(t("approveError"));
    }
  };

  const handleReject = () => {
    setAiResponse(null);
    setGenerateState("idle");
    setIsEditing(false);
  };

  const handleEditSave = () => {
    setIsEditing(false);
  };

  const handleEditCancel = () => {
    if (aiResponse) {
      setEditedText(aiResponse.generatedAnswer);
    }
    setIsEditing(false);
  };

  return (
    <Card className="glass-card h-full overflow-y-auto">
      <CardHeader className="border-b sticky top-0 glass-header z-10">
        <CardTitle className="text-base">Soru Detayı</CardTitle>
      </CardHeader>

      <CardContent className="p-4 space-y-4">
        {/* Product Info */}
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <Package className="h-4 w-4" />
          <span className="truncate">{question.productTitle || "Ürün bilgisi yok"}</span>
        </div>

        {/* Timestamp */}
        <div className="flex items-center gap-1 text-xs text-muted-foreground">
          <Clock className="h-3 w-3" />
          <span>{relativeTime}</span>
        </div>

        {/* Customer Question */}
        <div className="space-y-2">
          <div className="flex items-center gap-2 text-sm font-medium">
            <MessageSquare className="h-4 w-4" />
            <span>Müşteri Sorusu</span>
          </div>
          <div className="flex justify-start">
            <div className="bg-slate-100 dark:bg-slate-800 rounded-2xl rounded-tl-sm p-3 max-w-[85%] shadow-sm">
              <p className="text-sm">{question.customerQuestion}</p>
            </div>
          </div>
        </div>

        {/* AI Generation Section */}
        {question.status === "PENDING" && (
          <div className="space-y-3 pt-2 border-t">
            {generateState === "idle" && (
              <Button
                size="sm"
                variant="outline"
                onClick={handleGenerate}
                className="w-full gap-2"
              >
                <Sparkles className="h-4 w-4" />
                {t("generateButton")}
              </Button>
            )}

            {generateState === "generating" && (
              <div className="flex items-center justify-center gap-2 text-sm text-muted-foreground py-4">
                <Loader2 className="h-4 w-4 animate-spin" />
                {t("generating")}
              </div>
            )}

            {generateState === "generated" && aiResponse && (
              <div className="space-y-3">
                {/* Confidence Gauge */}
                <div className="flex items-center justify-between">
                  <span className="text-xs font-medium">Güven Skoru</span>
                  <ConfidenceGauge score={aiResponse.confidenceScore} size="sm" />
                </div>

                {/* Context Sources */}
                {aiResponse.contextSources && aiResponse.contextSources.length > 0 && (
                  <div className="space-y-1">
                    <div className="flex items-center gap-1 text-xs font-medium">
                      <BookOpen className="h-3 w-3" />
                      <span>Kaynaklar</span>
                    </div>
                    <div className="flex flex-wrap gap-1">
                      {aiResponse.contextSources.map((source, i) => (
                        <Badge
                          key={i}
                          variant="outline"
                          className={`text-xs ${SOURCE_COLORS[source.type] || SOURCE_COLORS.template}`}
                        >
                          {SOURCE_LABELS[source.type] || source.type}
                        </Badge>
                      ))}
                    </div>
                  </div>
                )}

                {/* AI Answer */}
                <div className="space-y-2">
                  <span className="text-xs font-medium">Önerilen Cevap</span>
                  {isEditing ? (
                    <Textarea
                      value={editedText}
                      onChange={(e) => setEditedText(e.target.value)}
                      rows={6}
                      className="text-sm"
                    />
                  ) : (
                    <div className="flex justify-end">
                      <div className="bg-blue-500 text-white rounded-2xl rounded-tr-sm p-3 max-w-[85%] shadow-sm">
                        <p className="text-sm">{editedText}</p>
                      </div>
                    </div>
                  )}
                </div>

                {/* Action Buttons */}
                <div className="flex flex-col gap-2">
                  {isEditing ? (
                    <div className="flex gap-2">
                      <Button size="sm" onClick={handleEditSave} className="flex-1 gap-2">
                        <Check className="h-4 w-4" />
                        Kaydet
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={handleEditCancel}
                        className="flex-1 gap-2"
                      >
                        <X className="h-4 w-4" />
                        İptal
                      </Button>
                    </div>
                  ) : (
                    <>
                      <Button
                        size="sm"
                        onClick={handleApprove}
                        disabled={approveMutation.isPending}
                        className="w-full gap-2"
                      >
                        {approveMutation.isPending ? (
                          <Loader2 className="h-4 w-4 animate-spin" />
                        ) : (
                          <Check className="h-4 w-4" />
                        )}
                        Onayla ve Gönder
                      </Button>
                      <div className="flex gap-2">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => setIsEditing(true)}
                          className="flex-1 gap-2"
                        >
                          <Edit2 className="h-4 w-4" />
                          Düzenle
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={handleReject}
                          className="flex-1 gap-2"
                        >
                          <X className="h-4 w-4" />
                          Reddet
                        </Button>
                      </div>
                    </>
                  )}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Answered State */}
        {question.status === "ANSWERED" && question.answers?.[0] && (
          <div className="space-y-2 pt-2 border-t">
            <div className="flex items-center gap-2">
              <Badge variant="outline" className="bg-green-50 text-green-700 border-green-200 dark:bg-green-900/20 dark:text-green-400 dark:border-green-800">
                <Check className="h-3 w-3 mr-1" />
                Cevaplandı
              </Badge>
              {question.answers[0].submittedAt && (
                <span className="text-xs text-muted-foreground">
                  {formatDistanceToNow(new Date(question.answers[0].submittedAt), {
                    addSuffix: true,
                    locale: tr,
                  })}
                </span>
              )}
            </div>
            <div className="flex justify-end">
              <div className="bg-blue-500 text-white rounded-2xl rounded-tr-sm p-3 max-w-[85%] shadow-sm">
                <p className="text-sm">{question.answers[0].answerText}</p>
              </div>
            </div>
          </div>
        )}

        {/* Cross-Sell Recommendations (shown when AI response includes them) */}
        {aiResponse?.crossSellProducts && aiResponse.crossSellProducts.length > 0 && (
          <div className="space-y-2 pt-2 border-t">
            <div className="flex items-center gap-1.5 text-xs font-medium">
              <ShoppingBag className="h-3.5 w-3.5 text-amber-600" />
              <span>Capraz Satis Onerileri</span>
            </div>
            <div className="space-y-1.5">
              {aiResponse.crossSellProducts.map((product: { barcode: string; title: string; image?: string; salePrice: number }, i: number) => (
                <div
                  key={i}
                  className="flex items-center gap-2 p-2 rounded-md bg-amber-50/50 dark:bg-amber-950/20 border border-amber-200/50 dark:border-amber-800/50"
                >
                  {product.image && (
                    <img
                      src={product.image}
                      alt=""
                      className="h-7 w-7 rounded object-cover"
                    />
                  )}
                  <div className="flex-1 min-w-0">
                    <p className="text-xs font-medium truncate">
                      {product.title}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {product.salePrice.toFixed(2)} TL
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
