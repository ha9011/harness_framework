"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import type { DashboardResponse } from "@/lib/types";
import CoffeeTree from "./components/CoffeeTree";
import AuthGuard from "./components/AuthGuard";

function HomeContent() {
  const { user } = useAuth();
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    api
      .get<DashboardResponse>("/dashboard")
      .then((data) => { if (!cancelled) setDashboard(data); })
      .catch(() => { if (!cancelled) setDashboard(null); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [user]);

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <p className="text-sm text-ink-muted">불러오는 중...</p>
      </div>
    );
  }

  if (!dashboard) {
    return (
      <div className="flex flex-col gap-4 items-center py-20">
        <p className="text-sm text-ink-muted">
          대시보드를 불러올 수 없습니다
        </p>
        <button
          onClick={() => window.location.reload()}
          className="text-sm text-primary font-semibold"
        >
          다시 시도
        </button>
      </div>
    );
  }

  const { todayReviewRemaining: remaining } = dashboard;
  const totalRemaining = remaining.word + remaining.pattern + remaining.sentence;
  const isEmpty =
    dashboard.wordCount === 0 &&
    dashboard.patternCount === 0 &&
    dashboard.sentenceCount === 0;

  return (
    <div className="flex flex-col gap-5">
      {/* 인사 헤더 */}
      <div>
        <p className="text-xs text-ink-muted">오늘도 한 모금 천천히</p>
        <h1 className="text-lg font-semibold text-ink tracking-tight mt-1">
          {user?.nickname}님 안녕하세요
        </h1>
      </div>

      {/* 커피나무 */}
      <div className="bg-raised rounded-[20px] border border-hairline p-5 shadow-sm flex flex-col items-center">
        <p className="text-xs font-semibold text-ink-muted uppercase tracking-wider self-start mb-3">
          나의 카페 정원
        </p>
        <CoffeeTree streak={dashboard.streak} size={140} />
        <div className="mt-3 flex items-center gap-2">
          <span
            className={`text-xs font-semibold ${dashboard.streak > 0 ? "text-sage" : "text-warn"}`}
          >
            {dashboard.streak > 0
              ? `${dashboard.streak}일 연속 복습 중`
              : "오늘 복습을 시작해보세요"}
          </span>
        </div>
      </div>

      {/* 빈 상태 */}
      {isEmpty && (
        <div className="bg-raised rounded-[20px] border border-hairline p-5 shadow-sm text-center">
          <p className="text-sm text-ink-soft">아직 등록된 학습 데이터가 없어요</p>
          <Link
            href="/words"
            className="mt-3 inline-block text-sm text-primary font-semibold"
          >
            단어 등록하러 가기
          </Link>
        </div>
      )}

      {/* 오늘의 복습 */}
      {!isEmpty && (
        <>
          <div>
            <div className="flex items-center justify-between mb-3">
              <p className="text-xs font-semibold text-ink-muted uppercase tracking-wider">
                오늘의 복습
              </p>
              <Link
                href="/settings"
                className="text-xs text-primary font-semibold"
              >
                설정
              </Link>
            </div>
            <div className="flex flex-col gap-2">
              {[
                {
                  label: "단어",
                  count: remaining.word,
                  hint: "영→한 / 한→영",
                },
                {
                  label: "패턴",
                  count: remaining.pattern,
                  hint: "교재 예문 포함",
                },
                {
                  label: "문장",
                  count: remaining.sentence,
                  hint: "상황과 함께",
                },
              ].map((r) => (
                <div
                  key={r.label}
                  className="bg-raised rounded-[16px] border border-hairline p-3 flex items-center gap-3"
                >
                  <div className="w-[38px] h-[38px] rounded-[12px] bg-soft flex items-center justify-center text-primary font-semibold text-sm">
                    {r.count}
                  </div>
                  <div className="flex-1">
                    <p className="text-sm font-semibold text-ink">{r.label}</p>
                    <p className="text-[11.5px] text-ink-muted mt-0.5">
                      {r.hint}
                    </p>
                  </div>
                  <span className="text-xs text-primary font-semibold">
                    {r.count}장
                  </span>
                </div>
              ))}
            </div>
            {totalRemaining > 0 && (
              <Link
                href="/review"
                className="mt-3 block bg-primary text-white rounded-[14px] h-[42px] flex items-center justify-center text-sm font-semibold shadow-sm"
              >
                오늘의 복습 시작 · {totalRemaining}장
              </Link>
            )}
          </div>
        </>
      )}

      {/* 누적 통계 */}
      <div>
        <p className="text-xs font-semibold text-ink-muted uppercase tracking-wider mb-3">
          누적 학습
        </p>
        <div className="grid grid-cols-4 gap-2">
          {[
            { n: dashboard.wordCount, l: "단어" },
            { n: dashboard.patternCount, l: "패턴" },
            { n: dashboard.sentenceCount, l: "예문" },
            { n: dashboard.streak, l: "연속일", accent: true },
          ].map((s, i) => (
            <div
              key={i}
              className={`rounded-[14px] border border-hairline p-3 text-center ${s.accent ? "bg-primary-soft" : "bg-raised"}`}
            >
              <p
                className={`text-xl font-semibold ${s.accent ? "text-primary-deep" : "text-ink"}`}
              >
                {s.n}
              </p>
              <p
                className={`text-[11px] mt-1 ${s.accent ? "text-primary-deep" : "text-ink-muted"}`}
              >
                {s.l}
              </p>
            </div>
          ))}
        </div>
      </div>

      {/* 최근 학습 */}
      {dashboard.recentStudyRecords.length > 0 && (
        <div>
          <div className="flex items-center justify-between mb-3">
            <p className="text-xs font-semibold text-ink-muted uppercase tracking-wider">
              최근 학습
            </p>
            <Link
              href="/history"
              className="text-xs text-primary font-semibold"
            >
              모두 보기
            </Link>
          </div>
          <div className="flex flex-col gap-1.5">
            {dashboard.recentStudyRecords.map((r) => (
              <div
                key={r.id}
                className="bg-raised rounded-[12px] border border-hairline px-3.5 py-2.5 flex items-center gap-3"
              >
                <span className="text-xs text-primary font-semibold w-12">
                  Day {r.dayNumber}
                </span>
                <span className="text-[13px] text-ink flex-1">
                  단어 {r.wordCount}개, 패턴 {r.patternCount}개
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

export default function Home() {
  return (
    <AuthGuard>
      <HomeContent />
    </AuthGuard>
  );
}
