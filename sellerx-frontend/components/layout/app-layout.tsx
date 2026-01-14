"use client";

import { Sidebar } from "./sidebar";
import { Header } from "./header";

interface AppLayoutProps {
  children: React.ReactNode;
}

export function AppLayout({ children }: AppLayoutProps) {
  return (
    <div className="min-h-screen bg-[#F5F5F5]">
      <Sidebar />
      <Header />
      <main className="ml-[220px] pt-14">
        <div className="p-4">
          {children}
        </div>
      </main>
    </div>
  );
}
