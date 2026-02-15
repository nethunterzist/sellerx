"use client";

import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { Clock, MessageSquare, Check, AlertCircle } from "lucide-react";
import { formatDistanceToNow } from "date-fns";
import { tr } from "date-fns/locale";
import type { Question } from "@/types/qa";

interface QuestionPreviewCardProps {
  question: Question;
  isSelected: boolean;
  onClick: () => void;
}

const STATUS_CONFIG = {
  PENDING: {
    color: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400",
    icon: Clock,
    dot: "bg-yellow-500",
  },
  ANSWERED: {
    color: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400",
    icon: Check,
    dot: "bg-green-500",
  },
} as const;

export function QuestionPreviewCard({
  question,
  isSelected,
  onClick,
}: QuestionPreviewCardProps) {
  const relativeTime = formatDistanceToNow(new Date(question.questionDate), {
    addSuffix: true,
    locale: tr,
  });

  const statusConfig = STATUS_CONFIG[question.status];
  const StatusIcon = statusConfig.icon;

  return (
    <button
      onClick={onClick}
      className={cn(
        "w-full text-left p-3 border-b transition-all hover:bg-muted/50",
        isSelected && "bg-gradient-to-r from-blue-50/50 to-blue-100/50 dark:from-blue-950/20 dark:to-blue-900/20 border-l-4 border-l-blue-500"
      )}
    >
      <div className="space-y-2">
        {/* Status dot + Product title */}
        <div className="flex items-center gap-2">
          <div className={cn("h-2 w-2 rounded-full", statusConfig.dot)} />
          <span className="text-xs text-muted-foreground truncate">
            {question.productTitle || "Ürün bilgisi yok"}
          </span>
        </div>

        {/* Question preview (2 lines max) */}
        <div className="flex items-start gap-2">
          <MessageSquare className="h-4 w-4 mt-0.5 text-muted-foreground flex-shrink-0" />
          <p className="text-sm font-medium line-clamp-2 leading-tight">
            {question.customerQuestion}
          </p>
        </div>

        {/* Footer: timestamp + status badge */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1 text-xs text-muted-foreground">
            <Clock className="h-3 w-3" />
            <span>{relativeTime}</span>
          </div>

          <Badge variant="outline" className={cn("text-xs h-5", statusConfig.color)}>
            <StatusIcon className="h-3 w-3 mr-1" />
            {question.status === "PENDING" ? "Bekliyor" : "Cevaplandı"}
          </Badge>
        </div>
      </div>
    </button>
  );
}
