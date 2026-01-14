"use client";

import { useState, useEffect } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Progress } from "@/components/ui/progress";
import {
  Loader2,
  ShoppingCart,
  Package,
  Calculator,
  CheckCircle,
} from "lucide-react";

interface LoadingDialogProps {
  isOpen: boolean;
  onClose?: () => void;
}

const loadingSteps = [
  {
    id: 1,
    text: "Siparişler kontrol ediliyor...",
    icon: ShoppingCart,
    duration: 2000,
  },
  {
    id: 2,
    text: "Ürün verileri toplanıyor...",
    icon: Package,
    duration: 1500,
  },
  {
    id: 3,
    text: "Karlılık hesaplanıyor...",
    icon: Calculator,
    duration: 1500,
  },
  {
    id: 4,
    text: "Veriler hazırlanıyor...",
    icon: Loader2,
    duration: 1000,
  },
];

export function LoadingDialog({ isOpen, onClose }: LoadingDialogProps) {
  const [currentStep, setCurrentStep] = useState(0);
  const [progress, setProgress] = useState(0);
  const [isCompleted, setIsCompleted] = useState(false);

  useEffect(() => {
    if (!isOpen) {
      setCurrentStep(0);
      setProgress(0);
      setIsCompleted(false);
      return;
    }

    let timer: NodeJS.Timeout;
    let progressTimer: NodeJS.Timeout;

    const startStep = (stepIndex: number) => {
      if (stepIndex >= loadingSteps.length) {
        setIsCompleted(true);
        setTimeout(() => {
          onClose?.();
        }, 1000);
        return;
      }

      setCurrentStep(stepIndex);
      const step = loadingSteps[stepIndex];
      const stepProgress = (stepIndex / loadingSteps.length) * 100;

      // Progress animasyonu
      let currentProgress = stepProgress;
      progressTimer = setInterval(() => {
        currentProgress += 25 / loadingSteps.length / 10; // Her adımda %25 ilerleme
        if (currentProgress >= stepProgress + 25 / loadingSteps.length) {
          currentProgress = stepProgress + 25 / loadingSteps.length;
          clearInterval(progressTimer);
        }
        setProgress(Math.min(currentProgress, 100));
      }, 50);

      timer = setTimeout(() => {
        clearInterval(progressTimer);
        startStep(stepIndex + 1);
      }, step.duration);
    };

    startStep(0);

    return () => {
      clearTimeout(timer);
      clearInterval(progressTimer);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const currentStepData = loadingSteps[currentStep];
  const CurrentIcon = currentStepData?.icon || Loader2;

  return (
    <Dialog open={isOpen} onOpenChange={() => {}}>
      <DialogContent className="sm:max-w-[400px]">
        <DialogHeader className="pb-4">
          <DialogTitle className="text-center text-xl font-bold">
            📊 Dashboard Güncelleniyor
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-6 py-4">
          {/* Ana İkon ve Durum */}
          <div className="flex flex-col items-center space-y-4">
            {isCompleted ? (
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-green-100">
                <CheckCircle className="h-8 w-8 text-green-600" />
              </div>
            ) : (
              <div className="flex h-16 w-16 items-center justify-center rounded-full bg-blue-100">
                <CurrentIcon className="h-8 w-8 animate-spin text-blue-600" />
              </div>
            )}

            <div className="text-center">
              <p className="text-lg font-semibold">
                {isCompleted ? "✅ Tamamlandı!" : currentStepData?.text}
              </p>
              {!isCompleted && (
                <p className="text-muted-foreground mt-1 text-sm">
                  Adım {currentStep + 1} / {loadingSteps.length}
                </p>
              )}
            </div>
          </div>

          {/* Progress Bar */}
          <div className="space-y-2">
            <Progress value={progress} className="h-2" />
            <div className="text-muted-foreground flex justify-between text-xs">
              <span>İlerleme</span>
              <span>%{Math.round(progress)}</span>
            </div>
          </div>

          {/* Adım Listesi */}
          <div className="space-y-2">
            {loadingSteps.map((step, index) => (
              <div
                key={step.id}
                className={`flex items-center space-x-3 rounded-lg p-2 transition-all ${
                  index < currentStep
                    ? "bg-green-50 text-green-700"
                    : index === currentStep
                      ? "bg-blue-50 text-blue-700"
                      : "bg-gray-50 text-gray-500"
                }`}
              >
                <div
                  className={`flex h-6 w-6 items-center justify-center rounded-full ${
                    index < currentStep
                      ? "bg-green-200"
                      : index === currentStep
                        ? "bg-blue-200"
                        : "bg-gray-200"
                  }`}
                >
                  {index < currentStep ? (
                    <CheckCircle className="h-3 w-3" />
                  ) : index === currentStep ? (
                    <step.icon className="h-3 w-3 animate-spin" />
                  ) : (
                    <div className="h-2 w-2 rounded-full bg-gray-400" />
                  )}
                </div>
                <span className="text-sm font-medium">{step.text}</span>
              </div>
            ))}
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
