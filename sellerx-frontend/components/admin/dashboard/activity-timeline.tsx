"use client";

import { useMemo } from "react";
import Link from "next/link";
import {
  UserPlus,
  Store,
  CheckCircle,
  XCircle,
  ShoppingCart,
  RefreshCw,
  Bell,
  type LucideIcon,
} from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import type { RecentActivity } from "@/types/admin";

interface ActivityTimelineProps {
  activities: RecentActivity[] | undefined;
  isLoading: boolean;
}

interface ActivityConfig {
  icon: LucideIcon;
  color: string;
  bgColor: string;
}

const activityConfig: Record<string, ActivityConfig> = {
  USER_CREATED: {
    icon: UserPlus,
    color: "text-blue-600 dark:text-blue-400",
    bgColor: "bg-blue-100 dark:bg-blue-900/40",
  },
  USER_REGISTERED: {
    icon: UserPlus,
    color: "text-blue-600 dark:text-blue-400",
    bgColor: "bg-blue-100 dark:bg-blue-900/40",
  },
  STORE_CREATED: {
    icon: Store,
    color: "text-purple-600 dark:text-purple-400",
    bgColor: "bg-purple-100 dark:bg-purple-900/40",
  },
  SYNC_COMPLETED: {
    icon: CheckCircle,
    color: "text-green-600 dark:text-green-400",
    bgColor: "bg-green-100 dark:bg-green-900/40",
  },
  SYNC_FAILED: {
    icon: XCircle,
    color: "text-red-600 dark:text-red-400",
    bgColor: "bg-red-100 dark:bg-red-900/40",
  },
  SYNC_STARTED: {
    icon: RefreshCw,
    color: "text-cyan-600 dark:text-cyan-400",
    bgColor: "bg-cyan-100 dark:bg-cyan-900/40",
  },
  ORDER_RECEIVED: {
    icon: ShoppingCart,
    color: "text-amber-600 dark:text-amber-400",
    bgColor: "bg-amber-100 dark:bg-amber-900/40",
  },
};

const defaultConfig: ActivityConfig = {
  icon: Bell,
  color: "text-gray-600 dark:text-gray-400",
  bgColor: "bg-gray-100 dark:bg-gray-900/40",
};

function formatRelativeTime(dateString: string): string {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffSecs = Math.floor(diffMs / 1000);
  const diffMins = Math.floor(diffSecs / 60);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffSecs < 60) return "Az önce";
  if (diffMins < 60) return `${diffMins} dk önce`;
  if (diffHours < 24) return `${diffHours} saat önce`;
  if (diffDays < 7) return `${diffDays} gün önce`;

  return date.toLocaleDateString("tr-TR", {
    day: "numeric",
    month: "short",
  });
}

export function ActivityTimeline({ activities, isLoading }: ActivityTimelineProps) {
  const displayActivities = useMemo(() => {
    if (!activities) return [];
    return activities.slice(0, 10);
  }, [activities]);

  if (isLoading) {
    return (
      <div className="bg-card rounded-xl border border-border p-6">
        <div className="flex items-center justify-between mb-4">
          <Skeleton className="h-5 w-32" />
          <Skeleton className="h-4 w-20" />
        </div>
        <div className="space-y-4">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="flex gap-3">
              <Skeleton className="h-8 w-8 rounded-full flex-shrink-0" />
              <div className="flex-1 space-y-1">
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-3 w-24" />
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (!displayActivities || displayActivities.length === 0) {
    return (
      <div className="bg-card rounded-xl border border-border p-6">
        <h3 className="font-semibold text-foreground mb-4">Son Aktiviteler</h3>
        <div className="flex items-center justify-center h-[250px] text-muted-foreground">
          Henüz aktivite yok
        </div>
      </div>
    );
  }

  return (
    <div className="bg-card rounded-xl border border-border p-6">
      <div className="flex items-center justify-between mb-4">
        <h3 className="font-semibold text-foreground">Son Aktiviteler</h3>
        {activities && activities.length > 10 && (
          <Link
            href="/admin/activities"
            className="text-xs text-primary hover:underline"
          >
            Tümünü Gör
          </Link>
        )}
      </div>
      <div className="space-y-4 max-h-[300px] overflow-y-auto pr-2">
        {displayActivities.map((activity, index) => {
          const config = activityConfig[activity.type] || defaultConfig;
          const Icon = config.icon;

          return (
            <div key={activity.id} className="flex gap-3 group">
              {/* Timeline dot and line */}
              <div className="relative flex flex-col items-center">
                <div className={`p-1.5 rounded-full ${config.bgColor}`}>
                  <Icon className={`h-3.5 w-3.5 ${config.color}`} />
                </div>
                {index < displayActivities.length - 1 && (
                  <div className="w-px h-full bg-border absolute top-8 left-1/2 -translate-x-1/2" />
                )}
              </div>

              {/* Content */}
              <div className="flex-1 min-w-0 pb-4">
                <p className="text-sm text-foreground leading-tight">
                  {activity.message}
                </p>
                <div className="flex items-center gap-2 mt-1">
                  <span className="text-xs text-muted-foreground">
                    {formatRelativeTime(activity.createdAt)}
                  </span>
                  {activity.userEmail && (
                    <Link
                      href={`/admin/users/${activity.userId}`}
                      className="text-xs text-primary hover:underline truncate max-w-[150px]"
                    >
                      {activity.userEmail}
                    </Link>
                  )}
                  {activity.storeName && (
                    <Link
                      href={`/admin/stores/${activity.storeId}`}
                      className="text-xs text-purple-600 dark:text-purple-400 hover:underline truncate max-w-[150px]"
                    >
                      {activity.storeName}
                    </Link>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
