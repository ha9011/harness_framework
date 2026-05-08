# Architecture Decision Records

## 철학
검증 가능한 MVP를 우선한다. 사용자 데이터 격리, 복습 알고리즘, Gemini 응답 파싱처럼 제품 신뢰도에 직접 연결되는 부분은 테스트로 고정하고, UI는 제공된 Cozy Cafe 디자인 목업을 실제 사용 흐름에 맞게 구현한다.

---

### ADR-001: 프론트엔드 스택
**결정**: 프론트엔드는 `frontend/`에 Next.js App Router, React, TypeScript strict mode, Tailwind CSS를 사용한다. UI 구성에는 shadcn/ui, 아이콘에는 lucide-react, 서버 상태에는 TanStack Query, 폼에는 React Hook Form과 Zod를 사용한다.

**이유**: Next.js App Router는 페이지 기반 학습 앱을 안정적으로 구성하기 좋고, Tailwind와 shadcn/ui는 제공된 모바일 목업의 컴포넌트를 빠르게 재현할 수 있다. TanStack Query는 인증 Cookie 기반 API 호출, 로딩, 에러, 캐시 무효화를 명확하게 관리한다.

**트레이드오프**: 초기 의존성이 늘어난다. 대신 폼 검증, 서버 상태, UI 패턴을 직접 구현하는 비용을 줄인다.

### ADR-002: 백엔드 스택
**결정**: 백엔드는 `backend/`에 Java 21, Spring Boot, Gradle, Spring Web MVC, Spring Data JPA, Spring Security, Bean Validation을 사용한다.

**이유**: 인증/인가, REST API, 데이터 격리, 테스트가 중요한 애플리케이션이므로 Spring 생태계의 표준 기능을 활용한다. Gradle은 루트 `package.json`의 백엔드 실행 스크립트 방향과 맞춘다.

**트레이드오프**: Node 기반 백엔드보다 초기 설정이 무겁다. 대신 타입 안정성, 보안 구성, 테스트 도구가 성숙하다.

### ADR-003: 데이터베이스와 스키마 관리
**결정**: PostgreSQL 16을 사용하고, JPA는 ORM/Repository 구현에 사용하며, Flyway는 DB 스키마 변경 이력 관리에 사용한다.

**이유**: JPA는 도메인 모델과 Repository 구현 생산성을 높이고, Flyway는 `V1__init.sql` 같은 명시적 migration으로 운영 가능한 스키마 이력을 남긴다.

**트레이드오프**: Entity와 migration이 서로 어긋나지 않게 관리해야 한다. 테스트에서 migration 기반 스키마를 검증한다.

### ADR-004: 인증과 사용자별 데이터 격리
**결정**: Spring Security와 JWT HttpOnly Cookie를 사용한다. 회원가입/로그인 외 모든 API는 인증 필수이며, 모든 학습 데이터는 `user_id`로 소유자를 검증한다.

**이유**: 브라우저 기반 앱에서 토큰을 JavaScript로 직접 다루지 않기 위해 HttpOnly Cookie를 사용한다. 사용자 데이터가 핵심 자산이므로 IDOR 방어를 API 레이어와 서비스 레이어에서 모두 고려한다.

**트레이드오프**: CORS와 Cookie 설정이 복잡해진다. 대신 XSS 상황에서 토큰 탈취 위험을 낮춘다.

### ADR-005: Gemini 연동 방식
**결정**: Gemini Vision/Text API는 `external-client` 성격의 클라이언트로 분리하고, structured JSON output과 schema validation을 사용한다. 실제 네트워크 호출은 fake client/fake server 테스트로 대체해 계약을 검증한다.

**이유**: 단어 추출, 패턴 추출, 예문 생성은 외부 응답 품질에 의존하므로 요청 구성, 응답 파싱, 재시도, fallback을 독립적으로 검증해야 한다.

**트레이드오프**: 초기 인터페이스 설계가 필요하다. 대신 서비스 테스트에서 외부 API 불안정성에 영향받지 않는다.

### ADR-006: 복습 알고리즘
**결정**: 원본 SM-2를 EASY/MEDIUM/HARD 3단계로 단순화한 커스텀 알고리즘을 사용한다.

**이유**: 사용자는 카드마다 빠르게 응답해야 하므로 3단계가 모바일 학습 흐름에 적합하다. interval_days와 ease_factor를 저장하면 장기 확장이 가능하다.

**트레이드오프**: 원본 SM-2의 세밀한 품질 점수는 쓰지 않는다. 대신 구현과 UX가 단순해진다.

### ADR-007: UI 컨셉
**결정**: `design/` 목업의 Cozy Cafe & Coffee Tree 컨셉을 기본 디자인 시스템으로 사용한다.

**이유**: 영어 학습을 부담스러운 시험 앱이 아니라 매일 반복 가능한 따뜻한 루틴으로 느끼게 한다. 커피나무 성장 상태는 복습 연속일과 연결되는 동기 부여 요소다.

**트레이드오프**: 일반적인 SaaS 대시보드보다 시각 요소가 많다. 단, 학습 흐름을 방해하지 않도록 모바일 밀도와 명확한 CTA를 우선한다.
