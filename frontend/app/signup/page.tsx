"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/lib/auth-context";
import { ApiError } from "@/lib/api";

export default function SignupPage() {
  const [nickname, setNickname] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [passwordTouched, setPasswordTouched] = useState(false);
  const [confirmTouched, setConfirmTouched] = useState(false);
  const { signup } = useAuth();
  const router = useRouter();

  const passwordError = passwordTouched && password.length > 0 && password.length < 8
    ? "비밀번호는 8자 이상이어야 합니다"
    : "";

  const confirmError = confirmTouched && confirmPassword.length > 0 && password !== confirmPassword
    ? "비밀번호가 일치하지 않습니다"
    : "";

  const passwordMatch = confirmPassword.length > 0 && password === confirmPassword;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    if (password.length < 8) {
      setError("비밀번호는 8자 이상이어야 합니다");
      return;
    }
    if (password !== confirmPassword) {
      setError("비밀번호가 일치하지 않습니다");
      return;
    }

    setSubmitting(true);
    try {
      await signup(email, password, nickname);
      router.push("/");
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("회원가입 중 오류가 발생했습니다");
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex flex-col min-h-[calc(100vh-80px)] -mt-10 -mx-4 px-5 pt-4">
      {/* TopBar */}
      <div className="flex items-center gap-3 mb-6">
        <Link href="/login" className="text-ink-muted">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
            <path d="M12 15l-5-5 5-5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
        </Link>
        <h1 className="text-base font-semibold text-ink">회원가입</h1>
      </div>

      {/* 헤딩 */}
      <p className="text-2xl font-semibold text-ink tracking-tight leading-tight italic" style={{ fontFamily: "var(--font-gowun), serif" }}>
        처음 오신 걸 환영해요
      </p>
      <p className="text-[13px] text-ink-muted mt-1.5">기본 정보만 알려주세요</p>

      <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-3">
        {/* 닉네임 */}
        <div>
          <label className="text-[11.5px] font-semibold text-ink-soft uppercase tracking-wide pl-0.5 mb-1.5 block">닉네임</label>
          <div className="flex items-center gap-2.5 px-3.5 py-3 bg-raised rounded-[14px] border border-hairline">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" className="text-ink-muted shrink-0">
              <circle cx="8" cy="6" r="2.6" stroke="currentColor" strokeWidth="1.4"/>
              <path d="M3 14c.6-2.6 2.6-4 5-4s4.4 1.4 5 4" stroke="currentColor" strokeWidth="1.4" fill="none" strokeLinecap="round"/>
            </svg>
            <input
              type="text"
              placeholder="닉네임"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              className="flex-1 text-sm text-ink bg-transparent outline-none placeholder:text-ink-muted"
              required
            />
          </div>
        </div>

        {/* 이메일 */}
        <div>
          <label className="text-[11.5px] font-semibold text-ink-soft uppercase tracking-wide pl-0.5 mb-1.5 block">이메일</label>
          <div className="flex items-center gap-2.5 px-3.5 py-3 bg-raised rounded-[14px] border border-hairline">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" className="text-ink-muted shrink-0">
              <rect x="2" y="3.5" width="12" height="9" rx="2" stroke="currentColor" strokeWidth="1.4"/>
              <path d="M3 5l5 4 5-4" stroke="currentColor" strokeWidth="1.4" fill="none" strokeLinecap="round"/>
            </svg>
            <input
              type="email"
              placeholder="you@cafe.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="flex-1 text-sm text-ink bg-transparent outline-none placeholder:text-ink-muted"
              required
            />
          </div>
        </div>

        {/* 비밀번호 */}
        <div>
          <label className="text-[11.5px] font-semibold text-ink-soft uppercase tracking-wide pl-0.5 mb-1.5 block">비밀번호</label>
          <div className={`flex items-center gap-2.5 px-3.5 py-3 bg-raised rounded-[14px] border ${passwordError ? "border-warn shadow-[0_0_0_3px_rgba(199,126,71,0.13)]" : "border-hairline"}`}>
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" className="text-ink-muted shrink-0">
              <rect x="3" y="7" width="10" height="7" rx="1.5" stroke="currentColor" strokeWidth="1.4"/>
              <path d="M5 7V5a3 3 0 016 0v2" stroke="currentColor" strokeWidth="1.4" fill="none"/>
            </svg>
            <input
              type={showPassword ? "text" : "password"}
              placeholder="8자 이상"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              onBlur={() => setPasswordTouched(true)}
              className="flex-1 text-sm text-ink bg-transparent outline-none placeholder:text-ink-muted"
              required
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="text-ink-muted"
            >
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <path d="M1.5 8s2.5-4.5 6.5-4.5S14.5 8 14.5 8s-2.5 4.5-6.5 4.5S1.5 8 1.5 8z" stroke="currentColor" strokeWidth="1.3" fill="none"/>
                <circle cx="8" cy="8" r="2" stroke="currentColor" strokeWidth="1.3"/>
              </svg>
            </button>
          </div>
          {passwordError && <p className="text-xs text-warn font-medium mt-1 pl-1">{passwordError}</p>}
          {!passwordError && <p className="text-[11px] text-ink-muted mt-1 pl-1">최소 8자</p>}
        </div>

        {/* 비밀번호 확인 */}
        <div>
          <label className="text-[11.5px] font-semibold text-ink-soft uppercase tracking-wide pl-0.5 mb-1.5 block">비밀번호 확인</label>
          <div className={`flex items-center gap-2.5 px-3.5 py-3 bg-raised rounded-[14px] border ${confirmError ? "border-warn shadow-[0_0_0_3px_rgba(199,126,71,0.13)]" : "border-hairline"}`}>
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" className="text-ink-muted shrink-0">
              <rect x="3" y="7" width="10" height="7" rx="1.5" stroke="currentColor" strokeWidth="1.4"/>
              <path d="M5 7V5a3 3 0 016 0v2" stroke="currentColor" strokeWidth="1.4" fill="none"/>
            </svg>
            <input
              type={showPassword ? "text" : "password"}
              placeholder="다시 한 번"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              onBlur={() => setConfirmTouched(true)}
              className="flex-1 text-sm text-ink bg-transparent outline-none placeholder:text-ink-muted"
              required
            />
            {passwordMatch && (
              <span className="w-5 h-5 rounded-full bg-sage text-white flex items-center justify-center">
                <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                  <path d="M2.5 6l2.5 2.5L9.5 3.5" stroke="currentColor" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"/>
                </svg>
              </span>
            )}
          </div>
          {confirmError && <p className="text-xs text-warn font-medium mt-1 pl-1">{confirmError}</p>}
        </div>

        {/* 에러 메시지 */}
        {error && (
          <p className="text-xs text-warn font-medium px-1">{error}</p>
        )}

        {/* 가입 버튼 */}
        <button
          type="submit"
          disabled={submitting}
          className="mt-3 bg-primary text-white rounded-[14px] h-[48px] text-[15px] font-semibold shadow-sm disabled:opacity-60"
        >
          {submitting ? "가입 중..." : "가입하기"}
        </button>
      </form>

      <p className="text-center mt-4 text-[13px] text-ink-muted">
        이미 회원이신가요?{" "}
        <Link href="/login" className="text-primary font-semibold">
          로그인
        </Link>
      </p>
    </div>
  );
}
