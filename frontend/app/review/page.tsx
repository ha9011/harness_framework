"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { RotateCcw } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Button } from "@/app/components/ui/button";
import { Chip } from "@/app/components/ui/chip";
import { ErrorState, LoadingState } from "@/app/components/ui/screen-state";
import { Surface } from "@/app/components/ui/surface";
import { ApiError } from "@/app/lib/api";
import { getReviewDeck, recordReview } from "@/app/lib/learning-api";
import type { ReviewCard, ReviewResult, ReviewType } from "@/app/lib/learning-api";

const reviewTypes: Array<{ type: ReviewType; label: string }> = [
  { type: "WORD", label: "단어" },
  { type: "PATTERN", label: "패턴" },
  { type: "SENTENCE", label: "예문" },
];

export default function ReviewPage() {
  const [activeType, setActiveType] = useState<ReviewType>("WORD");
  const [deckByType, setDeckByType] = useState<Record<ReviewType, ReviewCard[]>>({
    WORD: [],
    PATTERN: [],
    SENTENCE: [],
  });
  const [indexByType, setIndexByType] = useState<Record<ReviewType, number>>({ WORD: 0, PATTERN: 0, SENTENCE: 0 });
  const [flippedByType, setFlippedByType] = useState<Record<ReviewType, boolean>>({
    WORD: false,
    PATTERN: false,
    SENTENCE: false,
  });
  const [readOnlyByType, setReadOnlyByType] = useState<Record<ReviewType, boolean>>({
    WORD: false,
    PATTERN: false,
    SENTENCE: false,
  });
  const [excludedByType, setExcludedByType] = useState<Record<ReviewType, number[]>>({
    WORD: [],
    PATTERN: [],
    SENTENCE: [],
  });
  const [reviewedByType, setReviewedByType] = useState<Record<ReviewType, number[]>>({
    WORD: [],
    PATTERN: [],
    SENTENCE: [],
  });
  const [message, setMessage] = useState<string | null>(null);
  const excluded = excludedByType[activeType];
  const deckQuery = useQuery({
    queryKey: ["reviews", activeType, excluded.join(",")],
    queryFn: () => getReviewDeck(activeType, excluded),
  });
  const recordMutation = useMutation({
    mutationFn: ({ id, result }: { id: number; result: ReviewResult }) => recordReview(id, result),
    onSuccess: (_data, variables) => {
      setReviewedByType((prev) => ({
        ...prev,
        [activeType]: Array.from(new Set([...prev[activeType], variables.id])),
      }));
      setFlippedByType((prev) => ({ ...prev, [activeType]: false }));
      setIndexByType((prev) => ({ ...prev, [activeType]: prev[activeType] + 1 }));
      setMessage("복습 결과를 기록했습니다");
    },
    onError: (error) => setMessage(error instanceof ApiError ? error.message : "복습 결과 저장에 실패했습니다"),
  });

  useEffect(() => {
    if (deckQuery.data) {
      setDeckByType((prev) => ({ ...prev, [activeType]: deckQuery.data }));
      setIndexByType((prev) => ({ ...prev, [activeType]: 0 }));
      setFlippedByType((prev) => ({ ...prev, [activeType]: false }));
      setReadOnlyByType((prev) => ({ ...prev, [activeType]: false }));
    }
  }, [activeType, deckQuery.data]);

  const deck = deckByType[activeType];
  const index = indexByType[activeType];
  const flipped = flippedByType[activeType];
  const readOnly = readOnlyByType[activeType];
  const currentCard = deck[index];
  const completed = deck.length > 0 && index >= deck.length;
  const progress = useMemo(() => {
    if (deck.length === 0) return "0 / 0";
    return `${Math.min(index + 1, deck.length)} / ${deck.length}`;
  }, [deck.length, index]);

  function rate(result: ReviewResult) {
    if (!currentCard || readOnly) return;
    recordMutation.mutate({ id: currentCard.reviewItemId, result });
  }

  function replay() {
    setReadOnlyByType((prev) => ({ ...prev, [activeType]: true }));
    setIndexByType((prev) => ({ ...prev, [activeType]: 0 }));
    setFlippedByType((prev) => ({ ...prev, [activeType]: false }));
    setMessage("처음부터 다시 보는 중입니다. 결과는 기록하지 않습니다.");
  }

  function nextReadOnly() {
    setFlippedByType((prev) => ({ ...prev, [activeType]: false }));
    setIndexByType((prev) => ({ ...prev, [activeType]: prev[activeType] + 1 }));
  }

  function loadMore() {
    const reviewed = reviewedByType[activeType];
    setExcludedByType((prev) => ({ ...prev, [activeType]: reviewed }));
    setMessage("추가 복습을 불러옵니다");
  }

  return (
    <div className="space-y-5 px-4 py-5">
      <header className="space-y-2">
        <Chip variant="warm">Review</Chip>
        <div>
          <h1 className="text-2xl font-semibold text-cafe-ink">오늘의 복습</h1>
          <p className="mt-1 text-sm leading-relaxed text-cafe-ink-soft">
            카드 앞면을 눌러 뒤집고 Easy/Medium/Hard로 기억 정도를 기록합니다.
          </p>
        </div>
      </header>

      <div className="grid grid-cols-3 gap-2" role="tablist" aria-label="복습 타입">
        {reviewTypes.map((item) => (
          <button
            key={item.type}
            type="button"
            role="tab"
            aria-selected={activeType === item.type}
            className={`rounded-[14px] px-3 py-3 text-sm font-semibold ${
              activeType === item.type ? "bg-cafe-latte-soft text-cafe-latte-deep" : "bg-cafe-soft text-cafe-ink-soft"
            }`}
            onClick={() => {
              setActiveType(item.type);
              setMessage(null);
            }}
          >
            {item.label}
          </button>
        ))}
      </div>

      {deckQuery.isPending ? <LoadingState title="복습 카드를 불러오는 중입니다" /> : null}
      {deckQuery.isError ? (
        <ErrorState description="복습 카드를 가져오지 못했습니다." onRetry={() => void deckQuery.refetch()} />
      ) : null}

      {!deckQuery.isPending && !deckQuery.isError && deck.length === 0 ? (
        <Surface className="space-y-3 p-5 text-center">
          <h2 className="text-base font-semibold text-cafe-ink">복습할 카드가 없습니다</h2>
          <p className="text-sm text-cafe-ink-soft">새 단어와 패턴을 등록하거나 내일 다시 확인하세요.</p>
        </Surface>
      ) : null}

      {currentCard ? (
        <div className="space-y-4">
          <div className="flex items-center justify-between text-sm text-cafe-ink-soft">
            <span>{readOnly ? "읽기 전용 다시보기" : "진행률"}</span>
            <span>{progress}</span>
          </div>
          <button
            type="button"
            aria-label="복습 카드 뒤집기"
            className="w-full text-left"
            onClick={() => setFlippedByType((prev) => ({ ...prev, [activeType]: !prev[activeType] }))}
          >
            <Surface className="min-h-64 space-y-4 p-5">
              <div className="flex flex-wrap gap-2">
                <Chip variant="sage">{labelForType(currentCard.itemType)}</Chip>
                <Chip variant="outline">{currentCard.direction === "RECALL" ? "회상" : "인식"}</Chip>
              </div>
              {!flipped ? (
                <div className="space-y-3">
                  {currentCard.front.situation ? (
                    <p className="rounded-[14px] bg-cafe-soft px-3 py-2 text-sm text-cafe-ink-soft">
                      상황: {currentCard.front.situation}
                    </p>
                  ) : null}
                  <p className="text-2xl font-semibold leading-relaxed text-cafe-ink">{currentCard.front.text}</p>
                  <p className="text-sm text-cafe-ink-muted">탭해서 정답 보기</p>
                </div>
              ) : (
                <BackContent card={currentCard} />
              )}
            </Surface>
          </button>

          {readOnly ? (
            <Button type="button" className="w-full" onClick={nextReadOnly}>
              다음 카드
            </Button>
          ) : (
            <div className="grid grid-cols-3 gap-2">
              <Button type="button" variant="sage" disabled={recordMutation.isPending} onClick={() => rate("EASY")}>Easy</Button>
              <Button type="button" variant="secondary" disabled={recordMutation.isPending} onClick={() => rate("MEDIUM")}>Medium</Button>
              <Button type="button" variant="danger" disabled={recordMutation.isPending} onClick={() => rate("HARD")}>Hard</Button>
            </div>
          )}
        </div>
      ) : null}

      {completed ? (
        <Surface className="space-y-3 p-5 text-center">
          <h2 className="text-base font-semibold text-cafe-ink">이 덱을 완료했습니다</h2>
          <div className="grid grid-cols-2 gap-2">
            <Button type="button" variant="secondary" onClick={replay}>
              <RotateCcw className="h-4 w-4" aria-hidden="true" />
              처음부터 다시
            </Button>
            <Button type="button" onClick={loadMore}>추가 복습</Button>
          </div>
        </Surface>
      ) : null}

      {message ? <p className="text-sm font-medium text-cafe-ink-soft" role="status">{message}</p> : null}
    </div>
  );
}

function BackContent({ card }: { card: ReviewCard }) {
  const back = card.back;
  if (card.itemType === "WORD" && "meaning" in back) {
    return (
      <div className="space-y-3">
        <p className="text-xl font-semibold text-cafe-ink">{String(back.meaning)}</p>
        {renderText("발음", back.pronunciation)}
        {renderText("팁", back.tip)}
        {Array.isArray(back.examples) ? (
          <div className="space-y-1">
            {(back.examples as string[]).map((example) => <p key={example} className="text-sm text-cafe-ink-soft">{example}</p>)}
          </div>
        ) : null}
      </div>
    );
  }
  if (card.itemType === "WORD" && "word" in back) {
    return (
      <div className="space-y-3">
        <p className="text-xl font-semibold text-cafe-ink">{String(back.word)}</p>
        {renderText("발음", back.pronunciation)}
        {renderText("팁", back.tip)}
      </div>
    );
  }
  if (card.itemType === "PATTERN") {
    const examples = Array.isArray(back.examples) ? (back.examples as Array<{ sentence: string; translation: string }>) : [];
    return (
      <div className="space-y-3">
        <p className="text-xl font-semibold text-cafe-ink">
          {String("description" in back ? back.description : back.template)}
        </p>
        {examples.map((example) => (
          <div key={example.sentence} className="rounded-[14px] bg-cafe-soft p-3">
            <p className="font-semibold text-cafe-ink">{example.sentence}</p>
            <p className="mt-1 text-sm text-cafe-ink-soft">{example.translation}</p>
          </div>
        ))}
      </div>
    );
  }
  return (
    <div className="space-y-3">
      <p className="text-xl font-semibold text-cafe-ink">{String(back.translation ?? "")}</p>
      {renderText("패턴", back.pattern)}
      {Array.isArray(back.words) ? (
        <div className="flex flex-wrap gap-2">
          {(back.words as string[]).map((word) => <Chip key={word} variant="warm">{word}</Chip>)}
        </div>
      ) : null}
    </div>
  );
}

function renderText(label: string, value: unknown) {
  if (!value) return null;
  return (
    <p className="text-sm text-cafe-ink-soft">
      <span className="font-semibold text-cafe-ink">{label}: </span>
      {String(value)}
    </p>
  );
}

function labelForType(type: ReviewType) {
  if (type === "WORD") return "단어";
  if (type === "PATTERN") return "패턴";
  return "예문";
}
