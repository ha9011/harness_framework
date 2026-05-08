"use client";

import { useQuery } from "@tanstack/react-query";
import { CalendarDays, Coffee, Flame, PanelTop } from "lucide-react";
import Link from "next/link";
import type { ReactNode } from "react";
import { useAuth } from "@/app/components/providers/auth-provider";
import { Button } from "@/app/components/ui/button";
import { Chip } from "@/app/components/ui/chip";
import { EmptyState, ErrorState, LoadingState } from "@/app/components/ui/screen-state";
import { Surface } from "@/app/components/ui/surface";
import { getDashboard } from "@/app/lib/learning-api";
import type { StudyRecord } from "@/app/lib/learning-api";

export default function HomePage() {
  const { user } = useAuth();
  const dashboardQuery = useQuery({
    queryKey: ["dashboard"],
    queryFn: getDashboard,
  });

  if (dashboardQuery.isPending) {
    return (
      <div className="space-y-5 px-4 py-5">
        <HomeHeader nickname={user?.nickname} />
        <LoadingState title="대시보드를 불러오는 중입니다" />
      </div>
    );
  }

  if (dashboardQuery.isError) {
    return (
      <div className="space-y-5 px-4 py-5">
        <HomeHeader nickname={user?.nickname} />
        <ErrorState
          description="오늘의 학습 현황을 가져오지 못했습니다."
          onRetry={() => void dashboardQuery.refetch()}
        />
      </div>
    );
  }

  const dashboard = dashboardQuery.data;
  const remainingTotal =
    dashboard.todayReviewRemaining.word +
    dashboard.todayReviewRemaining.pattern +
    dashboard.todayReviewRemaining.sentence;
  const isEmpty =
    dashboard.wordCount === 0 &&
    dashboard.patternCount === 0 &&
    dashboard.sentenceCount === 0 &&
    dashboard.recentStudyRecords.length === 0;

  return (
    <div className="space-y-5 px-4 py-5">
      <HomeHeader nickname={user?.nickname} />

      <Surface className="space-y-5 p-5">
        <div className="flex items-start gap-3">
          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-[18px] bg-cafe-sage-bg text-cafe-sage">
            <Coffee className="h-6 w-6" aria-hidden="true" />
          </div>
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2">
              <h2 className="text-lg font-semibold text-cafe-ink">Coffee Tree</h2>
              <Chip variant="sage">{dashboard.streak}일 streak</Chip>
            </div>
            <p className="mt-1 text-sm leading-relaxed text-cafe-ink-soft">
              {treeMessage(dashboard.streak)}
            </p>
          </div>
        </div>
        <div className="h-3 overflow-hidden rounded-full bg-cafe-soft">
          <div
            className="h-full rounded-full bg-cafe-sage"
            style={{ width: `${Math.min(100, Math.max(12, dashboard.streak * 12))}%` }}
          />
        </div>
      </Surface>

      <Surface className="space-y-4 p-5">
        <div className="flex items-center justify-between gap-3">
          <div>
            <p className="text-sm text-cafe-ink-soft">오늘 남은 복습</p>
            <p className="text-3xl font-semibold text-cafe-ink">{remainingTotal}장</p>
          </div>
          <Button asChild size="lg">
            <Link href="/review">
              <PanelTop className="h-4 w-4" aria-hidden="true" />
              오늘의 복습 시작
            </Link>
          </Button>
        </div>
        <div className="grid grid-cols-3 gap-2">
          <ReviewStat label="단어" value={dashboard.todayReviewRemaining.word} />
          <ReviewStat label="패턴" value={dashboard.todayReviewRemaining.pattern} />
          <ReviewStat label="예문" value={dashboard.todayReviewRemaining.sentence} />
        </div>
      </Surface>

      <div className="grid grid-cols-2 gap-3">
        <TotalStat label="단어" value={dashboard.wordCount} />
        <TotalStat label="패턴" value={dashboard.patternCount} />
        <TotalStat label="예문" value={dashboard.sentenceCount} />
        <TotalStat label="연속 학습" value={dashboard.streak} suffix="일" icon={<Flame className="h-4 w-4" />} />
      </div>

      {isEmpty ? (
        <EmptyState
          title="아직 학습 데이터가 없습니다"
          description="단어와 패턴을 등록하면 이곳에서 성장 상태와 최근 기록을 볼 수 있습니다."
          action={
            <Button asChild variant="secondary">
              <Link href="/words">첫 단어 등록하기</Link>
            </Button>
          }
        />
      ) : (
        <Surface className="space-y-4 p-5">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-semibold text-cafe-ink">최근 학습 기록</h2>
            <Link href="/history" className="text-xs font-semibold text-cafe-latte-deep">
              전체 보기
            </Link>
          </div>
          <div className="space-y-3">
            {dashboard.recentStudyRecords.slice(0, 5).map((record) => (
              <StudyRecordRow key={record.id} record={record} />
            ))}
          </div>
        </Surface>
      )}

      <div className="grid grid-cols-2 gap-3">
        <Button asChild variant="secondary">
          <Link href="/history">
            <CalendarDays className="h-4 w-4" aria-hidden="true" />
            학습 기록
          </Link>
        </Button>
        <Button asChild variant="secondary">
          <Link href="/settings">설정</Link>
        </Button>
      </div>
    </div>
  );
}

function HomeHeader({ nickname }: { nickname?: string }) {
  return (
    <header className="space-y-2">
      <Chip variant="warm">Cozy Cafe</Chip>
      <div>
        <h1 className="text-2xl font-semibold text-cafe-ink">
          {nickname ? `${nickname}님, 오늘도 한 잔처럼 천천히` : "오늘도 한 잔처럼 천천히"}
        </h1>
        <p className="mt-1 text-sm leading-relaxed text-cafe-ink-soft">
          단어, 패턴, 예문을 매일 조금씩 쌓아가는 학습 대시보드입니다.
        </p>
      </div>
    </header>
  );
}

function ReviewStat({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-[14px] bg-cafe-soft p-3 text-center">
      <p className="text-xs font-medium text-cafe-ink-soft">{label}</p>
      <p className="mt-1 text-xl font-semibold text-cafe-ink">{value}</p>
    </div>
  );
}

function TotalStat({
  label,
  value,
  suffix,
  icon,
}: {
  label: string;
  value: number;
  suffix?: string;
  icon?: ReactNode;
}) {
  return (
    <Surface className="p-4">
      <div className="flex items-center gap-2 text-cafe-ink-soft">
        {icon}
        <span className="text-xs font-medium">{label}</span>
      </div>
      <p className="mt-2 text-2xl font-semibold text-cafe-ink">
        {value}
        {suffix}
      </p>
    </Surface>
  );
}

function StudyRecordRow({ record }: { record: StudyRecord }) {
  return (
    <div className="rounded-[14px] border border-[rgba(61,46,34,0.08)] bg-cafe-soft p-3">
      <div className="flex items-center justify-between gap-3">
        <p className="text-sm font-semibold text-cafe-ink">Day {record.dayNumber}</p>
        <p className="text-xs text-cafe-ink-muted">{record.studyDate}</p>
      </div>
      <div className="mt-2 flex flex-wrap gap-1.5">
        {record.items.map((item) => (
          <Chip key={`${item.type}-${item.id}`} variant={item.type === "WORD" ? "warm" : "sage"}>
            {item.name}
          </Chip>
        ))}
      </div>
    </div>
  );
}

function treeMessage(streak: number) {
  if (streak >= 7) return "커피나무에 잎이 풍성해졌습니다. 학습 리듬이 안정적입니다.";
  if (streak >= 3) return "작은 새싹이 단단해지고 있습니다. 오늘 복습으로 물을 주세요.";
  if (streak > 0) return "새싹이 막 올라왔습니다. 하루씩 이어가면 나무가 자랍니다.";
  return "첫 복습을 시작하면 커피나무가 자라기 시작합니다.";
}
