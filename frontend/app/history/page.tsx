"use client";

import { useState, useEffect } from "react";
import { api } from "@/lib/api";
import type { Page, StudyRecordDto } from "@/lib/types";

export default function HistoryPage() {
  const [records, setRecords] = useState<StudyRecordDto[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    api
      .get<Page<StudyRecordDto>>(`/study-records?page=${page}&size=20`)
      .then((data) => {
        if (!cancelled) {
          setRecords(data.content);
          setTotalPages(data.totalPages);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setRecords([]);
          setTotalPages(0);
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [page]);

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-lg font-semibold text-ink tracking-tight">
        학습 기록
      </h1>

      {loading ? (
        <p className="text-sm text-ink-muted text-center py-8">
          불러오는 중...
        </p>
      ) : records.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-sm text-ink-muted">학습 기록이 없습니다</p>
        </div>
      ) : (
        <div className="flex flex-col gap-2">
          {records.map((r) => (
            <div
              key={r.id}
              className="bg-raised rounded-[20px] border border-hairline p-4 shadow-sm"
            >
              <div className="flex items-center justify-between">
                <span className="text-sm font-semibold text-primary">
                  Day {r.dayNumber}
                </span>
                <span className="text-xs text-ink-muted">
                  {new Date(r.createdAt).toLocaleDateString("ko-KR")}
                </span>
              </div>
              <div className="flex gap-3 mt-2">
                <span className="px-2.5 py-1 rounded-full text-[11.5px] font-medium bg-soft text-ink-soft">
                  단어 {r.wordCount}개
                </span>
                <span className="px-2.5 py-1 rounded-full text-[11.5px] font-medium bg-soft text-ink-soft">
                  패턴 {r.patternCount}개
                </span>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* 페이지네이션 */}
      {totalPages > 1 && (
        <div className="flex justify-center gap-2 mt-2">
          <button
            onClick={() => setPage(Math.max(0, page - 1))}
            disabled={page === 0}
            className="px-3 py-1 rounded-[10px] text-xs bg-soft text-ink-soft disabled:opacity-40"
          >
            이전
          </button>
          <span className="text-xs text-ink-muted self-center">
            {page + 1} / {totalPages}
          </span>
          <button
            onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
            disabled={page >= totalPages - 1}
            className="px-3 py-1 rounded-[10px] text-xs bg-soft text-ink-soft disabled:opacity-40"
          >
            다음
          </button>
        </div>
      )}
    </div>
  );
}
