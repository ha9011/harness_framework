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
