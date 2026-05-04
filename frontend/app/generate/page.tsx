"use client";

import { useState } from "react";
import { api, ApiError } from "@/lib/api";
import {
  GenerateResponse,
  SentenceResponse,
  GenerationHistoryResponse,
  Page,
} from "@/lib/types";

const LEVELS = [
  { value: "TODDLER", label: "유아" },
  { value: "ELEMENTARY", label: "초등" },
  { value: "INTERMEDIATE", label: "중등" },
  { value: "ADVANCED", label: "고등" },
];

const COUNTS = [10, 20, 30];

export default function GeneratePage() {
  const [mode, setMode] = useState<"generate" | "history">("generate");
  const [level, setLevel] = useState("ELEMENTARY");
  const [count, setCount] = useState(10);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [sentences, setSentences] = useState<SentenceResponse[]>([]);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  // 이력 상태
  const [history, setHistory] = useState<GenerationHistoryResponse[]>([]);
  const [historyPage, setHistoryPage] = useState(0);
  const [historyTotalPages, setHistoryTotalPages] = useState(0);
  const [historyLoading, setHistoryLoading] = useState(false);

  const handleGenerate = async () => {
    setLoading(true);
    setError("");
    setSentences([]);
    try {
      const data = await api.post<GenerateResponse>("/generate", {
        level,
        count,
      });
      setSentences(data.sentences);
    } catch (e) {
      if (e instanceof ApiError) setError(e.message);
      else setError("예문 생성에 실패했습니다");
    } finally {
      setLoading(false);
    }
  };

  const loadHistory = async (page: number) => {
    setHistoryLoading(true);
    try {
      const data = await api.get<Page<GenerationHistoryResponse>>(
        `/generate/history?page=${page}&size=20`
      );
      setHistory(data.content);
      setHistoryTotalPages(data.totalPages);
      setHistoryPage(data.number);
    } catch {
      setHistory([]);
    } finally {
      setHistoryLoading(false);
    }
  };

  const handleTabChange = (tab: "generate" | "history") => {
    setMode(tab);
    if (tab === "history") {
      loadHistory(0);
    }
  };

  const getLevelLabel = (value: string) =>
    LEVELS.find((l) => l.value === value)?.label ?? value;

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-lg font-semibold text-ink tracking-tight">
        예문 생성
      </h1>

      {/* 탭 */}
      <div className="bg-soft rounded-[14px] border border-hairline p-1 flex gap-1.5">
        <button
          onClick={() => handleTabChange("generate")}
          className={`flex-1 py-2 rounded-[10px] text-xs font-medium transition-all ${
            mode === "generate"
              ? "bg-raised text-ink font-semibold shadow-sm"
              : "text-ink-muted"
          }`}
        >
          생성
        </button>
        <button
          onClick={() => handleTabChange("history")}
          className={`flex-1 py-2 rounded-[10px] text-xs font-medium transition-all ${
            mode === "history"
              ? "bg-raised text-ink font-semibold shadow-sm"
              : "text-ink-muted"
          }`}
        >
          이력
        </button>
      </div>

      {mode === "generate" && (
        <>
          {/* 난이도 선택 */}
          <div>
            <p className="text-xs font-semibold text-ink-soft uppercase mb-2">
              난이도
            </p>
            <div className="flex gap-2">
              {LEVELS.map((l) => (
                <button
                  key={l.value}
                  onClick={() => setLevel(l.value)}
                  className={`flex-1 py-2 rounded-[12px] text-xs font-medium border transition-all ${
                    level === l.value
                      ? "bg-primary-soft text-primary-deep border-primary/30"
                      : "bg-soft text-ink-soft border-hairline"
                  }`}
                >
                  {l.label}
                </button>
              ))}
            </div>
          </div>

          {/* 개수 선택 */}
          <div>
            <p className="text-xs font-semibold text-ink-soft uppercase mb-2">
              개수
            </p>
            <div className="flex gap-2">
              {COUNTS.map((c) => (
                <button
                  key={c}
                  onClick={() => setCount(c)}
                  className={`flex-1 py-2 rounded-[12px] text-xs font-medium border transition-all ${
                    count === c
                      ? "bg-primary-soft text-primary-deep border-primary/30"
                      : "bg-soft text-ink-soft border-hairline"
                  }`}
                >
                  {c}개
                </button>
              ))}
            </div>
          </div>

          {/* 생성 버튼 */}
          <button
            onClick={handleGenerate}
            disabled={loading}
            className="bg-primary text-white rounded-[14px] h-[42px] text-sm font-semibold disabled:opacity-50 transition-all"
          >
            {loading ? "생성 중..." : "예문 생성"}
          </button>

          {error && <p className="text-xs text-warn">{error}</p>}

          {/* 생성 결과 */}
          {sentences.length > 0 && (
            <div className="flex flex-col gap-3 mt-2">
              <p className="text-xs text-ink-muted">
                {sentences.length}개 예문이 생성되었습니다
              </p>
              {sentences.map((s) => (
                <div
                  key={s.id}
                  className="bg-raised rounded-[20px] border border-hairline p-4 shadow-sm"
                >
                  {/* 영어 문장 */}
                  <p className="text-sm font-semibold text-ink leading-relaxed">
                    {s.englishSentence}
                  </p>

                  {/* 상황 말풍선 */}
                  {s.situations.length > 0 && (
                    <div className="flex flex-wrap gap-1.5 mt-2">
                      {s.situations.map((sit, i) => (
                        <span
                          key={i}
                          className="bg-sage-bg text-sage rounded-[18px] px-3 py-1.5 text-[11px] font-medium border border-sage/10"
                        >
                          {sit}
                        </span>
                      ))}
                    </div>
                  )}

                  {/* 한국어 해석 (탭/클릭 시 펼침) */}
                  <button
                    onClick={() =>
                      setExpandedId(expandedId === s.id ? null : s.id)
                    }
                    className="text-[11px] text-ink-muted mt-3 flex items-center gap-1"
                  >
                    {expandedId === s.id ? "해석 접기" : "해석 보기"}
                    <span className="text-[10px]">
                      {expandedId === s.id ? "▲" : "▼"}
                    </span>
                  </button>
                  {expandedId === s.id && (
                    <p className="text-sm text-ink-soft mt-1 leading-relaxed">
                      {s.koreanTranslation}
                    </p>
                  )}
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {mode === "history" && (
        <div className="flex flex-col gap-3">
          {historyLoading && (
            <p className="text-xs text-ink-muted text-center py-4">
              로딩 중...
            </p>
          )}
          {!historyLoading && history.length === 0 && (
            <p className="text-xs text-ink-muted text-center py-4">
              생성 이력이 없습니다
            </p>
          )}
          {history.map((h) => (
            <div
              key={h.id}
              className="bg-raised rounded-[14px] border border-hairline p-3"
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="px-2 py-0.5 rounded-full text-[10px] font-medium bg-primary-soft text-primary-deep">
                    {getLevelLabel(h.level)}
                  </span>
                  <span className="text-xs text-ink-soft">
                    {h.actualCount}/{h.requestedCount}개
                  </span>
                </div>
                <span className="text-[10px] text-ink-muted">
                  {new Date(h.createdAt).toLocaleDateString("ko-KR")}
                </span>
              </div>
            </div>
          ))}

          {/* 페이지네이션 */}
          {historyTotalPages > 1 && (
            <div className="flex justify-center gap-2 mt-2">
              <button
                onClick={() => loadHistory(Math.max(0, historyPage - 1))}
                disabled={historyPage === 0}
                className="text-xs text-ink-muted disabled:opacity-30"
              >
                이전
              </button>
              <span className="text-xs text-ink-muted">
                {historyPage + 1} / {historyTotalPages}
              </span>
              <button
                onClick={() =>
                  loadHistory(
                    Math.min(historyTotalPages - 1, historyPage + 1)
                  )
                }
                disabled={historyPage >= historyTotalPages - 1}
                className="text-xs text-ink-muted disabled:opacity-30"
              >
                다음
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
