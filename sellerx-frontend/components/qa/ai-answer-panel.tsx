"use client";

import { useState } from "react";
import { useGenerateAiAnswer, useApproveAiAnswer } from "@/hooks/queries/use-ai";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Loader2, Sparkles, Check, X, Edit2, BookOpen, Package, Clock, AlertCircle } from "lucide-react";
import { toast } from "sonner";
import type { AiGenerateResponse } from "@/types/ai";
import type { Question } from "@/types/qa";

interface AiAnswerPanelProps {
  question: Question;
  onClose: () => void;
  onSuccess: () => void;
}

export function AiAnswerPanel({ question, onClose, onSuccess }: AiAnswerPanelProps) {
  const [aiResponse, setAiResponse] = useState<AiGenerateResponse | null>(null);
  const [editedAnswer, setEditedAnswer] = useState("");
  const [isEditing, setIsEditing] = useState(false);

  const generateMutation = useGenerateAiAnswer();
  const approveMutation = useApproveAiAnswer();

  const handleGenerate = async () => {
    try {
      const response = await generateMutation.mutateAsync(question.id);
      setAiResponse(response);
      setEditedAnswer(response.generatedAnswer);
    } catch (error: any) {
      toast.error(error.message || "AI cevap üretilemedi");
    }
  };

  const handleApprove = async () => {
    if (!aiResponse) return;

    try {
      await approveMutation.mutateAsync({
        questionId: question.id,
        data: {
          logId: aiResponse.logId,
          finalAnswer: editedAnswer,
        },
      });
      toast.success("Cevap Trendyol'a gönderildi");
      onSuccess();
    } catch (error: any) {
      toast.error(error.message || "Cevap gönderilemedi");
    }
  };

  const confidenceColor =
    aiResponse && aiResponse.confidenceScore >= 0.8
      ? "text-green-600 bg-green-50"
      : aiResponse && aiResponse.confidenceScore >= 0.6
      ? "text-yellow-600 bg-yellow-50"
      : "text-red-600 bg-red-50";

  const wasEdited = aiResponse && editedAnswer !== aiResponse.generatedAnswer;

  return (
    <Card className="border-blue-200 bg-blue-50/30">
      <CardHeader className="pb-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Sparkles className="h-5 w-5 text-blue-600" />
            <CardTitle className="text-base">AI Cevap Önerisi</CardTitle>
          </div>
          <Button variant="ghost" size="icon" onClick={onClose}>
            <X className="h-4 w-4" />
          </Button>
        </div>
      </CardHeader>

      <CardContent className="space-y-4">
        {/* Question Summary */}
        <div className="text-sm space-y-1">
          <div className="flex items-center gap-2 text-muted-foreground">
            <Package className="h-3 w-3" />
            <span className="truncate">{question.productTitle || "Ürün bilgisi yok"}</span>
          </div>
          <p className="font-medium">{question.customerQuestion}</p>
        </div>

        {/* Generate Button - Before Generation */}
        {!aiResponse && !generateMutation.isPending && (
          <Button onClick={handleGenerate} className="w-full bg-blue-600 hover:bg-blue-700">
            <Sparkles className="h-4 w-4 mr-2" />
            AI Cevap Üret
          </Button>
        )}

        {/* Loading State */}
        {generateMutation.isPending && (
          <div className="flex flex-col items-center justify-center py-8 space-y-3">
            <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
            <p className="text-sm text-muted-foreground">AI cevap üretiyor...</p>
          </div>
        )}

        {/* AI Response */}
        {aiResponse && (
          <div className="space-y-4">
            {/* Confidence Score */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Badge variant="outline" className={confidenceColor}>
                  Güven: %{Math.round(aiResponse.confidenceScore * 100)}
                </Badge>
                {aiResponse.confidenceScore < 0.7 && (
                  <div className="flex items-center gap-1 text-xs text-amber-600">
                    <AlertCircle className="h-3 w-3" />
                    Düşük güven skoru - kontrol edin
                  </div>
                )}
              </div>
              <div className="flex items-center gap-1 text-xs text-muted-foreground">
                <Clock className="h-3 w-3" />
                {aiResponse.generationTimeMs}ms
              </div>
            </div>

            {/* Context Sources */}
            {aiResponse.contextSources && aiResponse.contextSources.length > 0 && (
              <div className="space-y-2">
                <div className="flex items-center gap-2 text-sm font-medium">
                  <BookOpen className="h-4 w-4" />
                  Kullanılan Kaynaklar
                </div>
                <div className="flex flex-wrap gap-2">
                  {aiResponse.contextSources.map((source, index) => (
                    <Badge key={index} variant="secondary" className="text-xs">
                      {source.type === "product" && "Ürün"}
                      {source.type === "historical_qa" && "Geçmiş Soru"}
                      {source.type === "knowledge_base" && "Bilgi Bankası"}
                      {source.type === "template" && "Şablon"}
                      : {source.title}
                    </Badge>
                  ))}
                </div>
              </div>
            )}

            {/* Answer Editor */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium">Önerilen Cevap</span>
                {!isEditing && (
                  <Button variant="ghost" size="sm" onClick={() => setIsEditing(true)}>
                    <Edit2 className="h-3 w-3 mr-1" />
                    Düzenle
                  </Button>
                )}
              </div>
              {isEditing ? (
                <Textarea
                  value={editedAnswer}
                  onChange={(e) => setEditedAnswer(e.target.value)}
                  rows={6}
                  className="bg-white"
                />
              ) : (
                <div className="bg-white p-3 rounded-md border text-sm whitespace-pre-wrap">
                  {editedAnswer}
                </div>
              )}
              {wasEdited && (
                <p className="text-xs text-amber-600 flex items-center gap-1">
                  <Edit2 className="h-3 w-3" />
                  Cevap düzenlendi
                </p>
              )}
            </div>
          </div>
        )}
      </CardContent>

      {/* Actions */}
      {aiResponse && (
        <CardFooter className="flex gap-2">
          <Button
            variant="outline"
            className="flex-1"
            onClick={onClose}
            disabled={approveMutation.isPending}
          >
            İptal
          </Button>
          <Button
            className="flex-1 bg-green-600 hover:bg-green-700"
            onClick={handleApprove}
            disabled={approveMutation.isPending || !editedAnswer.trim()}
          >
            {approveMutation.isPending ? (
              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
            ) : (
              <Check className="h-4 w-4 mr-2" />
            )}
            Onayla ve Gönder
          </Button>
        </CardFooter>
      )}
    </Card>
  );
}
