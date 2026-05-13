"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import type { Page, PatternListResponse } from "@/lib/types";
import PatternAddModal from "./PatternAddModal";
import AuthGuard from "../components/AuthGuard";
import CoffeeSpinner from "../components/CoffeeSpinner";

interface FetchState {
  patterns: PatternListResponse[];
  totalPages: number;
  loading: boolean;
}

function PatternsContent() {
  const [state, setState] = useState<FetchState>({
    patterns: [],
    totalPages: 0,
    loading: true,
  });
  const [page, setPage] = useState(0);
  const [showAddModal, setShowAddModal] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);
  const { user } = useAuth();

  useEffect(() => {
    let cancelled = false;

    const params = new URLSearchParams({
      page: String(page),
      size: "20",
      sort: "createdAt,desc",
    });

    api
      .get<Page<PatternListResponse>>(`/patterns?${params.toString()}`)
      .then((data) => {
        if (!cancelled) {
          setState({
            patterns: data.content,
            totalPages: data.totalPages,
            loading: false,
          });
        }
      })
      .catch(() => {
        if (!cancelled) {
          setState({ patterns: [], totalPages: 0, loading: false });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [page, refreshKey, user]);

  const { patterns, totalPages, loading } = state;

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-semibold text-ink tracking-tight">
          🔤 패턴
        </h1>
        <button
          onClick={() => setShowAddModal(true)}
          className="bg-primary text-white rounded-[14px] h-[42px] px-4 text-sm font-semibold"
        >
          + 추가
        </button>
      </div>

      {loading ? (
        <div className="flex items-center justify-center gap-2 py-8">
          <CoffeeSpinner />
          <span className="text-sm text-ink-muted">불러오는 중...</span>
        </div>
      ) : patterns.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-sm text-ink-muted">등록된 패턴이 없습니다</p>
          <button
            onClick={() => setShowAddModal(true)}
            className="mt-3 text-sm text-primary font-semibold"
          >
            패턴 등록하러 가기
          </button>
        </div>
      ) : (
        <div className="flex flex-col gap-2">
          {patterns.map((p) => (
            <Link
              key={p.id}
              href={`/patterns/${p.id}`}
              className="bg-raised rounded-[20px] border border-hairline p-4 shadow-sm"
            >
              <div className="flex items-center justify-between">
                <span className="text-sm font-semibold text-ink">
                  {p.template}
                </span>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-medium bg-sage-bg text-sage">
                  예문 {p.exampleCount}개
                </span>
              </div>
              {p.description && (
                <p className="text-xs text-ink-soft mt-1">{p.description}</p>
              )}
            </Link>
          ))}
        </div>
      )}

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

      {showAddModal && (
        <PatternAddModal
          onClose={() => setShowAddModal(false)}
          onSuccess={() => {
            setShowAddModal(false);
            setRefreshKey((k) => k + 1);
          }}
        />
      )}
    </div>
  );
}

export default function PatternsPage() {
  return (
    <AuthGuard>
      <PatternsContent />
    </AuthGuard>
  );
}
