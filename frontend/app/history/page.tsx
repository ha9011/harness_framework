"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";
import { Button } from "@/app/components/ui/button";
import { Chip } from "@/app/components/ui/chip";
import { EmptyState, ErrorState, LoadingState } from "@/app/components/ui/screen-state";
import { Surface } from "@/app/components/ui/surface";
import { getStudyRecords } from "@/app/lib/learning-api";

export default function HistoryPage() {
  const [page, setPage] = useState(0);
  const recordsQuery = useQuery({
    queryKey: ["study-records", page],
    queryFn: () => getStudyRecords(page),
  });

  return (
    <div className="space-y-5 px-4 py-5">
      <header className="space-y-2">
        <Chip variant="warm">History</Chip>
        <div>
          <h1 className="text-2xl font-semibold text-cafe-ink">학습 기록</h1>
          <p className="mt-1 text-sm leading-relaxed text-cafe-ink-soft">
            등록한 단어와 패턴을 날짜별 최신순으로 확인합니다.
          </p>
        </div>
      </header>

      {recordsQuery.isPending ? <LoadingState title="학습 기록을 불러오는 중입니다" /> : null}

      {recordsQuery.isError ? (
        <ErrorState
          description="학습 기록을 가져오지 못했습니다."
          onRetry={() => void recordsQuery.refetch()}
        />
      ) : null}

      {recordsQuery.data && recordsQuery.data.content.length === 0 ? (
        <EmptyState
          title="아직 기록이 없습니다"
          description="단어 또는 패턴을 등록하면 Day 기록이 자동으로 쌓입니다."
          action={
            <Button asChild variant="secondary">
              <Link href="/words">단어 등록하기</Link>
            </Button>
          }
        />
      ) : null}

      {recordsQuery.data && recordsQuery.data.content.length > 0 ? (
        <Surface className="space-y-3 p-4">
          {recordsQuery.data.content.map((record) => (
            <article key={record.id} className="rounded-[16px] bg-cafe-soft p-4">
              <div className="flex items-center justify-between gap-3">
                <h2 className="text-base font-semibold text-cafe-ink">Day {record.dayNumber}</h2>
                <time className="text-xs font-medium text-cafe-ink-muted">{record.studyDate}</time>
              </div>
              <div className="mt-3 flex flex-wrap gap-2">
                {record.items.map((item) => (
                  <Chip key={`${record.id}-${item.type}-${item.id}`} variant={item.type === "WORD" ? "warm" : "sage"}>
                    {item.type === "WORD" ? "단어" : "패턴"} · {item.name}
                  </Chip>
                ))}
              </div>
            </article>
          ))}
        </Surface>
      ) : null}

      {recordsQuery.data && recordsQuery.data.totalPages > 1 ? (
        <div className="flex items-center justify-between gap-3">
          <Button type="button" variant="secondary" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>
            이전
          </Button>
          <span className="text-sm font-medium text-cafe-ink-soft">
            {page + 1} / {recordsQuery.data.totalPages}
          </span>
          <Button
            type="button"
            variant="secondary"
            disabled={page + 1 >= recordsQuery.data.totalPages}
            onClick={() => setPage((value) => value + 1)}
          >
            더 보기
          </Button>
        </div>
      ) : null}
    </div>
  );
}
