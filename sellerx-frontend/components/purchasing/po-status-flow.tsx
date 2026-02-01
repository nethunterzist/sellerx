"use client";

import type { PurchaseOrderStatus } from "@/types/purchasing";
import { cn } from "@/lib/utils";
import { FileText, PackageCheck, Truck, CheckCircle, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";

const steps: {
  status: PurchaseOrderStatus;
  label: string;
  icon: React.ElementType;
}[] = [
  { status: "DRAFT", label: "Taslak", icon: FileText },
  { status: "ORDERED", label: "Sipariş Verildi", icon: PackageCheck },
  { status: "SHIPPED", label: "Gönderildi", icon: Truck },
  { status: "CLOSED", label: "Kapatıldı", icon: CheckCircle },
];

const statusOrder: PurchaseOrderStatus[] = ["DRAFT", "ORDERED", "SHIPPED", "CLOSED"];

interface POStatusFlowProps {
  currentStatus: PurchaseOrderStatus;
  onStatusChange: (status: PurchaseOrderStatus) => void;
  isUpdating: boolean;
}

export function POStatusFlow({
  currentStatus,
  onStatusChange,
  isUpdating,
}: POStatusFlowProps) {
  const currentIndex = statusOrder.indexOf(currentStatus);
  const nextStatus = currentIndex < statusOrder.length - 1 ? statusOrder[currentIndex + 1] : null;

  return (
    <div className="bg-card rounded-lg border border-border p-4">
      <div className="flex items-center justify-between">
        {/* Status Flow */}
        <div className="flex items-center gap-2">
          {steps.map((step, index) => {
            const stepIndex = statusOrder.indexOf(step.status);
            const isCompleted = stepIndex < currentIndex;
            const isCurrent = stepIndex === currentIndex;
            const Icon = step.icon;

            return (
              <div key={step.status} className="flex items-center">
                <div
                  className={cn(
                    "flex items-center gap-2 px-3 py-2 rounded-lg transition-colors",
                    isCompleted && "bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400",
                    isCurrent && "bg-primary/10 text-primary border border-primary",
                    !isCompleted && !isCurrent && "text-muted-foreground"
                  )}
                >
                  <Icon className="h-4 w-4" />
                  <span className="text-sm font-medium hidden sm:inline">{step.label}</span>
                </div>
                {index < steps.length - 1 && (
                  <ChevronRight className="h-4 w-4 text-muted-foreground mx-1" />
                )}
              </div>
            );
          })}
        </div>

        {/* Next Action Button */}
        {nextStatus && (
          <Button
            onClick={() => onStatusChange(nextStatus)}
            disabled={isUpdating}
            size="sm"
          >
            {nextStatus === "ORDERED" && "Sipariş Ver"}
            {nextStatus === "SHIPPED" && "Gönderildi İşaretle"}
            {nextStatus === "CLOSED" && "Kapat"}
          </Button>
        )}
      </div>
    </div>
  );
}
