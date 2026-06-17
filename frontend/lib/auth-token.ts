// JWT 토큰 localStorage 헬퍼 (iOS 홈화면 PWA 세션 유지용, ADR-020)
// 쿠키가 1차 인증 경로, localStorage 토큰은 Authorization 헤더로 보내는 PWA 폴백
const TOKEN_KEY = "auth_token";

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  if (typeof window === "undefined") return;
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  if (typeof window === "undefined") return;
  localStorage.removeItem(TOKEN_KEY);
}
