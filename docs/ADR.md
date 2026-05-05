# Architecture Decision Records

## 철학
MVP 속도 최우선. TDD 기반 단계적 구현. 설계 문서(DESIGN.md)가 단일 진실 소스(SSOT).

---

### ADR-001: Next.js App Router + Spring Boot 분리 아키텍처
**결정**: 프론트엔드는 Next.js (App Router), 백엔드는 Spring Boot + JPA로 분리
**이유**: 프론트/백 독립 개발 가능. Spring Boot의 JPA + PostgreSQL 생태계가 성숙. Next.js의 App Router가 모바일 반응형에 적합
**트레이드오프**: 모노리스 대비 배포 복잡도 증가. CORS 설정 필요

### ADR-002: PostgreSQL 16 + Docker
**결정**: PostgreSQL 16을 Docker 컨테이너로 운영
**이유**: 로컬 개발 환경 일관성. docker-compose up 한 줄로 DB 기동. TestContainers로 통합 테스트 지원
**트레이드오프**: Docker 의존성 추가. SQLite 대비 초기 세팅 비용

### ADR-003: Gemini API (Vision + Text Generation)
**결정**: AI 기능 전체를 Google Gemini API로 통일
**이유**: Vision API(이미지 추출) + Text Generation(예문 생성/보강)을 하나의 API로 해결. response_mime_type으로 JSON 응답 강제 가능
**트레이드오프**: Gemini 의존도 높음. 장애 시 핵심 기능 마비 → fallback 정책 수립 (보강 실패 시 저장, 초기 1회 + 재시도 2회 = 총 3회, exponential backoff 1초→3초)

### ADR-004: Polymorphic Association (review_items, study_record_items)
**결정**: item_type + item_id 패턴으로 다형성 구현. DB FK 대신 애플리케이션 레벨 검증
**이유**: WORD/PATTERN/SENTENCE 3가지 타입을 하나의 테이블로 관리하여 복습 로직 단순화
**트레이드오프**: DB 레벨 FK 제약 불가 → 고아 레코드 위험. MVP에서 허용, 장기적으로 테이블 분리 검토

### ADR-005: SM-2 기반 커스텀 간격 반복
**결정**: 원본 SM-2(6단계)를 3단계(EASY/MEDIUM/HARD)로 단순화
**이유**: 사용자에게 6단계 선택은 부담. 3단계(기억남/애매/모름)가 직관적
**트레이드오프**: 원본 SM-2 대비 정밀도 감소. "SM-2"라 명칭하지만 커스텀 변형임을 문서화

### ADR-006: 복습 탭 분리 (단어/패턴/문장 독립)
**결정**: 복습 화면을 [단어][패턴][문장] 탭으로 분리. 각 탭에서 독립적으로 N개씩 선정
**이유**: 사용자가 원하는 타입만 골라서 복습 가능. 타입 간 간섭 없음
**트레이드오프**: 전체 섞기 대비 UX 복잡도 약간 증가

### ADR-007: JWT + HttpOnly Cookie 인증
**결정**: 인증 방식으로 JWT를 HttpOnly Cookie에 저장. Access Token만 사용 (24시간 만료)
**이유**: SPA + REST API 구조에 자연스러운 stateless 인증. HttpOnly Cookie는 localStorage 대비 XSS 공격 시 토큰 탈취 불가. 브라우저가 Cookie를 자동 전송하므로 프론트엔드 토큰 관리 불필요. `credentials: "include"` 추가만으로 동작
**트레이드오프**: Refresh Token 미사용 → 24시간 후 재로그인 필요 (MVP 허용). CSRF 위험은 SameSite=Lax + API-only 경로로 완화. Session 방식 대비 서버 상태 불필요 (Redis/DB 세션 저장소 없음)

### ADR-008: 전체 Entity에 user_id FK 추가 (사용자별 데이터 격리)
**결정**: Word, Pattern, GeneratedSentence, GenerationHistory, ReviewItem, StudyRecord, UserSetting 7개 Entity에 user_id FK 추가. 자식 Entity(PatternExample, SentenceSituation, GeneratedSentenceWord, StudyRecordItem)는 부모를 통해 접근하므로 미추가. ReviewLog는 reviewItemId를 @ManyToOne으로 변경하여 JPQL JOIN 지원
**이유**: 다중 사용자 지원 시 데이터 격리 필수. 인증과 동시에 구현하여 일관된 아키텍처 유지
**트레이드오프**: 변경 범위 큼 (7 Entity + 8 Repository + 8 Service + 7 Controller + 테스트 전체). 기존 DB 데이터 삭제 후 재시작 (`docker compose down -v && docker compose up`, ddl-auto: update 유지). DB UNIQUE 제약은 제거하고 앱 레벨 검증으로 전환 (soft delete 재등록 호환)
