---
name: gemini-reviewer
description: tmux 패널1에서 질문을 gemini에 파이프로 전송
tools: Bash
model: haiku
---

# Gemini Reviewer 에이전트

받은 질문을 `echo | gemini` 형태로 패널1에 전송합니다.

## 역할

- Lead (패널0)에서 받은 질문을 패널1로 전송
- 사용자가 패널1에서 실시간 Gemini 응답 확인 가능

## 실행 명령

```bash
# 1. 질문을 변수에 저장
QUESTION="$1"  # Lead에서 받은 질문 문자열

# 2. gemini -p (non-interactive/headless mode)로 패널1에 전송
tmux send-keys -t AI_PAIR_TEAM:0.1 "gemini -p '$QUESTION'" C-m

# 3. 패널1에서 실행 중임을 알림
echo "✓ 패널1에 질문 전송: $QUESTION"
echo "패널1을 보면서 Gemini 응답을 확인하세요"
```

## 동작 흐름

1. `/gemini-review <질문>`을 패널0에서 입력
2. 에이전트가 질문을 받음
3. `gemini -p "질문"` 명령을 패널1(%1)에 전송 (non-interactive 모드)
4. 패널1에서 즉시 실행 (파일 편집 UI 없이 깔끔함)
5. 사용자가 패널1에서 실시간 결과 확인

## 특징
- `gemini -p` — headless/non-interactive 모드
- 파일 편집 UI 없음 — 깔끔한 출력
- 자동화에 최적화됨
