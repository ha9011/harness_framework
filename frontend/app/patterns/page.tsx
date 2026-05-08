"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import type { ReactNode } from "react";
import { useState } from "react";
import { Button } from "@/app/components/ui/button";
import { Chip } from "@/app/components/ui/chip";
import { EmptyState, ErrorState, LoadingState } from "@/app/components/ui/screen-state";
import { Surface } from "@/app/components/ui/surface";
import { ApiError } from "@/app/lib/api";
import { createPattern, extractPattern, getPatterns } from "@/app/lib/learning-api";
import type { PatternPayload } from "@/app/lib/learning-api";

const emptyPattern: PatternPayload = {
  template: "",
  description: "",
  examples: [{ sentence: "", translation: "" }],
};

export default function PatternsPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [tab, setTab] = useState<"manual" | "image">("manual");
  const [form, setForm] = useState<PatternPayload>(emptyPattern);
  const [message, setMessage] = useState<string | null>(null);
  const patternsQuery = useQuery({
    queryKey: ["patterns", page],
    queryFn: () => getPatterns(page),
  });
  const createMutation = useMutation({
    mutationFn: createPattern,
    onSuccess: async () => {
      setMessage("패턴을 저장했습니다");
      setForm(emptyPattern);
      await queryClient.invalidateQueries({ queryKey: ["patterns"] });
    },
    onError: (error) => setMessage(error instanceof ApiError ? error.message : "패턴 저장에 실패했습니다"),
  });
  const extractMutation = useMutation({
    mutationFn: extractPattern,
    onSuccess: (data) => {
      setForm({
        template: data.template ?? "",
        description: data.description ?? "",
        examples: data.examples?.length ? data.examples : [{ sentence: "", translation: "" }],
      });
      setMessage(data.template ? "추출 결과를 확인한 뒤 저장하세요" : "추출된 패턴이 없습니다");
    },
    onError: (error) => setMessage(error instanceof ApiError ? error.message : "이미지 추출에 실패했습니다"),
  });

  function savePattern() {
    setMessage(null);
    const examples = form.examples.filter((example) => example.sentence.trim() && example.translation.trim());
    if (!form.template.trim() || !form.description.trim()) {
      setMessage("패턴과 설명을 입력해주세요");
      return;
    }
    createMutation.mutate({ ...form, examples });
  }

  return (
    <div className="space-y-5 px-4 py-5">
      <header className="space-y-2">
        <Chip variant="sage">Patterns</Chip>
        <div>
          <h1 className="text-2xl font-semibold text-cafe-ink">패턴</h1>
          <p className="mt-1 text-sm leading-relaxed text-cafe-ink-soft">
            표현 패턴과 교재 예문을 순서대로 등록합니다.
          </p>
        </div>
      </header>

      <Surface className="space-y-4 p-5">
        <div className="grid grid-cols-2 gap-2" role="tablist" aria-label="패턴 등록 방식">
          <TabButton active={tab === "manual"} onClick={() => setTab("manual")}>직접 입력</TabButton>
          <TabButton active={tab === "image"} onClick={() => setTab("image")}>이미지 추출</TabButton>
        </div>

        {tab === "image" ? (
          <input
            aria-label="패턴 이미지"
            type="file"
            accept="image/*"
            onChange={(event) => {
              const file = event.target.files?.[0];
              if (file) extractMutation.mutate(file);
            }}
          />
        ) : null}

        <TextInput label="패턴" value={form.template} onChange={(template) => setForm((prev) => ({ ...prev, template }))} />
        <TextInput label="설명" value={form.description} onChange={(description) => setForm((prev) => ({ ...prev, description }))} />

        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-semibold text-cafe-ink">예문</h2>
            <Button
              type="button"
              size="sm"
              variant="secondary"
              onClick={() => setForm((prev) => ({ ...prev, examples: [...prev.examples, { sentence: "", translation: "" }] }))}
            >
              추가
            </Button>
          </div>
          {form.examples.map((example, index) => (
            <div key={index} className="space-y-2 rounded-[14px] bg-cafe-soft p-3">
              <input
                aria-label={`예문 ${index + 1}`}
                className="w-full rounded-[12px] bg-cafe-raised px-3 py-2 text-sm"
                placeholder="I'm afraid that we'll be late."
                value={example.sentence}
                onChange={(event) => updateExample(index, "sentence", event.target.value, setForm)}
              />
              <input
                aria-label={`예문 해석 ${index + 1}`}
                className="w-full rounded-[12px] bg-cafe-raised px-3 py-2 text-sm"
                placeholder="유감스럽게도 우리는 늦을 것 같아요."
                value={example.translation}
                onChange={(event) => updateExample(index, "translation", event.target.value, setForm)}
              />
              {form.examples.length > 1 ? (
                <Button type="button" size="sm" variant="ghost" onClick={() => removeExample(index, setForm)}>
                  삭제
                </Button>
              ) : null}
            </div>
          ))}
        </div>

        {message ? <p className="text-sm font-medium text-cafe-ink-soft" role="status">{message}</p> : null}
        <Button type="button" className="w-full" disabled={createMutation.isPending} onClick={savePattern}>
          패턴 저장
        </Button>
      </Surface>

      {patternsQuery.isPending ? <LoadingState title="패턴 목록을 불러오는 중입니다" /> : null}
      {patternsQuery.isError ? (
        <ErrorState description="패턴 목록을 가져오지 못했습니다." onRetry={() => void patternsQuery.refetch()} />
      ) : null}
      {patternsQuery.data && patternsQuery.data.content.length === 0 ? (
        <EmptyState title="등록된 패턴이 없습니다" description="자주 쓰는 문장 패턴과 교재 예문을 등록해보세요." />
      ) : null}
      {patternsQuery.data && patternsQuery.data.content.length > 0 ? (
        <div className="space-y-3">
          {patternsQuery.data.content.map((pattern) => (
            <Surface key={pattern.id} className="space-y-3 p-4">
              <Link href={`/patterns/${pattern.id}`} className="text-lg font-semibold text-cafe-ink underline-offset-4 hover:underline">
                {pattern.template}
              </Link>
              <p className="text-sm text-cafe-ink-soft">{pattern.description}</p>
              <div className="space-y-1">
                {pattern.examples.map((example) => (
                  <p key={`${example.sortOrder}-${example.sentence}`} className="text-xs text-cafe-ink-muted">
                    {example.sortOrder ?? 0}. {example.sentence}
                  </p>
                ))}
              </div>
            </Surface>
          ))}
        </div>
      ) : null}

      {patternsQuery.data && patternsQuery.data.totalPages > 1 ? (
        <div className="flex items-center justify-between">
          <Button type="button" variant="secondary" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>이전</Button>
          <span className="text-sm text-cafe-ink-soft">{page + 1} / {patternsQuery.data.totalPages}</span>
          <Button type="button" variant="secondary" disabled={page + 1 >= patternsQuery.data.totalPages} onClick={() => setPage((value) => value + 1)}>다음</Button>
        </div>
      ) : null}
    </div>
  );
}

function TabButton({ active, children, onClick }: { active: boolean; children: ReactNode; onClick: () => void }) {
  return (
    <button
      type="button"
      role="tab"
      aria-selected={active}
      className={`rounded-[14px] px-3 py-2 text-sm font-semibold ${
        active ? "bg-cafe-latte-soft text-cafe-latte-deep" : "bg-cafe-soft text-cafe-ink-soft"
      }`}
      onClick={onClick}
    >
      {children}
    </button>
  );
}

function TextInput({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  const id = `pattern-${label}`;
  return (
    <div className="space-y-2">
      <label htmlFor={id} className="text-sm font-semibold text-cafe-ink">{label}</label>
      <input
        id={id}
        className="w-full rounded-[12px] border border-[rgba(61,46,34,0.10)] bg-cafe-soft px-3 py-2 text-sm"
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
    </div>
  );
}

function updateExample(
  index: number,
  key: "sentence" | "translation",
  value: string,
  setForm: (updater: (form: PatternPayload) => PatternPayload) => void,
) {
  setForm((form) => ({
    ...form,
    examples: form.examples.map((example, exampleIndex) =>
      exampleIndex === index ? { ...example, [key]: value } : example,
    ),
  }));
}

function removeExample(index: number, setForm: (updater: (form: PatternPayload) => PatternPayload) => void) {
  setForm((form) => ({ ...form, examples: form.examples.filter((_, exampleIndex) => exampleIndex !== index) }));
}
