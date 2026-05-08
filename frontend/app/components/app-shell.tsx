"use client";

import type { PropsWithChildren } from "react";
import { BottomNav } from "@/app/components/navigation/bottom-nav";

export function AppShell({ children }: PropsWithChildren) {
  return (
    <div className="min-h-dvh bg-cafe-bg text-cafe-ink">
      <div className="mx-auto flex min-h-dvh max-w-lg flex-col bg-cafe-bg shadow-[0_0_0_1px_rgba(61,46,34,0.04)]">
        <main className="flex-1 pb-24">{children}</main>
        <BottomNav />
      </div>
    </div>
  );
}
