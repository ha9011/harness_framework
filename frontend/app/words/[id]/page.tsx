"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { useParams } from "next/navigation";
import { useState } from "react";
import { Button } from "@/app/components/ui/button";
import { Chip } from "@/app/components/ui/chip";
import { EmptyState, ErrorState, LoadingState } from "@/app/components/ui/screen-state";
import { Surface } from "@/app/components/ui/surface";
import { ApiError } from "@/app/lib/api";
import { generateForWord, getWord } from "@/app/lib/learning-api";
import type { GenerateResponse } from "@/app/lib/learning-api";

const levels = ["유아", "초등", "중등", "고등"] as const;
const counts = [5, 10] as const;

export default function WordDetailPage() {
  const params = useParams<{ id: string }>();
  const wordId = Number(params.id);
  const [level, setLevel] = useState<(typeof levels)[number]>("초등");
  const [count, setCount] = useState<(typeof counts)[number]>(5);
  const [result, setResult] = useState<GenerateResponse | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const wordQuery = useQuery({
    queryKey: ["words", wordId],
    queryFn: () => getWord(wordId),
    enabled: Number.isFinite(wordId),
  });
  const generateMutation = useMutation({
    mutationFn: () => generateForWord(wordId, level, count),
    onSuccess: (data) => {
      setResult(data);
      setMessage(`${data.sentences.length}개 예문을 생성했습니다`);
    },
    onError: (error) => {
      setMessage(error instanceof ApiError ? error.message : "예문 생성에 실패했습니다");
    },
  });

  if (wordQuery.isPending) {
    return <div className="px-4 py-5"><LoadingState title="단어 상세를 불러오는 중입니다" /></div>;
  }

  if (wordQuery.isError) {
    return (
      <div className="px-4 py-5">
        <ErrorState description="단어 상세를 가져오지 못했습니다." onRetry={() => void wordQuery.refetch()} />
      </div>
    );
  }

  const word = wordQuery.data;

  return (
    <div className="space-y-5 px-4 py-5">
      <header className="space-y-2">
        <Chip variant={word.isImportant ? "sage" : "warm"}>{word.isImportant ? "중요 단어" : "Word"}</Chip>
        <div>
          <h1 className="text-2xl font-semibold text-cafe-ink">{word.word}</h1>
          <p className="mt-1 text-base text-cafe-ink-soft">{word.meaning}</p>
        </div>
      </header>

      <Surface className="space-y-3 p-5">
        <Info label="품사" value={word.partOfSpeech} />
        <Info label="발음" value={word.pronunciation} />
        <Info label="유의어" value={word.synonyms} />
        <Info label="팁" value={word.tip} />
      </Surface>

      <Surface className="space-y-4 p-5">
        <h2 className="text-base font-semibold text-cafe-ink">이 단어로 예문 추가</h2>
        <div className="grid grid-cols-2 gap-2">
          <select aria-label="난이도" className="rounded-[12px] bg-cafe-soft px-3 py-2 text-sm" value={level} onChange={(event) => setLevel(event.target.value as typeof level)}>
            {levels.map((item) => <option key={item}>{item}</option>)}
          </select>
          <select aria-label="개수" className="rounded-[12px] bg-cafe-soft px-3 py-2 text-sm" value={count} onChange={(event) => setCount(Number(event.target.value) as typeof count)}>
            {counts.map((item) => <option key={item}>{item}</option>)}
          </select>
        </div>
        <Button type="button" disabled={generateMutation.isPending} onClick={() => generateMutation.mutate()}>
          {generateMutation.isPending ? "생성 중" : "예문 생성"}
        </Button>
        {message ? <p className="text-sm font-medium text-cafe-ink-soft" role="status">{message}</p> : null}
      </Surface>

      {word.generatedSentences.length === 0 && !result ? (
        <EmptyState title="아직 생성된 예문이 없습니다" description="난이도와 개수를 선택해 이 단어가 포함된 예문을 만들어보세요." />
      ) : null}

      {[...(result?.sentences ?? []), ...word.generatedSentences].map((sentence) => (
        <Surface key={sentence.id} className="space-y-2 p-4">
          <Chip variant="outline">{sentence.level}</Chip>
          <p className="font-semibold text-cafe-ink">{sentence.sentence}</p>
          <p className="text-sm text-cafe-ink-soft">{sentence.translation}</p>
        </Surface>
      ))}
    </div>
  );
}

function Info({ label, value }: { label: string; value?: string | null }) {
  return (
    <div>
      <p className="text-xs font-semibold text-cafe-ink-muted">{label}</p>
      <p className="mt-1 text-sm text-cafe-ink">{value || "없음"}</p>
    </div>
  );
}
