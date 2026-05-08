"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Star } from "lucide-react";
import Link from "next/link";
import { useMemo, useState } from "react";
import { Button } from "@/app/components/ui/button";
import { Chip } from "@/app/components/ui/chip";
import { EmptyState, ErrorState, LoadingState } from "@/app/components/ui/screen-state";
import { Surface } from "@/app/components/ui/surface";
import { ApiError } from "@/app/lib/api";
import {
  createWord,
  createWordsBulk,
  extractWords,
  getWords,
  toggleWordImportant,
} from "@/app/lib/learning-api";
import type { WordPayload } from "@/app/lib/learning-api";

const partOptions = ["", "noun", "verb", "adjective", "phrase"] as const;

export default function WordsPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [partOfSpeech, setPartOfSpeech] = useState("");
  const [importantOnly, setImportantOnly] = useState(false);
  const [sort, setSort] = useState("createdAt");
  const [tab, setTab] = useState<"single" | "json" | "image">("single");
  const [single, setSingle] = useState<WordPayload>({ word: "", meaning: "", partOfSpeech: "" });
  const [jsonText, setJsonText] = useState('[{"word":"drink coffee","meaning":"커피를 마시다"}]');
  const [extracted, setExtracted] = useState<WordPayload[]>([]);
  const [message, setMessage] = useState<string | null>(null);
  const wordsQuery = useQuery({
    queryKey: ["words", { page, search, partOfSpeech, importantOnly, sort }],
    queryFn: () => getWords({ page, search, partOfSpeech, importantOnly, sort }),
  });

  const invalidateWords = () => queryClient.invalidateQueries({ queryKey: ["words"] });
  const createMutation = useMutation({
    mutationFn: createWord,
    onSuccess: async () => {
      setSingle({ word: "", meaning: "", partOfSpeech: "" });
      setMessage("단어를 저장했습니다");
      await invalidateWords();
    },
    onError: setApiMessage(setMessage, "단어 저장에 실패했습니다"),
  });
  const bulkMutation = useMutation({
    mutationFn: createWordsBulk,
    onSuccess: async (result) => {
      setMessage(`저장 ${result.saved.length}개 · 건너뜀 ${result.skipped.length}개 · 보강 실패 ${result.enrichmentFailed.length}개`);
      await invalidateWords();
    },
    onError: setApiMessage(setMessage, "벌크 저장에 실패했습니다"),
  });
  const extractMutation = useMutation({
    mutationFn: extractWords,
    onSuccess: (items) => {
      setExtracted(items);
      setMessage(items.length > 0 ? "추출 결과를 확인한 뒤 저장하세요" : "추출된 단어가 없습니다");
    },
    onError: setApiMessage(setMessage, "이미지 추출에 실패했습니다"),
  });
  const importantMutation = useMutation({
    mutationFn: toggleWordImportant,
    onSuccess: async () => {
      await invalidateWords();
    },
  });

  const canSaveSingle = single.word.trim().length > 0 && single.meaning.trim().length > 0;
  const parsedJson = useMemo(() => parseWordJson(jsonText), [jsonText]);

  function submitSingle() {
    setMessage(null);
    if (!canSaveSingle) {
      setMessage("단어와 뜻을 입력해주세요");
      return;
    }
    createMutation.mutate(single);
  }

  function submitBulk() {
    setMessage(null);
    if (!parsedJson.ok) {
      setMessage(parsedJson.error);
      return;
    }
    bulkMutation.mutate(parsedJson.items);
  }

  function saveExtracted() {
    setMessage(null);
    if (extracted.length === 0) {
      setMessage("저장할 추출 단어가 없습니다");
      return;
    }
    bulkMutation.mutate(extracted);
  }

  return (
    <div className="space-y-5 px-4 py-5">
      <header className="space-y-2">
        <Chip variant="warm">Words</Chip>
        <div>
          <h1 className="text-2xl font-semibold text-cafe-ink">단어장</h1>
          <p className="mt-1 text-sm leading-relaxed text-cafe-ink-soft">
            단어를 직접 등록하거나 JSON/이미지 추출 결과를 확인 후 저장합니다.
          </p>
        </div>
      </header>

      <Surface className="space-y-4 p-5">
        <div className="grid grid-cols-3 gap-2" role="tablist" aria-label="단어 등록 방식">
          {(["single", "json", "image"] as const).map((value) => (
            <button
              key={value}
              type="button"
              role="tab"
              aria-selected={tab === value}
              className={`rounded-[14px] px-3 py-2 text-sm font-semibold ${
                tab === value ? "bg-cafe-latte-soft text-cafe-latte-deep" : "bg-cafe-soft text-cafe-ink-soft"
              }`}
              onClick={() => {
                setMessage(null);
                setTab(value);
              }}
            >
              {value === "single" ? "단건" : value === "json" ? "JSON" : "이미지"}
            </button>
          ))}
        </div>

        {tab === "single" ? (
          <div className="space-y-3">
            <TextInput label="단어" value={single.word} onChange={(word) => setSingle((prev) => ({ ...prev, word }))} />
            <TextInput label="뜻" value={single.meaning} onChange={(meaning) => setSingle((prev) => ({ ...prev, meaning }))} />
            <TextInput
              label="품사"
              value={single.partOfSpeech ?? ""}
              placeholder="phrase"
              onChange={(value) => setSingle((prev) => ({ ...prev, partOfSpeech: value }))}
            />
            <Button type="button" className="w-full" disabled={createMutation.isPending} onClick={submitSingle}>
              단어 저장
            </Button>
          </div>
        ) : null}

        {tab === "json" ? (
          <div className="space-y-3">
            <label className="text-sm font-semibold text-cafe-ink" htmlFor="word-json">
              JSON 배열
            </label>
            <textarea
              id="word-json"
              className="min-h-32 w-full rounded-[14px] border border-[rgba(61,46,34,0.10)] bg-cafe-soft p-3 text-sm"
              value={jsonText}
              onChange={(event) => setJsonText(event.target.value)}
            />
            <Button type="button" className="w-full" disabled={bulkMutation.isPending} onClick={submitBulk}>
              JSON 저장
            </Button>
          </div>
        ) : null}

        {tab === "image" ? (
          <div className="space-y-4">
            <input
              aria-label="단어 이미지"
              type="file"
              accept="image/*"
              onChange={(event) => {
                const file = event.target.files?.[0];
                if (file) extractMutation.mutate(file);
              }}
            />
            {extracted.length > 0 ? (
              <div className="space-y-2">
                {extracted.map((item, index) => (
                  <div key={index} className="grid grid-cols-2 gap-2">
                    <input
                      aria-label={`추출 단어 ${index + 1}`}
                      className="rounded-[12px] bg-cafe-soft px-3 py-2 text-sm"
                      value={item.word}
                      onChange={(event) => updateExtracted(index, "word", event.target.value, setExtracted)}
                    />
                    <input
                      aria-label={`추출 뜻 ${index + 1}`}
                      className="rounded-[12px] bg-cafe-soft px-3 py-2 text-sm"
                      value={item.meaning}
                      onChange={(event) => updateExtracted(index, "meaning", event.target.value, setExtracted)}
                    />
                  </div>
                ))}
                <Button type="button" className="w-full" disabled={bulkMutation.isPending} onClick={saveExtracted}>
                  추출 결과 저장
                </Button>
              </div>
            ) : null}
          </div>
        ) : null}

        {message ? <p className="text-sm font-medium text-cafe-ink-soft" role="status">{message}</p> : null}
      </Surface>

      <Surface className="space-y-3 p-4">
        <div className="grid grid-cols-2 gap-2">
          <input
            aria-label="단어 검색"
            className="rounded-[12px] bg-cafe-soft px-3 py-2 text-sm"
            placeholder="검색"
            value={search}
            onChange={(event) => {
              setPage(0);
              setSearch(event.target.value);
            }}
          />
          <select
            aria-label="품사 필터"
            className="rounded-[12px] bg-cafe-soft px-3 py-2 text-sm"
            value={partOfSpeech}
            onChange={(event) => {
              setPage(0);
              setPartOfSpeech(event.target.value);
            }}
          >
            {partOptions.map((option) => (
              <option key={option || "all"} value={option}>
                {option || "전체 품사"}
              </option>
            ))}
          </select>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button type="button" variant={importantOnly ? "sage" : "secondary"} onClick={() => setImportantOnly((value) => !value)}>
            중요만 보기
          </Button>
          <Button type="button" variant={sort === "word" ? "sage" : "secondary"} onClick={() => setSort(sort === "word" ? "createdAt" : "word")}>
            {sort === "word" ? "알파벳순" : "최신순"}
          </Button>
        </div>
      </Surface>

      {wordsQuery.isPending ? <LoadingState title="단어 목록을 불러오는 중입니다" /> : null}
      {wordsQuery.isError ? (
        <ErrorState description="단어 목록을 가져오지 못했습니다." onRetry={() => void wordsQuery.refetch()} />
      ) : null}
      {wordsQuery.data && wordsQuery.data.content.length === 0 ? (
        <EmptyState title="등록된 단어가 없습니다" description="단건, JSON, 이미지 탭에서 첫 단어를 저장해보세요." />
      ) : null}
      {wordsQuery.data && wordsQuery.data.content.length > 0 ? (
        <div className="space-y-3">
          {wordsQuery.data.content.map((word) => (
            <Surface key={word.id} className="space-y-3 p-4">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <Link href={`/words/${word.id}`} className="text-lg font-semibold text-cafe-ink underline-offset-4 hover:underline">
                    {word.word}
                  </Link>
                  <p className="mt-1 text-sm text-cafe-ink-soft">{word.meaning}</p>
                </div>
                <Button
                  type="button"
                  size="icon"
                  variant={word.isImportant ? "sage" : "ghost"}
                  aria-label={`${word.word} 중요 토글`}
                  onClick={() => importantMutation.mutate(word.id)}
                >
                  <Star className="h-4 w-4" aria-hidden="true" />
                </Button>
              </div>
              <div className="flex flex-wrap gap-2">
                {word.partOfSpeech ? <Chip>{word.partOfSpeech}</Chip> : null}
                {word.pronunciation ? <Chip variant="outline">{word.pronunciation}</Chip> : null}
              </div>
            </Surface>
          ))}
        </div>
      ) : null}

      {wordsQuery.data && wordsQuery.data.totalPages > 1 ? (
        <div className="flex items-center justify-between">
          <Button type="button" variant="secondary" disabled={page === 0} onClick={() => setPage((value) => value - 1)}>
            이전
          </Button>
          <span className="text-sm text-cafe-ink-soft">{page + 1} / {wordsQuery.data.totalPages}</span>
          <Button type="button" variant="secondary" disabled={page + 1 >= wordsQuery.data.totalPages} onClick={() => setPage((value) => value + 1)}>
            다음
          </Button>
        </div>
      ) : null}
    </div>
  );
}

function TextInput({
  label,
  value,
  placeholder,
  onChange,
}: {
  label: string;
  value: string;
  placeholder?: string;
  onChange: (value: string) => void;
}) {
  const id = `word-field-${label}`;
  return (
    <div className="space-y-2">
      <label htmlFor={id} className="text-sm font-semibold text-cafe-ink">
        {label}
      </label>
      <input
        id={id}
        className="w-full rounded-[12px] border border-[rgba(61,46,34,0.10)] bg-cafe-soft px-3 py-2 text-sm"
        placeholder={placeholder}
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
    </div>
  );
}

function parseWordJson(text: string): { ok: true; items: WordPayload[] } | { ok: false; error: string } {
  try {
    const parsed = JSON.parse(text) as unknown;
    if (!Array.isArray(parsed)) return { ok: false, error: "JSON 배열을 입력해주세요" };
    const items = parsed.map((item) => item as Partial<WordPayload>);
    if (items.some((item) => !item.word || !item.meaning)) {
      return { ok: false, error: "각 항목에는 word와 meaning이 필요합니다" };
    }
    return { ok: true, items: items as WordPayload[] };
  } catch {
    return { ok: false, error: "JSON 형식을 확인해주세요" };
  }
}

function updateExtracted(
  index: number,
  key: "word" | "meaning",
  value: string,
  setExtracted: (updater: (items: WordPayload[]) => WordPayload[]) => void,
) {
  setExtracted((items) => items.map((item, itemIndex) => (itemIndex === index ? { ...item, [key]: value } : item)));
}

function setApiMessage(setMessage: (message: string) => void, fallback: string) {
  return (error: unknown) => setMessage(error instanceof ApiError ? error.message : fallback);
}
