"use client";

import { useState, useEffect, use } from "react";
import { useRouter } from "next/navigation";
import { api, ApiError } from "@/lib/api";
import type { PatternDetailResponse } from "@/lib/types";

export default function PatternDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const router = useRouter();
  const [pattern, setPattern] = useState<PatternDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchPattern = async () => {
      try {
        const data = await api.get<PatternDetailResponse>(`/patterns/${id}`);
        setPattern(data);
      } catch (e) {
        if (e instanceof ApiError) {
          setError(e.message);
        }
      } finally {
        setLoading(false);
      }
    };
    fetchPattern();
  }, [id]);

  const handleDelete = async () => {
    if (!confirm("정말 삭제하시겠습니까?")) return;
    try {
      await api.delete(`/patterns/${id}`);
      router.push("/patterns");
    } catch (e) {
      console.error(e);
    }
  };

  if (loading) {
    return (
      <p className="text-sm text-ink-muted text-center py-8">불러오는 중...</p>
    );
  }

  if (error || !pattern) {
    return (
      <p className="text-sm text-warn text-center py-8">
        {error || "패턴을 찾을 수 없습니다"}
      </p>
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <button
        onClick={() => router.push("/patterns")}
        className="text-sm text-ink-muted self-start"
      >
        ← 목록으로
      </button>

      {/* 패턴 정보 카드 */}
      <div className="bg-raised rounded-[20px] border border-hairline p-5 shadow-sm">
        <h1 className="text-lg font-semibold text-ink">{pattern.template}</h1>
        {pattern.description && (
          <p className="text-sm text-ink-soft mt-1">{pattern.description}</p>
        )}
      </div>

      {/* 교재 예문 목록 */}
      {pattern.examples.length > 0 && (
        <div>
          <h2 className="text-xs font-semibold text-ink-soft uppercase mb-2">
            교재 예문
          </h2>
          <div className="flex flex-col gap-2">
            {pattern.examples.map((ex, i) => (
              <div
                key={i}
                className="bg-raised rounded-[14px] border border-hairline p-3"
              >
                <p className="text-sm text-ink">{ex.sentence}</p>
                {ex.translation && (
                  <p className="text-xs text-ink-soft mt-1">
                    {ex.translation}
                  </p>
                )}
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
        패턴 삭제
      </button>
    </div>
  );
}
