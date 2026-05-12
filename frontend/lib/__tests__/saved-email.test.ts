// @vitest-environment jsdom
import { describe, it, expect, beforeEach } from "vitest";
import { getSavedEmail, setSavedEmail, clearSavedEmail } from "../saved-email";

describe("saved-email 헬퍼", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("저장된 이메일이 없으면 null을 반환한다", () => {
    expect(getSavedEmail()).toBeNull();
  });

  it("이메일을 저장하면 다시 읽을 수 있다", () => {
    setSavedEmail("test@example.com");
    expect(getSavedEmail()).toBe("test@example.com");
  });

  it("clearSavedEmail로 삭제하면 null을 반환한다", () => {
    setSavedEmail("test@example.com");
    clearSavedEmail();
    expect(getSavedEmail()).toBeNull();
  });
});
