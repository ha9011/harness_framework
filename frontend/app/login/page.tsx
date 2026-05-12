"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/lib/auth-context";
import { ApiError } from "@/lib/api";
import { getSavedEmail, setSavedEmail, clearSavedEmail } from "@/lib/saved-email";

export default function LoginPage() {
  const [email, setEmail] = useState(() => getSavedEmail() ?? "");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [rememberEmail, setRememberEmail] = useState(() => getSavedEmail() !== null);
  const { login } = useAuth();
  const router = useRouter();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setSubmitting(true);

    try {
      await login(email, password);
      if (rememberEmail) {
        setSavedEmail(email);
      } else {
        clearSavedEmail();
      }
      router.push("/");
    } catch (err) {
      if (err instanceof ApiError) {
        setError("이메일 또는 비밀번호가 올바르지 않습니다");
      } else {
        setError("로그인 중 오류가 발생했습니다");
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex flex-col min-h-[calc(100vh-80px)] -mt-10 -mx-4">
      {/* 창가 일러스트 */}
      <div className="relative h-[240px] overflow-hidden">
        <svg width="100%" height="100%" viewBox="0 0 320 240" preserveAspectRatio="xMidYMid slice" className="block">
          <defs>
            <linearGradient id="sky-g" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0" stopColor="#F4DCC1"/>
              <stop offset="1" stopColor="#E8C9A4"/>
            </linearGradient>
          </defs>
          <rect width="320" height="240" fill="url(#sky-g)"/>
          <g opacity="0.45">
            <circle cx="40" cy="160" r="40" fill="#7A8F6B"/>
            <circle cx="90" cy="170" r="34" fill="#7A8F6B"/>
            <circle cx="240" cy="155" r="44" fill="#7A8F6B"/>
            <circle cx="290" cy="170" r="30" fill="#7A8F6B"/>
          </g>
          <circle cx="240" cy="70" r="22" fill="#F5C26A" opacity="0.85"/>
          <rect x="0" y="0" width="320" height="240" fill="none" stroke="#8C6440" strokeWidth="10"/>
          <line x1="160" y1="0" x2="160" y2="240" stroke="#8C6440" strokeWidth="6"/>
          <line x1="0" y1="120" x2="320" y2="120" stroke="#8C6440" strokeWidth="6"/>
          <rect x="20" y="200" width="44" height="32" rx="3" fill="#E8DCC8" stroke="#8C6440" strokeWidth="1.5"/>
          <path d="M30 200 q4 -22 8 -2 M42 200 q4 -28 10 -4 M52 200 q3 -18 6 -2" stroke="#7A8F6B" strokeWidth="2.5" fill="none" strokeLinecap="round"/>
          <ellipse cx="248" cy="216" rx="22" ry="3" fill="#000" opacity="0.15"/>
          <path d="M232 200 v-14 q0 -2 2 -2 h28 q2 0 2 2 v14 q0 8 -8 8 h-16 q-8 0 -8 -8z" fill="#FFFCF7" stroke="#8C6440" strokeWidth="1.5"/>
          <ellipse cx="248" cy="186" rx="14" ry="2" fill="#8C6440"/>
          <path d="M264 192 q6 0 6 4 q0 4 -6 4" stroke="#8C6440" strokeWidth="1.5" fill="none"/>
          <path d="M242 178 q-3 -6 0 -10 q3 -4 0 -10" stroke="#9A8676" strokeWidth="1.4" fill="none" opacity="0.55" strokeLinecap="round"/>
          <path d="M250 178 q-3 -6 0 -10 q3 -4 0 -10" stroke="#9A8676" strokeWidth="1.4" fill="none" opacity="0.55" strokeLinecap="round"/>
          <path d="M258 178 q-3 -6 0 -10 q3 -4 0 -10" stroke="#9A8676" strokeWidth="1.4" fill="none" opacity="0.55" strokeLinecap="round"/>
        </svg>
        <div className="absolute left-0 right-0 bottom-0 h-[60px]" style={{ background: "linear-gradient(180deg, transparent, var(--color-cream, #FAF6F0))" }}/>
      </div>

      <div className="px-5 -mt-5 relative flex-1">
        <p className="text-[11.5px] text-sage font-bold tracking-widest uppercase">
          Window seat
        </p>
        <p className="text-[26px] font-medium text-ink mt-1 tracking-tight leading-tight italic" style={{ fontFamily: "var(--font-gowun), serif" }}>
          같은 자리, 같은 잔으로<br/>다시 시작해요
        </p>

        <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-3">
          {/* 이메일 */}
          <div className="flex items-center gap-2.5 px-3.5 py-3 bg-raised rounded-[14px] border border-hairline">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" className="text-ink-muted shrink-0">
              <rect x="2" y="3.5" width="12" height="9" rx="2" stroke="currentColor" strokeWidth="1.4"/>
              <path d="M3 5l5 4 5-4" stroke="currentColor" strokeWidth="1.4" fill="none" strokeLinecap="round"/>
            </svg>
            <input
              type="email"
              placeholder="이메일"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="flex-1 text-sm text-ink bg-transparent outline-none placeholder:text-ink-muted"
              required
            />
          </div>

          {/* 비밀번호 */}
          <div className="flex items-center gap-2.5 px-3.5 py-3 bg-raised rounded-[14px] border border-hairline">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" className="text-ink-muted shrink-0">
              <rect x="3" y="7" width="10" height="7" rx="1.5" stroke="currentColor" strokeWidth="1.4"/>
              <path d="M5 7V5a3 3 0 016 0v2" stroke="currentColor" strokeWidth="1.4" fill="none"/>
            </svg>
            <input
              type={showPassword ? "text" : "password"}
              placeholder="비밀번호"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
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

          {/* 에러 메시지 */}
          {error && (
            <p className="text-xs text-warn font-medium px-1">{error}</p>
          )}

          {/* 이메일 저장 */}
          <label className="flex items-center gap-2 px-1 cursor-pointer">
            <input
              type="checkbox"
              checked={rememberEmail}
              onChange={(e) => setRememberEmail(e.target.checked)}
              className="w-4 h-4 accent-primary"
            />
            <span className="text-[13px] text-ink-muted">이메일 저장</span>
          </label>

          {/* 로그인 버튼 */}
          <button
            type="submit"
            disabled={submitting}
            className="mt-1 bg-primary text-white rounded-[14px] h-[48px] text-[15px] font-semibold shadow-sm disabled:opacity-60"
          >
            {submitting ? "로그인 중..." : "로그인"}
          </button>
        </form>

        <p className="text-center mt-5 text-[13px] text-ink-muted">
          계정이 없나요?{" "}
          <Link href="/signup" className="text-primary font-semibold">
            회원가입 →
          </Link>
        </p>
      </div>
    </div>
  );
}
