export interface WordResponse {
  id: number;
  word: string;
  meaning: string;
  partOfSpeech: string | null;
  pronunciation: string | null;
  synonyms: string | null;
  tip: string | null;
  important: boolean;
  createdAt: string;
}

export interface WordListResponse {
  id: number;
  word: string;
  meaning: string;
  partOfSpeech: string | null;
  important: boolean;
  createdAt: string;
}

export interface WordDetailResponse extends WordResponse {
  examples: string[];
}

export interface BulkCreateResponse {
  saved: number;
  skipped: number;
  enrichmentFailed: number;
  words: WordResponse[];
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface PatternResponse {
  id: number;
  template: string;
  description: string | null;
  createdAt: string;
}

export interface PatternListResponse {
  id: number;
  template: string;
  description: string | null;
  exampleCount: number;
  createdAt: string;
}

export interface PatternDetailResponse {
  id: number;
  template: string;
  description: string | null;
  createdAt: string;
  examples: PatternExampleResponse[];
}

export interface PatternExampleResponse {
  sentence: string;
  translation: string;
  orderIndex: number;
}

export interface PatternExtractResponse {
  template: string;
  description: string;
  examples: { sentence: string; translation: string }[];
}

export interface WordExtractResponse {
  words: { word: string; meaning: string }[];
}

export interface GenerateRequest {
  level: string;
  count: number;
  wordId?: number;
  patternId?: number;
}

export interface SentenceResponse {
  id: number;
  englishSentence: string;
  koreanTranslation: string;
  level: string;
  situations: string[];
}

export interface GenerateResponse {
  generationId: number | null;
  sentences: SentenceResponse[];
}

export interface GenerationHistoryResponse {
  id: number;
  level: string;
  requestedCount: number;
  actualCount: number;
  wordId: number | null;
  patternId: number | null;
  createdAt: string;
}

export interface ReviewCardFront {
  word: string | null;
  meaning: string | null;
  template: string | null;
  description: string | null;
  englishSentence: string | null;
  situation: string | null;
}

export interface ReviewCardExample {
  englishSentence: string;
  koreanTranslation: string;
}

export interface ReviewCardBack {
  word: string | null;
  meaning: string | null;
  partOfSpeech: string | null;
  template: string | null;
  description: string | null;
  koreanTranslation: string | null;
  examples: ReviewCardExample[] | null;
}

export interface ReviewCard {
  id: number;
  itemType: string;
  direction: string;
  front: ReviewCardFront;
  back: ReviewCardBack;
}

export interface ReviewResultResponse {
  id: number;
  result: string;
  intervalDays: number;
  nextReviewDate: string;
  easeFactor: number;
  reviewCount: number;
}
