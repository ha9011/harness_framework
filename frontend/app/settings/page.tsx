"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useAuth } from "@/app/components/providers/auth-provider";
import { Button } from "@/app/components/ui/button";
import { Chip } from "@/app/components/ui/chip";
import { ErrorState, LoadingState } from "@/app/components/ui/screen-state";
import { Surface } from "@/app/components/ui/surface";
import { ApiError } from "@/app/lib/api";
import { getSettings, updateDailyReviewCount } from "@/app/lib/learning-api";

const options = [10, 20, 30] as const;

export default function SettingsPage() {
  const router = useRouter();
  const { logout } = useAuth();
  const queryClient = useQueryClient();
  const [selectedCount, setSelectedCount] = useState<number>(20);
  const [message, setMessage] = useState<string | null>(null);
  const [logoutError, setLogoutError] = useState<string | null>(null);
  const settingsQuery = useQuery({
    queryKey: ["settings"],
    queryFn: getSettings,
  });
  const saveMutation = useMutation({
    mutationFn: updateDailyReviewCount,
    onSuccess: (data) => {
      queryClient.setQueryData(["settings"], data);
      setMessage("설정을 저장했습니다");
    },
    onError: (error) => {
      setMessage(error instanceof ApiError ? error.message : "설정 저장에 실패했습니다");
    },
  });

  useEffect(() => {
    if (settingsQuery.data) {
      setSelectedCount(settingsQuery.data.dailyReviewCount);
    }
  }, [settingsQuery.data]);

  async function handleLogout() {
    setLogoutError(null);
    try {
      await logout();
      router.replace("/login");
    } catch {
      setLogoutError("로그아웃에 실패했습니다");
    }
  }

  return (
    <div className="space-y-5 px-4 py-5">
      <header className="space-y-2">
        <Chip variant="sage">Settings</Chip>
        <div>
          <h1 className="text-2xl font-semibold text-cafe-ink">설정</h1>
          <p className="mt-1 text-sm leading-relaxed text-cafe-ink-soft">
            하루 복습량을 정하고 학습 리듬을 유지합니다.
          </p>
        </div>
      </header>

      {settingsQuery.isPending ? <LoadingState title="설정을 불러오는 중입니다" /> : null}

      {settingsQuery.isError ? (
        <ErrorState description="설정을 가져오지 못했습니다." onRetry={() => void settingsQuery.refetch()} />
      ) : null}

      {settingsQuery.data ? (
        <Surface className="space-y-5 p-5">
          <div>
            <h2 className="text-base font-semibold text-cafe-ink">하루 타입별 복습 개수</h2>
            <p className="mt-1 text-sm text-cafe-ink-soft">
              타입별 {selectedCount}개씩, 하루 총 {selectedCount * 3}장을 복습합니다.
            </p>
          </div>
          <div className="grid grid-cols-3 gap-2" role="group" aria-label="하루 복습 개수">
            {options.map((option) => (
              <button
                key={option}
                type="button"
                aria-pressed={selectedCount === option}
                className={`rounded-[14px] border px-3 py-3 text-sm font-semibold ${
                  selectedCount === option
                    ? "border-cafe-latte bg-cafe-latte-soft text-cafe-latte-deep"
                    : "border-[rgba(61,46,34,0.10)] bg-cafe-soft text-cafe-ink-soft"
                }`}
                onClick={() => {
                  setMessage(null);
                  setSelectedCount(option);
                }}
              >
                {option}
              </button>
            ))}
          </div>
          {message ? (
            <p className="rounded-[12px] bg-cafe-soft px-3 py-2 text-sm font-medium text-cafe-ink-soft" role="status">
              {message}
            </p>
          ) : null}
          <Button
            type="button"
            className="w-full"
            disabled={saveMutation.isPending}
            onClick={() => saveMutation.mutate(selectedCount)}
          >
            {saveMutation.isPending ? "저장 중" : "저장"}
          </Button>
        </Surface>
      ) : null}

      <Surface className="space-y-3 p-5">
        <h2 className="text-base font-semibold text-cafe-ink">계정</h2>
        <p className="text-sm leading-relaxed text-cafe-ink-soft">
          MVP에서는 닉네임/비밀번호 변경 없이 로그아웃만 제공합니다.
        </p>
        {logoutError ? <p className="text-sm font-medium text-cafe-warning">{logoutError}</p> : null}
        <Button type="button" variant="ghost" onClick={() => void handleLogout()}>
          로그아웃
        </Button>
      </Surface>
    </div>
  );
}
