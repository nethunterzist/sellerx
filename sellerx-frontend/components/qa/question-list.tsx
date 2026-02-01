"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ExternalLink, Package, Calendar, User, Bot, Sparkles, MessageSquarePlus } from "lucide-react";
import { AiAnswerPanel } from "./ai-answer-panel";
import { AnswerModal } from "./answer-modal";
import type { Question } from "@/types/qa";

interface QuestionListProps {
  questions: Question[];
  isLoading: boolean;
  onRefresh?: () => void;
  aiEnabled?: boolean;
  storeId: string;
}

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString("tr-TR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

interface QuestionCardProps {
  question: Question;
  aiEnabled?: boolean;
  isAiPanelOpen: boolean;
  onToggleAiPanel: () => void;
  onAiSuccess: () => void;
  onAnswerClick: () => void;
}

function QuestionCard({ question, aiEnabled, isAiPanelOpen, onToggleAiPanel, onAiSuccess, onAnswerClick }: QuestionCardProps) {
  const t = useTranslations("qa.questionCard");
  const isPending = question.status === "PENDING";

  return (
    <div className="space-y-3">
      <Card className={`${isPending ? "border-orange-200 bg-orange-50/30" : "border-green-200 bg-green-50/30"}`}>
        <CardContent className="p-4">
          <div className="flex items-start justify-between gap-4">
            <div className="flex-1 space-y-3">
              {/* Status Badge */}
              <div className="flex items-center gap-2">
                <Badge variant={isPending ? "outline" : "default"} className={isPending ? "border-orange-400 text-orange-600" : "bg-green-600"}>
                  {isPending ? t("pending") : t("answered")}
                </Badge>
                {question.isPublic && (
                  <Badge variant="secondary" className="text-xs">
                    {t("public")}
                  </Badge>
                )}
              </div>

              {/* Customer Question */}
              <div className="space-y-1">
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <User className="h-3 w-3" />
                  <span>{t("customerQuestion")}</span>
                </div>
                <p className="text-sm font-medium">{question.customerQuestion}</p>
              </div>

              {/* Product Info */}
              {question.productTitle && (
                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                  <Package className="h-3 w-3" />
                  <span className="truncate">{question.productTitle}</span>
                  {question.barcode && (
                    <Badge variant="outline" className="text-xs">
                      {question.barcode}
                    </Badge>
                  )}
                </div>
              )}

              {/* Date */}
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <Calendar className="h-3 w-3" />
                <span>{formatDate(question.questionDate)}</span>
              </div>

              {/* Answers - if any */}
              {question.answers && question.answers.length > 0 && (
                <div className="mt-3 pt-3 border-t space-y-2">
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <Bot className="h-3 w-3" />
                    <span>{t("answer")}</span>
                  </div>
                  {question.answers.map((answer) => (
                    <div key={answer.id} className="text-sm bg-white/50 p-2 rounded border">
                      <p>{answer.answerText}</p>
                      {answer.isSubmitted && (
                        <p className="text-xs text-green-600 mt-1">
                          {t("submittedToTrendyol")} - {answer.submittedAt && formatDate(answer.submittedAt)}
                        </p>
                      )}
                      {!answer.isSubmitted && (
                        <p className="text-xs text-orange-600 mt-1">
                          {t("draft")}
                        </p>
                      )}
                    </div>
                  ))}
                </div>
              )}

              {/* Action buttons for pending questions */}
              {isPending && (
                <div className="mt-3 pt-3 border-t flex flex-wrap gap-2">
                  {/* Manual Answer Button */}
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={onAnswerClick}
                    className="gap-2"
                  >
                    <MessageSquarePlus className="h-4 w-4" />
                    {t("answerButton")}
                  </Button>

                  {/* AI Button */}
                  {aiEnabled && (
                    <Button
                      variant={isAiPanelOpen ? "secondary" : "outline"}
                      size="sm"
                      onClick={onToggleAiPanel}
                      className="gap-2"
                    >
                      <Sparkles className="h-4 w-4 text-blue-600" />
                      {isAiPanelOpen ? t("closeAiPanel") : t("aiSuggestButton")}
                    </Button>
                  )}

                  {/* AI disabled note */}
                  {!aiEnabled && (
                    <div className="flex items-center gap-2 text-xs text-muted-foreground bg-gray-50 p-2 rounded">
                      <Sparkles className="h-3 w-3 text-gray-400" />
                      <span>{t("aiDisabledNote")}</span>
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* External Link to Trendyol */}
            {question.productId && (
              <Button variant="ghost" size="icon" asChild>
                <a
                  href={`https://www.trendyol.com/pd/-p-${question.productId}`}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <ExternalLink className="h-4 w-4" />
                </a>
              </Button>
            )}
          </div>
        </CardContent>
      </Card>

      {/* AI Answer Panel */}
      {isAiPanelOpen && (
        <AiAnswerPanel
          question={question}
          onClose={onToggleAiPanel}
          onSuccess={onAiSuccess}
        />
      )}
    </div>
  );
}

export function QuestionList({ questions, isLoading, onRefresh, aiEnabled, storeId }: QuestionListProps) {
  const t = useTranslations("qa.emptyState");
  const [openAiPanelId, setOpenAiPanelId] = useState<string | null>(null);
  const [answerModalQuestion, setAnswerModalQuestion] = useState<Question | null>(null);

  const handleToggleAiPanel = (questionId: string) => {
    setOpenAiPanelId(prev => prev === questionId ? null : questionId);
  };

  const handleAiSuccess = () => {
    setOpenAiPanelId(null);
    onRefresh?.();
  };

  const handleAnswerSuccess = () => {
    setAnswerModalQuestion(null);
    onRefresh?.();
  };

  if (isLoading) {
    return (
      <div className="space-y-4">
        {[1, 2, 3].map((i) => (
          <Card key={i} className="animate-pulse">
            <CardContent className="p-4 h-32" />
          </Card>
        ))}
      </div>
    );
  }

  if (questions.length === 0) {
    return (
      <Card>
        <CardContent className="p-8 text-center text-muted-foreground">
          <MessageSquareIcon className="h-12 w-12 mx-auto mb-4 opacity-50" />
          <p>{t("title")}</p>
          <p className="text-sm">{t("description")}</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <>
      <div className="space-y-4">
        {questions.map((question) => (
          <QuestionCard
            key={question.id}
            question={question}
            aiEnabled={aiEnabled}
            isAiPanelOpen={openAiPanelId === question.id}
            onToggleAiPanel={() => handleToggleAiPanel(question.id)}
            onAiSuccess={handleAiSuccess}
            onAnswerClick={() => setAnswerModalQuestion(question)}
          />
        ))}
      </div>

      {/* Answer Modal */}
      <AnswerModal
        question={answerModalQuestion}
        storeId={storeId}
        open={!!answerModalQuestion}
        onClose={() => setAnswerModalQuestion(null)}
        onSuccess={handleAnswerSuccess}
      />
    </>
  );
}

function MessageSquareIcon({ className }: { className?: string }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
    >
      <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
    </svg>
  );
}
