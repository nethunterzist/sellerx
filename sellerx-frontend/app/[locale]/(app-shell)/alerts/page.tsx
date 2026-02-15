"use client";

import { AlertRulesSettings } from "@/components/settings/alert-rules-settings";
import { FadeIn } from "@/components/motion";

export default function AlertsPage() {
  return (
    <FadeIn>
    <div className="space-y-6">
      {/* Alert Rules Content */}
      <AlertRulesSettings />
    </div>
    </FadeIn>
  );
}
