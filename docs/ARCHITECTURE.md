# 아키텍처

## 디렉토리 구조
```
harness_framework/
├── docker-compose.yml          # PostgreSQL 16
├── backend/                    # Spring Boot + JPA
│   └── src/main/java/com/english/
│       ├── word/               # 단어 CRUD + Gemini 보강
│       ├── pattern/            # 패턴 CRUD + 이미지 추출
│       ├── generate/           # 예문 생성 + GeminiClient
│       ├── review/             # 복습 (SM-2 + 카드 선정)
│       ├── study/              # 학습 기록
│       ├── dashboard/          # 대시보드 통계
│       ├── setting/            # 사용자 설정
│       └── config/             # CORS, GlobalExceptionHandler
├── frontend/                   # Next.js (App Router)
│   └── app/
│       ├── page.tsx            # 홈 대시보드
│       ├── words/              # 단어 목록 + 상세
│       ├── patterns/           # 패턴 목록 + 상세
│       ├── generate/           # 예문 생성
│       ├── review/             # 복습 카드 ([단어][패턴][문장] 탭)
│       ├── history/            # 학습 기록
│       ├── settings/           # 설정
│       ├── components/         # 공통 컴포넌트
│       └── lib/api.ts          # 백엔드 API 호출
├── design/                     # 디자인 목업 (JSX + HTML)
├── docs/                       # 프로젝트 문서
├── phases/                     # harness 실행 메타데이터
└── harness/                    # harness 프레임워크
```

## 패턴
- **백엔드**: 도메인별 패키지 (word/, pattern/, generate/, review/, study/, setting/)
  - Entity → Repository → Service → Controller → DTO 계층
  - TDD: ServiceTest → Service, ControllerTest → Controller 순서
- **프론트엔드**: Next.js App Router
  - 페이지 컴포넌트: Server Component 기본
  - 인터랙션 필요한 곳(카드 플립, 탭 전환): Client Component ("use client")
  - API 호출: lib/api.ts에서 fetch 래핑

## 데이터 흐름
```
[프론트엔드]                    [백엔드]                    [외부]
사용자 입력                                               
  ↓                                                      
Next.js Client Component                                  
  ↓ fetch()                                              
  → → → →  Spring Boot Controller                        
              ↓ Service                                  
              ↓ (Gemini 필요시) → → GeminiClient → → Gemini API
              ↓                           ← ← JSON 응답
              ↓ Repository                                
              ↓ JPA → PostgreSQL                         
              ↓                                          
           ← ← ← ← JSON 응답                            
  ↓                                                      
UI 업데이트                                               
```

## 환경 설정
```
# 환경 변수 (backend 실행 전 필수)
GEMINI_API_KEY=your-api-key-here

# backend/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/english_app
    username: app
    password: app1234
  jpa:
    hibernate:
      ddl-auto: update    # 개발: Entity 기반 자동 스키마 생성
    show-sql: true

gemini:
  api-key: ${GEMINI_API_KEY}

# CORS (CorsConfig.java)
허용 origin: http://localhost:3000
허용 methods: GET, POST, PUT, PATCH, DELETE
```

## 데이터 모델 (12개 테이블)
> 상세 스키마는 DESIGN.md 참조. 여기는 관계 요약만.

```
words ──┬── generated_sentence_words ──── generated_sentences ──┬── sentence_situations (5개/예문)
        │                                        │              │
        │                                  generation_history    │
        │                                        │              │
patterns ──── pattern_examples (교재 예문 10개)    │              │
                                                  │              │
                                           (generation 시        │
                                            word_id/pattern_id)  │
                                                                 │
study_records ──── study_record_items (WORD/PATTERN)             │
                                                                 │
review_items ──── review_logs ← SM-2 결과 기록                    │
  ↑ (WORD/PATTERN: 양방향, SENTENCE: RECOGNITION만)              │
  └── 자동 생성 (단어/패턴/예문 등록 시)                            │

user_settings (daily_review_count 등)

⚠️ review_items, study_record_items는 Polymorphic Association (item_type + item_id)
   → DB FK 없음, Service에서 item_id 유효성 검증 필수
```

## 에러 처리 흐름
```
[프론트엔드]                [백엔드]                           [외부]

사용자 요청
  ↓ fetch()
  → → → →  Controller
              ↓ Service
              ↓ → → GeminiClient → → Gemini API
              ↓                      ← ← 실패 (timeout/500)
              ↓ GeminiClient: 재시도 1회(1초 후) → 재시도 2회(3초 후) → 실패 확정
              ↓
              ├─ 단어 보강 실패 → 보강 없이 저장 (word+meaning만)
              ├─ 이미지 추출 실패 → 502 + AI_SERVICE_ERROR
              └─ 예문 생성 실패 → 502 + AI_SERVICE_ERROR
              ↓
           ← ← ← ← JSON 에러 응답
  ↓
프론트: 에러 토스트 표시 + 재시도 안내

GlobalExceptionHandler:
  - DuplicateException → 409 DUPLICATE
  - EmptyRequestException → 400 EMPTY_REQUEST
  - NoWordsException → 400 NO_WORDS
  - NoPatternsException → 400 NO_PATTERNS
  - InvalidImageException → 400 INVALID_IMAGE_FORMAT
  - GeminiException → 502 AI_SERVICE_ERROR
  - 기타 → 500 INTERNAL_ERROR
```

## 상태 관리
- **서버 상태**: Spring Boot + JPA (PostgreSQL). 모든 비즈니스 데이터는 DB에 저장
- **클라이언트 상태**: React useState/useReducer
  - 카드 플립 상태, 탭 선택, 폼 입력 등 UI 상태만 클라이언트에서 관리
  - "처음부터 다시" 복습 시 기존 카드 데이터를 클라이언트에서 재사용 (API 재호출 X)
