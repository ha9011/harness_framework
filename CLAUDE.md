# 프로젝트: 영어 패턴 학습기 (Cozy Cafe)

## 기술 스택
- 프론트엔드: Next.js (App Router) + TypeScript + Tailwind CSS
- 백엔드: Spring Boot + JPA + Java
- DB: PostgreSQL 16 (Docker)
- AI: Gemini API (Vision + Text Generation)
- 테스트: JUnit5 + MockMvc + TestContainers (백엔드), TDD

## 아키텍처 규칙
- CRITICAL: 백엔드 코드는 반드시 `backend/` 폴더에서 작성할 것
- CRITICAL: 프론트엔드 코드는 반드시 `frontend/` 폴더에서 작성할 것
- CRITICAL: 루트 디렉토리에 직접 소스 코드를 두지 말 것

## 개발 프로세스
- CRITICAL: 새 기능 구현 시 반드시 테스트를 먼저 작성하고, 테스트가 통과하는 구현을 작성할 것 (TDD)
- 커밋 메시지는 conventional commits 형식을 따를 것 (feat:, fix:, docs:, refactor:)

## 명령어
docker compose up -d                # PostgreSQL 기동 (개발용)
cd backend && ./gradlew test        # 백엔드 테스트
cd backend && ./gradlew bootRun     # 백엔드 개발 서버 (localhost:8080)
cd frontend && npm run dev          # 프론트엔드 개발 서버 (localhost:3000)
cd frontend && npm run build        # 프론트엔드 빌드
cd frontend && npm run lint         # ESLint
docker compose -f docker-compose.prod.yml up -d  # 운영 4-서비스 (미니PC 또는 로컬 시뮬레이션)

## 환경 설정
- Gemini API 키: 환경 변수 `GEMINI_API_KEY`로 주입. backend/src/main/resources/application.yml에서 `${GEMINI_API_KEY}` 참조
- JWT Secret: 환경 변수 `JWT_SECRET`로 주입. application.yml에서 `${JWT_SECRET}` 참조. 미설정 시 개발용 기본값 사용
- DB 스키마: `spring.jpa.hibernate.ddl-auto=update` (개발). JPA Entity 기반 자동 생성
- CORS: CorsConfig.java에서 localhost:3000 허용 (개발 환경)
- Gemini 재시도: 총 3회 시도 (즉시 → 1초 후 → 3초 후). 3회 모두 실패 시 fallback
- 운영 profile: `application-prod.yml` — `SPRING_PROFILES_ACTIVE=prod`로 활성화. DB URL/계정/JWT/Gemini 키 모두 환경변수로만 받음(기본값 없음, fail-fast). 운영 시크릿은 미니PC `/home/hadong/work/project/english/.env`에 보관(`.gitignore`)
- 운영 배포: `main` push → GitHub Actions가 GHCR 빌드 → SSH로 미니PC `docker compose pull/up`. 상세는 `docs/DEPLOYMENT.md` 참조

## 에러 처리 원칙
- Gemini API 실패 → 보강 없이 저장 (단건), 부분 성공 (벌크). 총 3회 시도 후 fallback
- 이미지 추출 실패 → 에러 메시지 + 재시도 안내. 이미지 미저장
- 모든 삭제는 soft delete. 예문은 유지, 해당 타입 review_items만 삭제
- Polymorphic association (item_type + item_id) → 애플리케이션 레벨 ID 검증 필수

## 행동 원칙

### 코딩 전에 생각하기
- 가정을 명시적으로 말하라. 불확실하면 질문하라.
- 혼란스러우면 멈추고, 추측으로 진행하지 마라.
- 트레이드오프가 있으면 의견을 제시하라.

### 단순성 우선
- 요청한 기능만 구현하라. 그 이상은 금지.
- 한 번만 쓸 코드에 추상화를 만들지 마라.
- 발생할 수 없는 시나리오에 에러 처리를 추가하지 마라.

### 정밀한 변경
- 필요한 부분만 수정하라. 인접 코드를 "개선"하지 마라.
- 기존 코드의 스타일, 포맷, 주석을 건드리지 마라.
- 자신의 변경으로 생긴 미사용 코드만 제거하라.

### 목표 기반 실행
- 구현 전에 검증 가능한 성공 기준을 정의하라.
- "테스트 작성 → 통과"처럼 명령을 목표로 변환하라.
- 완료 후 성공 기준을 검증하라.
