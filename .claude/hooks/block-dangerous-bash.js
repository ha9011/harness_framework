#!/usr/bin/env node
/**
 * PreToolUse hook — Bash 도구의 위험 명령을 차단한다.
 * stdin으로 Claude Code hook JSON을 받아, 위험 패턴 감지 시 exit 2로 차단.
 *
 * 차단 대상:
 *   - rm -rf / rm --recursive --force (sudo 포함)
 *   - git push --force / -f / --force-with-lease (git -C 포함)
 *   - git reset --hard (git -C 포함)
 *   - DROP TABLE
 */
"use strict";

const fs = require("fs");

// --- stdin에서 command 추출 ---

let input;
try {
  input = JSON.parse(fs.readFileSync("/dev/stdin", "utf8"));
} catch {
  // JSON 파싱 실패 → 안전하게 차단
  process.exit(2);
}

const command = input.tool_input?.command || "";
if (!command) process.exit(0);

// --- 명령을 segment로 분리 (&&, ||, ;, |) ---

const segments = command.split(/\s*(?:&&|\|\||;|\|)\s*/);

for (const seg of segments) {
  const tokens = seg.trim().split(/\s+/);
  if (tokens.length === 0) continue;

  // sudo 제거 — sudo 뒤의 실제 명령을 검사
  const cmd = tokens[0] === "sudo" ? tokens.slice(1) : tokens;
  if (cmd.length === 0) continue;

  // git -C <path> 제거 — git -C path push ... → git push ...
  if (cmd[0] === "git" && cmd[1] === "-C" && cmd.length > 3) {
    cmd.splice(1, 2);
  }

  if (checkRm(cmd) || checkGitPush(cmd) || checkGitReset(cmd) || checkDropTable(seg)) {
    process.stderr.write("BLOCKED: 위험한 명령어가 감지되었습니다.\n");
    process.exit(2);
  }
}

process.exit(0);

// --- 검사 함수 ---

/**
 * rm에서 -r과 -f가 동시에 존재하는지 검사.
 * short flags (-rf, -r -f, -fr), long flags (--recursive --force) 모두 처리.
 */
function checkRm(tokens) {
  if (tokens[0] !== "rm") return false;

  const flags = tokens.filter((t) => t.startsWith("-"));
  // short flags를 모두 합친 문자열
  const shortAll = flags.filter((f) => !f.startsWith("--")).join("");
  const hasR = shortAll.includes("r") || flags.includes("--recursive");
  const hasF = shortAll.includes("f") || flags.includes("--force");
  return hasR && hasF;
}

/**
 * git push에서 --force, --force-with-lease, -f 옵션 검사.
 */
function checkGitPush(tokens) {
  if (tokens[0] !== "git" || tokens[1] !== "push") return false;
  return tokens.includes("--force") || tokens.includes("--force-with-lease") || tokens.includes("-f");
}

/**
 * git reset --hard 검사.
 */
function checkGitReset(tokens) {
  if (tokens[0] !== "git" || tokens[1] !== "reset") return false;
  return tokens.includes("--hard");
}

/**
 * DROP TABLE 검사 (대소문자 무시).
 */
function checkDropTable(segment) {
  return /drop\s+table/i.test(segment);
}
