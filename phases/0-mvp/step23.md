# Step 23: frontend-word-screens

## Step Contract

- Capability: word list, registration, extraction confirmation, and detail screens
- Layer: frontend-view
- Write Scope: `frontend/`
- Out of Scope: backend files, pattern screens, review screen, real image storage
- Critical Gates: `cd frontend && npm run test -- --run words`

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

단어 목록/등록/상세 화면을 구현한다.

필수 구현:
- `/words`: `GET /api/words` 목록, 검색, 품사 필터, 중요만 보기, 정렬, 페이지네이션.
- 등록 영역은 단건, JSON, 이미지 탭을 가진다.
- 단건 등록은 word/meaning form validation 후 `POST /api/words`.
- JSON 벌크 등록은 예시 placeholder와 JSON validation 후 `POST /api/words/bulk`.
- 벌크 결과는 saved/skipped/enrichmentFailed를 구분 표시한다.
- 이미지 업로드는 `POST /api/words/extract` 후 추출 결과를 편집 가능한 확인 form으로 보여주고 저장은 bulk API를 사용한다.
- 각 단어는 중요 토글 `PATCH /api/words/{id}/important`를 제공한다.
- `/words/[id]`: 단어 정보, AI 보강 정보, 유의어, 팁, 사용 예문 목록, 예문 추가 진입 UI.
- loading, error, empty 상태를 구현한다.

## Acceptance Criteria

```bash
cd frontend && npm run test -- --run words
cd frontend && npm run build
```

## 검증 절차

1. 단건/벌크/이미지 추출 확인 흐름을 MSW로 검증한다.
2. 검색/필터/중요 토글 query/mutation 동작을 확인한다.
3. 상세 화면의 예문 추가 진입이 level/count 선택으로 이어지는지 확인한다.
4. Step 23을 `completed`로 표시하고 summary에 word 화면 범위를 적는다.

## 금지사항

- backend word API를 수정하지 마라. 이유: frontend-view step이다.
- 이미지를 로컬에 저장하지 마라. 이유: PRD의 이미지 미저장 요구사항이다.
- 패턴 등록 UI와 섞지 마라. 이유: Step 24에서 구현한다.
- 작업 완료 후 직접 Git 커밋을 하지 마라. 이유: 하네스가 자동 커밋한다.
