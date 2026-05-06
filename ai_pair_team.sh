#!/bin/bash

# 1. 세션 이름 설정
SESSION="AI_PAIR_TEAM"

# 2. 기존 세션이 있다면 종료하고 새로 시작
tmux kill-session -t $SESSION 2>/dev/null
tmux new-session -d -s $SESSION -n "Codex-Gemini"

# 3. 패널 설정 및 이름 부여
# [패널 0]: Codex Lead (메인 화면)
tmux select-pane -t 0 -T "codex-lead"
tmux send-keys -t 0 "codex" C-m  # Codex CLI 실행

# [패널 1]: Gemini Reviewer (화면 오른쪽 분할)
tmux split-window -h
tmux select-pane -t 1 -T "gemini-reviewer"
# bash 대기 상태로 시작 (gemini -p 명령을 받을 준비)
tmux send-keys -t 1 "# Gemini Reviewer 준비 완료. /gemini-review 명령을 실행하세요." C-m

# [참고] Codex는 현재 미사용으로 주석 처리
# tmux split-window -v
# tmux select-pane -t 2 -T "codex-reviewer"
# tmux send-keys -t 2 "codex" C-m

# 4. 레이아웃 조정 (50:50 분할)
tmux select-layout even-horizontal

# 5. 시각적 가이드 설정 (상단에 패널 이름 표시)
tmux set -g pane-border-status top
tmux set -g pane-border-format " [ #T ] "

# 6. 세션 입장
tmux attach-session -t $SESSION
