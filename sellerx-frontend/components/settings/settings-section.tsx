"use client";

import { ReactNode } from "react";
import { cn } from "@/lib/utils";

interface SettingsSectionProps {
  title: string;
  description?: string;
  children: ReactNode;
  action?: ReactNode;
  className?: string;
  noPadding?: boolean;
}

export function SettingsSection({
  title,
  description,
  children,
  action,
  className,
  noPadding = false,
}: SettingsSectionProps) {
  return (
    <div className="space-y-6">
      {/* Section Header */}
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-lg font-semibold text-foreground">{title}</h2>
          {description && (
            <p className="text-sm text-muted-foreground mt-1">{description}</p>
          )}
        </div>
        {action && <div>{action}</div>}
      </div>

      {/* Section Content */}
      <div
        className={cn(
          "bg-card rounded-xl border border-border",
          !noPadding && "p-6",
          className
        )}
      >
        {children}
      </div>
    </div>
  );
}

interface SettingsCardProps {
  children: ReactNode;
  className?: string;
}

export function SettingsCard({ children, className }: SettingsCardProps) {
  return (
    <div
      className={cn(
        "bg-card rounded-xl border border-border p-6",
        className
      )}
    >
      {children}
    </div>
  );
}

interface SettingsRowProps {
  label: string;
  description?: string;
  children: ReactNode;
  className?: string;
}

export function SettingsRow({
  label,
  description,
  children,
  className,
}: SettingsRowProps) {
  return (
    <div
      className={cn(
        "flex items-center justify-between py-4 border-b border-border last:border-0",
        className
      )}
    >
      <div className="flex-1 pr-4">
        <p className="font-medium text-foreground">{label}</p>
        {description && (
          <p className="text-sm text-muted-foreground mt-0.5">{description}</p>
        )}
      </div>
      <div className="shrink-0">{children}</div>
    </div>
  );
}

interface SettingsDangerZoneProps {
  children: ReactNode;
  title?: string;
}

export function SettingsDangerZone({
  children,
  title = "Tehlikeli Bölge",
}: SettingsDangerZoneProps) {
  return (
    <div className="bg-card rounded-xl border border-red-200 p-6">
      <h3 className="font-medium text-red-600 mb-4">{title}</h3>
      <div className="space-y-4">{children}</div>
    </div>
  );
}
