"use client";

import { LoginForm } from "@/components/auth";

export default function SignInPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-[#F5F5F5]">
      <div className="w-full max-w-md p-8 bg-white rounded-lg shadow-lg">
        <LoginForm />
      </div>
    </div>
  );
}
