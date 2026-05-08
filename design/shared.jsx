// shared.jsx — Reusable Cozy Cafe components used across all screens.

// ─── Bottom navigation (mobile) ────────────────────────────────
function BottomNav({ active = 'home', onNav, theme }) {
  const items = [
    { key: 'home', label: '홈' },
    { key: 'words', label: '단어' },
    { key: 'patterns', label: '패턴' },
    { key: 'generate', label: '생성' },
    { key: 'review', label: '복습' },
  ];
  const icons = {
    home: (a) => (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
        <path d="M4 11l8-7 8 7v9a1 1 0 01-1 1h-4v-6h-6v6H5a1 1 0 01-1-1v-9z" stroke="currentColor" strokeWidth={a ? 2 : 1.6} fill={a ? 'currentColor' : 'none'} fillOpacity={a ? 0.12 : 0} strokeLinejoin="round"/>
      </svg>
    ),
    words: (a) => (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
        <path d="M5 5h11a3 3 0 013 3v11H8a3 3 0 01-3-3V5z" stroke="currentColor" strokeWidth={a ? 2 : 1.6} fill={a ? 'currentColor' : 'none'} fillOpacity={a ? 0.12 : 0} strokeLinejoin="round"/>
        <path d="M9 9h7M9 12h5" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"/>
      </svg>
    ),
    patterns: (a) => (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
        <path d="M4 7h16M4 12h10M4 17h16" stroke="currentColor" strokeWidth={a ? 2.2 : 1.7} strokeLinecap="round"/>
      </svg>
    ),
    generate: (a) => (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
        <path d="M12 3l1.6 4.4L18 9l-4.4 1.6L12 15l-1.6-4.4L6 9l4.4-1.6L12 3z" stroke="currentColor" strokeWidth={a ? 1.8 : 1.5} fill={a ? 'currentColor' : 'none'} fillOpacity={a ? 0.15 : 0} strokeLinejoin="round"/>
        <path d="M18 16l.7 1.8L20.5 18.5l-1.8.7L18 21l-.7-1.8L15.5 18.5l1.8-.7L18 16z" stroke="currentColor" strokeWidth="1.2" fill={a ? 'currentColor' : 'none'} fillOpacity={a ? 0.15 : 0}/>
      </svg>
    ),
    review: (a) => (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
        <rect x="4" y="6" width="14" height="12" rx="2" stroke="currentColor" strokeWidth={a ? 2 : 1.6} fill={a ? 'currentColor' : 'none'} fillOpacity={a ? 0.12 : 0}/>
        <rect x="7" y="3" width="14" height="12" rx="2" stroke="currentColor" strokeWidth={a ? 2 : 1.6} fill={theme.bgRaised}/>
      </svg>
    ),
  };

  return (
    <div style={{
      position: 'absolute', bottom: 0, left: 0, right: 0,
      paddingBottom: 28, paddingTop: 8, paddingLeft: 8, paddingRight: 8,
      background: theme.bgRaised,
      borderTop: `1px solid ${theme.hairline}`,
      display: 'flex', justifyContent: 'space-around', alignItems: 'center',
      zIndex: 30,
    }}>
      {items.map(it => {
        const a = it.key === active;
        return (
          <button key={it.key} onClick={() => onNav && onNav(it.key)} style={{
            background: 'none', border: 'none', padding: '6px 10px',
            display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2,
            color: a ? theme.primary : theme.inkMuted,
            cursor: 'pointer', fontFamily: 'inherit',
          }}>
            {icons[it.key](a)}
            <span style={{ fontSize: 10.5, fontWeight: a ? 600 : 400, letterSpacing: -0.2 }}>{it.label}</span>
          </button>
        );
      })}
    </div>
  );
}

// ─── Top header bar ─────────────────────────────────────
function TopBar({ title, theme, onBack, right, subtitle }) {
  return (
    <div style={{
      padding: '8px 16px 12px', display: 'flex', alignItems: 'center', gap: 10,
      borderBottom: `1px solid ${theme.divider}`,
      background: theme.bg,
    }}>
      {onBack && (
        <button onClick={onBack} style={{
          width: 32, height: 32, borderRadius: 16, border: 'none',
          background: theme.bgSoft, color: theme.ink,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          cursor: 'pointer', flexShrink: 0,
        }}>
          <svg width="14" height="14" viewBox="0 0 14 14"><path d="M9 2L4 7l5 5" stroke="currentColor" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"/></svg>
        </button>
      )}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 18, fontWeight: 600, color: theme.ink, letterSpacing: -0.4 }}>{title}</div>
        {subtitle && <div style={{ fontSize: 12, color: theme.inkMuted, marginTop: 1 }}>{subtitle}</div>}
      </div>
      {right}
    </div>
  );
}

// ─── Soft button ───────────────────────────────────────
function CafeButton({ children, onClick, variant = 'primary', size = 'md', theme, style = {}, full }) {
  const sizes = {
    sm: { padding: '8px 14px', fontSize: 13, radius: 12, height: 34 },
    md: { padding: '11px 18px', fontSize: 14, radius: 14, height: 42 },
    lg: { padding: '16px 22px', fontSize: 16, radius: 18, height: 52 },
  };
  const sz = sizes[size];
  const variants = {
    primary: { bg: theme.primary, color: '#FFFCF7', border: 'transparent', shadow: `0 6px 14px -4px ${theme.primary}66, inset 0 1px 0 rgba(255,255,255,0.18)` },
    secondary: { bg: theme.bgRaised, color: theme.ink, border: theme.hairline, shadow: theme.cardShadow },
    ghost: { bg: 'transparent', color: theme.ink, border: theme.hairline, shadow: 'none' },
    sage: { bg: theme.sage, color: '#FFFCF7', border: 'transparent', shadow: `0 6px 14px -4px ${theme.sage}66, inset 0 1px 0 rgba(255,255,255,0.18)` },
  };
  const v = variants[variant];
  return (
    <button onClick={onClick} style={{
      padding: sz.padding, fontSize: sz.fontSize, height: sz.height,
      borderRadius: sz.radius,
      background: v.bg, color: v.color,
      border: `1px solid ${v.border}`,
      boxShadow: v.shadow,
      fontFamily: 'inherit', fontWeight: 500, letterSpacing: -0.3,
      cursor: 'pointer', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 6,
      width: full ? '100%' : undefined,
      transition: 'transform 0.12s ease, box-shadow 0.12s ease',
      ...style,
    }}
    onMouseDown={(e) => e.currentTarget.style.transform = 'translateY(1px)'}
    onMouseUp={(e) => e.currentTarget.style.transform = 'translateY(0)'}
    onMouseLeave={(e) => e.currentTarget.style.transform = 'translateY(0)'}
    >{children}</button>
  );
}

// ─── Soft surface (card) ─────────────────────────────────
function Surface({ children, theme, style = {}, kraft = false, hover = false }) {
  return (
    <div style={{
      background: kraft ? theme.bgKraft : theme.bgRaised,
      borderRadius: 20,
      boxShadow: theme.cardShadow,
      border: `1px solid ${theme.hairline}`,
      ...style,
    }}>{children}</div>
  );
}

// ─── Tag chip ────────────────────────────────────────
function Chip({ children, theme, variant = 'default', style = {} }) {
  const V = {
    default: { bg: theme.bgSoft, fg: theme.inkSoft, border: theme.hairline },
    sage: { bg: theme.sageBg, fg: theme.sage, border: 'transparent' },
    warm: { bg: theme.primarySoft, fg: theme.primaryDeep, border: 'transparent' },
    outline: { bg: 'transparent', fg: theme.inkSoft, border: theme.hairline },
  };
  const v = V[variant];
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 4,
      padding: '4px 10px', borderRadius: 999,
      background: v.bg, color: v.fg, border: `1px solid ${v.border}`,
      fontSize: 11.5, fontWeight: 500, letterSpacing: -0.2,
      ...style,
    }}>{children}</span>
  );
}

// ─── Situation cloud (for sentence cards) ─────────────
function SituationCloud({ children, theme, style = {} }) {
  return (
    <div style={{ position: 'relative', display: 'inline-block', ...style }}>
      <div style={{
        background: theme.sageBg, color: theme.sage,
        padding: '8px 14px', borderRadius: 18,
        fontSize: 12.5, fontWeight: 500, letterSpacing: -0.2,
        border: `1px solid ${theme.sage}22`,
        display: 'inline-flex', alignItems: 'center', gap: 6,
        boxShadow: `0 2px 6px ${theme.sage}1a`,
      }}>
        <span style={{ fontSize: 13 }}>🎭</span>
        <span>{children}</span>
      </div>
      {/* tail */}
      <svg width="14" height="10" viewBox="0 0 14 10" style={{ position: 'absolute', bottom: -7, left: 22 }}>
        <path d="M2 0 Q 6 8 12 1 Z" fill={theme.sageBg} stroke={theme.sage + '22'} strokeWidth="1" />
      </svg>
    </div>
  );
}

// ─── Section header ────────────────────────────────
function SectionHeader({ title, action, theme }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'baseline', justifyContent: 'space-between',
      padding: '0 16px', marginTop: 22, marginBottom: 10,
    }}>
      <div style={{ fontSize: 13, fontWeight: 600, color: theme.inkSoft, letterSpacing: -0.2, textTransform: 'uppercase' }}>{title}</div>
      {action && <div style={{ fontSize: 12.5, color: theme.primary, fontWeight: 500 }}>{action}</div>}
    </div>
  );
}

// ─── Empty pot / state ───────────────────────
function EmptyState({ icon, title, body, action, theme }) {
  return (
    <div style={{ padding: '40px 24px', textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 14 }}>
      {icon}
      <div style={{ fontSize: 15, fontWeight: 600, color: theme.ink }}>{title}</div>
      <div style={{ fontSize: 13, color: theme.inkMuted, lineHeight: 1.6, maxWidth: 240 }}>{body}</div>
      {action}
    </div>
  );
}

// ─── Tab pills ───────────────────────────────
function TabPills({ tabs, active, onChange, theme, style = {} }) {
  return (
    <div style={{
      display: 'flex', gap: 6, padding: 4,
      background: theme.bgSoft, borderRadius: 14,
      border: `1px solid ${theme.hairline}`,
      ...style,
    }}>
      {tabs.map(t => {
        const a = t.key === active;
        return (
          <button key={t.key} onClick={() => onChange(t.key)} style={{
            flex: 1, padding: '8px 10px', border: 'none',
            background: a ? theme.bgRaised : 'transparent',
            color: a ? theme.ink : theme.inkMuted,
            borderRadius: 10, cursor: 'pointer',
            fontFamily: 'inherit', fontSize: 13, fontWeight: a ? 600 : 500,
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 5,
            boxShadow: a ? `0 2px 6px rgba(0,0,0,0.06)` : 'none',
            transition: 'all 0.15s ease',
          }}>
            <span>{t.label}</span>
            {t.badge !== undefined && (
              <span style={{
                background: a ? theme.primary : theme.bgKraft,
                color: a ? '#FFFCF7' : theme.inkSoft,
                fontSize: 10, fontWeight: 700,
                padding: '1px 6px', borderRadius: 999, minWidth: 14, textAlign: 'center',
              }}>{t.badge}</span>
            )}
          </button>
        );
      })}
    </div>
  );
}

// ─── Generic switch tweak (used by old Tweaks) — leave for shared
Object.assign(window, {
  BottomNav, TopBar, CafeButton, Surface, Chip, SituationCloud, SectionHeader, EmptyState, TabPills,
});
