"use client";

import { LogOut } from "lucide-react";
import { usePathname, useRouter } from "next/navigation";
import type { PropsWithChildren } from "react";
import { useEffect, useState } from "react";
import { BottomNav } from "@/app/components/navigation/bottom-nav";
import { useAuth } from "@/app/components/providers/auth-provider";
import { Button } from "@/app/components/ui/button";

const authRoutes = new Set(["/login", "/signup"]);

function isAuthRoute(pathname: string) {
  return authRoutes.has(pathname);
}

export function AppShell({ children }: PropsWithChildren) {
  const pathname = usePathname();
  const router = useRouter();
  const { logout, status, user } = useAuth();
  const [logoutError, setLogoutError] = useState<string | null>(null);
  const authRoute = isAuthRoute(pathname);

  useEffect(() => {
    if (status === "unauthenticated" && !authRoute) {
      router.replace("/login");
    }

    if (status === "authenticated" && authRoute) {
      router.replace("/");
    }
  }, [authRoute, router, status]);

  async function handleLogout() {
    setLogoutError(null);

    try {
      await logout();
      router.replace("/login");
    } catch {
      setLogoutError("로그아웃에 실패했습니다");
    }
  }

  if (authRoute) {
    return (
      <div className="min-h-dvh bg-cafe-bg text-cafe-ink">
        <div className="mx-auto min-h-dvh max-w-lg bg-cafe-bg">{children}</div>
      </div>
    );
  }

  if (status !== "authenticated") {
    return (
      <div className="min-h-dvh bg-cafe-bg text-cafe-ink">
        <div className="mx-auto flex min-h-dvh max-w-lg items-center justify-center bg-cafe-bg px-4">
          <p className="text-sm font-medium text-cafe-ink-soft">세션을 확인하고 있습니다</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-dvh bg-cafe-bg text-cafe-ink">
      <div className="mx-auto flex min-h-dvh max-w-lg flex-col bg-cafe-bg shadow-[0_0_0_1px_rgba(61,46,34,0.04)]">
        <header className="sticky top-0 z-30 border-b border-[rgba(61,46,34,0.08)] bg-cafe-raised px-4 py-3">
          <div className="flex items-center justify-between gap-3">
            <div className="min-w-0">
              <p className="text-[11px] font-semibold uppercase tracking-[0.12em] text-cafe-latte-deep">
                Cozy Cafe
              </p>
              <h1 className="truncate text-base font-semibold text-cafe-ink">영어 패턴 학습기</h1>
            </div>
            <div className="flex shrink-0 items-center gap-2">
              <span className="max-w-24 truncate text-xs font-medium text-cafe-ink-soft">{user?.nickname}</span>
              <Button
                type="button"
                variant="ghost"
                size="icon"
                aria-label="로그아웃"
                onClick={() => void handleLogout()}
              >
                <LogOut className="h-4 w-4" aria-hidden="true" />
              </Button>
            </div>
          </div>
          {logoutError ? <p className="mt-2 text-xs font-medium text-cafe-warning">{logoutError}</p> : null}
        </header>
        <main className="flex-1 pb-24">{children}</main>
        <BottomNav />
      </div>
    </div>
  );
}
