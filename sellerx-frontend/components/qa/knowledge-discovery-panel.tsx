"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import {
  Lightbulb,
  Check,
  X,
  Edit2,
  ChevronDown,
  ChevronUp,
  Loader2,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Input } from "@/components/ui/input";
import {
  useSuggestions,
  useApproveSuggestion,
  useRejectSuggestion,
  useModifySuggestion,
} from "@/hooks/queries/use-qa";
import type { KnowledgeSuggestion, SuggestionPriority } from "@/types/qa";

interface KnowledgeDiscoveryPanelProps {
  storeId: string;
  onSuggestionApproved?: () => void;
}

export function KnowledgeDiscoveryPanel({
  storeId,
  onSuggestionApproved,
}: KnowledgeDiscoveryPanelProps) {
  const t = useTranslations("qa.suggestions");
  const { data: suggestions, isLoading } = useSuggestions(storeId, "PENDING");
  const approveMutation = useApproveSuggestion();
  const rejectMutation = useRejectSuggestion();
  const modifyMutation = useModifySuggestion();

  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editTitle, setEditTitle] = useState("");
  const [editContent, setEditContent] = useState("");

  const getPriorityBadge = (priority: SuggestionPriority) => {
    const styles = {
      HIGH: "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400",
      MEDIUM: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400",
      LOW: "bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-400",
    };

    const labels = {
      HIGH: t("highPriority"),
      MEDIUM: t("mediumPriority"),
      LOW: t("lowPriority"),
    };

    return (
      <span className={`px-2 py-1 text-xs font-medium rounded-full ${styles[priority]}`}>
        {labels[priority]}
      </span>
    );
  };

  const handleApprove = async (suggestionId: string) => {
    try {
      await approveMutation.mutateAsync(suggestionId);
      onSuggestionApproved?.();
    } catch (error) {
      console.error("Failed to approve suggestion:", error);
    }
  };

  const handleReject = async (suggestionId: string) => {
    try {
      await rejectMutation.mutateAsync({ suggestionId });
    } catch (error) {
      console.error("Failed to reject suggestion:", error);
    }
  };

  const handleStartEdit = (suggestion: KnowledgeSuggestion) => {
    setEditingId(suggestion.id);
    setEditTitle(suggestion.suggestedTitle);
    setEditContent(suggestion.suggestedContent);
  };

  const handleCancelEdit = () => {
    setEditingId(null);
    setEditTitle("");
    setEditContent("");
  };

  const handleSaveEdit = async (suggestionId: string) => {
    try {
      await modifyMutation.mutateAsync({
        suggestionId,
        title: editTitle,
        content: editContent,
      });
      setEditingId(null);
      onSuggestionApproved?.();
    } catch (error) {
      console.error("Failed to modify suggestion:", error);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!suggestions || suggestions.length === 0) {
    return (
      <div className="text-center py-12">
        <Lightbulb className="h-12 w-12 mx-auto text-muted-foreground/50 mb-4" />
        <p className="text-muted-foreground">{t("noSuggestions")}</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-lg font-semibold flex items-center gap-2">
            <Lightbulb className="h-5 w-5 text-yellow-500" />
            {t("title")}
          </h3>
          <p className="text-sm text-muted-foreground">{t("description")}</p>
        </div>
        <span className="px-3 py-1 bg-primary/10 text-primary text-sm font-medium rounded-full">
          {suggestions.length} {t("newSuggestions")}
        </span>
      </div>

      <div className="space-y-4">
        {suggestions.map((suggestion) => (
          <div
            key={suggestion.id}
            className="border rounded-lg overflow-hidden bg-card"
          >
            {/* Header */}
            <div className="p-4 border-b bg-muted/30">
              <div className="flex items-start justify-between">
                <div className="space-y-1">
                  {getPriorityBadge(suggestion.priority)}
                  <p className="text-sm text-muted-foreground mt-2">
                    {t("peopleAsked", { count: suggestion.questionCount })}
                  </p>
                </div>
              </div>
            </div>

            {/* Content */}
            <div className="p-4 space-y-4">
              {editingId === suggestion.id ? (
                /* Edit Mode */
                <div className="space-y-4">
                  <div>
                    <label className="text-sm font-medium mb-1 block">
                      {t("suggestedTitle")}
                    </label>
                    <Input
                      value={editTitle}
                      onChange={(e) => setEditTitle(e.target.value)}
                      placeholder={t("titlePlaceholder")}
                    />
                  </div>
                  <div>
                    <label className="text-sm font-medium mb-1 block">
                      {t("suggestedAnswer")}
                    </label>
                    <Textarea
                      value={editContent}
                      onChange={(e) => setEditContent(e.target.value)}
                      rows={4}
                      placeholder={t("answerPlaceholder")}
                    />
                  </div>
                  <div className="flex gap-2">
                    <Button
                      size="sm"
                      onClick={() => handleSaveEdit(suggestion.id)}
                      disabled={modifyMutation.isPending}
                    >
                      {modifyMutation.isPending ? (
                        <Loader2 className="h-4 w-4 animate-spin mr-1" />
                      ) : (
                        <Check className="h-4 w-4 mr-1" />
                      )}
                      {t("saveAndApprove")}
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={handleCancelEdit}
                    >
                      {t("cancel")}
                    </Button>
                  </div>
                </div>
              ) : (
                /* View Mode */
                <>
                  <div>
                    <p className="text-sm font-medium text-muted-foreground mb-1">
                      {t("suggestedTitle")}
                    </p>
                    <p className="font-medium">{suggestion.suggestedTitle}</p>
                  </div>

                  <div className="bg-muted/50 rounded-lg p-3">
                    <p className="text-sm font-medium text-muted-foreground mb-1">
                      {t("suggestedAnswer")}
                    </p>
                    <p className="text-sm">{suggestion.suggestedContent}</p>
                  </div>

                  {/* Sample Questions Accordion */}
                  <div>
                    <button
                      onClick={() =>
                        setExpandedId(
                          expandedId === suggestion.id ? null : suggestion.id
                        )
                      }
                      className="flex items-center gap-1 text-sm text-primary hover:underline"
                    >
                      {t("sampleQuestions")}
                      {expandedId === suggestion.id ? (
                        <ChevronUp className="h-4 w-4" />
                      ) : (
                        <ChevronDown className="h-4 w-4" />
                      )}
                    </button>

                    {expandedId === suggestion.id && (
                      <ul className="mt-2 space-y-1 pl-4">
                        {suggestion.sampleQuestions.map((q, idx) => (
                          <li
                            key={idx}
                            className="text-sm text-muted-foreground list-disc"
                          >
                            "{q}"
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>

                  {/* Action Buttons */}
                  <div className="flex gap-2 pt-2">
                    <Button
                      size="sm"
                      onClick={() => handleApprove(suggestion.id)}
                      disabled={approveMutation.isPending}
                    >
                      {approveMutation.isPending ? (
                        <Loader2 className="h-4 w-4 animate-spin mr-1" />
                      ) : (
                        <Check className="h-4 w-4 mr-1" />
                      )}
                      {t("approve")}
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => handleStartEdit(suggestion)}
                    >
                      <Edit2 className="h-4 w-4 mr-1" />
                      {t("edit")}
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      className="text-destructive hover:text-destructive"
                      onClick={() => handleReject(suggestion.id)}
                      disabled={rejectMutation.isPending}
                    >
                      {rejectMutation.isPending ? (
                        <Loader2 className="h-4 w-4 animate-spin mr-1" />
                      ) : (
                        <X className="h-4 w-4 mr-1" />
                      )}
                      {t("reject")}
                    </Button>
                  </div>
                </>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
