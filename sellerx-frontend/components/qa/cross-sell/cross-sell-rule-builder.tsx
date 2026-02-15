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
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { Slider } from "@/components/ui/slider";
import { CrossSellProductSelector } from "./cross-sell-product-selector";
import { CrossSellPreviewPanel } from "./cross-sell-preview-panel";
import {
  useCreateCrossSellRule,
  useUpdateCrossSellRule,
} from "@/hooks/queries/use-cross-sell";
import { Loader2, Save, Plus, X, Zap } from "lucide-react";
import { toast } from "sonner";
import type {
  CrossSellRule,
  CrossSellSettings,
  TriggerCondition,
  TriggerType,
  RecommendationType,
  RecommendedProduct,
  RuleStatus,
  CreateCrossSellRuleRequest,
} from "@/types/cross-sell";

interface CrossSellRuleBuilderProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  storeId: string;
  editingRule?: CrossSellRule;
  settings?: CrossSellSettings;
}

const TRIGGER_TYPES: { value: TriggerType; labelKey: string }[] = [
  { value: "KEYWORD", labelKey: "keyword" },
  { value: "CATEGORY", labelKey: "category" },
  { value: "PRODUCT", labelKey: "product" },
  { value: "QUESTION_PATTERN", labelKey: "questionPattern" },
];

const RECOMMENDATION_TYPES: { value: RecommendationType; labelKey: string }[] = [
  { value: "COMPLEMENTARY", labelKey: "complementary" },
  { value: "UPSELL", labelKey: "upsell" },
  { value: "ALTERNATIVE", labelKey: "alternative" },
  { value: "BUNDLE", labelKey: "bundle" },
];

export function CrossSellRuleBuilder({
  open,
  onOpenChange,
  storeId,
  editingRule,
  settings,
}: CrossSellRuleBuilderProps) {
  const t = useTranslations("qa.crossSell.ruleBuilder");

  const createMutation = useCreateCrossSellRule();
  const updateMutation = useUpdateCrossSellRule();

  // Form state
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [triggerConditions, setTriggerConditions] = useState<TriggerCondition[]>([]);
  const [recommendationType, setRecommendationType] = useState<RecommendationType>("COMPLEMENTARY");
  const [recommendedProducts, setRecommendedProducts] = useState<RecommendedProduct[]>([]);
  const [maxRecommendations, setMaxRecommendations] = useState(3);
  const [messageTemplate, setMessageTemplate] = useState("");
  const [status, setStatus] = useState<RuleStatus>("ACTIVE");

  // Trigger input state
  const [newTriggerType, setNewTriggerType] = useState<TriggerType>("KEYWORD");
  const [newTriggerValue, setNewTriggerValue] = useState("");

  const isEditing = !!editingRule;
  const isPending = createMutation.isPending || updateMutation.isPending;

  // Populate form when editing
  useEffect(() => {
    if (editingRule) {
      setName(editingRule.name);
      setDescription(editingRule.description || "");
      setTriggerConditions(editingRule.triggerConditions);
      setRecommendationType(editingRule.recommendationType);
      setRecommendedProducts(editingRule.recommendedProducts);
      setMaxRecommendations(editingRule.maxRecommendations);
      setMessageTemplate(editingRule.messageTemplate || "");
      setStatus(editingRule.status);
    } else {
      resetForm();
    }
  }, [editingRule, open]);

  function resetForm() {
    setName("");
    setDescription("");
    setTriggerConditions([]);
    setRecommendationType("COMPLEMENTARY");
    setRecommendedProducts([]);
    setMaxRecommendations(3);
    setMessageTemplate("");
    setStatus("ACTIVE");
    setNewTriggerType("KEYWORD");
    setNewTriggerValue("");
  }

  const handleAddTrigger = () => {
    if (!newTriggerValue.trim()) return;

    const newTrigger: TriggerCondition = {
      type: newTriggerType,
      value: newTriggerValue.trim(),
      matchMode: newTriggerType === "KEYWORD" ? "CONTAINS" : undefined,
    };

    setTriggerConditions([...triggerConditions, newTrigger]);
    setNewTriggerValue("");
  };

  const handleRemoveTrigger = (index: number) => {
    setTriggerConditions(triggerConditions.filter((_, i) => i !== index));
  };

  const handleSubmit = async () => {
    // Validation
    if (!name.trim()) {
      toast.error(t("validation.nameRequired"));
      return;
    }
    if (triggerConditions.length === 0) {
      toast.error(t("validation.triggersRequired"));
      return;
    }
    if (recommendedProducts.length === 0) {
      toast.error(t("validation.productsRequired"));
      return;
    }

    const data: CreateCrossSellRuleRequest = {
      name: name.trim(),
      description: description.trim() || undefined,
      triggerConditions,
      recommendationType,
      recommendedProducts: recommendedProducts.map((p) => ({
        barcode: p.barcode,
        customMessage: p.customMessage,
        displayOrder: p.displayOrder,
      })),
      maxRecommendations,
      messageTemplate: messageTemplate.trim() || undefined,
      status,
    };

    try {
      if (isEditing && editingRule) {
        await updateMutation.mutateAsync({
          ruleId: editingRule.id,
          storeId,
          data,
        });
        toast.success(t("updateSuccess"));
      } else {
        await createMutation.mutateAsync({ storeId, data });
        toast.success(t("createSuccess"));
      }
      onOpenChange(false);
      resetForm();
    } catch {
      toast.error(isEditing ? t("updateError") : t("createError"));
    }
  };

  // Build preview rule object
  const previewRule: Partial<CrossSellRule> = {
    name,
    triggerConditions,
    recommendationType,
    recommendedProducts,
    maxRecommendations,
    messageTemplate: messageTemplate || undefined,
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-7xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Zap className="h-5 w-5 text-amber-500" />
            {isEditing ? t("editTitle") : t("createTitle")}
          </DialogTitle>
        </DialogHeader>

        <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
          {/* Left: Form (3 cols) */}
          <div className="lg:col-span-3 space-y-5">
            {/* Rule Name */}
            <div className="space-y-2">
              <Label>{t("name")}</Label>
              <Input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder={t("namePlaceholder")}
              />
            </div>

            {/* Description */}
            <div className="space-y-2">
              <Label>{t("description")}</Label>
              <Textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder={t("descriptionPlaceholder")}
                rows={2}
              />
            </div>

            {/* Trigger Conditions */}
            <div className="space-y-3">
              <Label>{t("triggers")}</Label>

              {/* Existing triggers */}
              {triggerConditions.length > 0 && (
                <div className="flex flex-wrap gap-1.5">
                  {triggerConditions.map((tc, i) => (
                    <Badge
                      key={i}
                      variant="secondary"
                      className="gap-1 pr-1"
                    >
                      <span className="text-xs opacity-60">{tc.type}:</span>
                      {tc.value}
                      <button
                        type="button"
                        onClick={() => handleRemoveTrigger(i)}
                        className="ml-0.5 rounded-full hover:bg-muted p-0.5"
                      >
                        <X className="h-3 w-3" />
                      </button>
                    </Badge>
                  ))}
                </div>
              )}

              {/* Add trigger */}
              <div className="flex items-center gap-2">
                <Select
                  value={newTriggerType}
                  onValueChange={(v) => setNewTriggerType(v as TriggerType)}
                >
                  <SelectTrigger className="w-40">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {TRIGGER_TYPES.map((tt) => (
                      <SelectItem key={tt.value} value={tt.value}>
                        {t(`triggerTypes.${tt.labelKey}`)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <Input
                  value={newTriggerValue}
                  onChange={(e) => setNewTriggerValue(e.target.value)}
                  placeholder={t("triggerValuePlaceholder")}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") {
                      e.preventDefault();
                      handleAddTrigger();
                    }
                  }}
                  className="flex-1"
                />
                <Button
                  type="button"
                  variant="outline"
                  size="icon"
                  onClick={handleAddTrigger}
                  disabled={!newTriggerValue.trim()}
                >
                  <Plus className="h-4 w-4" />
                </Button>
              </div>
            </div>

            {/* Recommendation Type */}
            <div className="space-y-2">
              <Label>{t("recommendationType")}</Label>
              <Select
                value={recommendationType}
                onValueChange={(v) =>
                  setRecommendationType(v as RecommendationType)
                }
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {RECOMMENDATION_TYPES.map((rt) => (
                    <SelectItem key={rt.value} value={rt.value}>
                      {t(`recommendationTypes.${rt.labelKey}`)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Product Selector */}
            <div className="space-y-2">
              <Label>{t("products")}</Label>
              <CrossSellProductSelector
                storeId={storeId}
                selectedProducts={recommendedProducts}
                onProductsChange={setRecommendedProducts}
                maxProducts={5}
              />
            </div>

            {/* Max Recommendations */}
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <Label>{t("maxRecommendations")}</Label>
                <span className="text-sm font-semibold text-blue-600">
                  {maxRecommendations}
                </span>
              </div>
              <Slider
                value={[maxRecommendations]}
                onValueChange={(v) => setMaxRecommendations(v[0])}
                min={1}
                max={5}
                step={1}
              />
            </div>

            {/* Message Template */}
            <div className="space-y-2">
              <Label>{t("messageTemplate")}</Label>
              <Textarea
                value={messageTemplate}
                onChange={(e) => setMessageTemplate(e.target.value)}
                placeholder={t("messageTemplatePlaceholder")}
                rows={2}
              />
            </div>

            {/* Status */}
            <div className="space-y-2">
              <Label>{t("status")}</Label>
              <Select
                value={status}
                onValueChange={(v) => setStatus(v as RuleStatus)}
              >
                <SelectTrigger className="w-40">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ACTIVE">{t("statusActive")}</SelectItem>
                  <SelectItem value="INACTIVE">{t("statusInactive")}</SelectItem>
                  <SelectItem value="DRAFT">{t("statusDraft")}</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* Right: Preview (2 cols) */}
          <div className="lg:col-span-2 hidden lg:block">
            <CrossSellPreviewPanel rule={previewRule} settings={settings} />
          </div>
        </div>

        <DialogFooter className="gap-2 sm:gap-0">
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            {t("cancel")}
          </Button>
          <Button onClick={handleSubmit} disabled={isPending}>
            {isPending ? (
              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
            ) : (
              <Save className="h-4 w-4 mr-2" />
            )}
            {isEditing ? t("update") : t("create")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
