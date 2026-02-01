"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useQuestions, useQaStats, useSyncQuestions, useSuggestions, useConflicts } from "@/hooks/queries/use-qa";
import { useAiSettings } from "@/hooks/queries/use-ai";
import { QaStatsCards } from "@/components/qa/qa-stats-cards";
import { QuestionList } from "@/components/qa/question-list";
import { KnowledgeDiscoveryPanel } from "@/components/qa/knowledge-discovery-panel";
import { SeniorityDashboard } from "@/components/qa/seniority-dashboard";
import { ConflictAlertBanner } from "@/components/qa/conflict-alert-banner";
import { ConflictDetailModal } from "@/components/qa/conflict-detail-modal";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { RefreshCw, MessageSquare, Sparkles, Settings, Lightbulb, GraduationCap, AlertTriangle, Loader2 } from "lucide-react";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import Link from "next/link";
import { useLocale } from "next-intl";
import type { ConflictAlert } from "@/types/qa";
import {
  StatCardSkeleton,
  TabsSkeleton,
  TableSkeleton,
} from "@/components/ui/skeleton-blocks";

function QaPageSkeleton() {
  return (
    <div className="space-y-6">
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => <StatCardSkeleton key={i} />)}
      </div>
      <TabsSkeleton tabCount={4} />
      <TableSkeleton columns={5} rows={8} />
    </div>
  );
}

export default function QaPage() {
  const t = useTranslations("qa");
  const locale = useLocale();
  const { data: selectedStore } = useSelectedStore();
  const [mainTab, setMainTab] = useState<"questions" | "suggestions" | "seniority" | "alerts">("questions");
  const [questionTab, setQuestionTab] = useState<"all" | "pending" | "answered">("all");
  const [page, setPage] = useState(0);
  const [selectedConflict, setSelectedConflict] = useState<ConflictAlert | null>(null);
  const [conflictModalOpen, setConflictModalOpen] = useState(false);

  const storeId = selectedStore?.selectedStoreId;

  // Fetch questions based on active tab
  const statusFilter = questionTab === "all" ? undefined : questionTab === "pending" ? "PENDING" : "ANSWERED";

  const { data: questionsData, isLoading: questionsLoading, refetch: refetchQuestions } = useQuestions(
    storeId || "",
    statusFilter || "ALL",
    page,
    20
  );

  const { data: stats, isLoading: statsLoading, refetch: refetchStats } = useQaStats(storeId || "");

  const { data: aiSettings } = useAiSettings(storeId);

  // Fetch suggestions count
  const { data: suggestions } = useSuggestions(storeId || "");
  const pendingSuggestionsCount = suggestions?.filter(s => s.status === "PENDING").length || 0;

  // Fetch active conflicts count
  const { data: conflicts } = useConflicts(storeId || "");
  const activeConflictsCount = conflicts?.filter(c => c.status === "ACTIVE").length || 0;

  const syncMutation = useSyncQuestions();
  const aiEnabled = aiSettings?.aiEnabled ?? false;

  const handleSync = () => {
    if (storeId) {
      syncMutation.mutate(storeId, {
        onSuccess: () => {
          refetchQuestions();
          refetchStats();
        },
      });
    }
  };

  const handleViewConflictDetails = (conflict: ConflictAlert) => {
    setSelectedConflict(conflict);
    setConflictModalOpen(true);
  };

  if (!storeId) {
    return (
      <div className="container mx-auto py-8">
        <Card>
          <CardContent className="p-8 text-center text-muted-foreground">
            <MessageSquare className="h-12 w-12 mx-auto mb-4 opacity-50" />
            <p>{t("noStoreSelected")}</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (statsLoading && questionsLoading) {
    return <QaPageSkeleton />;
  }

  return (
    <div className="container mx-auto py-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <p className="text-muted-foreground">{t("description")}</p>
        </div>
        <Button onClick={handleSync} disabled={syncMutation.isPending}>
          <RefreshCw className={`mr-2 h-4 w-4 ${syncMutation.isPending ? "animate-spin" : ""}`} />
          {syncMutation.isPending ? t("syncing") : t("syncButton")}
        </Button>
      </div>

      {/* AI Status Info */}
      <Alert className={aiEnabled ? "border-blue-200 bg-blue-50/50 dark:border-blue-800 dark:bg-blue-950/30" : undefined}>
        <Sparkles className={`h-4 w-4 ${aiEnabled ? "text-blue-600 dark:text-blue-400" : ""}`} />
        <AlertTitle className="flex items-center justify-between">
          <span>{aiEnabled ? t("aiEnabled") : t("aiDisabled")}</span>
          <Link href={`/${locale}/settings?section=ai`}>
            <Button variant="ghost" size="sm" className="h-auto p-1 text-xs">
              <Settings className="h-3 w-3 mr-1" />
              {t("aiSettings")}
            </Button>
          </Link>
        </AlertTitle>
        <AlertDescription>
          {aiEnabled ? t("aiEnabledDescription") : t("aiDisabledDescription")}
        </AlertDescription>
      </Alert>

      {/* Stats Cards */}
      <QaStatsCards stats={stats} isLoading={statsLoading} />

      {/* Main Tabs */}
      <Tabs value={mainTab} onValueChange={(v) => setMainTab(v as typeof mainTab)}>
        <TabsList className="grid w-full grid-cols-4">
          <TabsTrigger value="questions" className="flex items-center gap-2">
            <MessageSquare className="h-4 w-4" />
            <span className="hidden sm:inline">{t("mainTabs.questions")}</span>
          </TabsTrigger>
          <TabsTrigger value="suggestions" className="flex items-center gap-2">
            <Lightbulb className="h-4 w-4" />
            <span className="hidden sm:inline">{t("mainTabs.suggestions")}</span>
            {pendingSuggestionsCount > 0 && (
              <span className="bg-primary text-primary-foreground text-xs px-1.5 py-0.5 rounded-full">
                {pendingSuggestionsCount}
              </span>
            )}
          </TabsTrigger>
          <TabsTrigger value="seniority" className="flex items-center gap-2">
            <GraduationCap className="h-4 w-4" />
            <span className="hidden sm:inline">{t("mainTabs.seniority")}</span>
          </TabsTrigger>
          <TabsTrigger value="alerts" className="flex items-center gap-2">
            <AlertTriangle className="h-4 w-4" />
            <span className="hidden sm:inline">{t("mainTabs.alerts")}</span>
            {activeConflictsCount > 0 && (
              <span className="bg-destructive text-destructive-foreground text-xs px-1.5 py-0.5 rounded-full">
                {activeConflictsCount}
              </span>
            )}
          </TabsTrigger>
        </TabsList>

        {/* Questions Tab */}
        <TabsContent value="questions" className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle>{t("questionsTitle")}</CardTitle>
              <CardDescription>{t("questionsDescription")}</CardDescription>
            </CardHeader>
            <CardContent>
              <Tabs value={questionTab} onValueChange={(v) => { setQuestionTab(v as typeof questionTab); setPage(0); }}>
                <TabsList className="mb-4">
                  <TabsTrigger value="all">{t("tabs.all")}</TabsTrigger>
                  <TabsTrigger value="pending">{t("tabs.pending")}</TabsTrigger>
                  <TabsTrigger value="answered">{t("tabs.answered")}</TabsTrigger>
                </TabsList>

                <TabsContent value={questionTab}>
                  <QuestionList
                    questions={questionsData?.content || []}
                    isLoading={questionsLoading}
                    aiEnabled={aiEnabled}
                    storeId={storeId}
                    onRefresh={() => {
                      refetchQuestions();
                      refetchStats();
                    }}
                  />

                  {/* Pagination */}
                  {questionsData && questionsData.totalPages > 1 && (
                    <div className="flex items-center justify-center gap-2 mt-6">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setPage((p) => Math.max(0, p - 1))}
                        disabled={page === 0}
                      >
                        {t("pagination.previous")}
                      </Button>
                      <span className="text-sm text-muted-foreground">
                        {t("pagination.pageInfo", { current: page + 1, total: questionsData.totalPages })}
                      </span>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => setPage((p) => p + 1)}
                        disabled={page >= questionsData.totalPages - 1}
                      >
                        {t("pagination.next")}
                      </Button>
                    </div>
                  )}
                </TabsContent>
              </Tabs>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Suggestions Tab */}
        <TabsContent value="suggestions" className="mt-6">
          <KnowledgeDiscoveryPanel storeId={storeId} />
        </TabsContent>

        {/* Seniority Tab */}
        <TabsContent value="seniority" className="mt-6">
          <Card>
            <CardContent className="pt-6">
              <SeniorityDashboard storeId={storeId} />
            </CardContent>
          </Card>
        </TabsContent>

        {/* Alerts Tab */}
        <TabsContent value="alerts" className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <AlertTriangle className="h-5 w-5 text-yellow-500" />
                {t("conflicts.title")}
              </CardTitle>
              <CardDescription>{t("conflicts.description")}</CardDescription>
            </CardHeader>
            <CardContent>
              {!conflicts ? (
                <div className="flex items-center justify-center py-8">
                  <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
                </div>
              ) : conflicts.filter(c => c.status === "ACTIVE").length === 0 ? (
                <div className="text-center py-12">
                  <AlertTriangle className="h-12 w-12 mx-auto text-muted-foreground/50 mb-4" />
                  <p className="text-muted-foreground">{t("conflicts.noConflicts")}</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {conflicts
                    .filter(c => c.status === "ACTIVE")
                    .sort((a, b) => {
                      const severityOrder = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3 };
                      return severityOrder[a.severity] - severityOrder[b.severity];
                    })
                    .map((conflict) => (
                      <ConflictAlertBanner
                        key={conflict.id}
                        alert={conflict}
                        onViewDetails={() => handleViewConflictDetails(conflict)}
                      />
                    ))}
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Sync Result Message */}
      {syncMutation.isSuccess && syncMutation.data && (
        <Alert>
          <RefreshCw className="h-4 w-4" />
          <AlertTitle>{t("syncSuccess")}</AlertTitle>
          <AlertDescription>
            {t("syncResultMessage", {
              newQuestions: syncMutation.data.newQuestions,
              updatedQuestions: syncMutation.data.updatedQuestions,
            })}
          </AlertDescription>
        </Alert>
      )}

      {/* Conflict Detail Modal */}
      <ConflictDetailModal
        conflict={selectedConflict}
        open={conflictModalOpen}
        onClose={() => {
          setConflictModalOpen(false);
          setSelectedConflict(null);
        }}
      />
    </div>
  );
}
