# 프로젝트: 영어 패턴 학습기

## 기술 스택
- Frontend: Next.js App Router, React, TypeScript strict mode, Tailwind CSS
- Frontend Libraries: shadcn/ui, lucide-react, TanStack Query, React Hook Form, Zod
- Frontend Test: Vitest, Testing Library, MSW
- Backend: Java 21, Spring Boot, Gradle, Spring Web MVC, Spring Data JPA, Spring Security, Bean Validation
- Backend DB: PostgreSQL 16, Flyway migration
- Backend Test: JUnit5, MockMvc, Testcontainers, AssertJ
- AI: Gemini API Vision/Text, structured JSON output, fake client 기반 테스트
- Auth: JWT HttpOnly Cookie, BCrypt, 사용자별 데이터 격리

## 아키텍처 규칙
- CRITICAL: 백엔드 코드는 반드시 `backend/` 폴더에서 작성할 것
- CRITICAL: 프론트엔드 코드는 반드시 `frontend/` 폴더에서 작성할 것
- CRITICAL: 루트 디렉토리에 직접 소스 코드를 두지 말 것
- CRITICAL: DB 스키마 변경은 `backend/src/main/resources/db/migration/`의 Flyway migration으로 관리할 것
- CRITICAL: Gemini API 연동은 백엔드 external client로 분리하고, 서비스 테스트에서 실제 네트워크를 호출하지 말 것
- CRITICAL: 모든 사용자 학습 데이터는 현재 로그인 사용자의 `user_id`로 조회/수정/삭제 범위를 제한할 것
- CRITICAL: `/api/auth/signup`, `/api/auth/login` 외 모든 API는 인증을 요구할 것
- `integration-hardening` 목적이 아닌 작업에서 `backend/`와 `frontend/`를 동시에 수정하지 말 것
- 삭제는 soft delete를 기본으로 하며, 단어/패턴 삭제 시 예문은 유지하고 관련 review_items만 비활성화할 것
- 업로드 이미지는 Gemini 추출 요청에만 사용하고 저장하지 말 것

## 개발 프로세스
- CRITICAL: 새 기능 구현 시 반드시 테스트를 먼저 작성하고, 테스트가 통과하는 구현을 작성할 것 (TDD)
- 커밋 메시지는 conventional commits 형식을 따를 것 (feat:, fix:, docs:, refactor:)
- 기존 테스트의 기대값을 변경하지 말 것. 불가피하면 PRD/ADR 근거와 사유를 명시할 것
- 외부 연동은 fake provider/fake server 테스트로 request 구성, 인증 정보 주입, 응답 파싱, 실패 처리를 검증할 것

## 명령어
npm run dev      # 개발 서버
npm run build    # 프로덕션 빌드
npm run lint     # ESLint
npm run test     # 테스트

## 행동 원칙

### 코딩 전에 생각하기
- 가정을 명시적으로 말하라. 불확실하면 질문하라.
- 혼란스러우면 멈추고, 추측으로 진행하지 마라.
- 트레이드오프가 있으면 의견을 제시하라.
- 구현 전에 검증 가능한 성공 기준을 정의하라.

### 단순성 우선
- 요청한 기능만 구현하라. 그 이상은 금지.
- 한 번만 쓸 코드에 추상화를 만들지 마라.
- 발생할 수 없는 시나리오에 에러 처리를 추가하지 마라.

### 정밀한 변경
- 필요한 부분만 수정하라. 인접 코드를 "개선"하지 마라.
- 기존 코드의 스타일, 포맷, 주석을 건드리지 마라.
- 자신의 변경으로 생긴 미사용 코드만 제거하라.
- 사용자나 다른 에이전트가 만든 변경을 되돌리지 마라.

### 목표 기반 실행
- "테스트 작성 → 통과"처럼 명령을 목표로 변환하라.
- 완료 후 성공 기준을 검증하라.
- 완료 상태로 표시하기 전에 핵심 기능이 테스트로 증명되었는지 확인하라.
