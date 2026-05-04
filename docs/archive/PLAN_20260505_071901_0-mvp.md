# PLAN: 영어 패턴 학습기 MVP 구현

## 작업 목표
영어 단어×패턴 AI 예문 생성 + SM-2 간격 반복 복습 웹 서비스의 전체 MVP 구현

## 구현할 기능
1. 단어 CRUD (단건/벌크/이미지 추출 + AI 보강)
2. 패턴 CRUD (직접 입력/이미지 추출)
3. 예문 생성 (일반/단어 지정/패턴 지정, 난이도 4단계)
4. 복습 시스템 (SM-2 + 탭 분리 + 처음부터 다시/추가 복습)
5. 학습 기록 자동 생성
6. 홈 대시보드 (통계 + 커피나무 성장)
7. 설정 (복습 개수)

## 기술적 제약사항
- TDD: 테스트 먼저 작성, 테스트 통과하는 구현 작성
- Gemini API 실패 시 fallback (보강 없이 저장, 초기 시도(즉시) + 1초 후 재시도 + 3초 후 재시도 = 총 3회)
- Polymorphic association (review_items, study_record_items): item_type + item_id 패턴. DB FK 걸 수 없으므로 **Service 레이어에서 item_id가 해당 타입 테이블에 실제 존재하는지 검증** 필수
- 모든 삭제는 soft delete
- 단어/패턴 삭제 시 예문은 유지, 해당 타입 review_items만 삭제 (SENTENCE 카드 유지)

## 핵심 알고리즘 (구현 시 참조)

### SM-2 기반 커스텀 간격 반복
```
초기값: interval_days = 1, ease_factor = 2.5

EASY   → new_interval = Math.round(interval × ease_factor × 1.3)
          ease_factor += 0.15
MEDIUM → new_interval = Math.round(interval × ease_factor)
          ease_factor 유지
HARD   → new_interval = 1 (리셋)
          ease_factor = Math.max(1.3, ease_factor - 0.2)

next_review_date = 오늘 + new_interval
```

### 복습 카드 선정 (타입별 독립)
```
입력: type (WORD/PATTERN/SENTENCE), exclude (제외 ID 목록)

1. WHERE next_review_date <= 오늘 AND deleted=false AND item_type=type AND id NOT IN (exclude)
2. ORDER BY (이 순서대로 적용):
   ① last_result = 'HARD' 인 것 먼저
   ② last_reviewed_at 오래된 순 (ASC)
   ③ review_count 낮은 순 (ASC)
3. LIMIT N (daily_review_count, 부족하면 있는 만큼)
4. 랜덤 셔플 후 반환
```

### 예문 생성 시 단어 선택
```
1. deleted=false인 단어 전체 조회
2. 정렬: is_important=true 먼저 → review_count 낮은 순(WORD RECOGNITION) → 랜덤
3. 상위 최대 50개 선택
4. Gemini 프롬프트에 [{"id":1, "word":"...", "meaning":"..."}] 전달
5. Gemini JSON 응답 파싱
6. 응답의 wordIds가 DB에 실제 존재하는지 검증 → 없는 ID는 매핑(generated_sentence_words)에서 제외, 예문 자체는 저장
```

### 구현 시 자주 헷갈리는 규칙
```
WORD 인식 카드 뒷면 예문:
  - 서버가 generated_sentence_words에서 해당 word_id를 포함한 예문 조회
  - 3개 이하면 전부, 4개 이상이면 랜덤 3개 선택하여 examples 배열로 반환

SENTENCE 카드 앞면 situation:
  - 서버가 sentence_situations에서 해당 sentence_id의 5개 중 랜덤 1개 선택하여 반환
  - 클라이언트는 받은 1개만 표시 (선택 로직 없음)

streak (복습 연속일):
  - review_logs에 해당 날짜의 기록이 1개라도 있으면 "복습한 날"로 카운트
  - 타입 구분 없음 (WORD만 해도 streak 유지)

Gemini 재시도:
  - 총 3회 시도: 초기 시도(즉시) → 1초 후 재시도 → 3초 후 재시도
  - 3회 모두 실패 시 fallback (보강 없이 저장 또는 에러 반환)

"처음부터 다시":
  - 프론트에서 현재 state에 저장된 카드 배열을 인덱스 0부터 다시 표시
  - 새로고침 시 데이터 날아감 → 다시 API 호출해서 새 카드 받으면 됨
  - localStorage 저장 불필요 (MVP)

day_number 계산:
  - SELECT COALESCE(MAX(day_number), 0) + 1 FROM study_records
  - soft delete된 레코드도 MAX에 포함됨 (study_records에는 deleted 컬럼 없음)
```

### 핵심 테이블 구조 요약
```
generation_history: id, level, requested_count, actual_count, word_id(nullable), pattern_id(nullable), created_at
  - word_id: POST /api/generate/word 시 지정된 단어 ID
  - pattern_id: POST /api/generate/pattern 시 지정된 패턴 ID
  - 일반 생성(POST /api/generate): 둘 다 null

review_items: id, item_type(WORD/PATTERN/SENTENCE), item_id, direction(RECOGNITION/RECALL), deleted, next_review_date, interval_days, ease_factor, review_count, last_result, last_reviewed_at
  - UNIQUE(item_type, item_id, direction)
  - WORD/PATTERN: RECOGNITION + RECALL 2개씩
  - SENTENCE: RECOGNITION만 1개

study_record_items: id, study_record_id, item_type(WORD/PATTERN), item_id
  - UNIQUE(study_record_id, item_type, item_id)
```

## 테스트 전략
- 기존 테스트 영향: 없음 (신규 프로젝트)
- 신규 테스트: 각 Step의 Service + Controller 테스트
- TestContainers: Step 6에서 통합 테스트
- 엣지케이스 테스트: 빈 배열 요청, 중복 등록, 단어/패턴 0개 생성, Gemini 실패 fallback

## Phase/Step 초안

### Step 0: 프로젝트 세팅 + 공통 인프라
- 작업:
  - docker-compose.yml (PostgreSQL 16)
  - Spring Boot 초기화 (Web, JPA, PostgreSQL, Validation)
  - Next.js 초기화 (TypeScript, Tailwind CSS)
  - CORS 설정 (localhost:3000 → localhost:8080 허용)
  - GlobalExceptionHandler (DuplicateException, EmptyRequestException, GeminiException 등)
  - **GeminiClient** (Vision + Text + 총 3회 시도: 즉시→1초→3초 + fallback)
  - GeminiClient 구현 시: `response_mime_type="application/json"` + `response_schema` 설정 필수
  - **StudyRecordService** 기본 구조 (등록 시 자동 학습 기록 생성)
  - application.yml에 Gemini API 키 설정: `gemini.api-key: ${GEMINI_API_KEY}`
  - DB 스키마: `spring.jpa.hibernate.ddl-auto=update` (개발 환경)
- 산출물: `docker compose up` + `cd backend && ./gradlew build` + `cd frontend && npm run build` 성공
- 성공 기준:
  - PostgreSQL 컨테이너 정상 기동 (포트 5432)
  - Spring Boot 앱 기동 시 DB 연결 성공 + 테이블 자동 생성
  - Next.js dev 서버 정상 기동 (포트 3000)
  - CORS로 localhost:3000 → localhost:8080 요청 가능
  - GeminiClient: Gemini API 호출 + JSON 파싱 + 실패 시 재시도 테스트 통과
  - 환경 변수: `GEMINI_API_KEY` 미설정 시 명확한 에러 메시지

### Step 1: 단어 CRUD (TDD)
- 선행: Step 0 완료 (GeminiClient, StudyRecordService 사용 가능)
- 작업: WordServiceTest → WordService, WordControllerTest → WordController, 프론트 단어 페이지
- 성공 기준:
  - 단건 등록: POST /api/words → 201 + AI 보강 정보
  - 벌크 등록: POST /api/words/bulk → 201 + saved/skipped/enrichmentFailed
  - 목록 조회: GET /api/words?page=0&size=20 → 페이지네이션 응답
  - 검색/필터: search, partOfSpeech, importantOnly, sort 파라미터 동작
  - 상세 조회: GET /api/words/{id} → 단어 + 예문 목록
  - 중요 토글: PATCH /api/words/{id}/important → isImportant 반전
  - soft delete: DELETE /api/words/{id} → WORD review_items만 삭제, 예문 유지
  - 엣지케이스:
    - 중복 단어 등록 → 409 DUPLICATE
    - 빈 배열 벌크 → 400 EMPTY_REQUEST
    - 존재하지 않는 ID 조회 → 404 NOT_FOUND
    - 단어 등록 시 study_records에 오늘 날짜 레코드 자동 생성 확인
    - 단어 등록 시 study_record_items에 (WORD, word_id) 추가 확인
    - 단어 등록 시 review_items 2개(RECOGNITION+RECALL) 자동 생성 확인

### Step 2: 패턴 등록 + 이미지 추출 (TDD)
- 선행: Step 1 완료
- 작업: Step 0의 GeminiClient 활용하여 PatternServiceTest → PatternService, 프론트 패턴 페이지
- 성공 기준:
  - 직접 등록: POST /api/patterns → 201 + 교재 예문 순서 보존
  - 이미지 추출: POST /api/patterns/extract → 200 + 패턴+예문 JSON
  - 단어 이미지 추출: POST /api/words/extract → 200 + 단어 목록 JSON
  - soft delete: DELETE /api/patterns/{id} → PATTERN review_items만 삭제
  - 등록 시 학습 기록 자동 생성 (study_records + study_record_items)
  - 등록 시 review_items 자동 생성 (RECOGNITION + RECALL)
  - 엣지케이스:
    - 중복 패턴 → 409 DUPLICATE
    - 이미지 형식 오류 → 400 INVALID_IMAGE_FORMAT
    - 추출 결과 없음 → 200 + 빈 객체 {}
    - Gemini 장애 → 502 AI_SERVICE_ERROR (총 3회 시도 후)
    - Gemini 보강 실패 → 보강 없이 저장

### Step 3: 예문 생성 (TDD)
- 선행: Step 2 완료 (단어+패턴 데이터 존재)
- 작업: GenerateServiceTest → GenerateService, GenerationHistory 저장, 프론트 생성 페이지
- 참고: DESIGN.md의 "난이도 기준" 테이블 (유아=3~5단어, 초등=카페주문, 중등=카톡, 고등=회사대화) + "예문 표시 방식" (영어+상황 앞면, 해석은 탭시 펼침)
- 성공 기준:
  - 일반 생성: POST /api/generate → 201 + generationId + sentences 배열
  - 단어 지정: POST /api/generate/word → 해당 단어 포함 예문
  - 패턴 지정: POST /api/generate/pattern → 해당 패턴으로만 예문
  - 생성 이력: GET /api/generate/history → 페이지네이션
  - 예문마다 상황(situation) 5개 저장
  - 예문 등록 시 review_items 자동 생성 (SENTENCE RECOGNITION만)
  - 단어 선택 우선순위: ⭐중요 > 복습 적은 것(WORD RECOGNITION review_count) > 랜덤
  - 최대 50개 단어 선택 (Gemini 토큰 한도)
  - 엣지케이스:
    - 단어 0개 → 400 NO_WORDS
    - 패턴 0개 → 400 NO_PATTERNS
    - Gemini가 잘못된 word_id 반환 → 예문 저장, 매핑만 무시
    - 요청 30개인데 25개만 생성 → generation_history에 actual_count=25 기록

### Step 4: 복습 시스템 (TDD)
- 선행: Step 3 완료 (review_items 데이터 존재)
- 작업: ReviewServiceTest → ReviewService, 프론트 복습 페이지
- 산출물: 복습 카드 선정 → 응답 → SM-2 간격 조정 전체 흐름
- 성공 기준:
  - 카드 선정: GET /api/reviews/today?type=WORD → N개 카드 반환
  - SM-2 적용: POST /api/reviews/{id} → nextReviewDate + intervalDays
    - EASY: interval × ease_factor × 1.3, ease_factor += 0.15
    - MEDIUM: interval × ease_factor
    - HARD: interval = 1, ease_factor = max(1.3, ef - 0.2)
  - 우선순위: HARD 먼저 → 오래된 순 → 복습 적은 순
  - 탭 분리: type=WORD/PATTERN/SENTENCE 각각 독립
  - 추가 복습: exclude 파라미터로 이미 한 카드 제외
  - 처음부터 다시: 프론트에서 읽기 전용 재표시 (API 호출 X)
  - SENTENCE 카드 front에 situation 포함 (서버가 5개 중 랜덤 1개)
  - WORD 인식 카드 back에 examples 배열 (최대 2~3개 랜덤)
  - 엣지케이스:
    - 복습 대상 0개 → 빈 배열 반환
    - 이미 모두 복습 완료 후 추가 복습 → 빈 배열
    - next_review_date가 미래인 카드 → 선정 안 됨
    - deleted=true인 review_items → 선정 안 됨

### Step 5: 학습 기록 + 설정 + 대시보드
- 선행: Step 4 완료
- 작업: StudyRecordService, UserSettingService, DashboardService, 프론트 홈/기록/설정 페이지
- 산출물: 대시보드 + 학습 기록 + 설정 변경 동작
- 성공 기준:
  - 학습 기록: GET /api/study-records → 최신순, 페이지네이션
  - day_number: MAX(day_number) + 1로 계산 (학습한 날만 카운트)
  - 설정 조회/변경: GET/PUT /api/settings
  - 대시보드: GET /api/dashboard → wordCount, patternCount, sentenceCount, streak, todayReviewRemaining(타입별), recentStudyRecords
  - streak 계산: 오늘/어제부터 역순 연속 카운트
  - 커피나무 성장: streak 기반 SVG 렌더링 (design/coffee-tree.jsx 참고)
  - 엣지케이스:
    - 첫 사용자 (모든 카운트 0) → 빈 상태 UI + "단어 등록하러 가기"
    - streak 끊김 → 시들음 표시
    - 설정 변경 후 즉시 반영 (다음 복습부터)

### Step 6: 통합 마무리
- 선행: Step 0~5 전부 완료
- 작업: 통합 테스트 (TestContainers), soft delete 연쇄 검증, Gemini fallback 테스트, UX 정리
- 산출물: 전체 E2E 검증 통과
- 성공 기준:
  - docker compose up → ./gradlew test → 전체 통과
  - 단어 등록 → 예문 생성 → 복습 → 간격 조정 E2E 흐름
  - 단어 삭제 → WORD review_items 삭제 + 예문/SENTENCE 카드 유지 확인
  - Gemini 타임아웃 시 보강 없이 저장 확인
  - 빈 상태에서 대시보드 정상 표시 확인

## 미결 사항
- 없음 (설계 문서 6차 검토 완료)
