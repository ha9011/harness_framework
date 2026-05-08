"use client";

import type { PropsWithChildren } from "react";
import { AuthProvider } from "@/app/components/providers/auth-provider";
import { QueryProvider } from "@/app/components/providers/query-provider";
import { ShellProvider } from "@/app/components/providers/shell-provider";

export function AppProviders({ children }: PropsWithChildren) {
  return (
    <QueryProvider>
      <AuthProvider>
        <ShellProvider>{children}</ShellProvider>
      </AuthProvider>
    </QueryProvider>
  );
}
