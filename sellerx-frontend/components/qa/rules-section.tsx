"use client";

import { useState, useEffect } from "react";
import { useTranslations } from "next-intl";
import { useAiSettings, useUpdateAiSettings } from "@/hooks/queries/use-ai";
import { RulesLivePreview } from "./rules-live-preview";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import { Slider } from "@/components/ui/slider";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import {
  Palette,
  Shield,
  FileText,
  Scale,
  Heart,
  BookX,
  Tag,
  Loader2,
  Save,
  Check,
  ArrowRight,
} from "lucide-react";
import { toast } from "sonner";
import { StaggerChildren } from "@/components/motion";

interface RulesSectionProps {
  storeId: string;
}

export function RulesSection({ storeId }: RulesSectionProps) {
  const t = useTranslations("qa.rules");
  const { data: aiSettings, isLoading } = useAiSettings(storeId);
  const updateMutation = useUpdateAiSettings();

  const [tone, setTone] = useState<"professional" | "friendly" | "formal">("professional");
  const [language, setLanguage] = useState("tr");
  const [maxLength, setMaxLength] = useState(500);
  const [includeGreeting, setIncludeGreeting] = useState(false);
  const [includeSignature, setIncludeSignature] = useState(false);
  const [signatureText, setSignatureText] = useState("");
  const [confidenceThreshold, setConfidenceThreshold] = useState(70);
  const [autoAnswer, setAutoAnswer] = useState(false);
  const [isDirty, setIsDirty] = useState(false);

  useEffect(() => {
    if (aiSettings) {
      setTone(aiSettings.tone);
      setLanguage(aiSettings.language);
      setMaxLength(aiSettings.maxAnswerLength);
      setIncludeGreeting(aiSettings.includeGreeting);
      setIncludeSignature(aiSettings.includeSignature);
      setSignatureText(aiSettings.signatureText || "");
      setConfidenceThreshold(Math.round(aiSettings.confidenceThreshold * 100));
      setAutoAnswer(aiSettings.autoAnswer);
      setIsDirty(false);
    }
  }, [aiSettings]);

  const handleSave = async () => {
    try {
      await updateMutation.mutateAsync({
        storeId,
        data: {
          tone,
          language,
          maxAnswerLength: maxLength,
          includeGreeting,
          includeSignature,
          signatureText: includeSignature ? signatureText : undefined,
          confidenceThreshold: confidenceThreshold / 100,
          autoAnswer,
        },
      });
      toast.success(t("saved"));
      setIsDirty(false);
    } catch {
      toast.error("Ayarlar kaydedilemedi");
    }
  };

  const markDirty = () => setIsDirty(true);

  const safetyRules = [
    {
      type: "LEGAL_RISK",
      icon: Scale,
      description: t("safetyRule.legalRisk"),
      severity: "critical" as const,
    },
    {
      type: "HEALTH_SAFETY",
      icon: Heart,
      description: t("safetyRule.healthSafety"),
      severity: "high" as const,
    },
    {
      type: "KNOWLEDGE_CONFLICT",
      icon: BookX,
      description: t("safetyRule.knowledgeConflict"),
      severity: "medium" as const,
    },
    {
      type: "BRAND_INCONSISTENCY",
      icon: Tag,
      description: t("safetyRule.brandInconsistency"),
      severity: "medium" as const,
    },
  ];

  const severityColors = {
    critical: "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400",
    high: "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400",
    medium: "bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400",
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      {/* Left: Settings */}
      <StaggerChildren className="space-y-6">
        {/* Card 1: Tone & Style */}
        <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Palette className="h-5 w-5 text-purple-500" />
            {t("toneAndStyle")}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-5">
          {/* Tone & Language Selectors */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">{t("tone")}</label>
              <Select value={tone} onValueChange={(v: "professional" | "friendly" | "formal") => { setTone(v); markDirty(); }}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="professional">{t("toneOptions.professional")}</SelectItem>
                  <SelectItem value="friendly">{t("toneOptions.friendly")}</SelectItem>
                  <SelectItem value="formal">{t("toneOptions.formal")}</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <label className="text-sm font-medium">{t("language")}</label>
              <Select value={language} onValueChange={(v) => { setLanguage(v); markDirty(); }}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="tr">Türkçe</SelectItem>
                  <SelectItem value="en">English</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          {/* Greeting & Signature */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="flex items-center justify-between rounded-lg border p-3">
              <label className="text-sm font-medium">{t("includeGreeting")}</label>
              <Switch
                checked={includeGreeting}
                onCheckedChange={(v) => { setIncludeGreeting(v); markDirty(); }}
              />
            </div>
            <div className="flex items-center justify-between rounded-lg border p-3">
              <label className="text-sm font-medium">{t("includeSignature")}</label>
              <Switch
                checked={includeSignature}
                onCheckedChange={(v) => { setIncludeSignature(v); markDirty(); }}
              />
            </div>
          </div>

          {includeSignature && (
            <div className="space-y-2">
              <label className="text-sm font-medium">{t("signatureText")}</label>
              <Textarea
                value={signatureText}
                onChange={(e) => { setSignatureText(e.target.value); markDirty(); }}
                rows={2}
                placeholder="Saygılarımızla, Mağaza Adı"
              />
            </div>
          )}

          {/* Confidence Threshold */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <label className="text-sm font-medium">{t("confidenceThreshold")}</label>
              <span className="text-sm font-semibold text-blue-600">%{confidenceThreshold}</span>
            </div>
            <Slider
              value={[confidenceThreshold]}
              onValueChange={(v) => { setConfidenceThreshold(v[0]); markDirty(); }}
              min={0}
              max={100}
              step={5}
            />
          </div>

          {/* Auto Answer */}
          <div className="flex items-center justify-between rounded-lg border p-3">
            <div>
              <label className="text-sm font-medium">{t("autoAnswer")}</label>
              <p className="text-xs text-muted-foreground">{t("autoAnswerWarning")}</p>
            </div>
            <Switch
              checked={autoAnswer}
              onCheckedChange={(v) => { setAutoAnswer(v); markDirty(); }}
            />
          </div>

          {/* Save Button */}
          <Button
            onClick={handleSave}
            disabled={!isDirty || updateMutation.isPending}
            className="w-full"
          >
            {updateMutation.isPending ? (
              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
            ) : isDirty ? (
              <Save className="h-4 w-4 mr-2" />
            ) : (
              <Check className="h-4 w-4 mr-2" />
            )}
            {updateMutation.isPending
              ? t("saving")
              : isDirty
              ? t("saveSettings")
              : t("saved")}
          </Button>
        </CardContent>
      </Card>

      {/* Card 2: Safety Rules */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Shield className="h-5 w-5 text-red-500" />
            {t("safetyRules")}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-3">
            {safetyRules.map((rule) => {
              const Icon = rule.icon;
              return (
                <div
                  key={rule.type}
                  className="flex items-start gap-3 rounded-lg border p-3"
                >
                  <div className="mt-0.5">
                    <Icon className="h-4 w-4 text-muted-foreground" />
                  </div>
                  <div className="flex-1">
                    <p className="text-sm">{rule.description}</p>
                  </div>
                  <Badge variant="outline" className={severityColors[rule.severity]}>
                    {rule.severity === "critical" ? "Kritik" : rule.severity === "high" ? "Yüksek" : "Orta"}
                  </Badge>
                </div>
              );
            })}
          </div>
        </CardContent>
      </Card>

      {/* Card 3: Auto Answer Policy */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <FileText className="h-5 w-5 text-blue-500" />
            {t("autoAnswerPolicy")}
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <p className="text-sm text-muted-foreground">
            {t("policyDescription")}
          </p>
          <Button variant="outline" size="sm" className="gap-2" asChild>
            <a href="#" onClick={(e) => { e.preventDefault(); /* Tab switch handled by parent */ }}>
              {t("goToAiBrain")}
              <ArrowRight className="h-4 w-4" />
            </a>
          </Button>
        </CardContent>
      </Card>
      </StaggerChildren>

      {/* Right: Live Preview */}
      <div className="hidden lg:block">
        <RulesLivePreview
          settings={{
            tone,
            language,
            maxAnswerLength: maxLength,
            includeGreeting,
            includeSignature,
            signatureText,
          }}
        />
      </div>
    </div>
  );
}
