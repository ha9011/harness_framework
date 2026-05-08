# Step 24: frontend-pattern-screens

## Step Contract

- Capability: pattern list, registration, extraction confirmation, and detail screens
- Layer: frontend-view
- Write Scope: `frontend/`
- Out of Scope: backend files, word screens, review screen
- Critical Gates: `cd frontend && npm run test -- --run patterns`

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `/docs/PLAN.md`
- `/docs/ARCHITECTURE.md`
- `/docs/ADR.md`
- `/docs/UI_GUIDE.md`
- `/API설계서.md`
- `/design/screens-other.jsx`
- `/frontend/app/lib/api.ts`
- `/frontend/app/components/`

## 작업

패턴 목록/등록/상세 화면을 구현한다.

필수 구현:
- `/patterns`: `GET /api/patterns` 목록과 페이지네이션.
- 직접 입력 form은 template, description, 예문 목록을 받는다.
- 예문은 sentence/translation 쌍이며 동적 추가/삭제 UI를 제공한다.
- 이미지 업로드는 `POST /api/patterns/extract` 후 추출 결과를 편집 가능한 확인 form으로 보여주고 `POST /api/patterns`로 저장한다.
- `/patterns/[id]`: 패턴 정보, 교재 예문 순서, 이 패턴으로 생성된 예문 목록을 표시한다.
- 패턴 상세에서 난이도/개수 선택 후 `/api/generate/pattern` 호출 진입 UI를 제공한다.
- loading, error, empty 상태를 구현한다.

## Acceptance Criteria

```bash
cd frontend && npm run test -- --run patterns
cd frontend && npm run build
```

## 검증 절차

1. 직접 입력과 이미지 추출 확인 흐름을 MSW로 검증한다.
2. 교재 예문 순서가 보존되어 표시되는지 확인한다.
3. 패턴 예문 생성 진입 UI가 올바른 API payload를 만드는지 확인한다.
4. Step 24를 `completed`로 표시하고 summary에 pattern 화면 범위를 적는다.

## 금지사항

- backend pattern API를 수정하지 마라. 이유: frontend-view step이다.
- 단어 화면 컴포넌트를 불필요하게 대규모 리팩터링하지 마라. 이유: scope를 좁게 유지한다.
- 이미지 파일을 저장하지 마라. 이유: 추출 후 미저장 정책이다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.
