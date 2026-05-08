"use client";

import { createContext, useContext } from "react";
import type { PropsWithChildren } from "react";

type ShellNavItem = {
  href: string;
  label: string;
};

type ShellContextValue = {
  appName: string;
  navItems: ShellNavItem[];
};

const shellContextValue: ShellContextValue = {
  appName: "영어 패턴 학습기",
  navItems: [
    { href: "/", label: "홈" },
    { href: "/words", label: "단어" },
    { href: "/patterns", label: "패턴" },
    { href: "/generate", label: "생성" },
    { href: "/review", label: "복습" },
  ],
};

const ShellContext = createContext<ShellContextValue | null>(null);

export function ShellProvider({ children }: PropsWithChildren) {
  return <ShellContext.Provider value={shellContextValue}>{children}</ShellContext.Provider>;
}

export function useShell() {
  const value = useContext(ShellContext);

  if (!value) {
    throw new Error("useShell must be used within ShellProvider");
  }

  return value;
}
