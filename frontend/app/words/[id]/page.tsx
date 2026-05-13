"use client";

import { useState, useEffect, use } from "react";
import { useRouter } from "next/navigation";
import { api, ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import type { WordDetailResponse, WordResponse } from "@/lib/types";
import AuthGuard from "../../components/AuthGuard";
import CoffeeSpinner from "../../components/CoffeeSpinner";

function WordDetailContent({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const router = useRouter();
  const { user } = useAuth();
  const [word, setWord] = useState<WordDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchWord = async () => {
      try {
        const data = await api.get<WordDetailResponse>(`/words/${id}`);
        setWord(data);
      } catch (e) {
        if (e instanceof ApiError) {
          setError(e.message);
        }
      } finally {
        setLoading(false);
      }
    };
    fetchWord();
  }, [id, user]);

  const handleToggleImportant = async () => {
    if (!word) return;
    try {
      const updated = await api.patch<WordResponse>(
        `/words/${id}/important`
      );
      setWord({ ...word, important: updated.important });
    } catch (e) {
      console.error(e);
    }
  };

  const handleDelete = async () => {
    if (!confirm("정말 삭제하시겠습니까?")) return;
    try {
      await api.delete(`/words/${id}`);
      router.push("/words");
    } catch (e) {
      console.error(e);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center gap-2 py-8">
        <CoffeeSpinner />
        <span className="text-sm text-ink-muted">불러오는 중...</span>
      </div>
    );
  }

  if (error || !word) {
    return (
      <p className="text-sm text-warn text-center py-8">
        {error || "단어를 찾을 수 없습니다"}
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <button
        onClick={() => router.push("/words")}
        className="text-sm text-ink-muted self-start"
      >
        ← 목록으로
      </button>

      {/* 단어 정보 카드 */}
      <div className="bg-raised rounded-[20px] border border-hairline p-5 shadow-sm">
        <div className="flex items-center justify-between">
          <h1 className="text-lg font-semibold text-ink">{word.word}</h1>
          <button
            onClick={handleToggleImportant}
            className="text-xl"
          >
            {word.important ? "⭐" : "☆"}
          </button>
        </div>
        <p className="text-sm text-ink-soft mt-1">{word.meaning}</p>

        {/* AI 보강 정보 */}
        <div className="mt-4 flex flex-col gap-2">
          {word.partOfSpeech && (
            <div className="flex items-center gap-2">
              <span className="text-xs text-ink-muted w-12">품사</span>
              <span className="px-2.5 py-1 rounded-full text-[11.5px] font-medium bg-sage-bg text-sage">
                {word.partOfSpeech}
              </span>
            </div>
          )}
          {word.pronunciation && (
            <div className="flex items-center gap-2">
              <span className="text-xs text-ink-muted w-12">발음</span>
              <span className="text-sm text-ink-soft">{word.pronunciation}</span>
            </div>
          )}
          {word.synonyms && (
            <div className="flex items-center gap-2">
              <span className="text-xs text-ink-muted w-12">유의어</span>
              <span className="text-sm text-ink-soft">{word.synonyms}</span>
            </div>
          )}
          {word.tip && (
            <div className="flex gap-2 mt-1">
              <span className="text-xs text-ink-muted w-12 shrink-0">팁</span>
              <span className="text-sm text-ink-soft">{word.tip}</span>
            </div>
          )}
        </div>
      </div>

      {/* 예문 목록 */}
      {word.examples.length > 0 && (
        <div>
          <h2 className="text-xs font-semibold text-ink-soft uppercase mb-2">
            관련 예문
          </h2>
          <div className="flex flex-col gap-2">
            {word.examples.map((ex, i) => (
              <div
                key={i}
                className="bg-raised rounded-[14px] border border-hairline p-3"
              >
                <p className="text-sm text-ink">{ex}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 삭제 버튼 */}
      <button
        onClick={handleDelete}
        className="mt-4 text-sm text-warn self-center"
      >
        단어 삭제
      </button>
    </div>
  );
}

export default function WordDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  return (
    <AuthGuard>
      <WordDetailContent params={params} />
    </AuthGuard>
  );
}
