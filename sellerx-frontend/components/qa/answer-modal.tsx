"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Loader2, Package } from "lucide-react";
import { useSubmitAnswer } from "@/hooks/queries/use-qa";
import { toast } from "sonner";
import type { Question } from "@/types/qa";

interface AnswerModalProps {
  question: Question | null;
  storeId: string;
  open: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

export function AnswerModal({ question, storeId, open, onClose, onSuccess }: AnswerModalProps) {
  const t = useTranslations("qa.answerModal");
  const [answerText, setAnswerText] = useState("");

  const mutation = useSubmitAnswer();

  if (!question) return null;

  const handleSubmit = async () => {
    if (!answerText.trim()) return;

    try {
      await mutation.mutateAsync({
        storeId,
        questionId: question.questionId,
        answerText: answerText.trim(),
      });
      toast.success(t("success"));
      setAnswerText("");
      onSuccess?.();
      onClose();
    } catch (error: any) {
      toast.error(error.message || t("error"));
    }
  };

  const handleClose = () => {
    if (!mutation.isPending) {
      setAnswerText("");
      onClose();
    }
  };

  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && handleClose()}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>{t("title")}</DialogTitle>
          <DialogDescription>{t("description")}</DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          {/* Question Display */}
          <div className="p-3 bg-muted rounded-lg space-y-2">
            <p className="text-sm font-medium">{question.customerQuestion}</p>
            {question.productTitle && (
              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                <Package className="h-3 w-3" />
                <span>{question.productTitle}</span>
              </div>
            )}
          </div>

          {/* Answer Textarea */}
          <div className="space-y-2">
            <Textarea
              value={answerText}
              onChange={(e) => setAnswerText(e.target.value)}
              placeholder={t("placeholder")}
              rows={5}
              maxLength={2000}
              disabled={mutation.isPending}
            />
            <p className="text-xs text-muted-foreground text-right">
              {t("charCount", { count: answerText.length })}
            </p>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={handleClose} disabled={mutation.isPending}>
            {t("cancel")}
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={mutation.isPending || !answerText.trim()}
          >
            {mutation.isPending ? (
              <>
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                {t("submitting")}
              </>
            ) : (
              t("submit")
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
