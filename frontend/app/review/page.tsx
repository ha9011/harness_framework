"use client";

import { useState, useEffect } from "react";
import { api, ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { ReviewCard, ReviewResultResponse } from "@/lib/types";
import FlipCard from "../components/FlipCard";
import AuthGuard from "../components/AuthGuard";

const TABS = [
  { value: "WORD", label: "단어" },
  { value: "PATTERN", label: "패턴" },
  { value: "SENTENCE", label: "문장" },
];

function ReviewContent() {
  const [activeTab, setActiveTab] = useState("WORD");
  const [cards, setCards] = useState<ReviewCard[]>([]);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [flipped, setFlipped] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [completedIds, setCompletedIds] = useState<number[]>([]);
  const [isReadOnly, setIsReadOnly] = useState(false);
  const [isComplete, setIsComplete] = useState(false);
  const { user } = useAuth();

  useEffect(() => {
    let cancelled = false;

    const params = new URLSearchParams({ type: activeTab });
    api
      .get<ReviewCard[]>(`/reviews/today?${params}`)
      .then((data) => {
        if (!cancelled) {
          setCards(data);
          setCurrentIndex(0);
          setFlipped(false);
          setIsReadOnly(false);
          setIsComplete(data.length === 0);
          setLoading(false);
        }
      })
      .catch((e) => {
        if (!cancelled) {
          setError(e instanceof ApiError ? e.message : "카드를 불러오는데 실패했습니다");
          setLoading(false);
        }
      });

    return () => { cancelled = true; };
  }, [activeTab, user]);

  const loadCards = async (type: string, exclude: number[] = []) => {
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams({ type });
      exclude.forEach((id) => params.append("exclude", String(id)));
      const data = await api.get<ReviewCard[]>(`/reviews/today?${params}`);
      setCards(data);
      setCurrentIndex(0);
      setFlipped(false);
      setIsReadOnly(false);
      setIsComplete(data.length === 0);
    } catch (e) {
      if (e instanceof ApiError) setError(e.message);
      else setError("카드를 불러오는데 실패했습니다");
    } finally {
      setLoading(false);
    }
  };

  const handleResult = async (result: string) => {
    if (isReadOnly) {
      // 읽기 전용 모드에서는 다음 카드로 이동만
      goNext();
      return;
    }

    const card = cards[currentIndex];
    try {
      await api.post<ReviewResultResponse>(`/reviews/${card.id}`, { result });
      setCompletedIds((prev) => [...prev, card.id]);
      goNext();
    } catch (e) {
      if (e instanceof ApiError) setError(e.message);
    }
  };

  const goNext = () => {
    if (currentIndex < cards.length - 1) {
      setCurrentIndex(currentIndex + 1);
      setFlipped(false);
    } else {
      setIsComplete(true);
    }
  };

  const handleRestart = () => {
    // 처음부터 다시: 읽기 전용 재표시
    setCurrentIndex(0);
    setFlipped(false);
    setIsReadOnly(true);
    setIsComplete(false);
  };

  const handleAdditionalReview = () => {
    // 추가 복습: exclude에 완료된 ID 넣고 재호출
    loadCards(activeTab, completedIds);
  };

  const handleTabChange = (tab: string) => {
    setActiveTab(tab);
    setCompletedIds([]);
    setLoading(true);
  };

  const currentCard = cards[currentIndex];

  return (
    <div className="space-y-5">
      <h1 className="text-lg font-semibold text-ink tracking-tight">복습</h1>

      {/* 탭 */}
      <div className="bg-soft rounded-[14px] border border-hairline p-1 flex gap-1.5">
        {TABS.map((tab) => (
          <button
            key={tab.value}
            onClick={() => handleTabChange(tab.value)}
            className={`flex-1 py-2 px-3 rounded-[10px] text-sm font-medium transition-all ${
              activeTab === tab.value
                ? "bg-raised text-ink font-semibold shadow-sm"
                : "bg-transparent text-ink-muted"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* 에러 */}
      {error && (
        <div className="bg-red-50 text-red-700 rounded-xl px-4 py-3 text-sm">
          {error}
        </div>
      )}

      {/* 로딩 */}
      {loading && (
        <div className="text-center py-12 text-ink-muted text-sm">
          카드를 불러오는 중...
        </div>
      )}

      {/* 완료 화면 */}
      {!loading && isComplete && (
        <div className="text-center py-12 space-y-4">
          <p className="text-ink-soft text-sm">
            {cards.length === 0
              ? "복습할 카드가 없습니다"
              : "모든 카드를 복습했습니다! 🎉"}
          </p>
          {cards.length > 0 && (
            <div className="flex gap-3 justify-center">
              <button
                onClick={handleRestart}
                className="px-4 py-2.5 bg-raised text-ink rounded-[14px] border border-hairline shadow-sm text-sm font-medium"
              >
                처음부터 다시
              </button>
              <button
                onClick={handleAdditionalReview}
                className="px-4 py-2.5 bg-primary text-white rounded-[14px] shadow-sm text-sm font-medium"
              >
                추가 복습
              </button>
            </div>
          )}
        </div>
      )}

      {/* 카드 영역 */}
      {!loading && !isComplete && currentCard && (
        <div className="space-y-4">
          {/* 진행 표시 */}
          <div className="flex justify-between items-center text-xs text-ink-muted">
            <span>
              {currentIndex + 1} / {cards.length}
            </span>
            {isReadOnly && (
              <span className="text-primary font-medium">읽기 전용</span>
            )}
          </div>

          {/* 플립 카드 */}
          <FlipCard
            flipped={flipped}
            onFlip={() => setFlipped(!flipped)}
            front={<CardFront card={currentCard} />}
            back={<CardBack card={currentCard} />}
          />

          {/* 결과 버튼 */}
          {flipped && !isReadOnly && (
            <div className="flex gap-3">
              <button
                onClick={() => handleResult("HARD")}
                className="flex-1 py-3 bg-raised text-red-600 rounded-[14px] border border-hairline text-sm font-semibold"
              >
                모름
              </button>
              <button
                onClick={() => handleResult("MEDIUM")}
                className="flex-1 py-3 bg-raised text-amber-600 rounded-[14px] border border-hairline text-sm font-semibold"
              >
                애매
              </button>
              <button
                onClick={() => handleResult("EASY")}
                className="flex-1 py-3 bg-raised text-green-600 rounded-[14px] border border-hairline text-sm font-semibold"
              >
                기억남
              </button>
            </div>
          )}

          {/* 읽기 전용 모드: 다음 버튼 */}
          {flipped && isReadOnly && (
            <button
              onClick={goNext}
              className="w-full py-3 bg-primary text-white rounded-[14px] text-sm font-semibold"
            >
              다음
            </button>
          )}

          {/* 플립 안내 */}
          {!flipped && (
            <p className="text-center text-xs text-ink-muted">
              카드를 탭하여 뒤집기
            </p>
          )}
        </div>
      )}
    </div>
  );
}

function CardFront({ card }: { card: ReviewCard }) {
  const { front, itemType, direction } = card;

  if (itemType === "WORD" && direction === "RECOGNITION") {
    return (
      <div className="text-center">
        <p className="text-xs text-ink-muted mb-2">단어 → 뜻</p>
        <p className="text-2xl font-semibold text-ink">{front.word}</p>
      </div>
    );
  }

  if (itemType === "WORD" && direction === "RECALL") {
    return (
      <div className="text-center">
        <p className="text-xs text-ink-muted mb-2">뜻 → 단어</p>
        <p className="text-xl text-ink">{front.meaning}</p>
      </div>
    );
  }

  if (itemType === "PATTERN" && direction === "RECOGNITION") {
    return (
      <div className="text-center">
        <p className="text-xs text-ink-muted mb-2">패턴 → 설명</p>
        <p className="text-xl font-semibold text-ink">{front.template}</p>
      </div>
    );
  }

  if (itemType === "PATTERN" && direction === "RECALL") {
    return (
      <div className="text-center">
        <p className="text-xs text-ink-muted mb-2">설명 → 패턴</p>
        <p className="text-lg text-ink">{front.description}</p>
      </div>
    );
  }

  if (itemType === "SENTENCE") {
    return (
      <div className="text-center space-y-3">
        <p className="text-xs text-ink-muted">문장 → 해석</p>
        <p className="text-lg font-semibold text-ink">{front.englishSentence}</p>
        {front.situation && (
          <div className="bg-sage-bg text-sage rounded-[18px] px-3.5 py-2 border border-sage/10 text-sm">
            💭 {front.situation}
          </div>
        )}
      </div>
    );
  }

  return null;
}

function CardBack({ card }: { card: ReviewCard }) {
  const { back, itemType, direction } = card;

  if (itemType === "WORD" && direction === "RECOGNITION") {
    return (
      <div className="text-center space-y-3 w-full">
        <p className="text-xl font-semibold text-ink">{back.meaning}</p>
        {back.partOfSpeech && (
          <p className="text-xs text-ink-muted">{back.partOfSpeech}</p>
        )}
        {back.examples && back.examples.length > 0 && (
          <div className="text-left space-y-2 mt-3 w-full">
            <p className="text-xs text-ink-muted font-medium">예문</p>
            {back.examples.map((ex, i) => (
              <div key={i} className="text-sm space-y-0.5">
                <p className="text-ink">{ex.englishSentence}</p>
                <p className="text-ink-muted text-xs">{ex.koreanTranslation}</p>
              </div>
            ))}
          </div>
        )}
      </div>
    );
  }

  if (itemType === "WORD" && direction === "RECALL") {
    return (
      <div className="text-center">
        <p className="text-2xl font-semibold text-ink">{back.word}</p>
        {back.partOfSpeech && (
          <p className="text-xs text-ink-muted mt-1">{back.partOfSpeech}</p>
        )}
      </div>
    );
  }

  if (itemType === "PATTERN" && direction === "RECOGNITION") {
    return (
      <div className="text-center space-y-3 w-full">
        <p className="text-lg text-ink">{back.description}</p>
        {back.examples && back.examples.length > 0 && (
          <div className="text-left space-y-2 mt-3 w-full">
            <p className="text-xs text-ink-muted font-medium">교재 예문</p>
            {back.examples.slice(0, 3).map((ex, i) => (
              <div key={i} className="text-sm space-y-0.5">
                <p className="text-ink">{ex.englishSentence}</p>
                <p className="text-ink-muted text-xs">{ex.koreanTranslation}</p>
              </div>
            ))}
          </div>
        )}
      </div>
    );
  }

  if (itemType === "PATTERN" && direction === "RECALL") {
    return (
      <div className="text-center">
        <p className="text-xl font-semibold text-ink">{back.template}</p>
      </div>
    );
  }

  if (itemType === "SENTENCE") {
    return (
      <div className="text-center">
        <p className="text-xl text-ink">{back.koreanTranslation}</p>
      </div>
    );
  }

  return null;
}

export default function ReviewPage() {
  return (
    <AuthGuard>
      <ReviewContent />
    </AuthGuard>
  );
}
