"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useQuestions, useQaStats } from "@/hooks/queries/use-qa";
import { QuestionPreviewCard } from "./question-preview-card";
import { QuestionDetailPanel } from "./question-detail-panel";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import {
  Clock,
  CheckCircle2,
  UserRound,
  Loader2,
  MessageSquare,
} from "lucide-react";

interface AnswerFlowSplitViewProps {
  storeId: string;
  aiEnabled: boolean;
}

type FilterType = "pending" | "answered" | "human";

interface FilterTab {
  id: FilterType;
  label: string;
  icon: typeof Clock;
  color: string;
}

const FILTER_TABS: FilterTab[] = [
  {
    id: "pending",
    label: "Bekleyen",
    icon: Clock,
    color: "text-yellow-600 dark:text-yellow-400",
  },
  {
    id: "answered",
    label: "Cevaplanan",
    icon: CheckCircle2,
    color: "text-green-600 dark:text-green-400",
  },
  {
    id: "human",
    label: "Manuel",
    icon: UserRound,
    color: "text-orange-600 dark:text-orange-400",
  },
];

export function AnswerFlowSplitView({ storeId }: AnswerFlowSplitViewProps) {
  const t = useTranslations("qa.answerFlow");
  const tPagination = useTranslations("qa.pagination");

  const [filter, setFilter] = useState<FilterType>("pending");
  const [selectedQuestionId, setSelectedQuestionId] = useState<string | null>(null);
  const [page, setPage] = useState(0);

  const status = filter === "pending" ? "PENDING" : "ANSWERED";

  const {
    data: questionsData,
    isLoading,
    refetch,
  } = useQuestions(storeId, status, page, 10);

  const { data: statsData, refetch: refetchStats } = useQaStats(storeId);

  const handleRefresh = () => {
    refetch();
    refetchStats();
  };

  const questions = questionsData?.content || [];
  const selectedQuestion = questions.find((q) => q.id === selectedQuestionId);

  // Auto-select first question when switching filters or loading
  const handleFilterChange = (newFilter: FilterType) => {
    setFilter(newFilter);
    setPage(0);
    setSelectedQuestionId(null);
  };

  // Set first question as selected when questions load
  if (!selectedQuestionId && questions.length > 0) {
    setSelectedQuestionId(questions[0].id);
  }

  const handleQuestionSelect = (questionId: string) => {
    setSelectedQuestionId(questionId);
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-[40%_60%] gap-4 h-[calc(100vh-200px)]">
      {/* Left: Question List */}
      <div className="flex flex-col h-full border rounded-lg overflow-hidden glass-card">
        {/* Filter tabs */}
        <div className="flex border-b p-2 gap-1 bg-muted/30">
          {FILTER_TABS.map((tab) => {
            const Icon = tab.icon;
            const isActive = filter === tab.id;
            const count =
              tab.id === "pending"
                ? statsData?.pendingQuestions ?? 0
                : tab.id === "answered"
                  ? statsData?.answeredQuestions ?? 0
                  : 0;

            return (
              <button
                key={tab.id}
                onClick={() => handleFilterChange(tab.id)}
                className={cn(
                  "flex items-center gap-2 px-3 py-2 rounded-md text-sm font-medium transition-colors flex-1",
                  isActive
                    ? "bg-background shadow-sm"
                    : "hover:bg-background/50 text-muted-foreground"
                )}
              >
                <Icon className={cn("h-4 w-4", isActive && tab.color)} />
                <span className="hidden sm:inline">{tab.label}</span>
                {count > 0 && (
                  <Badge variant="secondary" className="ml-auto text-xs h-5 px-1.5">
                    {count}
                  </Badge>
                )}
              </button>
            );
          })}
        </div>

        {/* Scrollable question list */}
        <div className="flex-1 overflow-y-auto">
          {questions.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-full text-center p-6">
              <MessageSquare className="h-12 w-12 text-muted-foreground/40 mb-3" />
              <p className="text-sm text-muted-foreground">{t("noQuestions")}</p>
            </div>
          ) : (
            questions.map((question) => (
              <QuestionPreviewCard
                key={question.id}
                question={question}
                isSelected={selectedQuestionId === question.id}
                onClick={() => handleQuestionSelect(question.id)}
              />
            ))
          )}
        </div>

        {/* Pagination */}
        {questionsData && questionsData.totalPages > 1 && (
          <div className="border-t p-3 bg-muted/30">
            <div className="flex items-center justify-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage(Math.max(0, page - 1))}
                disabled={page === 0}
              >
                {tPagination("previous")}
              </Button>
              <span className="text-xs text-muted-foreground">
                {tPagination("pageInfo", { current: page + 1, total: questionsData.totalPages })}
              </span>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage(page + 1)}
                disabled={page >= questionsData.totalPages - 1}
              >
                {tPagination("next")}
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Right: Detail Panel (Desktop) */}
      <div className="hidden lg:block h-full">
        {selectedQuestion ? (
          <QuestionDetailPanel
            question={selectedQuestion}
            storeId={storeId}
            onRefresh={handleRefresh}
          />
        ) : (
          <EmptyDetailState />
        )}
      </div>
    </div>
  );
}

function EmptyDetailState() {
  return (
    <div className="glass-card h-full flex flex-col items-center justify-center p-8 text-center">
      <MessageSquare className="h-16 w-16 text-muted-foreground/40 mb-4" />
      <p className="text-sm text-muted-foreground">
        Detayları görmek için sol taraftan bir soru seçin
      </p>
    </div>
  );
}
