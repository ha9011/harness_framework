// screens-review.jsx — Review flip card with 3 tabs (단어/패턴/문장), 4 card style variations.

const REVIEW_DATA = {
  word: [
    { front: 'reluctant', backTitle: '꺼리는, 마지못해 하는', examples: ['She was reluctant to leave.', 'I’m a bit reluctant to ask him.'], tip: 're-(다시) + luct(싸우다) → 다시 싸우려 하는' },
    { front: 'cozy', backTitle: '아늑한, 편안한', examples: ['What a cozy little cafe!', 'The blanket feels so cozy.'], tip: '카페·집의 분위기를 묘사할 때 자주 써요' },
    { front: 'hesitate', backTitle: '망설이다, 주저하다', examples: ['Don’t hesitate to call me.', 'He hesitated for a moment.'], tip: '“혹시…”라고 말 꺼낼 때의 그 느낌' },
  ],
  pattern: [
    { front: 'I’m about to ~', backTitle: '막 ~하려는 참이야', examples: ['I’m about to leave the office.', 'She’s about to call you back.'], tip: '곧 일어날 일 — 1~2분 안의 가까운 미래' },
    { front: 'It depends on ~', backTitle: '~에 달렸어 / ~에 따라 달라', examples: ['It depends on the weather.', 'It depends on how busy you are.'] },
  ],
  sentence: [
    { front: 'I’m about to head out — want anything?', situation: '퇴근 전 동료에게', back: '나 곧 나가는데, 뭐 필요한 거 있어?' },
    { front: 'Could I get a small oat latte to go?', situation: '카페에서 주문', back: '오트 라떼 작은 사이즈로 테이크아웃 할게요.' },
    { front: 'Honestly, it depends on the day.', situation: '친구가 컨디션 묻는 상황', back: '솔직히 그날그날 달라.' },
  ],
};

// Card body — switches between style variants
function FlipCardBody({ data, tab, flipped, onFlip, theme, type, style = 'menu' }) {
  // style: 'menu' | 'napkin' | 'modern' | 'kraft'
  const dark = theme.bg.startsWith('#1') || theme.bg.startsWith('#2');

  const wrapStyle = {
    width: '100%', aspectRatio: '3 / 4', perspective: 1200,
    cursor: 'pointer', userSelect: 'none',
  };

  const baseSurface = {
    position: 'absolute', inset: 0, borderRadius: 24,
    backfaceVisibility: 'hidden', WebkitBackfaceVisibility: 'hidden',
    display: 'flex', flexDirection: 'column',
    padding: '24px 22px',
    border: `1px solid ${theme.hairline}`,
    boxShadow: theme.cardShadow,
    overflow: 'hidden',
  };

  const styleVariants = {
    menu: {
      front: { background: theme.bgKraft,
        backgroundImage: `radial-gradient(circle at 20% 10%, rgba(255,255,255,0.5), transparent 40%), repeating-linear-gradient(${dark ? '0deg' : '0deg'}, ${theme.hairline} 0 1px, transparent 1px 28px)`,
      },
      back: { background: theme.bgRaised },
    },
    napkin: {
      front: { background: theme.bgRaised,
        backgroundImage: `repeating-linear-gradient(135deg, ${theme.bgSoft}88 0 6px, transparent 6px 12px)`,
      },
      back: { background: theme.bgRaised },
    },
    modern: {
      front: { background: theme.bgRaised, boxShadow: theme.cardShadowHover },
      back: { background: theme.primarySoft },
    },
    kraft: {
      front: { background: theme.bgKraft,
        backgroundImage: `radial-gradient(circle at 30% 20%, rgba(255,255,255,0.3), transparent 60%)`,
        boxShadow: `${theme.cardShadow}, inset 0 0 24px rgba(120,90,60,0.06)`,
      },
      back: { background: theme.cream },
    },
  };
  const sv = styleVariants[style] || styleVariants.menu;

  const renderFront = () => {
    if (tab === 'word') {
      return (
        <>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Chip theme={theme} variant="outline">영어 → 한국어</Chip>
            <span style={{ fontSize: 11, color: theme.inkMuted }}>탭하면 뒤집어요</span>
          </div>
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 10 }}>
            <div style={{ fontFamily: type.serif, fontSize: 44, fontWeight: 600, color: theme.ink, letterSpacing: -0.8, textAlign: 'center' }}>
              {data.front}
            </div>
            <div style={{ fontSize: 13, color: theme.inkMuted, fontStyle: 'italic' }}>/rɪˈlʌktənt/</div>
          </div>
          <div style={{ textAlign: 'center', fontSize: 11, color: theme.inkMuted, opacity: 0.7 }}>· · ·</div>
        </>
      );
    }
    if (tab === 'pattern') {
      return (
        <>
          <Chip theme={theme} variant="outline">패턴 · 인식</Chip>
          <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <div style={{ fontFamily: type.serif, fontSize: 32, fontWeight: 500, color: theme.ink, letterSpacing: -0.4, textAlign: 'center', lineHeight: 1.3 }}>
              {data.front}
            </div>
          </div>
          <div style={{ textAlign: 'center', fontSize: 11, color: theme.inkMuted }}>탭해서 의미 확인</div>
        </>
      );
    }
    // sentence
    return (
      <>
        <SituationCloud theme={theme} style={{ alignSelf: 'flex-start', marginBottom: 14 }}>
          {data.situation}
        </SituationCloud>
        <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ fontFamily: type.serif, fontSize: 22, fontWeight: 500, color: theme.ink, lineHeight: 1.45, textAlign: 'center', letterSpacing: -0.2 }}>
            “{data.front}”
          </div>
        </div>
        <div style={{ textAlign: 'center', fontSize: 11, color: theme.inkMuted }}>탭해서 해석 확인</div>
      </>
    );
  };

  const renderBack = () => {
    if (tab === 'word') {
      return (
        <>
          <Chip theme={theme} variant="warm">의미</Chip>
          <div style={{ marginTop: 14, fontFamily: type.serif, fontSize: 24, fontWeight: 600, color: theme.ink, letterSpacing: -0.4 }}>{data.backTitle}</div>
          <div style={{ marginTop: 16, display: 'flex', flexDirection: 'column', gap: 8 }}>
            {data.examples.map((e, i) => (
              <div key={i} style={{ fontSize: 13, color: theme.inkSoft, lineHeight: 1.5, fontFamily: type.serif }}>
                <span style={{ color: theme.primary, marginRight: 4 }}>·</span>{e}
              </div>
            ))}
          </div>
          {data.tip && (
            <div style={{ marginTop: 'auto', padding: '10px 12px', borderRadius: 12, background: theme.sageBg, color: theme.sage, fontSize: 12, lineHeight: 1.5, border: `1px dashed ${theme.sage}55` }}>
              💡 {data.tip}
            </div>
          )}
        </>
      );
    }
    if (tab === 'pattern') {
      return (
        <>
          <Chip theme={theme} variant="warm">의미</Chip>
          <div style={{ marginTop: 14, fontSize: 18, fontWeight: 600, color: theme.ink, fontFamily: type.heading }}>{data.backTitle}</div>
          <div style={{ marginTop: 16, fontSize: 11, color: theme.inkMuted, fontWeight: 600, textTransform: 'uppercase', letterSpacing: 0.3 }}>교재 예문</div>
          <div style={{ marginTop: 6, display: 'flex', flexDirection: 'column', gap: 8 }}>
            {data.examples.map((e, i) => (
              <div key={i} style={{ fontSize: 13.5, color: theme.ink, lineHeight: 1.45, fontFamily: type.serif }}>
                <span style={{ color: theme.primary }}>{i + 1}.</span> {e}
              </div>
            ))}
          </div>
        </>
      );
    }
    return (
      <>
        <Chip theme={theme} variant="warm">해석</Chip>
        <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ fontFamily: type.heading, fontSize: 22, fontWeight: 500, color: theme.ink, lineHeight: 1.5, textAlign: 'center', letterSpacing: -0.3 }}>
            {data.back}
          </div>
        </div>
      </>
    );
  };

  return (
    <div style={wrapStyle} onClick={onFlip}>
      <div style={{
        width: '100%', height: '100%', position: 'relative',
        transition: 'transform 0.6s cubic-bezier(0.4, 0.0, 0.2, 1)',
        transformStyle: 'preserve-3d',
        transform: flipped ? 'rotateY(180deg)' : 'rotateY(0deg)',
      }}>
        <div style={{ ...baseSurface, ...sv.front }}>
          {renderFront()}
        </div>
        <div style={{ ...baseSurface, ...sv.back, transform: 'rotateY(180deg)' }}>
          {renderBack()}
        </div>
      </div>
    </div>
  );
}

// Full Review screen — interactive, with tabs + flip + grading
function ReviewScreen({ theme, type, cardStyle = 'menu', onNav }) {
  const [tab, setTab] = React.useState('word');
  const [idx, setIdx] = React.useState(0);
  const [flipped, setFlipped] = React.useState(false);
  const [graded, setGraded] = React.useState(0);

  const deck = REVIEW_DATA[tab];
  const data = deck[idx % deck.length];
  const total = { word: 15, pattern: 8, sentence: 20 }[tab];

  const grade = (level) => {
    if (!flipped) { setFlipped(true); return; }
    setGraded(g => g + 1);
    setFlipped(false);
    setIdx(i => i + 1);
  };

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column', background: theme.bg, fontFamily: type.body }}>
      {/* header */}
      <div style={{ padding: '12px 16px 6px', display: 'flex', alignItems: 'center', gap: 10 }}>
        <button onClick={() => onNav && onNav('home')} style={{ background: 'none', border: 'none', color: theme.ink, padding: 6, cursor: 'pointer' }}>
          <svg width="20" height="20" viewBox="0 0 24 24"><path d="M15 18l-6-6 6-6" stroke="currentColor" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"/></svg>
        </button>
        <div style={{ flex: 1, fontFamily: type.heading, fontSize: 17, fontWeight: type.headingWeight, color: theme.ink }}>오늘의 복습</div>
        <div style={{ fontSize: 12, color: theme.inkMuted }}>{Math.min(graded + 1, total)} / {total}</div>
      </div>

      {/* progress bar */}
      <div style={{ height: 4, background: theme.bgSoft, margin: '0 16px', borderRadius: 2, overflow: 'hidden' }}>
        <div style={{ height: '100%', width: `${(graded / total) * 100}%`, background: theme.primary, transition: 'width 0.4s ease' }} />
      </div>

      {/* tabs */}
      <div style={{ padding: '12px 16px 0' }}>
        <TabPills theme={theme} active={tab} onChange={(k) => { setTab(k); setIdx(0); setFlipped(false); }} tabs={[
          { key: 'word', label: '단어', badge: 15 },
          { key: 'pattern', label: '패턴', badge: 8 },
          { key: 'sentence', label: '문장', badge: 20 },
        ]} />
      </div>

      {/* card */}
      <div style={{ flex: 1, padding: '20px 22px 12px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <FlipCardBody data={data} tab={tab} flipped={flipped} onFlip={() => setFlipped(f => !f)} theme={theme} type={type} style={cardStyle} />
      </div>

      {/* actions */}
      <div style={{ padding: '0 16px 16px', display: 'flex', gap: 8 }}>
        {[
          { k: 'hard', label: '모름', sub: '다시', bg: '#C77E47', text: '#FFFCF7' },
          { k: 'med', label: '애매', sub: '5분 후', bg: theme.bgRaised, text: theme.ink },
          { k: 'easy', label: '기억남', sub: '내일', bg: theme.sage, text: '#FFFCF7' },
        ].map(b => (
          <button key={b.k} onClick={() => grade(b.k)} style={{
            flex: 1, padding: '12px 0',
            background: b.bg, color: b.text,
            border: `1px solid ${b.k === 'med' ? theme.hairline : 'transparent'}`,
            borderRadius: 16, cursor: 'pointer', fontFamily: 'inherit',
            display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2,
            boxShadow: b.k === 'med' ? theme.cardShadow : `0 6px 14px -4px ${b.bg}66`,
          }}>
            <span style={{ fontSize: 14, fontWeight: 600, letterSpacing: -0.2 }}>{b.label}</span>
            <span style={{ fontSize: 10.5, opacity: 0.85 }}>{b.sub}</span>
          </button>
        ))}
      </div>
    </div>
  );
}

Object.assign(window, { ReviewScreen, FlipCardBody, REVIEW_DATA });
