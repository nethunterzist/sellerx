"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ConfidenceGauge } from "./confidence-gauge";
import { AnswerModal } from "./answer-modal";
import { useGenerateAiAnswer, useApproveAiAnswer } from "@/hooks/queries/use-ai";
import {
  Sparkles,
  Check,
  Edit2,
  X,
  Package,
  Clock,
  Loader2,
  ChevronDown,
  ChevronUp,
  MessageSquare,
  AlertCircle,
  BookOpen,
} from "lucide-react";
import { Textarea } from "@/components/ui/textarea";
import { toast } from "sonner";
import type { Question } from "@/types/qa";
import type { AiGenerateResponse } from "@/types/ai";
import { formatDistanceToNow } from "date-fns";
import { tr } from "date-fns/locale";

interface AiQuestionCardProps {
  question: Question;
  storeId: string;
  variant: "pending-approval" | "auto-answered" | "human-required";
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
  product: "\u00DCr\u00FCn",
  historical_qa: "Ge\u00E7mi\u015F S&S",
  knowledge_base: "Bilgi Taban\u0131",
  template: "\u015Eablon",
};

const BORDER_COLORS = {
  "pending-approval": "border-l-blue-500",
  "auto-answered": "border-l-green-500",
  "human-required": "border-l-orange-500",
} as const;

export function AiQuestionCard({
  question,
  storeId,
  variant,
  onRefresh,
}: AiQuestionCardProps) {
  const t = useTranslations("qa.answerFlow");
  const t2 = useTranslations("qa.questionCard");

  const [generateState, setGenerateState] = useState<GenerateState>("idle");
  const [aiResponse, setAiResponse] = useState<AiGenerateResponse | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [editedText, setEditedText] = useState("");
  const [expanded, setExpanded] = useState(false);
  const [answerModalOpen, setAnswerModalOpen] = useState(false);

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
    <>
      <Card className={`border-l-4 ${BORDER_COLORS[variant]}`}>
        <CardContent className="p-4 space-y-3">
          {/* Top row: product + time */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <Package className="h-3.5 w-3.5" />
              <span className="truncate max-w-[250px]">
                {question.productTitle || "\u00DCr\u00FCn bilgisi yok"}
              </span>
            </div>
            <div className="flex items-center gap-1 text-xs text-muted-foreground flex-shrink-0">
              <Clock className="h-3 w-3" />
              <span>{relativeTime}</span>
            </div>
          </div>

          {/* Customer question */}
          <div className="flex items-start gap-2">
            <MessageSquare className="h-4 w-4 mt-0.5 text-muted-foreground flex-shrink-0" />
            <p className="text-sm font-semibold">{question.customerQuestion}</p>
          </div>

          {/* Variant: pending-approval */}
          {variant === "pending-approval" && (
            <div className="space-y-3">
              {generateState === "idle" && (
                <Button
                  size="sm"
                  variant="outline"
                  onClick={handleGenerate}
                  className="gap-2"
                >
                  <Sparkles className="h-4 w-4" />
                  {t("generateButton")}
                </Button>
              )}

              {generateState === "generating" && (
                <div className="flex items-center gap-2 text-sm text-muted-foreground py-2">
                  <Loader2 className="h-4 w-4 animate-spin" />
                  {t("generating")}
                </div>
              )}

              {generateState === "generated" && aiResponse && (
                <AiResultSection
                  aiResponse={aiResponse}
                  editedText={editedText}
                  isEditing={isEditing}
                  onEditedTextChange={setEditedText}
                  onApprove={handleApprove}
                  onEdit={() => setIsEditing(true)}
                  onEditSave={handleEditSave}
                  onEditCancel={handleEditCancel}
                  onReject={handleReject}
                  isApproving={approveMutation.isPending}
                  t={t}
                />
              )}
            </div>
          )}

          {/* Variant: auto-answered */}
          {variant === "auto-answered" && (
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <Badge
                  variant="outline"
                  className="bg-green-50 text-green-700 border-green-200 dark:bg-green-900/20 dark:text-green-400 dark:border-green-800"
                >
                  <Check className="h-3 w-3 mr-1" />
                  {t2("answered")}
                </Badge>
                {question.answers?.[0]?.submittedAt && (
                  <span className="text-xs text-muted-foreground">
                    {formatDistanceToNow(new Date(question.answers[0].submittedAt), {
                      addSuffix: true,
                      locale: tr,
                    })}
                  </span>
                )}
              </div>
              <button
                onClick={() => setExpanded(!expanded)}
                className="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground transition-colors"
              >
                {expanded ? (
                  <ChevronUp className="h-3 w-3" />
                ) : (
                  <ChevronDown className="h-3 w-3" />
                )}
                {expanded ? t("hideAnswer") : t("showAnswer")}
              </button>
              {expanded && question.answers?.[0] && (
                <div className="p-3 bg-muted/50 rounded-md text-sm">
                  {question.answers[0].answerText}
                </div>
              )}
            </div>
          )}

          {/* Variant: human-required */}
          {variant === "human-required" && (
            <div className="space-y-3">
              {generateState === "idle" && (
                <div className="flex items-center gap-2">
                  <Button
                    size="sm"
                    onClick={() => setAnswerModalOpen(true)}
                    className="gap-2"
                  >
                    <Edit2 className="h-4 w-4" />
                    {t("manualAnswer")}
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={handleGenerate}
                    className="gap-2"
                  >
                    <Sparkles className="h-4 w-4" />
                    {t("generateButton")}
                  </Button>
                </div>
              )}

              {generateState === "generating" && (
                <div className="flex items-center gap-2 text-sm text-muted-foreground py-2">
                  <Loader2 className="h-4 w-4 animate-spin" />
                  {t("generating")}
                </div>
              )}

              {generateState === "generated" && aiResponse && (
                <AiResultSection
                  aiResponse={aiResponse}
                  editedText={editedText}
                  isEditing={isEditing}
                  onEditedTextChange={setEditedText}
                  onApprove={handleApprove}
                  onEdit={() => setIsEditing(true)}
                  onEditSave={handleEditSave}
                  onEditCancel={handleEditCancel}
                  onReject={handleReject}
                  isApproving={approveMutation.isPending}
                  t={t}
                />
              )}
            </div>
          )}
        </CardContent>
      </Card>

      <AnswerModal
        question={question}
        storeId={storeId}
        open={answerModalOpen}
        onClose={() => setAnswerModalOpen(false)}
        onSuccess={onRefresh}
      />
    </>
  );
}

/* Extracted sub-component for AI generation result display */
function AiResultSection({
  aiResponse,
  editedText,
  isEditing,
  onEditedTextChange,
  onApprove,
  onEdit,
  onEditSave,
  onEditCancel,
  onReject,
  isApproving,
  t,
}: {
  aiResponse: AiGenerateResponse;
  editedText: string;
  isEditing: boolean;
  onEditedTextChange: (text: string) => void;
  onApprove: () => void;
  onEdit: () => void;
  onEditSave: () => void;
  onEditCancel: () => void;
  onReject: () => void;
  isApproving: boolean;
  t: (key: string) => string;
}) {
  return (
    <div className="space-y-3 p-3 bg-muted/30 rounded-lg border border-border/50">
      {/* Confidence + answer */}
      <div className="flex items-start gap-3">
        <div className="flex-shrink-0">
          <ConfidenceGauge score={aiResponse.confidenceScore} size="sm" />
        </div>
        <div className="flex-1 min-w-0">
          {isEditing ? (
            <div className="space-y-2">
              <Textarea
                value={editedText}
                onChange={(e) => onEditedTextChange(e.target.value)}
                rows={4}
                className="text-sm"
              />
              <div className="flex gap-2">
                <Button size="sm" variant="outline" onClick={onEditSave}>
                  <Check className="h-3 w-3 mr-1" />
                  {t("saveEdit")}
                </Button>
                <Button size="sm" variant="ghost" onClick={onEditCancel}>
                  <X className="h-3 w-3 mr-1" />
                  {t("cancelEdit")}
                </Button>
              </div>
            </div>
          ) : (
            <p className="text-sm">{editedText}</p>
          )}
        </div>
      </div>

      {/* Context source badges */}
      {aiResponse.contextSources.length > 0 && (
        <div className="flex flex-wrap gap-1.5">
          {aiResponse.contextSources.map((source, idx) => (
            <Badge
              key={idx}
              variant="secondary"
              className={`text-xs ${SOURCE_COLORS[source.type] || SOURCE_COLORS.template}`}
            >
              {source.type === "product" && <Package className="h-2.5 w-2.5 mr-1" />}
              {source.type === "historical_qa" && <Clock className="h-2.5 w-2.5 mr-1" />}
              {source.type === "knowledge_base" && <BookOpen className="h-2.5 w-2.5 mr-1" />}
              {source.type === "template" && <AlertCircle className="h-2.5 w-2.5 mr-1" />}
              {SOURCE_LABELS[source.type] || source.type}
            </Badge>
          ))}
        </div>
      )}

      {/* Action buttons */}
      {!isEditing && (
        <div className="flex items-center gap-2">
          <Button
            size="sm"
            onClick={onApprove}
            disabled={isApproving}
            className="gap-1.5 bg-green-600 hover:bg-green-700 text-white"
          >
            {isApproving ? (
              <Loader2 className="h-3 w-3 animate-spin" />
            ) : (
              <Check className="h-3 w-3" />
            )}
            {t("approveAndSend")}
          </Button>
          <Button size="sm" variant="outline" onClick={onEdit} className="gap-1.5">
            <Edit2 className="h-3 w-3" />
            {t("edit")}
          </Button>
          <Button size="sm" variant="ghost" onClick={onReject} className="gap-1.5 text-muted-foreground">
            <X className="h-3 w-3" />
            {t("reject")}
          </Button>
        </div>
      )}
    </div>
  );
}
