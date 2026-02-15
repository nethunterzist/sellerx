"use client";

import { useState, useEffect } from "react";
import { useTranslations } from "next-intl";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Loader2, X } from "lucide-react";
import { toast } from "sonner";
import { useCreateKnowledge, useUpdateKnowledge } from "@/hooks/queries/use-ai";
import { KNOWLEDGE_CATEGORIES } from "@/types/ai";
import type { KnowledgeBaseItem } from "@/types/ai";

interface KnowledgeFormModalProps {
  open: boolean;
  onClose: () => void;
  storeId: string;
  editItem?: KnowledgeBaseItem | null;
  onSuccess?: () => void;
}

export function KnowledgeFormModal({
  open,
  onClose,
  storeId,
  editItem,
  onSuccess,
}: KnowledgeFormModalProps) {
  const t = useTranslations("qa.aiBrain");

  const [category, setCategory] = useState("");
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [keywords, setKeywords] = useState<string[]>([]);
  const [keywordInput, setKeywordInput] = useState("");
  const [priority, setPriority] = useState("3");

  const createMutation = useCreateKnowledge();
  const updateMutation = useUpdateKnowledge();

  const isEditing = !!editItem;
  const isSubmitting = createMutation.isPending || updateMutation.isPending;

  useEffect(() => {
    if (editItem) {
      setCategory(editItem.category);
      setTitle(editItem.title);
      setContent(editItem.content);
      setKeywords(editItem.keywords || []);
      setPriority(String(editItem.priority || 3));
    } else {
      resetForm();
    }
  }, [editItem, open]);

  function resetForm() {
    setCategory("");
    setTitle("");
    setContent("");
    setKeywords([]);
    setKeywordInput("");
    setPriority("3");
  }

  function handleClose() {
    resetForm();
    onClose();
  }

  function handleKeywordInput(value: string) {
    if (value.includes(",")) {
      const parts = value.split(",");
      const newKeywords = parts
        .slice(0, -1)
        .map((k) => k.trim())
        .filter((k) => k.length > 0 && !keywords.includes(k));
      setKeywords((prev) => [...prev, ...newKeywords]);
      setKeywordInput(parts[parts.length - 1]);
    } else {
      setKeywordInput(value);
    }
  }

  function handleKeywordKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Enter" && keywordInput.trim()) {
      e.preventDefault();
      const trimmed = keywordInput.trim();
      if (!keywords.includes(trimmed)) {
        setKeywords((prev) => [...prev, trimmed]);
      }
      setKeywordInput("");
    }
    if (e.key === "Backspace" && !keywordInput && keywords.length > 0) {
      setKeywords((prev) => prev.slice(0, -1));
    }
  }

  function removeKeyword(keyword: string) {
    setKeywords((prev) => prev.filter((k) => k !== keyword));
  }

  function handleSubmit() {
    if (!category || !title.trim() || !content.trim()) return;

    const data = {
      category,
      title: title.trim(),
      content: content.trim(),
      keywords,
      priority: Number(priority),
    };

    if (isEditing && editItem) {
      updateMutation.mutate(
        { id: editItem.id, storeId, data },
        {
          onSuccess: () => {
            toast.success(t("save"));
            onSuccess?.();
            handleClose();
          },
        }
      );
    } else {
      createMutation.mutate(
        { storeId, data },
        {
          onSuccess: () => {
            toast.success(t("save"));
            onSuccess?.();
            handleClose();
          },
        }
      );
    }
  }

  const isValid = category && title.trim() && content.trim();

  return (
    <Dialog open={open} onOpenChange={(o) => !o && handleClose()}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>
            {isEditing ? t("editKnowledge") : t("addKnowledge")}
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-4 py-2">
          {/* Category */}
          <div className="space-y-1.5">
            <label className="text-sm font-medium">{t("category")}</label>
            <Select value={category} onValueChange={setCategory}>
              <SelectTrigger>
                <SelectValue placeholder={t("category")} />
              </SelectTrigger>
              <SelectContent>
                {KNOWLEDGE_CATEGORIES.map((cat) => (
                  <SelectItem key={cat.value} value={cat.value}>
                    {cat.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Title */}
          <div className="space-y-1.5">
            <label className="text-sm font-medium">{t("titleField")}</label>
            <Input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder={t("titleField")}
            />
          </div>

          {/* Content */}
          <div className="space-y-1.5">
            <label className="text-sm font-medium">{t("content")}</label>
            <Textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder={t("content")}
              rows={5}
            />
          </div>

          {/* Keywords */}
          <div className="space-y-1.5">
            <label className="text-sm font-medium">{t("keywords")}</label>
            <div className="flex flex-wrap gap-1.5 rounded-md border p-2 focus-within:ring-1 focus-within:ring-ring">
              {keywords.map((kw) => (
                <Badge key={kw} variant="secondary" className="gap-1">
                  {kw}
                  <button
                    type="button"
                    onClick={() => removeKeyword(kw)}
                    className="ml-0.5 rounded-full hover:bg-muted-foreground/20"
                  >
                    <X className="h-3 w-3" />
                  </button>
                </Badge>
              ))}
              <Input
                value={keywordInput}
                onChange={(e) => handleKeywordInput(e.target.value)}
                onKeyDown={handleKeywordKeyDown}
                placeholder={keywords.length === 0 ? t("keywordsPlaceholder") : ""}
                className="h-7 min-w-[120px] flex-1 border-0 p-0 shadow-none focus-visible:ring-0"
              />
            </div>
          </div>

          {/* Priority */}
          <div className="space-y-1.5">
            <label className="text-sm font-medium">{t("priority")}</label>
            <Select value={priority} onValueChange={setPriority}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {[1, 2, 3, 4, 5].map((p) => (
                  <SelectItem key={p} value={String(p)}>
                    {p}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={handleClose} disabled={isSubmitting}>
            {t("cancel")}
          </Button>
          <Button onClick={handleSubmit} disabled={!isValid || isSubmitting}>
            {isSubmitting ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                {t("saving")}
              </>
            ) : (
              t("save")
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
