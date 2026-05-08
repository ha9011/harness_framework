"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { useParams } from "next/navigation";
import { useState } from "react";
import { Button } from "@/app/components/ui/button";
import { Chip } from "@/app/components/ui/chip";
import { EmptyState, ErrorState, LoadingState } from "@/app/components/ui/screen-state";
import { Surface } from "@/app/components/ui/surface";
import { ApiError } from "@/app/lib/api";
import { generateForPattern, getPattern } from "@/app/lib/learning-api";
import type { GenerateResponse } from "@/app/lib/learning-api";

const levels = ["유아", "초등", "중등", "고등"] as const;
const counts = [10, 20, 30] as const;

export default function PatternDetailPage() {
  const params = useParams<{ id: string }>();
  const patternId = Number(params.id);
  const [level, setLevel] = useState<(typeof levels)[number]>("중등");
  const [count, setCount] = useState<(typeof counts)[number]>(10);
  const [result, setResult] = useState<GenerateResponse | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const patternQuery = useQuery({
    queryKey: ["patterns", patternId],
    queryFn: () => getPattern(patternId),
    enabled: Number.isFinite(patternId),
  });
  const generateMutation = useMutation({
    mutationFn: () => generateForPattern(patternId, level, count),
    onSuccess: (data) => {
      setResult(data);
      setMessage(`${data.sentences.length}개 예문을 생성했습니다`);
    },
    onError: (error) => setMessage(error instanceof ApiError ? error.message : "예문 생성에 실패했습니다"),
  });

  if (patternQuery.isPending) {
    return <div className="px-4 py-5"><LoadingState title="패턴 상세를 불러오는 중입니다" /></div>;
  }

  if (patternQuery.isError) {
    return (
      <div className="px-4 py-5">
        <ErrorState description="패턴 상세를 가져오지 못했습니다." onRetry={() => void patternQuery.refetch()} />
      </div>
    );
  }

  const pattern = patternQuery.data;

  return (
    <div className="space-y-5 px-4 py-5">
      <header className="space-y-2">
        <Chip variant="sage">Pattern</Chip>
        <div>
          <h1 className="text-2xl font-semibold text-cafe-ink">{pattern.template}</h1>
          <p className="mt-1 text-base text-cafe-ink-soft">{pattern.description}</p>
        </div>
      </header>

      <Surface className="space-y-3 p-5">
        <h2 className="text-base font-semibold text-cafe-ink">교재 예문</h2>
        {pattern.examples.map((example, index) => (
          <article key={`${example.sentence}-${index}`} className="rounded-[14px] bg-cafe-soft p-3">
            <p className="text-xs font-semibold text-cafe-ink-muted">#{example.sortOrder ?? index + 1}</p>
            <p className="mt-1 font-semibold text-cafe-ink">{example.sentence}</p>
            <p className="mt-1 text-sm text-cafe-ink-soft">{example.translation}</p>
          </article>
        ))}
      </Surface>

      <Surface className="space-y-4 p-5">
        <h2 className="text-base font-semibold text-cafe-ink">이 패턴으로 예문 생성</h2>
        <div className="grid grid-cols-2 gap-2">
          <select aria-label="난이도" className="rounded-[12px] bg-cafe-soft px-3 py-2 text-sm" value={level} onChange={(event) => setLevel(event.target.value as typeof level)}>
            {levels.map((item) => <option key={item}>{item}</option>)}
          </select>
          <select aria-label="개수" className="rounded-[12px] bg-cafe-soft px-3 py-2 text-sm" value={count} onChange={(event) => setCount(Number(event.target.value) as typeof count)}>
            {counts.map((item) => <option key={item}>{item}</option>)}
          </select>
        </div>
        <Button type="button" disabled={generateMutation.isPending} onClick={() => generateMutation.mutate()}>
          {generateMutation.isPending ? "생성 중" : "패턴 예문 생성"}
        </Button>
        {message ? <p className="text-sm font-medium text-cafe-ink-soft" role="status">{message}</p> : null}
      </Surface>

      {pattern.generatedSentences.length === 0 && !result ? (
        <EmptyState title="이 패턴으로 생성된 예문이 없습니다" description="난이도와 개수를 선택하면 패턴 기반 예문을 만들 수 있습니다." />
      ) : null}

      {[...(result?.sentences ?? []), ...pattern.generatedSentences].map((sentence) => (
        <Surface key={sentence.id} className="space-y-2 p-4">
          <Chip variant="outline">{sentence.level}</Chip>
          <p className="font-semibold text-cafe-ink">{sentence.sentence}</p>
          <p className="text-sm text-cafe-ink-soft">{sentence.translation}</p>
        </Surface>
      ))}
    </div>
  );
}
