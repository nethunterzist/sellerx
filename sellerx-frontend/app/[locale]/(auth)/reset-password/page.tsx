"use client";

import { Suspense } from "react";
import { ResetPasswordForm } from "@/components/auth";
import { FadeIn } from "@/components/motion";
import { Loader2 } from "lucide-react";

function ResetPasswordContent() {
  return <ResetPasswordForm />;
}

export default function ResetPasswordPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-muted">
      <FadeIn direction="up" duration={0.4}>
        <div className="w-full max-w-md p-8 bg-card rounded-lg shadow-lg">
          <Suspense
          fallback={
            <div className="text-center">
              <Loader2 className="mx-auto h-8 w-8 animate-spin text-[#1D70F1]" />
              <p className="mt-4 text-gray-600">Yükleniyor...</p>
            </div>
          }
        >
          <ResetPasswordContent />
        </Suspense>
        </div>
      </FadeIn>
    </div>
  );
}
