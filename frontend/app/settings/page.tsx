"use client";

import { useState, useEffect } from "react";
import { api } from "@/lib/api";
import type { UserSettingResponse } from "@/lib/types";

const REVIEW_COUNT_OPTIONS = [10, 20, 30];

export default function SettingsPage() {
  const [dailyReviewCount, setDailyReviewCount] = useState(10);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState(false);

  useEffect(() => {
    api
      .get<UserSettingResponse>("/settings")
      .then((data) => setDailyReviewCount(data.dailyReviewCount))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const handleSave = async () => {
    setSaving(true);
    setSaved(false);
    setError(false);
    try {
      const result = await api.put<UserSettingResponse>("/settings", {
        dailyReviewCount,
      });
      setDailyReviewCount(result.dailyReviewCount);
      setSaved(true);
      setTimeout(() => setSaved(false), 2000);
    } catch {
      setError(true);
      setTimeout(() => setError(false), 3000);
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <p className="text-sm text-ink-muted">불러오는 중...</p>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-5">
      <h1 className="text-lg font-semibold text-ink tracking-tight">설정</h1>

      <div className="bg-raised rounded-[20px] border border-hairline p-5 shadow-sm">
        <p className="text-sm font-semibold text-ink mb-1">
          하루 복습 개수
        </p>
        <p className="text-xs text-ink-muted mb-4">
          타입별(단어/패턴/문장) 각 N개씩 복습합니다
        </p>

        <div className="flex gap-2">
          {REVIEW_COUNT_OPTIONS.map((n) => (
            <button
              key={n}
              onClick={() => setDailyReviewCount(n)}
              className={`flex-1 py-2.5 rounded-[12px] text-sm font-semibold transition-colors ${
                dailyReviewCount === n
                  ? "bg-primary text-white"
                  : "bg-soft text-ink-soft border border-hairline"
              }`}
            >
              {n}개
            </button>
          ))}
        </div>

        <button
          onClick={handleSave}
          disabled={saving}
          className="mt-4 w-full bg-primary text-white rounded-[14px] h-[42px] text-sm font-semibold disabled:opacity-50"
        >
          {saving ? "저장 중..." : saved ? "저장 완료" : "저장"}
        </button>
        {error && (
          <p className="mt-2 text-xs text-warn text-center">
            저장에 실패했습니다. 다시 시도해주세요.
          </p>
        )}
      </div>
    </div>
  );
}
