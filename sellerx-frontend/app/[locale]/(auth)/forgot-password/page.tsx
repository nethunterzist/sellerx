"use client";

import { ForgotPasswordForm } from "@/components/auth";
import { FadeIn } from "@/components/motion";

export default function ForgotPasswordPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-muted">
      <FadeIn direction="up" duration={0.4}>
        <div className="w-full max-w-md p-8 bg-card rounded-lg shadow-lg">
          <ForgotPasswordForm />
        </div>
      </FadeIn>
    </div>
  );
}
