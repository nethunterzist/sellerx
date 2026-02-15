"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import {
  useKnowledgeBase,
  useDeleteKnowledge,
  useToggleKnowledge,
} from "@/hooks/queries/use-ai";
import {
  useSuggestions,
  useApproveSuggestion,
  useRejectSuggestion,
  useModifySuggestion,
} from "@/hooks/queries/use-qa";
import { KnowledgeFormModal } from "./knowledge-form-modal";
import { CategoryBorderCard, type CategoryType } from "./category-border-card";
import { cn } from "@/lib/utils";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Switch } from "@/components/ui/switch";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { StaggerChildren } from "@/components/motion";
import {
  BookOpen,
  Plus,
  Edit2,
  Trash2,
  Lightbulb,
  Check,
  X,
  ChevronDown,
  ChevronUp,
  Loader2,
} from "lucide-react";
import { toast } from "sonner";
import type { KnowledgeBaseItem } from "@/types/ai";
import type { KnowledgeSuggestion, SuggestionPriority } from "@/types/qa";

interface KnowledgeHubProps {
  storeId: string;
}

const categoryColors: Record<string, string> = {
  shipping: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400",
  returns: "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400",
  payment: "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400",
  product: "bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400",
  general: "bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-400",
};

export function KnowledgeHub({ storeId }: KnowledgeHubProps) {
  const t = useTranslations("qa.aiBrain");
  const tSuggestions = useTranslations("qa.suggestions");

  const { data: knowledgeItems, isLoading: knowledgeLoading } = useKnowledgeBase(storeId);
  const deleteMutation = useDeleteKnowledge();
  const toggleMutation = useToggleKnowledge();

  const { data: suggestions, isLoading: suggestionsLoading } = useSuggestions(storeId, "PENDING");
  const approveMutation = useApproveSuggestion();
  const rejectMutation = useRejectSuggestion();
  const modifyMutation = useModifySuggestion();

  const [formOpen, setFormOpen] = useState(false);
  const [editItem, setEditItem] = useState<KnowledgeBaseItem | null>(null);

  // Suggestion editing state
  const [editingSuggestionId, setEditingSuggestionId] = useState<string | null>(null);
  const [editTitle, setEditTitle] = useState("");
  const [editContent, setEditContent] = useState("");
  const [expandedSuggestionId, setExpandedSuggestionId] = useState<string | null>(null);

  const handleDelete = async (id: string) => {
    if (!confirm(t("deleteConfirm"))) return;
    try {
      await deleteMutation.mutateAsync({ id, storeId });
    } catch {
      toast.error("Silinemedi");
    }
  };

  const handleToggle = async (id: string, active: boolean) => {
    try {
      await toggleMutation.mutateAsync({ id, active, storeId });
    } catch {
      toast.error("Güncellenemedi");
    }
  };

  const handleApproveSuggestion = async (suggestionId: string) => {
    try {
      await approveMutation.mutateAsync(suggestionId);
      toast.success(tSuggestions("approved"));
    } catch {
      toast.error("Onaylanamadı");
    }
  };

  const handleRejectSuggestion = async (suggestionId: string) => {
    try {
      await rejectMutation.mutateAsync({ suggestionId });
      toast.success(tSuggestions("rejected"));
    } catch {
      toast.error("Reddedilemedi");
    }
  };

  const handleStartEditSuggestion = (suggestion: KnowledgeSuggestion) => {
    setEditingSuggestionId(suggestion.id);
    setEditTitle(suggestion.suggestedTitle);
    setEditContent(suggestion.suggestedContent);
  };

  const handleSaveEditSuggestion = async (suggestionId: string) => {
    try {
      await modifyMutation.mutateAsync({ suggestionId, title: editTitle, content: editContent });
      setEditingSuggestionId(null);
      toast.success(tSuggestions("approved"));
    } catch {
      toast.error("Kaydedilemedi");
    }
  };

  const getPriorityBadge = (priority: SuggestionPriority) => {
    const styles = {
      HIGH: "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400",
      MEDIUM: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400",
      LOW: "bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-400",
    };
    const labels = { HIGH: tSuggestions("high"), MEDIUM: tSuggestions("medium"), LOW: tSuggestions("low") };
    return (
      <span className={`px-2 py-0.5 text-xs font-medium rounded-full ${styles[priority]}`}>
        {labels[priority]}
      </span>
    );
  };

  const activeCount = knowledgeItems?.filter((i) => i.isActive).length ?? 0;

  return (
    <StaggerChildren className="space-y-6">
      {/* Knowledge Base Section */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center gap-2">
              <BookOpen className="h-5 w-5 text-blue-500" />
              {t("knowledgeBase")}
              {activeCount > 0 && (
                <Badge variant="secondary" className="text-xs">
                  {t("activeItems", { count: activeCount })}
                </Badge>
              )}
            </CardTitle>
            <Button size="sm" onClick={() => { setEditItem(null); setFormOpen(true); }}>
              <Plus className="h-4 w-4 mr-1" />
              {t("addKnowledge")}
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          {knowledgeLoading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
            </div>
          ) : !knowledgeItems || knowledgeItems.length === 0 ? (
            <div className="text-center py-10">
              <BookOpen className="h-10 w-10 mx-auto text-muted-foreground/40 mb-3" />
              <p className="text-sm text-muted-foreground">{t("emptyKnowledge")}</p>
            </div>
          ) : (
            <div className="space-y-3">
              {knowledgeItems.map((item) => (
                <CategoryBorderCard
                  key={item.id}
                  category={item.category as CategoryType}
                  className={cn("group", !item.isActive && "opacity-50")}
                >
                  <CardContent className="p-3">
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-1">
                          <Badge
                            variant="outline"
                            className={`text-xs ${categoryColors[item.category] || categoryColors.general}`}
                          >
                            {item.category}
                          </Badge>
                          <span className="font-medium text-sm truncate">{item.title}</span>
                        </div>
                        <p className="text-sm text-muted-foreground line-clamp-2">{item.content}</p>
                        {item.keywords.length > 0 && (
                          <div className="flex flex-wrap gap-1 mt-2">
                            {item.keywords.map((kw, i) => (
                              <Badge key={i} variant="secondary" className="text-xs">
                                {kw}
                              </Badge>
                            ))}
                          </div>
                        )}
                      </div>
                      <div className="flex items-center gap-2 flex-shrink-0">
                        <Switch
                          checked={item.isActive}
                          onCheckedChange={(v) => handleToggle(item.id, v)}
                        />
                        <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity duration-200 sm:opacity-0 max-sm:opacity-100">
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8"
                            onClick={() => { setEditItem(item); setFormOpen(true); }}
                          >
                            <Edit2 className="h-3.5 w-3.5" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8 text-destructive hover:text-destructive"
                            onClick={() => handleDelete(item.id)}
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </Button>
                        </div>
                      </div>
                    </div>
                  </CardContent>
                </CategoryBorderCard>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Learning Suggestions Section */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Lightbulb className="h-5 w-5 text-yellow-500" />
            {t("learningSuggestions")}
            {suggestions && suggestions.length > 0 && (
              <Badge variant="secondary" className="text-xs">
                {suggestions.length} {tSuggestions("newSuggestions")}
              </Badge>
            )}
          </CardTitle>
        </CardHeader>
        <CardContent>
          {suggestionsLoading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
            </div>
          ) : !suggestions || suggestions.length === 0 ? (
            <div className="text-center py-10">
              <Lightbulb className="h-10 w-10 mx-auto text-muted-foreground/40 mb-3" />
              <p className="text-sm text-muted-foreground">{t("emptySuggestions")}</p>
            </div>
          ) : (
            <div className="space-y-3">
              {suggestions.map((suggestion) => (
                <div key={suggestion.id} className="border rounded-lg overflow-hidden">
                  <div className="p-3 bg-muted/30 border-b">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        {getPriorityBadge(suggestion.priority)}
                        <span className="text-xs text-muted-foreground">
                          {tSuggestions("peopleAsked", { count: suggestion.questionCount })}
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="p-3 space-y-3">
                    {editingSuggestionId === suggestion.id ? (
                      <div className="space-y-3">
                        <Input
                          value={editTitle}
                          onChange={(e) => setEditTitle(e.target.value)}
                          placeholder={tSuggestions("titleLabel")}
                        />
                        <Textarea
                          value={editContent}
                          onChange={(e) => setEditContent(e.target.value)}
                          rows={3}
                        />
                        <div className="flex gap-2">
                          <Button
                            size="sm"
                            onClick={() => handleSaveEditSuggestion(suggestion.id)}
                            disabled={modifyMutation.isPending}
                          >
                            {modifyMutation.isPending ? (
                              <Loader2 className="h-3 w-3 animate-spin mr-1" />
                            ) : (
                              <Check className="h-3 w-3 mr-1" />
                            )}
                            {tSuggestions("save")}
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => setEditingSuggestionId(null)}
                          >
                            {tSuggestions("cancel")}
                          </Button>
                        </div>
                      </div>
                    ) : (
                      <>
                        <div>
                          <p className="font-medium text-sm">{suggestion.suggestedTitle}</p>
                          <p className="text-sm text-muted-foreground mt-1 line-clamp-2">
                            {suggestion.suggestedContent}
                          </p>
                        </div>

                        {suggestion.sampleQuestions.length > 0 && (
                          <div>
                            <button
                              onClick={() =>
                                setExpandedSuggestionId(
                                  expandedSuggestionId === suggestion.id ? null : suggestion.id
                                )
                              }
                              className="flex items-center gap-1 text-xs text-primary hover:underline"
                            >
                              {tSuggestions("sampleQuestions")}
                              {expandedSuggestionId === suggestion.id ? (
                                <ChevronUp className="h-3 w-3" />
                              ) : (
                                <ChevronDown className="h-3 w-3" />
                              )}
                            </button>
                            {expandedSuggestionId === suggestion.id && (
                              <ul className="mt-1 space-y-0.5 pl-3">
                                {suggestion.sampleQuestions.map((q, i) => (
                                  <li key={i} className="text-xs text-muted-foreground list-disc">
                                    "{q}"
                                  </li>
                                ))}
                              </ul>
                            )}
                          </div>
                        )}

                        <div className="flex gap-2">
                          <Button
                            size="sm"
                            onClick={() => handleApproveSuggestion(suggestion.id)}
                            disabled={approveMutation.isPending}
                          >
                            {approveMutation.isPending ? (
                              <Loader2 className="h-3 w-3 animate-spin mr-1" />
                            ) : (
                              <Check className="h-3 w-3 mr-1" />
                            )}
                            {tSuggestions("approve")}
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleStartEditSuggestion(suggestion)}
                          >
                            <Edit2 className="h-3 w-3 mr-1" />
                            {tSuggestions("edit")}
                          </Button>
                          <Button
                            size="sm"
                            variant="ghost"
                            className="text-destructive hover:text-destructive"
                            onClick={() => handleRejectSuggestion(suggestion.id)}
                            disabled={rejectMutation.isPending}
                          >
                            {rejectMutation.isPending ? (
                              <Loader2 className="h-3 w-3 animate-spin mr-1" />
                            ) : (
                              <X className="h-3 w-3 mr-1" />
                            )}
                            {tSuggestions("reject")}
                          </Button>
                        </div>
                      </>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <KnowledgeFormModal
        open={formOpen}
        onClose={() => { setFormOpen(false); setEditItem(null); }}
        storeId={storeId}
        editItem={editItem}
      />
    </StaggerChildren>
  );
}
