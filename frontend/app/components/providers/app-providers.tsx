"use client";

import type { PropsWithChildren } from "react";
import { QueryProvider } from "@/app/components/providers/query-provider";
import { ShellProvider } from "@/app/components/providers/shell-provider";

export function AppProviders({ children }: PropsWithChildren) {
  return (
    <QueryProvider>
      <ShellProvider>{children}</ShellProvider>
    </QueryProvider>
  );
}
