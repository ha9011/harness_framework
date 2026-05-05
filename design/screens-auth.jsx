// screens-auth.jsx — Login (window seat) + Signup (stamp card · minimal fields)

// ─── Cozy input field ──────────────────────────────────
function CafeInput({ theme, type, label, placeholder, value, icon, secure, hint, error, suffix }) {
  return (
    <label style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
      {label && (
        <span style={{ fontSize: 11.5, fontWeight: 600, color: theme.inkSoft, letterSpacing: 0.2, textTransform: 'uppercase', paddingLeft: 2 }}>
          {label}
        </span>
      )}
      <div style={{
        display: 'flex', alignItems: 'center', gap: 10,
        padding: '13px 14px',
        background: theme.bgRaised,
        borderRadius: 14,
        border: `1px solid ${error ? '#C77E47' : theme.hairline}`,
        boxShadow: error ? `0 0 0 3px #C77E4722` : `inset 0 1px 0 rgba(255,255,255,0.4)`,
      }}>
        {icon && <span style={{ color: theme.inkMuted, display: 'flex' }}>{icon}</span>}
        <span style={{
          flex: 1, fontSize: 14,
          color: value ? theme.ink : theme.inkMuted,
          fontFamily: type.body, letterSpacing: -0.2,
        }}>
          {secure && value ? '•'.repeat(Math.min(value.length, 12)) : (value || placeholder)}
        </span>
        {suffix}
      </div>
      {hint && !error && <span style={{ fontSize: 11, color: theme.inkMuted, paddingLeft: 4 }}>{hint}</span>}
      {error && <span style={{ fontSize: 11, color: '#C77E47', paddingLeft: 4, fontWeight: 500 }}>⚠ {error}</span>}
    </label>
  );
}

// ─── Tiny icons ────────────────────────────────────────
const ico = {
  mail: <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><rect x="2" y="3.5" width="12" height="9" rx="2" stroke="currentColor" strokeWidth="1.4"/><path d="M3 5l5 4 5-4" stroke="currentColor" strokeWidth="1.4" fill="none" strokeLinecap="round"/></svg>,
  lock: <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><rect x="3" y="7" width="10" height="7" rx="1.5" stroke="currentColor" strokeWidth="1.4"/><path d="M5 7V5a3 3 0 016 0v2" stroke="currentColor" strokeWidth="1.4" fill="none"/></svg>,
  user: <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><circle cx="8" cy="6" r="2.6" stroke="currentColor" strokeWidth="1.4"/><path d="M3 14c.6-2.6 2.6-4 5-4s4.4 1.4 5 4" stroke="currentColor" strokeWidth="1.4" fill="none" strokeLinecap="round"/></svg>,
  eye: <svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M1.5 8s2.5-4.5 6.5-4.5S14.5 8 14.5 8s-2.5 4.5-6.5 4.5S1.5 8 1.5 8z" stroke="currentColor" strokeWidth="1.3" fill="none"/><circle cx="8" cy="8" r="2" stroke="currentColor" strokeWidth="1.3"/></svg>,
  check: <svg width="12" height="12" viewBox="0 0 12 12" fill="none"><path d="M2.5 6l2.5 2.5L9.5 3.5" stroke="currentColor" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"/></svg>,
};

// ─── Stamp card row (loyalty card metaphor) ────────────
function StampRow({ theme, count = 0, total = 10 }) {
  return (
    <div style={{ display: 'flex', gap: 6, justifyContent: 'center' }}>
      {[...Array(total)].map((_, i) => (
        <div key={i} style={{
          width: 18, height: 18, borderRadius: 9,
          background: i < count ? theme.primary : 'transparent',
          border: `1.5px dashed ${i < count ? 'transparent' : theme.primary + '66'}`,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          color: '#FFFCF7', fontSize: 9,
        }}>
          {i < count ? '☕' : ''}
        </div>
      ))}
    </div>
  );
}

// ─── Social pill button ────────────────────────────────
function SocialPill({ theme, label, mark }) {
  return (
    <button style={{
      flex: 1, padding: '11px 10px',
      background: theme.bgRaised, color: theme.ink,
      border: `1px solid ${theme.hairline}`,
      borderRadius: 14,
      display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
      fontFamily: 'inherit', fontSize: 13, fontWeight: 500, letterSpacing: -0.2,
      cursor: 'pointer', boxShadow: theme.cardShadow,
    }}>
      {mark}<span>{label}</span>
    </button>
  );
}

const socialMarks = {
  google: <svg width="14" height="14" viewBox="0 0 24 24"><path fill="#4285F4" d="M22 12.2c0-.7-.1-1.4-.2-2H12v3.8h5.6c-.2 1.3-1 2.4-2.1 3.1v2.6h3.4c2-1.8 3.1-4.5 3.1-7.5z"/><path fill="#34A853" d="M12 22c2.8 0 5.2-.9 6.9-2.5l-3.4-2.6c-.9.6-2.1 1-3.5 1-2.7 0-5-1.8-5.8-4.3H2.7v2.7C4.4 19.7 7.9 22 12 22z"/><path fill="#FBBC05" d="M6.2 13.6c-.2-.6-.3-1.3-.3-2s.1-1.4.3-2V6.9H2.7C2 8.4 1.5 10.1 1.5 12s.5 3.6 1.2 5.1l3.5-2.7z"/><path fill="#EA4335" d="M12 5.7c1.5 0 2.9.5 4 1.5l3-3C17.2 2.6 14.8 1.5 12 1.5 7.9 1.5 4.4 3.8 2.7 7l3.5 2.7C7 7.4 9.3 5.7 12 5.7z"/></svg>,
  kakao: <svg width="14" height="14" viewBox="0 0 24 24" fill="#3A1D1D"><path d="M12 3C6.48 3 2 6.48 2 10.8c0 2.78 1.86 5.22 4.66 6.6-.21.78-.76 2.84-.87 3.28-.13.55.2.55.42.4.18-.12 2.83-1.92 3.96-2.7.6.08 1.21.12 1.83.12 5.52 0 10-3.48 10-7.7C22 6.48 17.52 3 12 3z"/></svg>,
};

// ═══════════════════════════════════════════════════════
//   LOGIN — Window seat (illustration hero)
// ═══════════════════════════════════════════════════════
function Login({ theme, type, onSignup }) {
  const dark = theme.bg.startsWith('#1') || theme.bg.startsWith('#2');
  return (
    <div style={{ height: '100%', overflow: 'auto', paddingBottom: 40, fontFamily: type.body, background: theme.bg }}>
      {/* Window scene */}
      <div style={{ position: 'relative', height: 240, overflow: 'hidden' }}>
        <svg width="100%" height="100%" viewBox="0 0 320 240" preserveAspectRatio="xMidYMid slice" style={{ display: 'block' }}>
          <defs>
            <linearGradient id="sky-g" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0" stopColor={dark ? '#3A2D22' : '#F4DCC1'}/>
              <stop offset="1" stopColor={dark ? '#2A2018' : '#E8C9A4'}/>
            </linearGradient>
          </defs>
          <rect width="320" height="240" fill="url(#sky-g)"/>
          <g opacity="0.45">
            <circle cx="40" cy="160" r="40" fill={theme.sage}/>
            <circle cx="90" cy="170" r="34" fill={theme.sage}/>
            <circle cx="240" cy="155" r="44" fill={theme.sage}/>
            <circle cx="290" cy="170" r="30" fill={theme.sage}/>
          </g>
          <circle cx="240" cy="70" r="22" fill={dark ? '#D89465' : '#F5C26A'} opacity="0.85"/>
          <rect x="0" y="0" width="320" height="240" fill="none" stroke={theme.primaryDeep} strokeWidth="10"/>
          <line x1="160" y1="0" x2="160" y2="240" stroke={theme.primaryDeep} strokeWidth="6"/>
          <line x1="0" y1="120" x2="320" y2="120" stroke={theme.primaryDeep} strokeWidth="6"/>
          <rect x="20" y="200" width="44" height="32" rx="3" fill={theme.bgKraft} stroke={theme.primaryDeep} strokeWidth="1.5"/>
          <path d="M30 200 q4 -22 8 -2 M42 200 q4 -28 10 -4 M52 200 q3 -18 6 -2" stroke={theme.sage} strokeWidth="2.5" fill="none" strokeLinecap="round"/>
          <ellipse cx="248" cy="216" rx="22" ry="3" fill="#000" opacity="0.15"/>
          <path d="M232 200 v-14 q0 -2 2 -2 h28 q2 0 2 2 v14 q0 8 -8 8 h-16 q-8 0 -8 -8z" fill={theme.bgRaised} stroke={theme.primaryDeep} strokeWidth="1.5"/>
          <ellipse cx="248" cy="186" rx="14" ry="2" fill={theme.primaryDeep}/>
          <path d="M264 192 q6 0 6 4 q0 4 -6 4" stroke={theme.primaryDeep} strokeWidth="1.5" fill="none"/>
          <path d="M242 178 q-3 -6 0 -10 q3 -4 0 -10" stroke={theme.inkMuted} strokeWidth="1.4" fill="none" opacity="0.55" strokeLinecap="round"/>
          <path d="M250 178 q-3 -6 0 -10 q3 -4 0 -10" stroke={theme.inkMuted} strokeWidth="1.4" fill="none" opacity="0.55" strokeLinecap="round"/>
          <path d="M258 178 q-3 -6 0 -10 q3 -4 0 -10" stroke={theme.inkMuted} strokeWidth="1.4" fill="none" opacity="0.55" strokeLinecap="round"/>
        </svg>
        <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0, height: 60,
          background: `linear-gradient(180deg, transparent, ${theme.bg})` }}/>
      </div>

      <div style={{ padding: '0 22px', marginTop: -20, position: 'relative' }}>
        <div style={{ fontSize: 11.5, color: theme.sage, fontWeight: 700, letterSpacing: 2, textTransform: 'uppercase' }}>
          Window seat · 4 PM
        </div>
        <div style={{
          fontFamily: type.serif, fontSize: 26, fontWeight: 500,
          color: theme.ink, marginTop: 4, letterSpacing: -0.5, lineHeight: 1.2,
          fontStyle: 'italic',
        }}>같은 자리, 같은 잔으로<br/>다시 시작해요</div>

        <div style={{ marginTop: 22, display: 'flex', flexDirection: 'column', gap: 12 }}>
          <CafeInput theme={theme} type={type} placeholder="이메일" value="hyejin@daum.net" icon={ico.mail}/>
          <CafeInput theme={theme} type={type} placeholder="비밀번호" value="cozycafe" secure icon={ico.lock}
            suffix={<span style={{ color: theme.inkMuted, cursor: 'pointer', display: 'flex' }}>{ico.eye}</span>}/>

          <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: -2 }}>
            <span style={{ fontSize: 12, color: theme.primary, fontWeight: 500, cursor: 'pointer' }}>
              비밀번호 잊으셨어요?
            </span>
          </div>

          <CafeButton theme={theme} size="lg" full style={{ marginTop: 4 }}>로그인</CafeButton>

          <div style={{ display: 'flex', alignItems: 'center', gap: 10, margin: '6px 0 0' }}>
            <div style={{ flex: 1, height: 1, background: theme.divider }}/>
            <span style={{ fontSize: 11, color: theme.inkMuted, letterSpacing: 0.4 }}>또는</span>
            <div style={{ flex: 1, height: 1, background: theme.divider }}/>
          </div>

          <div style={{ display: 'flex', gap: 8 }}>
            <SocialPill theme={theme} label="Google" mark={socialMarks.google}/>
            <SocialPill theme={theme} label="카카오" mark={socialMarks.kakao}/>
          </div>

          <div style={{ textAlign: 'center', marginTop: 14, fontSize: 13, color: theme.inkMuted }}>
            계정이 없나요?{' '}
            <span onClick={onSignup} style={{ color: theme.primary, fontWeight: 600, cursor: 'pointer' }}>
              회원가입 →
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════
//   SIGNUP — Stamp card · minimal (nickname / email / pw / confirm)
// ═══════════════════════════════════════════════════════
function Signup({ theme, type, onLogin }) {
  return (
    <div style={{ height: '100%', overflow: 'auto', paddingBottom: 40, fontFamily: type.body, background: theme.bg }}>
      <TopBar theme={theme} title="회원가입" onBack={() => onLogin && onLogin()}/>

      <div style={{ padding: '28px 22px 0' }}>
        {/* Heading */}
        <div style={{
          fontFamily: type.serif, fontSize: 24, fontWeight: 600,
          color: theme.ink, letterSpacing: -0.5, lineHeight: 1.25,
          fontStyle: 'italic',
        }}>
          처음 오신 걸 환영해요
        </div>
        <div style={{ fontSize: 13, color: theme.inkMuted, marginTop: 6 }}>
          기본 정보만 알려주세요
        </div>

        {/* Form — nickname / email / password / confirm */}
        <div style={{ marginTop: 24, display: 'flex', flexDirection: 'column', gap: 12 }}>
          <CafeInput theme={theme} type={type} label="닉네임" placeholder="혜진" value="혜진" icon={ico.user}/>
          <CafeInput theme={theme} type={type} label="이메일" placeholder="you@cafe.com" value="hyejin@daum.net" icon={ico.mail}/>
          <CafeInput theme={theme} type={type} label="비밀번호" placeholder="8자 이상" value="cozycafe2026" secure icon={ico.lock}
            suffix={<span style={{ color: theme.inkMuted, cursor: 'pointer', display: 'flex' }}>{ico.eye}</span>}/>
          <CafeInput theme={theme} type={type} label="비밀번호 확인" placeholder="다시 한 번" value="cozycafe2026" secure icon={ico.lock}
            suffix={
              <span style={{
                width: 20, height: 20, borderRadius: 10,
                background: theme.sage, color: '#FFFCF7',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>{ico.check}</span>
            }/>
        </div>

        <CafeButton theme={theme} size="lg" full style={{ marginTop: 24 }}>
          가입하기
        </CafeButton>

        <div style={{ textAlign: 'center', marginTop: 16, fontSize: 13, color: theme.inkMuted }}>
          이미 회원이신가요?{' '}
          <span onClick={onLogin} style={{ color: theme.primary, fontWeight: 600, cursor: 'pointer' }}>로그인</span>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, {
  Login, Signup,
  // legacy aliases so existing references keep working
  LoginC: Login, SignupA: Signup,
  CafeInput, SocialPill, StampRow,
});
