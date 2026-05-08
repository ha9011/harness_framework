# 아키텍처

## 디렉토리 구조
```
backend/                         # Spring Boot 백엔드 코드
  src/main/java/com/english/
    auth/                        # 회원가입, 로그인, JWT, 사용자
    word/                        # 단어 Entity/Repository/Service/Controller/DTO
    pattern/                     # 패턴, 교재 예문
    generate/                    # 예문 생성, Gemini client, 생성 이력
    review/                      # 복습 카드, SM-2, 복습 로그
    study/                       # 학습 기록
    dashboard/                   # 홈 통계
    setting/                     # 사용자 설정
    config/                      # Security, CORS, exception handling
  src/main/resources/db/migration/ # Flyway migration
  src/test/                      # JUnit5, MockMvc, Testcontainers

frontend/                        # Next.js App Router 프론트엔드 코드
  app/
    page.tsx                     # 홈 대시보드
    login/page.tsx               # 로그인
    signup/page.tsx              # 회원가입
    words/page.tsx               # 단어 목록 + 등록
    words/[id]/page.tsx          # 단어 상세
    patterns/page.tsx            # 패턴 목록 + 등록
    patterns/[id]/page.tsx       # 패턴 상세
    generate/page.tsx            # 예문 생성
    review/page.tsx              # 복습 카드
    history/page.tsx             # 학습 기록
    settings/page.tsx            # 설정
    components/                  # 화면/공통 컴포넌트
    lib/                         # API client, auth context, query helpers

design/                          # HTML/JSX 디자인 목업 시안
docs/                            # 프로젝트 문서
phases/                          # harness 실행 메타데이터
harness/                         # harness 프레임워크
```

## 백엔드 패턴
- Controller는 HTTP 요청/응답, 인증 사용자 추출, DTO validation만 담당한다.
- Service는 비즈니스 규칙, 사용자 소유권 검증, soft delete, review_items 자동 생성, 학습 기록 자동 생성을 담당한다.
- Repository는 Spring Data JPA 기반으로 구성한다.
- Flyway migration은 테이블, 인덱스, 제약 조건의 단일 기준이다.
- JPA Entity는 migration과 일치해야 하며, 테스트에서 실제 PostgreSQL/Testcontainers 기반으로 검증한다.
- Gemini 호출은 `generate/GeminiClient` 인터페이스 뒤로 숨기고, 서비스는 인터페이스만 의존한다.
- 모든 목록 API는 `page`, `size` 기반 페이지네이션 응답을 사용한다.

## 프론트엔드 패턴
- App Router를 사용하며 화면 단위 라우트는 `app/` 아래에 둔다.
- API 호출은 `app/lib/api.ts`에서 `credentials: "include"`를 기본값으로 둔다.
- 서버 상태는 TanStack Query로 관리하고, mutation 성공 시 관련 query를 무효화한다.
- 폼 상태는 React Hook Form으로 관리하고, 입력 검증은 Zod schema로 고정한다.
- 공통 UI는 shadcn/ui 기반 컴포넌트와 `design/` 목업에서 추출한 Cozy Cafe 토큰을 함께 사용한다.
- 인증 상태는 `AuthProvider`와 `useAuth`에서 관리한다.
- 미로그인 사용자는 로그인/회원가입 외 페이지 접근 시 `/login`으로 리다이렉트한다.

## 데이터 흐름

### 인증
```
회원가입/로그인 폼
→ POST /api/auth/signup 또는 /api/auth/login
→ Spring Security 인증 처리
→ JWT HttpOnly Cookie 발급
→ GET /api/auth/me로 현재 사용자 확인
→ AuthProvider가 사용자 상태 갱신
```

### 단어/패턴 등록
```
사용자 입력 또는 이미지 업로드
→ 프론트 폼 검증
→ 직접 등록 API 또는 extract API 호출
→ Gemini 추출/보강 결과를 사용자 확인 폼에 표시
→ 저장 API 호출
→ backend Service가 중복 검사, 저장, 학습 기록 생성, review_items 생성
→ TanStack Query 캐시 무효화
→ 목록/상세 UI 갱신
```

### 예문 생성
```
난이도/개수 선택
→ POST /api/generate 또는 상세 전용 generate API 호출
→ backend Service가 사용자 소유 단어/패턴 조회
→ 중요 단어, 복습 횟수, 랜덤 기준으로 단어 후보 선정
→ GeminiClient가 structured JSON 응답 요청
→ wordId/patternId 유효성 검증
→ generation_history, generated_sentences, sentence_situations 저장
→ SENTENCE review_items 생성
→ 생성 결과 반환
```

### 복습
```
복습 탭 선택
→ GET /api/reviews/today?type=WORD|PATTERN|SENTENCE
→ user_settings.daily_review_count 기준으로 타입별 카드 선정
→ 카드 플립 후 EASY/MEDIUM/HARD 선택
→ POST /api/reviews/{reviewItemId}
→ ReviewService가 review_logs 저장 및 SM-2 값 갱신
→ 탭 진행률과 남은 카드 갱신
```

## 상태 관리
- 서버 상태: TanStack Query
  - 사용자: `["auth", "me"]`
  - 단어 목록/상세: `["words", filters]`, `["word", id]`
  - 패턴 목록/상세: `["patterns", filters]`, `["pattern", id]`
  - 대시보드: `["dashboard"]`
  - 복습 카드: `["reviews", type, exclude]`
  - 설정: `["settings"]`
- 폼 상태: React Hook Form
  - 로그인/회원가입
  - 단어 단건/벌크 저장
  - 패턴 직접 등록
  - 예문 생성 조건
  - 설정 변경
- 로컬 UI 상태: `useState` 또는 `useReducer`
  - 복습 카드 flip 여부
  - 현재 탭과 카드 index
  - 읽기 전용 다시보기 모드
  - 모달/드롭다운 열림 상태

## 데이터 소유권 규칙
- 모든 사용자 데이터 테이블은 `user_id`를 가진다.
- 조회, 수정, 삭제, 생성 연관 작업은 현재 로그인 사용자의 `user_id`로 제한한다.
- 다른 사용자의 리소스를 직접 ID로 요청하면 403을 반환한다.
- 같은 단어/패턴은 사용자별로 독립 등록할 수 있다.
- soft delete된 단어/패턴/예문은 기본 목록과 복습 대상에서 제외한다.

## 외부 연동 규칙
- 업로드 이미지는 Gemini 추출 요청에만 사용하고 저장하지 않는다.
- Gemini API Key는 백엔드 환경 변수 `GEMINI_API_KEY`로 주입한다.
- `GEMINI_API_KEY`가 없거나 Gemini 호출이 실패하면 기능별 fallback 정책을 따른다.
- 외부 연동 step의 테스트는 실제 Gemini 네트워크 호출을 하지 않는다.

## 보안과 CORS
- JWT Cookie는 HttpOnly, Path `/api`, SameSite Lax, 24시간 만료를 기본값으로 한다.
- 프론트와 백엔드가 다른 origin에서 실행될 수 있으므로 CORS는 정확한 origin과 `allowCredentials(true)`를 사용한다.
- 와일드카드 origin과 credentials 조합은 금지한다.
- 로그인 실패 메시지는 이메일/비밀번호 구분 없이 동일하게 반환한다.
