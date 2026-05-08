import { apiFetch } from "@/app/lib/api";

export type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
};

export type StudyRecordItem = {
  type: "WORD" | "PATTERN";
  id: number;
  name: string;
};

export type StudyRecord = {
  id: number;
  studyDate: string;
  dayNumber: number;
  items: StudyRecordItem[];
};

export type Dashboard = {
  wordCount: number;
  patternCount: number;
  sentenceCount: number;
  streak: number;
  todayReviewRemaining: {
    word: number;
    pattern: number;
    sentence: number;
  };
  recentStudyRecords: StudyRecord[];
};

export type UserSettings = {
  dailyReviewCount: number;
};

export type GeneratedSentence = {
  id: number;
  sentence: string;
  translation: string;
  situations: string[];
  level: string;
  pattern?: {
    id: number;
    template: string;
  } | null;
  words: Array<{
    id: number;
    word: string;
  }>;
};

export type GenerateResponse = {
  generationId: number;
  sentences: GeneratedSentence[];
};

export type GeneratedSentenceSummary = {
  id: number;
  sentence: string;
  translation: string;
  level: string;
  createdAt: string;
};

export type Word = {
  id: number;
  word: string;
  meaning: string;
  partOfSpeech?: string | null;
  pronunciation?: string | null;
  synonyms?: string | null;
  tip?: string | null;
  isImportant: boolean;
  createdAt?: string;
  updatedAt?: string;
};

export type WordDetail = Word & {
  generatedSentences: GeneratedSentenceSummary[];
};

export type WordPayload = {
  word: string;
  meaning: string;
  partOfSpeech?: string | null;
  pronunciation?: string | null;
  synonyms?: string | null;
  tip?: string | null;
};

export type WordBulkResult = {
  saved: Word[];
  skipped: Array<{ word: string; reason: string }>;
  enrichmentFailed: Array<{ id: number; word: string; meaning: string; reason: string }>;
};

export type PatternExample = {
  id?: number;
  sortOrder?: number;
  sentence: string;
  translation: string;
};

export type Pattern = {
  id: number;
  template: string;
  description: string;
  examples: PatternExample[];
  createdAt?: string;
  updatedAt?: string;
};

export type PatternDetail = Pattern & {
  generatedSentences: GeneratedSentenceSummary[];
};

export type PatternPayload = {
  template: string;
  description: string;
  examples: Array<{
    sentence: string;
    translation: string;
  }>;
};

export type ReviewType = "WORD" | "PATTERN" | "SENTENCE";
export type ReviewResult = "EASY" | "MEDIUM" | "HARD";

export type ReviewCard = {
  reviewItemId: number;
  itemType: ReviewType;
  direction: "RECOGNITION" | "RECALL";
  front: {
    text: string;
    situation?: string | null;
  };
  back: Record<string, unknown>;
};

export function getDashboard() {
  return apiFetch<Dashboard>("/api/dashboard");
}

export function getStudyRecords(page: number, size = 10) {
  return apiFetch<PageResponse<StudyRecord>>(`/api/study-records?page=${page}&size=${size}`);
}

export function getSettings() {
  return apiFetch<UserSettings>("/api/settings");
}

export function updateDailyReviewCount(value: number) {
  return apiFetch<UserSettings>("/api/settings/daily_review_count", {
    method: "PUT",
    json: { value: String(value) },
  });
}

export function getWords(params: {
  page: number;
  size?: number;
  search?: string;
  partOfSpeech?: string;
  importantOnly?: boolean;
  sort?: string;
}) {
  const query = new URLSearchParams({
    page: String(params.page),
    size: String(params.size ?? 10),
    importantOnly: String(params.importantOnly ?? false),
    sort: params.sort ?? "createdAt",
  });

  if (params.search) query.set("search", params.search);
  if (params.partOfSpeech) query.set("partOfSpeech", params.partOfSpeech);

  return apiFetch<PageResponse<Word>>(`/api/words?${query.toString()}`);
}

export function getWord(id: number) {
  return apiFetch<WordDetail>(`/api/words/${id}`);
}

export function createWord(payload: WordPayload) {
  return apiFetch<Word>("/api/words", { method: "POST", json: cleanPayload(payload) });
}

export function createWordsBulk(payload: WordPayload[]) {
  return apiFetch<WordBulkResult>("/api/words/bulk", { method: "POST", json: payload.map(cleanPayload) });
}

export function extractWords(image: File) {
  const formData = new FormData();
  formData.set("image", image);

  return apiFetch<WordPayload[]>("/api/words/extract", {
    method: "POST",
    body: formData,
  });
}

export function toggleWordImportant(id: number) {
  return apiFetch<Word>(`/api/words/${id}/important`, { method: "PATCH" });
}

export function getPatterns(page: number, size = 10) {
  return apiFetch<PageResponse<Pattern>>(`/api/patterns?page=${page}&size=${size}`);
}

export function getPattern(id: number) {
  return apiFetch<PatternDetail>(`/api/patterns/${id}`);
}

export function createPattern(payload: PatternPayload) {
  return apiFetch<Pattern>("/api/patterns", { method: "POST", json: payload });
}

export function extractPattern(image: File) {
  const formData = new FormData();
  formData.set("image", image);

  return apiFetch<PatternPayload>("/api/patterns/extract", {
    method: "POST",
    body: formData,
  });
}

export function generateSentences(level: string, count: number) {
  return apiFetch<GenerateResponse>("/api/generate", {
    method: "POST",
    json: { level, count },
  });
}

export function generateForWord(wordId: number, level: string, count: number) {
  return apiFetch<GenerateResponse>("/api/generate/word", {
    method: "POST",
    json: { wordId, level, count },
  });
}

export function generateForPattern(patternId: number, level: string, count: number) {
  return apiFetch<GenerateResponse>("/api/generate/pattern", {
    method: "POST",
    json: { patternId, level, count },
  });
}

export function getReviewDeck(type: ReviewType, excludedIds: number[] = []) {
  const query = new URLSearchParams({ type });
  if (excludedIds.length > 0) {
    query.set("exclude", excludedIds.join(","));
  }

  return apiFetch<ReviewCard[]>(`/api/reviews/today?${query.toString()}`);
}

export function recordReview(reviewItemId: number, result: ReviewResult) {
  return apiFetch<{ nextReviewDate: string; intervalDays: number }>(`/api/reviews/${reviewItemId}`, {
    method: "POST",
    json: { result },
  });
}

function cleanPayload<T extends Record<string, unknown>>(payload: T) {
  return Object.fromEntries(
    Object.entries(payload).map(([key, value]) => [key, typeof value === "string" && value.trim() === "" ? null : value]),
  ) as T;
}
