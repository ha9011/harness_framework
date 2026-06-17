// @vitest-environment jsdom
import { describe, it, expect, beforeEach } from "vitest";
import { getToken, setToken, clearToken } from "../auth-token";

describe("auth-token 헬퍼", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("저장된 토큰이 없으면 null을 반환한다", () => {
    expect(getToken()).toBeNull();
  });

  it("토큰을 저장하면 다시 읽을 수 있다", () => {
    setToken("jwt-token-value");
    expect(getToken()).toBe("jwt-token-value");
  });

  it("clearToken으로 삭제하면 null을 반환한다", () => {
    setToken("jwt-token-value");
    clearToken();
    expect(getToken()).toBeNull();
  });
});
