# API 설계서: 영어 패턴 학습기

## 기본 정보
- Base URL: `/api`
- 응답 형식: JSON
- 인증: JWT (HttpOnly Cookie). `/api/auth/signup`, `/api/auth/login` 외 모든 API는 인증 필수
- 삭제: 모두 soft delete
- 페이지네이션: 목록 API는 `page` (0부터), `size` (기본 20) 파라미터 지원

### 페이지네이션 응답 구조
```json
{
  "content": [...],
  "totalElements": 150,
  "totalPages": 8,
  "page": 0,
  "size": 20
}
```

### 에러 응답 구조
```json
{
  "error": "ERROR_CODE",
  "message": "사용자에게 표시할 메시지"
}
```

| 상황 | HTTP 상태 | error 코드 |
|------|----------|-----------|
| 이미지 형식 오류 | 400 | INVALID_IMAGE_FORMAT |
| 중복 등록 | 409 | DUPLICATE |
| 리소스 없음 | 404 | NOT_FOUND |
| Gemini API 오류 | 502 | AI_SERVICE_ERROR |
| 빈 배열 요청 | 400 | EMPTY_REQUEST |
| 단어 없음 (예문 생성 시) | 400 | NO_WORDS |
| 패턴 없음 (예문 생성 시) | 400 | NO_PATTERNS |
| 미인증 | 401 | UNAUTHORIZED |
| 권한 없음 (IDOR) | 403 | FORBIDDEN |
| 서버 내부 오류 | 500 | INTERNAL_ERROR |

---

## 0. 인증 API

> 인증 불필요 엔드포인트. 나머지 모든 API는 인증 필수 (Cookie에 JWT 자동 전송)

### POST /api/auth/signup
회원가입 + 자동 로그인

**Request:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "nickname": "혜진"
}
```

**Response:** `201 Created` + `Set-Cookie: token=<jwt>; HttpOnly; Path=/api; SameSite=Lax; Max-Age=86400`
```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "혜진"
}
```

**에러:**
- 이메일 중복: `409 DUPLICATE`
- 비밀번호 8글자 미만: `400 BAD_REQUEST`

### POST /api/auth/login
로그인

**Request:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:** `200 OK` + `Set-Cookie: token=<jwt>; HttpOnly; Path=/api; SameSite=Lax; Max-Age=86400`
```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "혜진"
}
```

**에러:** `401 UNAUTHORIZED` — "이메일 또는 비밀번호가 올바르지 않습니다" (이메일/비밀번호 구분 금지)

### POST /api/auth/logout
로그아웃 (Cookie 삭제)

**Response:** `204 No Content` + `Set-Cookie: token=; HttpOnly; Path=/api; Max-Age=0`

### GET /api/auth/me
현재 로그인 사용자 정보 조회

**Response:** `200 OK`
```json
{
  "id": 1,
  "email": "user@example.com",
  "nickname": "혜진"
}
```

**에러:** `401 UNAUTHORIZED` (미로그인)

---

## 1. 단어 API

### GET /api/words
단어 목록 조회 (deleted=false만, 페이지네이션)

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| page | Integer | X | 페이지 번호 (기본 0) |
| size | Integer | X | 페이지 크기 (기본 20) |
| search | String | X | 단어/뜻 검색 |
| partOfSpeech | String | X | 품사 필터 (noun, verb, phrase...) |
| importantOnly | Boolean | X | 중요 체크만 |
| sort | String | X | 정렬 (createdAt, word) |

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": 1,
      "word": "make a bed",
      "meaning": "침대를 정리하다",
      "partOfSpeech": "phrase",
      "pronunciation": "/meɪk ə bɛd/",
      "synonyms": "tidy up the bed",
      "tip": "make the bed도 같은 의미",
      "isImportant": true,
      "createdAt": "2026-05-02T10:00:00"
    }
  ],
  "totalElements": 45,
  "totalPages": 3,
  "page": 0,
  "size": 20
}
```

### GET /api/words/{id}
단어 상세 + 이 단어가 사용된 예문 목록

### POST /api/words
단건 등록 (+ Gemini 보강). Gemini 실패 시 보강 없이 저장.

**Request:**
```json
{
  "word": "make a bed",
  "meaning": "침대를 정리하다"
}
```

**Response:** `201 Created` — AI 보강된 단어 정보

### POST /api/words/bulk
JSON 벌크 등록 (1회 Gemini 호출로 일괄 보강)

**Request:**
```json
[
  { "word": "drink coffee", "meaning": "커피를 마시다" },
  { "word": "make a bed", "meaning": "침대를 정리하다" }
]
```

**Response:** `201 Created`
```json
{
  "saved": [
    {
      "id": 1,
      "word": "drink coffee",
      "meaning": "커피를 마시다",
      "partOfSpeech": "phrase",
      "pronunciation": "/drɪŋk ˈkɒfi/"
    }
  ],
  "skipped": [
    { "word": "make a bed", "reason": "이미 등록된 단어입니다" }
  ],
  "enrichmentFailed": [
    {
      "id": 5,
      "word": "obscure term",
      "meaning": "모호한 표현",
      "reason": "AI 보강 실패 — 보강 없이 저장됨"
    }
  ]
}
```

> `enrichmentFailed`는 **저장은 성공했지만 AI 보강만 실패**한 항목. DB에는 word+meaning만 저장되어 있음. `saved`와 `enrichmentFailed`는 **겹치지 않음** (분리 표시).

```
```

### POST /api/words/extract
이미지에서 단어 추출 (Gemini Vision + 보강, 이미지 미저장)

**Request:** `multipart/form-data` — image 파일

**Response:** `200 OK` — 추출된 단어 목록 (확인용, 아직 저장 안 됨)
```json
[
  { "word": "drink coffee", "meaning": "커피를 마시다" },
  { "word": "make a bed", "meaning": "침대를 정리하다" }
]
```

> 추출 후 저장 흐름: extract → 프론트에서 확인/수정 → `POST /api/words/bulk`로 저장

**에러:** 이미지 형식 오류 시 `400`, 추출 결과 없음 시 `200` + 빈 배열, Gemini 오류 시 `502`

### PUT /api/words/{id}
단어 수정

### PATCH /api/words/{id}/important
중요 체크 토글

### DELETE /api/words/{id}
soft delete (예문은 유지, 해당 단어의 review_items만 soft delete)

---

## 2. 패턴 API

### GET /api/patterns
패턴 목록 (예문 포함, deleted=false만, 페이지네이션)

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| page | Integer | X | 페이지 번호 (기본 0) |
| size | Integer | X | 페이지 크기 (기본 20) |

### GET /api/patterns/{id}
패턴 상세 + 교재 예문 + 이 패턴으로 생성된 예문 목록

### POST /api/patterns
직접 등록

**Request:**
```json
{
  "template": "I'm afraid that...",
  "description": "유감스럽게도 ~인/할 것 같아요",
  "examples": [
    {
      "sentence": "I'm afraid that we'll be late.",
      "translation": "유감스럽게도 우리는 지각할 것 같아요."
    }
  ]
}
```

### POST /api/patterns/extract
이미지에서 패턴+예문+해석 추출 (Gemini Vision, 이미지 미저장)

**Request:** `multipart/form-data` — image 파일

**Response:** `200 OK` — 추출된 패턴 정보 (확인용, 아직 저장 안 됨)
```json
{
  "template": "I'm afraid that...",
  "description": "유감스럽게도 ~인/할 것 같아요",
  "examples": [
    {
      "sentence": "I'm afraid that we'll be late.",
      "translation": "유감스럽게도 우리는 지각할 것 같아요."
    }
  ]
}
```

> 추출 후 저장 흐름: extract → 프론트에서 확인/수정 → `POST /api/patterns`로 저장

**에러:** 이미지 형식 오류 시 `400`, 추출 결과 없음 시 `200` + `{}` 빈 객체, Gemini 오류 시 `502`

### PUT /api/patterns/{id}
패턴 수정

### DELETE /api/patterns/{id}
soft delete (예문은 유지, 해당 패턴의 review_items만 soft delete)

---

## 3. 예문 생성 API

### POST /api/generate
패턴×단어 조합 예문 생성

**Request:**
```json
{
  "level": "중등",
  "count": 30
}
```

**Response:** `201 Created`
```json
{
  "generationId": 1,
  "sentences": [
    {
      "id": 1,
      "sentence": "I'm afraid that I forgot to make my bed.",
      "translation": "유감스럽게도 침대 정리를 깜빡한 것 같아요.",
      "situations": [
        "아침에 급하게 나왔는데 엄마한테 전화가 온 상황",
        "군대에서 내무반 점검 전에 후임에게 말하는 상황",
        "룸메이트한테 잔소리하는 상황",
        "아이에게 생활 습관을 가르치는 상황",
        "호텔 체크아웃 후 직원에게 사과하는 상황"
      ],
      "level": "중등",
      "pattern": { "id": 1, "template": "I'm afraid that..." },
      "words": [
        { "id": 3, "word": "make a bed" }
      ]
    }
  ]
}
```

### POST /api/generate/word
특정 단어 예문 생성 (패턴 없이)

**Request:**
```json
{
  "wordId": 3,
  "level": "초등",
  "count": 5
}
```

### POST /api/generate/pattern
특정 패턴으로 예문 생성 (단어는 우선순위 기반 자동 선택)

**Request:**
```json
{
  "patternId": 1,
  "level": "중등",
  "count": 10
}
```

**Response:** `201 Created` — POST /api/generate와 동일한 응답 구조 (generationId + sentences 배열). 모든 예문이 지정된 패턴을 사용.

### GET /api/generate/history
생성 이력 조회 (페이지네이션)

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| page | Integer | X | 페이지 번호 (기본 0) |
| size | Integer | X | 페이지 크기 (기본 20) |

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": 1,
      "level": "중등",
      "requestedCount": 30,
      "actualCount": 28,
      "createdAt": "2026-05-02T14:00:00"
    }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "page": 0,
  "size": 20
}
```

---

## 4. 학습 기록 API

### GET /api/study-records
날짜별 학습 기록 목록 (패턴+단어 포함, 페이지네이션, **최신순 정렬**)

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| page | Integer | X | 페이지 번호 (기본 0) |
| size | Integer | X | 페이지 크기 (기본 20) |

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "studyDate": "2026-05-02",
      "dayNumber": 15,
      "items": [
        { "type": "PATTERN", "id": 1, "name": "I'm afraid that..." },
        { "type": "WORD", "id": 3, "name": "make a bed" }
      ]
    }
  ],
  "totalElements": 15,
  "totalPages": 1,
  "page": 0,
  "size": 20
}
```

---

## 5. 복습 API

### GET /api/reviews/today
오늘 복습할 카드 목록. 서버가 user_settings의 `daily_review_count`를 읽어 해당 타입 N개 선정.

**Query Parameters:**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| type | String | O | 복습 타입 (WORD / PATTERN / SENTENCE) |
| exclude | String | X | 제외할 reviewItemId 목록 (콤마 구분, 예: `1,2,3`). 추가 복습 시 이미 복습한 카드 제외용 |

**복습 완료 후 동작:**
- **처음부터 다시**: 프론트에서 같은 카드 덱을 읽기 전용으로 다시 표시 (API 호출 X, SM-2 미적용, EASY/MEDIUM/HARD 없음, [다음 카드 →]만)
- **추가 복습**: `exclude`에 이미 복습한 카드 ID를 넘겨서 다음 N개 요청 (SM-2 적용). 0개 반환 시 "복습할 카드가 없습니다" 표시

**Response:**
```json
[
  {
    "reviewItemId": 1,
    "itemType": "WORD",
    "direction": "RECOGNITION",
    "front": {
      "text": "make a bed"
    },
    "back": {
      "meaning": "침대를 정리하다",
      "pronunciation": "/meɪk ə bɛd/",
      "tip": "make the bed도 같은 의미",
      "examples": [
        "I'm afraid that I forgot to make my bed.",
        "She makes her bed every morning."
      ]
    }
  },
  {
    "reviewItemId": 2,
    "itemType": "WORD",
    "direction": "RECALL",
    "front": {
      "text": "침대를 정리하다"
    },
    "back": {
      "word": "make a bed",
      "pronunciation": "/meɪk ə bɛd/",
      "tip": "make the bed도 같은 의미"
    }
  },
  {
    "reviewItemId": 3,
    "itemType": "PATTERN",
    "direction": "RECOGNITION",
    "front": {
      "text": "I'm afraid that..."
    },
    "back": {
      "description": "유감스럽게도 ~인/할 것 같아요",
      "examples": [
        { "sentence": "I'm afraid that we'll be late.", "translation": "유감스럽게도 우리는 지각할 것 같아요." },
        { "sentence": "I'm afraid that it might rain.", "translation": "유감스럽게도 비가 올 수도 있어요." }
      ]
    }
  },
  {
    "reviewItemId": 4,
    "itemType": "PATTERN",
    "direction": "RECALL",
    "front": {
      "text": "유감스럽게도 ~인/할 것 같아요"
    },
    "back": {
      "template": "I'm afraid that...",
      "examples": [
        { "sentence": "I'm afraid that we'll be late.", "translation": "유감스럽게도 우리는 지각할 것 같아요." }
      ]
    }
  },
  {
    "reviewItemId": 5,
    "itemType": "SENTENCE",
    "direction": "RECOGNITION",
    "front": {
      "text": "I'm afraid that I forgot to make my bed.",
      "situation": "아침에 급하게 나왔는데 엄마한테 전화가 온 상황"
    },
    "back": {
      "translation": "유감스럽게도 침대 정리를 깜빡한 것 같아요.",
      "pattern": "I'm afraid that...",
      "words": ["make a bed"]
    }
  }
]
```

### POST /api/reviews/{reviewItemId}
복습 결과 기록

**Request:**
```json
{
  "result": "EASY"
}
```

**Response:**
```json
{
  "nextReviewDate": "2026-05-05",
  "intervalDays": 3
}
```

---

## 6. 대시보드 API

### GET /api/dashboard
홈 화면 통계 조회

**Response:**
```json
{
  "wordCount": 45,
  "patternCount": 12,
  "sentenceCount": 180,
  "streak": 7,
  "todayReviewRemaining": {
    "word": 15,
    "pattern": 8,
    "sentence": 20
  },
  "recentStudyRecords": [
    { "studyDate": "2026-05-02", "dayNumber": 15, "patternName": "I'm afraid that..." },
    { "studyDate": "2026-05-01", "dayNumber": 14, "patternName": "I used to..." }
  ]
}
```

---

## 7. 설정 API

### GET /api/settings
전체 설정 조회

### PUT /api/settings/{key}
설정 변경

**Request:**
```json
{
  "value": "20"
}
```
