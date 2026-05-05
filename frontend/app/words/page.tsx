"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import type { Page, WordListResponse } from "@/lib/types";
import WordAddModal from "./WordAddModal";
import AuthGuard from "../components/AuthGuard";

interface FetchState {
  words: WordListResponse[];
  totalPages: number;
  loading: boolean;
}

function WordsContent() {
  const [state, setState] = useState<FetchState>({
    words: [],
    totalPages: 0,
    loading: true,
  });
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [importantOnly, setImportantOnly] = useState(false);
  const [sort, setSort] = useState("latest");
  const [showAddModal, setShowAddModal] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);
  const { user } = useAuth();

  useEffect(() => {
    let cancelled = false;

    const params = new URLSearchParams({
      page: String(page),
      size: "20",
      sort: sort,
      importantOnly: String(importantOnly),
    });
    if (search) params.set("search", search);

    api
      .get<Page<WordListResponse>>(`/words?${params.toString()}`)
      .then((data) => {
        if (!cancelled) {
          setState({
            words: data.content,
            totalPages: data.totalPages,
            loading: false,
          });
        }
      })
      .catch(() => {
        if (!cancelled) {
          setState({ words: [], totalPages: 0, loading: false });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [page, search, importantOnly, sort, refreshKey, user]);

  const { words, totalPages, loading } = state;

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-lg font-semibold text-ink tracking-tight">
          📖 단어
        </h1>
        <button
          onClick={() => setShowAddModal(true)}
          className="bg-primary text-white rounded-[14px] h-[42px] px-4 text-sm font-semibold"
        >
          + 추가
        </button>
      </div>

      {/* 검색 + 필터 */}
      <div className="flex gap-2">
        <input
          type="text"
          placeholder="단어 또는 뜻 검색..."
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setPage(0);
          }}
          className="flex-1 bg-soft rounded-[12px] border border-hairline px-4 py-3 text-sm text-ink placeholder:text-ink-muted"
        />
      </div>

      <div className="flex gap-2">
        <button
          onClick={() => {
            setImportantOnly(!importantOnly);
            setPage(0);
          }}
          className={`px-2.5 py-1 rounded-full text-[11.5px] font-medium ${
            importantOnly
              ? "bg-primary-soft text-primary-deep"
              : "bg-soft text-ink-soft"
          }`}
        >
          ⭐ 중요만
        </button>
        <button
          onClick={() => setSort(sort === "latest" ? "name" : "latest")}
          className="px-2.5 py-1 rounded-full text-[11.5px] font-medium bg-soft text-ink-soft"
        >
          {sort === "latest" ? "최신순" : "이름순"}
        </button>
      </div>

      {/* 단어 목록 */}
      {loading ? (
        <p className="text-sm text-ink-muted text-center py-8">불러오는 중...</p>
      ) : words.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-sm text-ink-muted">등록된 단어가 없습니다</p>
          <button
            onClick={() => setShowAddModal(true)}
            className="mt-3 text-sm text-primary font-semibold"
          >
            단어 등록하러 가기
          </button>
        </div>
      ) : (
        <div className="flex flex-col gap-2">
          {words.map((w) => (
            <Link
              key={w.id}
              href={`/words/${w.id}`}
              className="bg-raised rounded-[20px] border border-hairline p-4 shadow-sm flex items-center justify-between"
            >
              <div>
                <div className="flex items-center gap-2">
                  <span className="text-sm font-semibold text-ink">
                    {w.word}
                  </span>
                  {w.important && <span className="text-xs">⭐</span>}
                  {w.partOfSpeech && (
                    <span className="px-2 py-0.5 rounded-full text-[10px] font-medium bg-sage-bg text-sage">
                      {w.partOfSpeech}
                    </span>
                  )}
                </div>
                <p className="text-xs text-ink-soft mt-1">{w.meaning}</p>
              </div>
            </Link>
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

      {/* 등록 모달 */}
      {showAddModal && (
        <WordAddModal
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

export default function WordsPage() {
  return (
    <AuthGuard>
      <WordsContent />
    </AuthGuard>
  );
}
