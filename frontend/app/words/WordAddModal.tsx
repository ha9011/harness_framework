"use client";

import { useState } from "react";
import { api, ApiError } from "@/lib/api";
import type { WordResponse, BulkCreateResponse } from "@/lib/types";

interface Props {
  onClose: () => void;
  onSuccess: () => void;
}

export default function WordAddModal({ onClose, onSuccess }: Props) {
  const [mode, setMode] = useState<"single" | "bulk">("single");
  const [word, setWord] = useState("");
  const [meaning, setMeaning] = useState("");
  const [bulkJson, setBulkJson] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [result, setResult] = useState("");

  const handleSingleSubmit = async () => {
    if (!word.trim() || !meaning.trim()) return;
    setLoading(true);
    setError("");
    try {
      await api.post<WordResponse>("/words", { word: word.trim(), meaning: meaning.trim() });
      setResult("등록 완료!");
      setTimeout(onSuccess, 500);
    } catch (e) {
      if (e instanceof ApiError) {
        setError(e.message);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleBulkSubmit = async () => {
    if (!bulkJson.trim()) return;
    setLoading(true);
    setError("");
    try {
      const parsed = JSON.parse(bulkJson);
      const data = await api.post<BulkCreateResponse>("/words/bulk", parsed);
      setResult(
        `저장: ${data.saved}개, 건너뜀: ${data.skipped}개, 보강 실패: ${data.enrichmentFailed}개`
      );
      setTimeout(onSuccess, 1000);
    } catch (e) {
      if (e instanceof SyntaxError) {
        setError("JSON 형식이 올바르지 않습니다");
      } else if (e instanceof ApiError) {
        setError(e.message);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/30 flex items-end justify-center z-50">
      <div className="bg-raised w-full max-w-md rounded-t-[20px] p-5 pb-10">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-sm font-semibold text-ink">단어 등록</h2>
          <button onClick={onClose} className="text-ink-muted text-sm">
            닫기
          </button>
        </div>

        {/* 탭 */}
        <div className="bg-soft rounded-[14px] border border-hairline p-1 flex gap-1.5 mb-4">
          <button
            onClick={() => setMode("single")}
            className={`flex-1 py-2 rounded-[10px] text-xs font-medium ${
              mode === "single"
                ? "bg-raised text-ink font-semibold shadow-sm"
                : "text-ink-muted"
            }`}
          >
            단건 등록
          </button>
          <button
            onClick={() => setMode("bulk")}
            className={`flex-1 py-2 rounded-[10px] text-xs font-medium ${
              mode === "bulk"
                ? "bg-raised text-ink font-semibold shadow-sm"
                : "text-ink-muted"
            }`}
          >
            벌크 등록
          </button>
        </div>

        {mode === "single" ? (
          <div className="flex flex-col gap-3">
            <input
              type="text"
              placeholder="영어 단어"
              value={word}
              onChange={(e) => setWord(e.target.value)}
              className="bg-soft rounded-[12px] border border-hairline px-4 py-3 text-sm text-ink placeholder:text-ink-muted"
            />
            <input
              type="text"
              placeholder="뜻"
              value={meaning}
              onChange={(e) => setMeaning(e.target.value)}
              className="bg-soft rounded-[12px] border border-hairline px-4 py-3 text-sm text-ink placeholder:text-ink-muted"
            />
            <button
              onClick={handleSingleSubmit}
              disabled={loading}
              className="bg-primary text-white rounded-[14px] h-[42px] text-sm font-semibold disabled:opacity-50"
            >
              {loading ? "등록 중..." : "등록"}
            </button>
          </div>
        ) : (
          <div className="flex flex-col gap-3">
            <textarea
              placeholder={'[{"word":"apple","meaning":"사과"},...]'}
              value={bulkJson}
              onChange={(e) => setBulkJson(e.target.value)}
              rows={5}
              className="bg-soft rounded-[12px] border border-hairline px-4 py-3 text-sm text-ink placeholder:text-ink-muted resize-none"
            />
            <button
              onClick={handleBulkSubmit}
              disabled={loading}
              className="bg-primary text-white rounded-[14px] h-[42px] text-sm font-semibold disabled:opacity-50"
            >
              {loading ? "등록 중..." : "벌크 등록"}
            </button>
          </div>
        )}

        {error && (
          <p className="text-xs text-warn mt-2">{error}</p>
        )}
        {result && (
          <p className="text-xs text-sage mt-2">{result}</p>
        )}
      </div>
    </div>
  );
}
