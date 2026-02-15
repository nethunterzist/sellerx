"use client";

import { useTranslations } from "next-intl";
import { useQuestions } from "@/hooks/queries/use-qa";
import { StaggerChildren } from "@/components/motion";
import { Bot, Check, Loader2 } from "lucide-react";
import { formatDistanceToNow } from "date-fns";
import { tr } from "date-fns/locale";

interface AiActivityFeedProps {
  storeId: string;
  limit?: number;
}

export function AiActivityFeed({ storeId, limit = 5 }: AiActivityFeedProps) {
  const t = useTranslations("qa.activityFeed");
  const { data, isLoading } = useQuestions(storeId, "ANSWERED", 0, limit);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8">
        <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
      </div>
    );
  }

  const questions = data?.content ?? [];

  if (questions.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-8 text-muted-foreground gap-2">
        <Bot className="h-8 w-8" />
        <p className="text-sm">Hen\u00FCz AI aktivitesi yok</p>
      </div>
    );
  }

  return (
    <StaggerChildren className="space-y-1">
      {questions.map((q) => {
        const truncated =
          q.customerQuestion.length > 60
            ? q.customerQuestion.slice(0, 60) + "..."
            : q.customerQuestion;

        const relativeTime = formatDistanceToNow(new Date(q.questionDate), {
          addSuffix: true,
          locale: tr,
        });

        return (
          <div
            key={q.id}
            className="flex items-start gap-2 py-1.5 px-2 rounded-md hover:bg-muted/50 transition-colors"
          >
            <div className="mt-0.5 flex-shrink-0 h-5 w-5 rounded-full bg-primary/10 flex items-center justify-center">
              <Bot className="h-3 w-3 text-primary" />
            </div>
            <div className="min-w-0 flex-1">
              <p className="text-sm text-foreground truncate">{truncated}</p>
              <div className="flex items-center gap-1 text-xs text-muted-foreground">
                <Check className="h-3 w-3 text-green-500" />
                <span>{relativeTime}</span>
              </div>
            </div>
          </div>
        );
      })}
    </StaggerChildren>
  );
}
