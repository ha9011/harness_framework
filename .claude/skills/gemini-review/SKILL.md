---
name: gemini-review
description: Gemini CLI를 사용해 Claude의 코드/답변을 리뷰. "gemini review", "제미나이 리뷰", "gemini로 검토" 등의 요청을 감지하고 Gemini 에이전트에서 피드백을 수집합니다.
---

# Gemini Review Skill

Gemini CLI를 통해 Claude의 코드나 텍스트 응답을 리뷰하고 피드백을 제공합니다.

## 필수 요구사항

- Gemini CLI 설치: `npm install -g @google/generative-ai-cli` 또는 동등한 설치

## 워크플로우

### Phase 1: 리뷰 대상 준비
1. 사용자의 요청에서 리뷰해야 할 내용 추출
2. 코드 관련 요청이면 자동으로 컨텍스트 수집:
   - 현재 디렉토리의 관련 파일
   - 프로젝트 구조 정보
3. 리뷰 프롬프트 구성

### Phase 2: CLI 사용 가능 여부 확인
```bash
which gemini 2>/dev/null && echo "GEMINI_OK" || echo "GEMINI_NOT_FOUND"
```
- 설치되지 않음: 사용자에게 설치 가이드 제시 후 중단
- 설치됨: Phase 3 진행

### Phase 3: tmux 패널1에 non-interactive 모드로 실행

**Gemini CLI의 `-p` (headless) 플래그 사용:**

```
gemini-reviewer 에이전트가:
1. 받은 질문을 변수에 저장
2. tmux send-keys로 패널1(%1)에 전송:
   tmux send-keys -t AI_PAIR_TEAM:0.1 "gemini -p '질문'" C-m
3. 패널1에서 non-interactive 모드로 즉시 실행 (파일 UI 없음)
```

**실행 흐름:**
- 패널0: `/gemini-review <질문>` 입력
- 에이전트: `gemini -p "질문"` 명령을 패널1에 전송
- 패널1: headless 모드로 즉시 실행 (UI 모드 없음)
- 사용자: 패널1에서 실시간 결과 확인
- Claude: 동시에 결과 분석

### Phase 4: 결과 합성 및 출력

```markdown
## Gemini 리뷰 결과

### 주요 의견
[Gemini의 주요 피드백]

### 상세 분석
[Gemini의 상세한 분석 결과]

### Claude의 평가
[Claude의 추가 의견]
```

### Phase 5: 정리
완료 후 `gemini-reviewer` 에이전트는 자동으로 종료됨.
결과는 Claude Lead 세션에 통합되어 표시됩니다.

## 에러 처리
| 상황 | 동작 |
|---|---|
| Gemini CLI 미설치 | 설치 가이드 제시 |
| API 에러 | 에러 메시지 출력 및 실패 보고 |
| 타임아웃 | 타임아웃 메시지 + 부분 결과 (가능시) |
| 기타 CLI 실패 | 에러 메시지 그대로 반환 |

## 프롬프트 구성 규칙
| 요청 유형 | 포함할 컨텍스트 |
|---|---|
| 코드 리뷰 | 파일 내용 + 프로젝트 구조 |
| 아키텍처 평가 | 주요 설정 파일 + 디렉토리 구조 |
| 버그 분석 | 에러 로그 + 관련 코드 |
| 일반 질문 | 질문만 |
