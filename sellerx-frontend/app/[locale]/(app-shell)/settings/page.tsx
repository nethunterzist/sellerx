"use client";

import { useState, useEffect, Suspense } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import {
  SettingsLayout,
  ProfileSettings,
  SecuritySettings,
  AppearanceSettings,
  NotificationSettings,
  StoresSettings,
  ActivityLogSettings,
  SubscriptionSettings,
  InvoicesSettings,
} from "@/components/settings";
import { AiSettings } from "@/components/settings/ai-settings";
import { ReferralTab } from "@/components/billing/referral-tab";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent } from "@/components/ui/card";

type SettingsSection =
  | "profile"
  | "security"
  | "subscription"
  | "invoices"
  | "referral"
  | "appearance"
  | "notifications"
  | "activity"
  | "stores"
  | "ai";

function SettingsPageSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton className="h-8 w-32" />
      <div className="flex gap-6">
        <div className="w-48 space-y-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-9 w-full rounded-md" />
          ))}
        </div>
        <div className="flex-1">
          <Card>
            <CardContent className="p-6 space-y-4">
              {Array.from({ length: 5 }).map((_, i) => (
                <div key={i} className="space-y-2">
                  <Skeleton className="h-4 w-24" />
                  <Skeleton className="h-10 w-full rounded-md" />
                </div>
              ))}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}

function SettingsContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [activeSection, setActiveSection] = useState<SettingsSection>("profile");

  // Sync with URL params
  useEffect(() => {
    const section = searchParams.get("section") as SettingsSection;
    if (section && isValidSection(section)) {
      setActiveSection(section);
    }
  }, [searchParams]);

  const isValidSection = (section: string): section is SettingsSection => {
    return [
      "profile",
      "security",
      "subscription",
      "invoices",
      "referral",
      "appearance",
      "notifications",
      "activity",
      "stores",
      "ai",
    ].includes(section);
  };

  const handleSectionChange = (section: string) => {
    setActiveSection(section as SettingsSection);
    // Update URL without page reload
    const url = new URL(window.location.href);
    url.searchParams.set("section", section);
    router.push(url.pathname + url.search, { scroll: false });
  };

  const renderContent = () => {
    switch (activeSection) {
      case "profile":
        return <ProfileSettings />;
      case "security":
        return <SecuritySettings />;
      case "subscription":
        return <SubscriptionSettings />;
      case "invoices":
        return <InvoicesSettings />;
      case "referral":
        return <ReferralTab />;
      case "appearance":
        return <AppearanceSettings />;
      case "notifications":
        return <NotificationSettings />;
      case "activity":
        return <ActivityLogSettings />;
      case "stores":
        return <StoresSettings />;
      case "ai":
        return <AiSettings />;
      default:
        return <ProfileSettings />;
    }
  };

  return (
    <SettingsLayout
      activeSection={activeSection}
      onSectionChange={handleSectionChange}
    >
      {renderContent()}
    </SettingsLayout>
  );
}

export default function SettingsPage() {
  return (
    <Suspense fallback={<SettingsPageSkeleton />}>
      <SettingsContent />
    </Suspense>
  );
}
