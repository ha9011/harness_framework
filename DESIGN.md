# 영어 패턴 학습기 — 구현 계획 (SSOT)

> 이 문서가 프로젝트의 **단일 진실 소스(Single Source of Truth)**이다.
> DB 스키마, API, 알고리즘, 구현 순서 등 기술적 결정은 모두 이 문서를 기준으로 한다.

## Context
영어 단어와 패턴을 등록하고, Gemini API로 둘을 조합해 난이도별 예문을 생성하는 학습 도구.
교재 사진이나 유튜브 캡쳐를 업로드하면 AI가 자동 추출해주는 것이 핵심 UX.
생성된 예문은 간격 반복 기반 카드 복습으로 이어진다.

## 기술 스택

| 영역 | 기술 | 비고 |
|------|------|------|
| 프론트엔드 | Next.js (App Router) | `frontend/` 폴더 |
| 백엔드 | Spring Boot + JPA | `backend/` 폴더 |
| DB | PostgreSQL 16 | Docker 컨테이너 |
| AI | Gemini API | Vision + Text Generation |
| 테스트 | JUnit5 + MockMvc + TestContainers | TDD |

## 사용 흐름

### 등록
1. **단어 등록** — 직접 입력(단어+뜻), JSON 벌크 등록(1회 Gemini 호출로 일괄 보강), 또는 이미지 업로드 → Gemini Vision 추출. 등록 시 Gemini가 품사/발음/유의어/한줄팁 자동 보강 → 확인 후 저장. 이미지는 추출 후 저장하지 않음. 중복 단어는 알림 후 스킵. 벌크 등록 시 보강 실패한 항목은 보강 없이 저장(부분 성공). 등록 시 해당 날짜의 학습 기록에 자동 추가.
   - JSON 벌크 입력 UI: 텍스트 영역에 플레이스홀더로 예시 표시
   ```
   [
     { "word": "drink coffee", "meaning": "커피를 마시다" },
     { "word": "make a bed", "meaning": "침대를 정리하다" }
   ]
   ```
2. **패턴 등록** — 직접 입력 또는 교재 사진 업로드 → Gemini Vision이 패턴/설명/예문10개/해석 추출 → 확인 후 저장. 이미지는 추출 후 저장하지 않음. 중복 패턴은 알림 후 스킵. 등록 시 해당 날짜의 학습 기록에 자동 추가.

### 학습
3. **예문 생성** — 난이도(유아/초등/중등/고등) + 개수(10/20/30) 선택 → 단어 선택 우선순위: ⭐중요 체크 > 📉복습 적은 것 > 🎲랜덤 → 패턴도 랜덤 조합 → Gemini API에 단어 목록(ID 포함, 최대 50개)을 넘기고 JSON 형식 응답 요청 → 어떤 단어가 사용됐는지 정확히 매핑하여 저장
4. **단어 예문 생성** — 단어 상세에서 [예문 추가] → 해당 단어로 예문 N개 생성
4-1. **패턴 예문 생성** — 패턴 상세에서 난이도+개수 선택 → 해당 패턴으로 예문 N개 생성 (단어는 우선순위 기반 자동 선택). 패턴 집중 학습의 핵심 기능
5. **학습 기록** — 단어/패턴 등록 시 자동 생성. 날짜별로 공부한 패턴 + 단어 기록. day_number는 총 학습일 기준 (쉬는 날 상관없이 학습한 날만 카운트)

### 복습
6. **오늘의 복습** — 하루 복습 개수 설정(10/20/30, DB 저장) → **타입별 각 N개씩 독립 선정** (설정값 20이면 WORD 20 + PATTERN 20 + SENTENCE 20). 부족한 타입은 있는 만큼만.
   - **[단어] [패턴] [문장] 탭으로 분리** — 원하는 타입만 골라서 복습 가능
   - 각 탭 내 선정 우선순위: ① 😰모름(HARD) 응답했던 것 ② 오래 안 본 것 ③ 복습 횟수 적은 것
   - 각 탭 내에서 랜덤 셔플
   - 복습 완료 후:
     - **[처음부터 다시]** — 같은 카드 1번부터 다시 보여줌. **읽기 전용 모드** (SM-2 미적용, EASY/MEDIUM/HARD 버튼 없음, [다음 카드 →]만 있음)
     - **[추가 복습]** — 이미 복습한 카드 제외하고 다음 N개 (SM-2 적용). 0개면 "복습할 카드가 없습니다" 표시
7. **카드 복습** — 카드 유형별 앞/뒤:
   - WORD 카드 (인식): 앞면 = 영어 단어 → 뒤면 = 뜻 + 예문(최대 2~3개 랜덤, 없으면 생략) + 팁
   - WORD 카드 (회상): 앞면 = 한국어 뜻 → 뒤면 = 영어 단어 + 발음 + 팁 ← 실력 향상에 핵심
   - PATTERN 카드 (인식): 앞면 = 영어 패턴 → 뒤면 = 한국어 설명 + 교재 예문
   - PATTERN 카드 (회상): 앞면 = 한국어 설명 → 뒤면 = 영어 패턴 + 교재 예문
   - SENTENCE 카드 (인식만): 앞면 = 영어 예문 + 상황(구름 말풍선, 서버가 5개 중 랜덤 1개 선택) → 뒤면 = 한국어 해석
   - 기억남/애매/모름 응답 → 다음 복습 간격 자동 조정 (SM-2 기반 커스텀 알고리즘)
     - EASY: interval × ease_factor × 1.3, ease_factor += 0.15
     - MEDIUM: interval × ease_factor, ease_factor 유지
     - HARD: interval = 1 (리셋), ease_factor = max(1.3, ease_factor - 0.2)

## DB 스키마

### words
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| word | VARCHAR(200) UNIQUE | 단어 또는 표현 — 사용자 입력, 중복 방지 |
| meaning | VARCHAR(500) | 한국어 뜻 — 사용자 입력 |
| part_of_speech | VARCHAR(50) | 품사 — AI 보강 |
| pronunciation | VARCHAR(200) | 발음 — AI 보강 |
| synonyms | VARCHAR(500) | 유의어/관련 표현 — AI 보강 |
| tip | VARCHAR(500) | 한줄 팁 — AI 보강 |
| is_important | BOOLEAN DEFAULT false | 중요 체크 (예문 생성 시 우선 선택) |
| deleted | BOOLEAN DEFAULT false | soft delete |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

**인덱스:** `words(deleted)` — 거의 모든 쿼리에 WHERE deleted=false

### patterns (인덱스: `patterns(deleted)`)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| template | VARCHAR(255) UNIQUE | 패턴 (예: "I'm afraid that..."), 중복 방지 |
| description | VARCHAR(500) | 한국어 설명 |
| deleted | BOOLEAN DEFAULT false | soft delete |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |

### pattern_examples (패턴 등록 시 교재 예문)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| pattern_id | BIGINT FK → patterns.id | ON DELETE CASCADE |
| sort_order | INT | 예문 순서 (1~10) |
| sentence | VARCHAR(500) | 영어 예문 |
| translation | VARCHAR(500) | 한국어 해석 |

### generation_history (예문 생성 이력)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| level | VARCHAR(10) | 유아/초등/중등/고등 |
| requested_count | INT | 요청한 생성 개수 |
| actual_count | INT | 실제 생성된 개수 |
| word_id | BIGINT FK nullable | 단어 지정 생성 시 (POST /api/generate/word) |
| pattern_id | BIGINT FK nullable | 패턴 지정 생성 시 (POST /api/generate/pattern) |
| created_at | TIMESTAMP | |

### generated_sentences (AI 생성 예문)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| generation_id | BIGINT FK → generation_history.id | 어떤 생성 요청에서 만들어졌는지 |
| pattern_id | BIGINT FK nullable | 사용된 패턴 (단어만 생성 시 null) |
| sentence | TEXT | 생성된 영어 예문 |
| translation | TEXT | 한국어 해석 |
| level | VARCHAR(10) | 유아/초등/중등/고등 |
| deleted | BOOLEAN DEFAULT false | soft delete |
| created_at | TIMESTAMP | |

### generated_sentence_words (예문-단어 매핑)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| sentence_id | BIGINT FK → generated_sentences.id | |
| word_id | BIGINT FK → words.id | |

**인덱스:** `generated_sentence_words(word_id)` — 단어 상세에서 예문 조회

### sentence_situations (예문별 상황 — 감정이입용, 예문당 5개. 복습 시 매번 랜덤 1개 표시)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| sentence_id | BIGINT FK → generated_sentences.id | |
| situation | TEXT | 상황 설명 (예: "택시 안에서 친구에게 전화하는 상황") |

### study_records (학습 기록)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| study_date | DATE UNIQUE | 학습 날짜 (하루에 하나) |
| day_number | INT | 총 학습일 기준 몇 일차 |
| created_at | TIMESTAMP | |

### study_record_items (학습 기록 상세 — 패턴+단어 모두 기록)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| study_record_id | BIGINT FK → study_records.id | |
| item_type | VARCHAR(10) | WORD / PATTERN |
| item_id | BIGINT | 해당 단어 또는 패턴의 ID |
| UNIQUE(study_record_id, item_type, item_id) | | 같은 날 같은 항목 중복 방지 |

> ⚠️ study_record_items, review_items는 polymorphic association (item_type + item_id) 패턴이다.
> DB 레벨 FK 제약을 걸 수 없으므로, **애플리케이션 레벨에서 ID 유효성을 반드시 검증**해야 한다.
> MVP에서는 이 구조를 유지하되, 장기적으로 별도 테이블 분리를 검토한다.

### review_items (복습 대상 — 간격 반복)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| item_type | VARCHAR(20) | WORD / PATTERN / SENTENCE |
| item_id | BIGINT | 해당 타입의 ID |
| direction | VARCHAR(15) DEFAULT 'RECOGNITION' | RECOGNITION(영→한) / RECALL(한→영). **SENTENCE는 RECOGNITION만** |
| deleted | BOOLEAN DEFAULT false | soft delete 연쇄용 (단어/패턴 삭제 시 함께 비활성화) |
| next_review_date | DATE | 다음 복습 날짜 |
| interval_days | INT DEFAULT 1 | 현재 간격 (1, 3, 7, 14...) |
| ease_factor | FLOAT DEFAULT 2.5 | 난이도 계수 |
| review_count | INT DEFAULT 0 | 복습 횟수 (예문 생성 우선순위에 활용) |
| last_result | VARCHAR(10) | 마지막 응답 (EASY/MEDIUM/HARD, 초기 null) |
| last_reviewed_at | TIMESTAMP | |
| created_at | TIMESTAMP | |
| UNIQUE(item_type, item_id, direction) | | 중복 생성 방지 (같은 단어라도 방향별로 1개씩) |

**인덱스:**
- `review_items(next_review_date)` — 매일 복습 카드 선정
- `review_items(item_type, last_result)` — HARD 우선 정렬
- `review_items(deleted)` — 삭제된 항목 필터링

### review_logs (복습 이력)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| review_item_id | BIGINT FK → review_items.id | |
| result | VARCHAR(10) | EASY / MEDIUM / HARD |
| reviewed_at | TIMESTAMP | |

**인덱스:** `review_logs(reviewed_at)` — 복습 연속일(streak) 계산

### user_settings (사용자 설정)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGSERIAL PK | |
| setting_key | VARCHAR(50) UNIQUE | 설정 키 (예: "daily_review_count") |
| setting_value | VARCHAR(200) | 설정 값 (예: "20") |
| updated_at | TIMESTAMP | |

## REST API

> 기본 정보: Base URL `/api`, 응답 형식 JSON, 삭제는 모두 soft delete

### 단어
- `GET /api/words?page=0&size=20` — 목록 (deleted=false만, 검색/품사필터/중요필터/정렬 + **페이지네이션**)
- `GET /api/words/{id}` — 단어 상세 + 이 단어가 사용된 예문 목록
- `POST /api/words` — 단건 등록 (+ Gemini 보강). Gemini 실패 시 보강 없이 저장
- `POST /api/words/bulk` — JSON 벌크 등록 (1회 Gemini 호출로 일괄 보강). 응답: `{ saved: [...], skipped: [...], enrichmentFailed: [...] }`
- `POST /api/words/extract` — 이미지 → 단어 추출 (Gemini Vision + 보강, 이미지 미저장)
- `PUT /api/words/{id}` — 수정
- `PATCH /api/words/{id}/important` — 중요 체크 토글
- `DELETE /api/words/{id}` — soft delete (예문+SENTENCE 카드는 유지, 해당 단어의 WORD review_items만 soft delete)

### 패턴
- `GET /api/patterns?page=0&size=20` — 목록 (예문 포함, deleted=false만, **페이지네이션**)
- `GET /api/patterns/{id}` — 패턴 상세 + 교재 예문 + 이 패턴으로 생성된 예문 목록
- `POST /api/patterns` — 직접 등록
- `POST /api/patterns/extract` — 이미지 → 패턴+예문+해석 추출 (Gemini Vision, 이미지 미저장)
- `PUT /api/patterns/{id}` — 수정
- `DELETE /api/patterns/{id}` — soft delete (예문+SENTENCE 카드는 유지, 해당 패턴의 PATTERN review_items만 soft delete)

### 예문 생성
- `POST /api/generate` — body: { level(유아/초등/중등/고등), count(10/20/30) } → 중요체크 > 복습적은것 > 랜덤 순 단어 선택(최대 50개) + 패턴 랜덤 → Gemini API → 예문
- `POST /api/generate/word` — body: { wordId, level, count } → 단어 예문 N개 생성 (패턴 없이)
- `POST /api/generate/pattern` — body: { patternId, level, count } → 특정 패턴으로 예문 N개 생성 (단어는 우선순위 기반 자동 선택)
- `GET /api/generate/history?page=0&size=20` — 생성 이력 (**페이지네이션**)

### 학습 기록
- `GET /api/study-records?page=0&size=20` — 기록 목록 (패턴+단어 포함, **페이지네이션**)
- (학습 기록은 패턴/단어 등록 시 자동 생성, 별도 POST 불필요)

### 복습
- `GET /api/reviews/today?type=WORD` — 오늘 복습할 카드 목록. type 파라미터로 타입 지정 (WORD/PATTERN/SENTENCE). 서버가 user_settings의 daily_review_count를 읽어 해당 타입 N개 선정. `exclude` 파라미터로 이미 복습한 카드 ID 제외 가능 (추가 복습용)
- `POST /api/reviews/{reviewItemId}` — 복습 결과 기록 (EASY/MEDIUM/HARD) → 간격 자동 조정

### 대시보드
- `GET /api/dashboard` — 홈 화면 통계
  - wordCount, patternCount, sentenceCount (각 deleted=false)
  - streak (복습 연속일)
  - todayReviewRemaining (오늘 남은 복습 수)
  - recentStudyRecords (최근 학습 목록, 최대 5건)

### 설정
- `GET /api/settings` — 전체 설정 조회
- `PUT /api/settings/{key}` — 설정 변경 (예: daily_review_count = 20)

### 에러 응답 규칙
- extract API (words/extract, patterns/extract) 실패 시:
  - 이미지 형식 오류: `400 Bad Request` + `{ error: "INVALID_IMAGE_FORMAT", message: "..." }`
  - 추출 결과 없음: `200 OK` + 빈 결과 (단어: 빈 배열 `[]`, 패턴: 빈 객체 `{}`)
  - Gemini API 오류: `502 Bad Gateway` + `{ error: "AI_SERVICE_ERROR", message: "..." }`
- 중복 등록 시: `409 Conflict` + `{ error: "DUPLICATE", message: "..." }`
- 빈 배열 요청 시: `400 Bad Request` + `{ error: "EMPTY_REQUEST", message: "최소 1개 이상 입력해주세요" }`
- 예문 생성 시 단어 0개: `400 Bad Request` + `{ error: "NO_WORDS", message: "등록된 단어가 없습니다" }`
- 예문 생성 시 패턴 0개 (POST /api/generate): `400 Bad Request` + `{ error: "NO_PATTERNS", message: "등록된 패턴이 없습니다" }`

## 핵심 알고리즘

### SM-2 기반 커스텀 간격 반복
> 원본 SM-2(Piotr Wozniak, 1987)를 3단계로 단순화한 변형 알고리즘이다.

```
초기값:
  interval_days = 1
  ease_factor = 2.5

응답별 처리:
  EASY   → new_interval = interval × ease_factor × 1.3
           ease_factor += 0.15
  MEDIUM → new_interval = interval × ease_factor
           ease_factor 유지
  HARD   → new_interval = 1 (리셋)
           ease_factor = max(1.3, ease_factor - 0.2)

next_review_date = 오늘 + new_interval (소수점 반올림)
```

### 복습 카드 선정 알고리즘
```
입력: type (WORD / PATTERN / SENTENCE), exclude (제외할 카드 ID 목록, 선택)

1. next_review_date <= 오늘 AND deleted=false AND item_type=type인 review_items 조회
2. exclude에 포함된 ID 제외 (추가 복습 시)
3. 우선순위 정렬 (이 순서대로 적용):
   - last_result = 'HARD' 인 것 먼저 (모르는 걸 먼저)
   - last_reviewed_at 오래된 순 (오래 안 본 걸 먼저)
   - review_count 낮은 순 (동점 시 덜 복습한 것 우선)
4. 상위 N개 선정 (daily_review_count, 부족하면 있는 만큼만)
5. 랜덤 셔플
6. 반환 (0개면 "복습할 카드가 없습니다")

※ "추가 복습": 오늘 대상(next_review_date<=오늘) 중 이미 복습한 카드를 exclude하고 다음 N개 선정. 대상 45장 중 20장 했으면, 나머지 25장에서 다음 20장.
```

### 난이도 기준
> 모든 예문은 **실생활/일상**에 가까워야 한다. 시험 지문이 아니라 실제로 말할 법한 문장.

| 레벨 | 문장 길이 | 문법 범위 | 느낌 |
|------|----------|----------|------|
| 유아 | 3~5 단어 | 현재 단순시제만 | "I like it." 수준 |
| 초등 | 5~10 단어 | 과거/현재/미래 단순시제 | 카페에서 주문하는 수준 |
| 중등 | 8~15 단어 | 완료/진행/조건문 | 친구랑 카톡하는 수준 |
| 고등 | 15+ 단어 | 가정법/관계절/분사구문 | 회사에서 동료랑 대화하는 수준 |

### 예문 표시 방식
- 영어 문장 + 상황(situation)을 먼저 표시
- 한국어 해석은 탭하면 펼쳐지는 방식 (학습 효과를 위해 바로 보여주지 않음)
- 해석 펼치면 패턴/단어 태그도 함께 표시

### 예문 생성 시 단어 선택 알고리즘
```
1. deleted=false인 단어 전체 조회
2. 우선순위 정렬:
   - is_important=true 먼저
   - review_items의 review_count 낮은 순 (WORD RECOGNITION 기준)
   - 랜덤
3. 상위 최대 50개 선택 (Gemini 토큰 한도 고려)
4. Gemini 프롬프트에 단어 목록(ID+word+meaning) 전달
5. Gemini에게 JSON 형식으로 응답 요청 (사용한 word_id 포함)
6. 응답 파싱 후 word_id가 실제 존재하는지 DB 검증 → 없는 ID는 매핑에서 무시 (예문 자체는 저장)
```

### 복습 연속일(streak) 계산
```
review_logs에서 날짜별 복습 여부를 역순으로 조회:
  오늘 복습했으면 → 오늘부터 역순 카운트 시작
  오늘 아직 안 했으면 → 어제부터 역순 카운트 시작
  연속된 날짜 수 = streak

SELECT DISTINCT DATE(reviewed_at) as review_date
FROM review_logs
ORDER BY review_date DESC
→ 연속된 날짜 수 = streak
```

## Gemini API 연동

### Gemini JSON 응답 신뢰성 확보
- `response_mime_type: "application/json"` + `response_schema` 옵션 사용
- 응답 파싱 실패 시 최대 2회 재시도 (exponential backoff)
- 예문 생성 시 wordIds 유효성 검증 (DB에 존재하는 ID인지)

### 단어 보강 프롬프트 (예시)
```
다음 영어 단어들의 품사, 발음, 유의어, 한줄 학습 팁을 JSON으로 알려줘.

입력:
[
  {"id": 1, "word": "make a bed", "meaning": "침대를 정리하다"},
  {"id": 2, "word": "opportunity", "meaning": "기회"}
]

응답 형식:
[
  {
    "id": 1,
    "partOfSpeech": "phrase",
    "pronunciation": "/meɪk ə bɛd/",
    "synonyms": "tidy up the bed, fix the bed",
    "tip": "make the bed도 같은 의미. 매일 아침 루틴에서 자주 쓰임"
  }
]
```

### 예문 생성 프롬프트 (예시)
```
다음 영어 단어와 패턴을 조합하여 중등 수준의 자연스러운 예문을 10개 만들어줘.
시험 지문이 아니라 실제 일상에서 말할 법한 문장으로 만들어줘. 같은 단어가 여러 예문에 사용되어도 괜찮아.
각 예문에 사용한 단어 ID, 패턴 ID, 그리고 감정이입할 수 있는 구체적인 상황 5개를 포함하여 JSON으로 응답해줘.
상황은 그 예문을 실제로 말할 법한 일상적인 장면이어야 해.

단어:
[
  {"id": 1, "word": "make a bed", "meaning": "침대를 정리하다"},
  {"id": 2, "word": "opportunity", "meaning": "기회"}
]

패턴:
[
  {"id": 1, "template": "I'm afraid that..."},
  {"id": 2, "template": "I used to..."}
]

응답 형식:
[
  {
    "sentence": "I'm afraid that I forgot to make my bed this morning.",
    "translation": "유감스럽게도 오늘 아침 침대 정리를 깜빡한 것 같아요.",
    "patternId": 1,
    "wordIds": [1],
    "situations": [
      "아침에 급하게 나왔는데 엄마한테 전화가 온 상황",
      "군대에서 내무반 점검 전에 후임에게 말하는 상황",
      "룸메이트한테 잔소리하는 상황",
      "아이에게 생활 습관을 가르치는 상황",
      "호텔 체크아웃 후 직원에게 사과하는 상황"
    ]
  }
]
```

### Gemini 실패 시 fallback 정책
| 상황 | 대응 |
|------|------|
| 단건 단어 보강 실패 | 보강 없이 저장 (word, meaning만) |
| 벌크 단어 보강 실패 | 보강 없이 전체 저장 (FR-01-08) |
| 이미지 추출 실패 | 에러 메시지 표시 + 재시도 안내 |
| 예문 생성 실패 | 에러 메시지 + 재시도 |
| 재시도 정책 | 총 3회 시도 (초기 즉시 + 1초 후 재시도 + 3초 후 재시도) |

## 프로젝트 구조

```
harness_framework/
├── docker-compose.yml
├── backend/
│   └── src/main/java/com/english/
│       ├── EnglishApplication.java
│       ├── word/
│       │   ├── Word.java                  (Entity)
│       │   ├── WordRepository.java        (JPA Repository)
│       │   ├── WordService.java           (Service)
│       │   ├── WordController.java        (REST Controller)
│       │   └── WordDto.java               (Request/Response DTO)
│       ├── pattern/
│       │   ├── Pattern.java
│       │   ├── PatternExample.java
│       │   ├── PatternRepository.java
│       │   ├── PatternService.java
│       │   ├── PatternController.java
│       │   └── PatternDto.java
│       ├── generate/
│       │   ├── GenerationHistory.java
│       │   ├── GeneratedSentence.java
│       │   ├── GeneratedSentenceWord.java
│       │   ├── SentenceSituation.java
│       │   ├── GeneratedSentenceRepository.java
│       │   ├── GenerateService.java
│       │   ├── GenerateController.java
│       │   └── GeminiClient.java          (Gemini API 호출)
│       ├── review/
│       │   ├── ReviewItem.java
│       │   ├── ReviewLog.java
│       │   ├── ReviewRepository.java
│       │   ├── ReviewService.java
│       │   └── ReviewController.java
│       ├── study/
│       │   ├── StudyRecord.java
│       │   ├── StudyRecordItem.java
│       │   ├── StudyRecordRepository.java
│       │   ├── StudyRecordService.java
│       │   └── StudyRecordController.java
│       ├── dashboard/
│       │   ├── DashboardService.java
│       │   └── DashboardController.java
│       ├── setting/
│       │   ├── UserSetting.java
│       │   ├── UserSettingRepository.java
│       │   ├── UserSettingService.java
│       │   └── UserSettingController.java
│       └── config/
│           ├── CorsConfig.java
│           └── GlobalExceptionHandler.java
├── frontend/
│   └── app/
│       ├── layout.tsx
│       ├── page.tsx                       (홈 대시보드)
│       ├── words/
│       │   ├── page.tsx                   (단어 목록+등록)
│       │   └── [id]/page.tsx             (단어 상세)
│       ├── patterns/
│       │   ├── page.tsx                   (패턴 목록+등록)
│       │   └── [id]/page.tsx             (패턴 상세)
│       ├── generate/
│       │   └── page.tsx                   (예문 생성)
│       ├── review/
│       │   └── page.tsx                   (복습 카드)
│       ├── history/
│       │   └── page.tsx                   (학습 기록)
│       ├── settings/
│       │   └── page.tsx                   (설정)
│       ├── components/
│       │   ├── WordForm.tsx
│       │   ├── BulkWordForm.tsx
│       │   ├── PatternForm.tsx
│       │   ├── FlipCard.tsx
│       │   └── SentenceCard.tsx
│       └── lib/
│           └── api.ts                     (백엔드 API 호출)
```

## Docker 설정

```yaml
# docker-compose.yml
services:
  db:
    image: postgres:16
    environment:
      POSTGRES_DB: english_app
      POSTGRES_USER: app
      POSTGRES_PASSWORD: app1234
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

## 백엔드 패키지 구조
```
com.english/
├── word/        (Word, WordRepository, WordService, WordController, WordDto)
├── pattern/     (Pattern, PatternExample, PatternRepository, PatternService, PatternController, PatternDto)
├── generate/    (GenerationHistory, GeneratedSentence, GeneratedSentenceWord, SentenceSituation, GenerateService, GenerateController, GeminiClient)
├── review/      (ReviewItem, ReviewLog, ReviewRepository, ReviewService, ReviewController)
├── study/       (StudyRecord, StudyRecordItem, StudyRecordRepository, StudyRecordService, StudyRecordController)
├── dashboard/   (DashboardService, DashboardController)
├── setting/     (UserSetting, UserSettingRepository, UserSettingService, UserSettingController)
└── config/      (CorsConfig, GlobalExceptionHandler)
```

## 프론트엔드 페이지
```
/                → 홈 (GET /api/dashboard 호출)
                   - 오늘의 복습 (남은 개수 + [복습 시작] 버튼)
                   - 누적 통계 (단어 N개, 패턴 N개, 예문 N개, 복습 연속 N일)
                   - 최근 학습 목록 (Day N - 패턴명)
/words           → 단어 목록 + 등록 (단건입력 / JSON벌크 / 이미지업로드) + ⭐중요 토글
                   - 검색 (단어/뜻), 품사별 필터, 중요체크만 보기, 등록일 정렬
/words/[id]      → 단어 상세 (AI보강정보) + 이 단어가 사용된 예문 목록 + [예문 추가] 버튼
/patterns        → 패턴 목록 + 등록 (직접 입력 or 이미지 업로드 → AI 추출 → 확인 → 저장)
/patterns/[id]   → 패턴 상세 (교재 예문 목록) + 이 패턴으로 생성된 예문 목록 + [패턴 예문 생성] (난이도+개수 선택)
/generate        → 난이도 + 개수 선택 → 예문 생성 결과
/review          → 오늘의 복습 카드 ([단어][패턴][문장] 탭 분리, 각 탭에서 독립 복습)
/history         → 날짜별 학습 기록 (패턴 + 단어)
/settings        → 설정 (하루 복습 개수 등)
```

## 구현 순서

### Phase 1: 프로젝트 세팅
1. `docker-compose.yml` — PostgreSQL
2. `backend/` — Spring Boot 초기화 (Web, JPA, PostgreSQL, Validation)
3. `frontend/` — Next.js 초기화
4. CORS 설정
5. GlobalExceptionHandler 기본 구조

### Phase 2: 단어 CRUD (TDD)
1. WordServiceTest → WordService (단건/벌크 등록 + soft delete + 중요 토글)
2. WordControllerTest → WordController (페이지네이션 포함)
3. 프론트 단어 페이지 (직접 입력 + JSON 벌크 + 중요 체크 + 검색/필터)

### Phase 3: 패턴 등록 + 이미지 추출 (TDD)
1. GeminiClient (Vision API + Text API 호출 + retry + fallback)
2. PatternServiceTest → PatternService (등록 시 학습 기록 + review_items 자동 생성 포함)
3. 프론트 패턴 페이지 (직접 입력 + 이미지 업로드 → 추출 결과 확인 → 저장)

### Phase 4: 예문 생성 (TDD)
1. GenerateServiceTest → GenerateService (난이도 + 개수 + 중요/복습기반 단어 선택 + 최대 50개 제한)
2. GenerationHistory 생성 + generated_sentences.generation_id 연결
3. 프론트 생성 페이지 (난이도 + 개수 선택 + 로딩 UI + 결과)
4. 단어 상세 예문 추가 기능

### Phase 5: 복습 시스템 (TDD)
1. ReviewServiceTest → ReviewService (SM-2 커스텀 + 타입별 독립 선정 + 셔플)
2. 단어/패턴/예문 등록 시 → review_items 자동 생성 (WORD/PATTERN: 양방향, SENTENCE: RECOGNITION만)
3. 프론트 복습 페이지 (플립 카드 UI — WORD/PATTERN/SENTENCE 유형별 앞뒤)

### Phase 6: 학습 기록 + 단어 이미지 추출 + 설정 + 대시보드
1. StudyRecord + StudyRecordItem 조회 (패턴+단어 모두)
2. 단어 이미지 추출 API (Gemini Vision + 보강)
3. UserSetting CRUD
4. DashboardService (통계 + streak 계산)
5. 프론트 히스토리 + 설정 + 홈 대시보드

### Phase 7: 통합 마무리
1. 통합 테스트 (TestContainers)
2. soft delete 연쇄 처리 검증 (단어/패턴 삭제 → 예문 + review_items 함께 삭제)
3. Gemini 실패 fallback 테스트
4. UX 정리

## 검증 방법
1. `docker compose up` → PostgreSQL 정상 기동
2. `./gradlew test` → 전체 테스트 통과
3. JSON 벌크 등록 → AI 보강 결과 확인 + 부분 실패 응답 구조 확인
4. 교재 사진 업로드 → 패턴+예문 추출 정상 확인
5. 패턴 직접 입력 등록 → 학습 기록 자동 생성 확인
6. 단어 등록 → 난이도+개수 선택 → 예문 30개 생성 E2E 확인
7. 중요 체크 단어가 예문 생성에 우선 반영되는지 확인
8. 복습 카드 플립 → 기억남/애매/모름 → 간격 조정 확인
9. 복습 [단어] 탭 → WORD 카드만 N개 나오는지, [패턴] 탭 → PATTERN만, [문장] 탭 → SENTENCE만 확인
10. 단어 soft delete → 예문 유지 + 해당 단어 review_items만 soft delete 확인
11. 학습 기록 날짜별 조회 (패턴+단어) + day_number 정확성 확인
12. 설정 변경 → 복습 개수 반영 확인
13. GET /api/dashboard → 통계 + streak 정확성 확인
14. Gemini API 실패 시 fallback 동작 확인

## MVP 제외 (향후)
- TTS/STT 말하기 기능 (Web Speech API, 비용 0)
- 퀴즈/빈칸 테스트
- 즐겨찾기
- 이미지 묘사 학습 — AI(Gemini)로 상황 이미지 생성 → 수준별(초/중/고) 묘사 예문 생성. 등록 단어/패턴과 연계. TOEIC Speaking/OPIC 대비에도 효과적
