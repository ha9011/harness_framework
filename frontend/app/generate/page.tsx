"use client";

import { useMutation } from "@tanstack/react-query";
import { Sparkles } from "lucide-react";
import type { ReactNode } from "react";
import { useState } from "react";
import { Button } from "@/app/components/ui/button";
import { Chip } from "@/app/components/ui/chip";
import { EmptyState } from "@/app/components/ui/screen-state";
import { Surface } from "@/app/components/ui/surface";
import { ApiError } from "@/app/lib/api";
import { generateSentences } from "@/app/lib/learning-api";
import type { GenerateResponse, GeneratedSentence } from "@/app/lib/learning-api";

const levels = ["유아", "초등", "중등", "고등"] as const;
const counts = [10, 20, 30] as const;

export default function GeneratePage() {
  const [level, setLevel] = useState<(typeof levels)[number]>("중등");
  const [count, setCount] = useState<(typeof counts)[number]>(10);
  const [result, setResult] = useState<GenerateResponse | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const generateMutation = useMutation({
    mutationFn: () => generateSentences(level, count),
    onMutate: () => {
      setMessage(null);
      setResult(null);
    },
    onSuccess: (data) => {
      setResult(data);
      setMessage(`${data.sentences.length}개 예문을 저장했습니다`);
    },
    onError: (error) => {
      if (error instanceof ApiError) {
        const code =
          error.data && typeof error.data === "object" && "error" in error.data
            ? String((error.data as { error?: unknown }).error)
            : "";
        setMessage(errorMessage(code, error.message));
      } else {
        setMessage("예문 생성에 실패했습니다");
      }
    },
  });

  return (
    <div className="space-y-5 px-4 py-5">
      <header className="space-y-2">
        <Chip variant="warm">Generate</Chip>
        <div>
          <h1 className="text-2xl font-semibold text-cafe-ink">예문 생성</h1>
          <p className="mt-1 text-sm leading-relaxed text-cafe-ink-soft">
            등록된 단어와 패턴을 조합해 상황이 있는 영어 예문을 만듭니다.
          </p>
        </div>
      </header>

      <Surface className="space-y-5 p-5">
        <div className="space-y-2">
          <h2 className="text-sm font-semibold text-cafe-ink">난이도</h2>
          <div className="grid grid-cols-4 gap-2">
            {levels.map((item) => (
              <Option key={item} active={level === item} onClick={() => setLevel(item)}>
                {item}
              </Option>
            ))}
          </div>
        </div>
        <div className="space-y-2">
          <h2 className="text-sm font-semibold text-cafe-ink">개수</h2>
          <div className="grid grid-cols-3 gap-2">
            {counts.map((item) => (
              <Option key={item} active={count === item} onClick={() => setCount(item)}>
                {item}
              </Option>
            ))}
          </div>
        </div>
        <Button
          type="button"
          size="lg"
          className="w-full"
          disabled={generateMutation.isPending}
          onClick={() => generateMutation.mutate()}
        >
          <Sparkles className="h-4 w-4" aria-hidden="true" />
          {generateMutation.isPending ? "Gemini가 문장을 내리는 중" : "예문 생성"}
        </Button>
        {generateMutation.isPending ? (
          <p className="rounded-[14px] bg-cafe-soft px-3 py-2 text-sm font-medium text-cafe-ink-soft">
            따뜻한 커피를 내리듯 문장을 준비하고 있습니다.
          </p>
        ) : null}
        {message ? <p className="text-sm font-medium text-cafe-ink-soft" role="status">{message}</p> : null}
      </Surface>

      {!result && !generateMutation.isPending ? (
        <EmptyState title="아직 생성 결과가 없습니다" description="난이도와 개수를 선택한 뒤 예문을 생성하세요." />
      ) : null}

      {result ? (
        <div className="space-y-3">
          {result.sentences.map((sentence) => (
            <SentenceCard key={sentence.id} sentence={sentence} />
          ))}
        </div>
      ) : null}
    </div>
  );
}

function Option({ active, children, onClick }: { active: boolean; children: ReactNode; onClick: () => void }) {
  return (
    <button
      type="button"
      aria-pressed={active}
      className={`rounded-[14px] px-3 py-3 text-sm font-semibold ${
        active ? "bg-cafe-latte-soft text-cafe-latte-deep" : "bg-cafe-soft text-cafe-ink-soft"
      }`}
      onClick={onClick}
    >
      {children}
    </button>
  );
}

function SentenceCard({ sentence }: { sentence: GeneratedSentence }) {
  return (
    <Surface className="space-y-3 p-4">
      <div className="flex flex-wrap gap-2">
        <Chip variant="outline">{sentence.level}</Chip>
        {sentence.pattern ? <Chip variant="sage">{sentence.pattern.template}</Chip> : null}
        {sentence.words.map((word) => (
          <Chip key={word.id} variant="warm">{word.word}</Chip>
        ))}
      </div>
      <p className="text-lg font-semibold text-cafe-ink">{sentence.sentence}</p>
      {sentence.situations[0] ? (
        <p className="rounded-[14px] bg-cafe-soft px-3 py-2 text-sm text-cafe-ink-soft">
          상황: {sentence.situations[0]}
        </p>
      ) : null}
      <details className="rounded-[14px] bg-cafe-soft px-3 py-2 text-sm text-cafe-ink-soft">
        <summary className="cursor-pointer font-semibold text-cafe-ink">한국어 해석 보기</summary>
        <p className="mt-2">{sentence.translation}</p>
      </details>
    </Surface>
  );
}

function errorMessage(code: string, fallback: string) {
  if (code === "NO_WORDS") return "예문 생성에 사용할 단어가 없습니다. 먼저 단어를 등록하세요.";
  if (code === "NO_PATTERNS") return "예문 생성에 사용할 패턴이 없습니다. 먼저 패턴을 등록하세요.";
  if (code === "AI_SERVICE_ERROR") return "AI 예문 생성에 실패했습니다. 잠시 후 다시 시도하세요.";
  return fallback;
}
