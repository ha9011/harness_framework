// screens-other.jsx — Words list, Word detail, Patterns list, Pattern detail, Generate, History, Settings.

// ─── Words list + register ──────────────────────────────
function WordsScreen({ theme, type, onNav }) {
  const [mode, setMode] = React.useState('single');
  return (
    <div style={{ height: '100%', overflow: 'auto', paddingBottom: 96, background: theme.bg, fontFamily: type.body }}>
      <TopBar theme={theme} title="단어" onBack={() => onNav('home')} subtitle="248개 등록됨" right={
        <button style={{ width: 32, height: 32, borderRadius: 16, border: 'none', background: theme.primary, color: '#FFFCF7', cursor: 'pointer' }}>
          <svg width="14" height="14" viewBox="0 0 14 14" style={{ display: 'block', margin: 'auto' }}><path d="M7 2v10M2 7h10" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/></svg>
        </button>
      }/>

      {/* register */}
      <div style={{ padding: 16 }}>
        <div style={{ background: theme.bgRaised, borderRadius: 18, padding: 14, border: `1px solid ${theme.hairline}` }}>
          <TabPills theme={theme} active={mode} onChange={setMode} tabs={[
            { key: 'single', label: '단건' },
            { key: 'bulk', label: 'JSON' },
            { key: 'image', label: '이미지' },
          ]}/>
          <div style={{ marginTop: 12 }}>
            {mode === 'single' && (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                <div style={{ padding: '12px 14px', background: theme.bgSoft, borderRadius: 12, border: `1px solid ${theme.hairline}`, fontSize: 13.5, color: theme.inkMuted }}>cozy</div>
                <div style={{ padding: '12px 14px', background: theme.bgSoft, borderRadius: 12, border: `1px solid ${theme.hairline}`, fontSize: 13.5, color: theme.inkMuted }}>아늑한, 편안한</div>
                <CafeButton theme={theme} full>저장</CafeButton>
              </div>
            )}
            {mode === 'bulk' && (
              <div>
                <div style={{ padding: 12, background: theme.bgSoft, borderRadius: 12, fontFamily: 'monospace', fontSize: 11.5, color: theme.inkMuted, lineHeight: 1.6, minHeight: 100 }}>
                  {`[\n  { "word": "cozy", "meaning": "아늑한" },\n  { "word": "reluctant", "meaning": "꺼리는" }\n]`}
                </div>
                <div style={{ display: 'flex', gap: 6, marginTop: 10 }}>
                  <Chip theme={theme} variant="sage">✓ 12 saved</Chip>
                  <Chip theme={theme}>↺ 2 skipped</Chip>
                  <Chip theme={theme} variant="warm">⚠ 1 failed</Chip>
                </div>
              </div>
            )}
            {mode === 'image' && (
              <div style={{ border: `2px dashed ${theme.primary}77`, borderRadius: 14, padding: '24px 12px', textAlign: 'center', background: theme.bgSoft }}>
                <div style={{ fontSize: 28, marginBottom: 6 }}>📷</div>
                <div style={{ fontSize: 13, color: theme.ink, fontWeight: 600 }}>교재 사진 / 캡쳐 올리기</div>
                <div style={{ fontSize: 11.5, color: theme.inkMuted, marginTop: 4 }}>AI가 단어를 자동 추출해요</div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* search */}
      <div style={{ padding: '0 16px', display: 'flex', gap: 8 }}>
        <div style={{ flex: 1, padding: '10px 14px', background: theme.bgRaised, borderRadius: 12, border: `1px solid ${theme.hairline}`, fontSize: 13, color: theme.inkMuted, display: 'flex', alignItems: 'center', gap: 8 }}>
          <span>🔍</span><span>단어 검색</span>
        </div>
        <button style={{ padding: '0 14px', background: theme.bgRaised, borderRadius: 12, border: `1px solid ${theme.hairline}`, fontSize: 13, color: theme.ink, fontFamily: 'inherit' }}>⭐ 중요</button>
      </div>

      {/* list */}
      <SectionHeader theme={theme} title="단어 목록" action="알파벳순" />
      <div style={{ padding: '0 16px', display: 'flex', flexDirection: 'column', gap: 6 }}>
        {[
          { w: 'cozy', m: '아늑한, 편안한', pos: 'adj', star: true },
          { w: 'reluctant', m: '꺼리는, 마지못해 하는', pos: 'adj' },
          { w: 'hesitate', m: '망설이다, 주저하다', pos: 'v', star: true },
          { w: 'oblige', m: '의무를 지우다', pos: 'v' },
          { w: 'serene', m: '고요한, 평화로운', pos: 'adj' },
          { w: 'linger', m: '오래 머물다', pos: 'v' },
        ].map((it, i) => (
          <div key={i} style={{ background: theme.bgRaised, borderRadius: 14, padding: '12px 14px', display: 'flex', alignItems: 'center', gap: 10, border: `1px solid ${theme.hairline}` }}>
            <div style={{ flex: 1 }}>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: 8 }}>
                <span style={{ fontFamily: type.serif, fontSize: 16, fontWeight: 600, color: theme.ink, letterSpacing: -0.3 }}>{it.w}</span>
                <span style={{ fontSize: 10.5, color: theme.primary, fontStyle: 'italic' }}>{it.pos}.</span>
              </div>
              <div style={{ fontSize: 12.5, color: theme.inkMuted, marginTop: 2 }}>{it.m}</div>
            </div>
            <span style={{ fontSize: 16, color: it.star ? '#E8C97B' : theme.inkMuted, opacity: it.star ? 1 : 0.4 }}>⭐</span>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── Word detail ──────────────────────────────
function WordDetailScreen({ theme, type, onNav }) {
  return (
    <div style={{ height: '100%', overflow: 'auto', paddingBottom: 96, background: theme.bg, fontFamily: type.body }}>
      <TopBar theme={theme} title="단어 상세" onBack={() => onNav('words')} right={
        <button style={{ width: 32, height: 32, borderRadius: 16, border: 'none', background: theme.bgSoft, color: theme.ink, cursor: 'pointer' }}>⋯</button>
      }/>
      <div style={{ padding: 16 }}>
        <div style={{ background: theme.bgRaised, borderRadius: 20, padding: 18, border: `1px solid ${theme.hairline}`, boxShadow: theme.cardShadow }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Chip theme={theme} variant="warm">adj.</Chip>
            <span style={{ fontSize: 12, color: theme.inkMuted }}>/ˈkoʊzi/</span>
            <span style={{ marginLeft: 'auto', fontSize: 18, color: '#E8C97B' }}>⭐</span>
          </div>
          <div style={{ fontFamily: type.serif, fontSize: 36, fontWeight: 600, color: theme.ink, marginTop: 8, letterSpacing: -0.6 }}>cozy</div>
          <div style={{ fontFamily: type.heading, fontSize: 16, color: theme.inkSoft, marginTop: 4 }}>아늑한, 편안한</div>
          <div style={{ marginTop: 14, padding: '10px 12px', borderRadius: 12, background: theme.sageBg, color: theme.sage, fontSize: 12.5, lineHeight: 1.5, border: `1px dashed ${theme.sage}55` }}>
            💡 카페나 작은 공간의 따뜻한 분위기를 묘사할 때
          </div>
          <div style={{ display: 'flex', gap: 6, marginTop: 12, flexWrap: 'wrap' }}>
            <Chip theme={theme}>snug</Chip>
            <Chip theme={theme}>warm</Chip>
            <Chip theme={theme}>comfortable</Chip>
          </div>
        </div>

        <SectionHeader theme={theme} title="이 단어가 사용된 예문" action="+ 예문 추가" />
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {[
            { e: 'What a cozy little cafe this is!', s: '카페 첫 방문' },
            { e: 'The blanket feels so cozy on a cold night.', s: '겨울 밤 거실' },
            { e: 'We found a cozy corner by the window.', s: '데이트 분위기' },
          ].map((it, i) => (
            <div key={i} style={{ background: theme.bgRaised, borderRadius: 14, padding: '12px 14px', border: `1px solid ${theme.hairline}` }}>
              <div style={{ display: 'inline-flex', padding: '3px 8px', background: theme.sageBg, color: theme.sage, fontSize: 10.5, borderRadius: 99, marginBottom: 6 }}>🎭 {it.s}</div>
              <div style={{ fontFamily: type.serif, fontSize: 14, color: theme.ink, lineHeight: 1.5 }}>{it.e}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ─── Patterns list ──────────────────────────────
function PatternsScreen({ theme, type, onNav }) {
  return (
    <div style={{ height: '100%', overflow: 'auto', paddingBottom: 96, background: theme.bg, fontFamily: type.body }}>
      <TopBar theme={theme} title="패턴" onBack={() => onNav('home')} subtitle="36개 등록됨" right={
        <button style={{ width: 32, height: 32, borderRadius: 16, border: 'none', background: theme.primary, color: '#FFFCF7' }}>+</button>
      }/>
      <div style={{ padding: 16 }}>
        <div style={{ background: theme.bgRaised, borderRadius: 18, padding: 14, border: `1px solid ${theme.hairline}`, display: 'flex', gap: 10 }}>
          <CafeButton theme={theme} variant="secondary" size="sm" style={{ flex: 1 }}>✏️ 직접 입력</CafeButton>
          <CafeButton theme={theme} variant="secondary" size="sm" style={{ flex: 1 }}>📷 이미지</CafeButton>
        </div>
      </div>
      <SectionHeader theme={theme} title="패턴 목록" action="등록일순" />
      <div style={{ padding: '0 16px', display: 'flex', flexDirection: 'column', gap: 8 }}>
        {[
          { p: 'I’m about to ~', d: '막 ~하려는 참이야', e: 'I’m about to head out.' },
          { p: 'It depends on ~', d: '~에 달렸어', e: 'It depends on the weather.' },
          { p: 'How come ~?', d: '어쩌다 ~?', e: 'How come you’re here?' },
          { p: 'I might as well ~', d: '~하는 게 낫겠어', e: 'I might as well stay.' },
          { p: 'No wonder ~', d: '~한 게 당연하지', e: 'No wonder she’s tired.' },
        ].map((it, i) => (
          <div key={i} style={{ background: theme.bgRaised, borderRadius: 16, padding: 14, border: `1px solid ${theme.hairline}` }}>
            <div style={{ fontFamily: type.serif, fontSize: 17, fontWeight: 600, color: theme.ink, letterSpacing: -0.3 }}>{it.p}</div>
            <div style={{ fontSize: 12.5, color: theme.inkSoft, marginTop: 3 }}>{it.d}</div>
            <div style={{ fontSize: 12, color: theme.inkMuted, marginTop: 8, fontStyle: 'italic', fontFamily: type.serif }}>e.g. {it.e}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── Pattern detail ──────────────────────────────
function PatternDetailScreen({ theme, type, onNav }) {
  const [open, setOpen] = React.useState(0);
  return (
    <div style={{ height: '100%', overflow: 'auto', paddingBottom: 96, background: theme.bg, fontFamily: type.body }}>
      <TopBar theme={theme} title="패턴 상세" onBack={() => onNav('patterns')} right={<button style={{ width: 32, height: 32, borderRadius: 16, border: 'none', background: theme.bgSoft, color: theme.ink }}>⋯</button>}/>
      <div style={{ padding: 16 }}>
        <div style={{ background: theme.bgRaised, borderRadius: 20, padding: 18, border: `1px solid ${theme.hairline}`, boxShadow: theme.cardShadow }}>
          <Chip theme={theme} variant="warm">패턴</Chip>
          <div style={{ fontFamily: type.serif, fontSize: 26, fontWeight: 600, color: theme.ink, marginTop: 10, letterSpacing: -0.4 }}>I’m about to ~</div>
          <div style={{ fontSize: 13.5, color: theme.inkSoft, marginTop: 4, lineHeight: 1.5 }}>막 ~하려는 참이야 — 곧 일어날 일을 말할 때 (1~2분 안의 가까운 미래)</div>
        </div>

        <SectionHeader theme={theme} title="교재 예문 (1~5)" />
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          {[
            'I’m about to leave the office.',
            'She’s about to call you back.',
            'They were about to start dinner.',
            'I was just about to text you!',
            'He’s about to make a decision.',
          ].map((e, i) => (
            <div key={i} style={{ background: theme.bgRaised, borderRadius: 12, padding: '10px 14px', display: 'flex', gap: 10, border: `1px solid ${theme.hairline}` }}>
              <span style={{ fontFamily: type.serif, color: theme.primary, fontWeight: 600, fontSize: 13 }}>{i + 1}</span>
              <span style={{ flex: 1, fontFamily: type.serif, fontSize: 13.5, color: theme.ink }}>{e}</span>
            </div>
          ))}
        </div>

        <SectionHeader theme={theme} title="AI 생성 예문" action="+ 예문 생성" />
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          {[
            { e: 'I’m about to head out — want anything?', s: '퇴근 전 동료에게', t: '나 곧 나가는데, 뭐 필요해?' },
            { e: 'She’s about to fall asleep on the couch.', s: '거실 풍경 묘사', t: '걔 거의 소파에서 잠들기 직전이야.' },
          ].map((it, i) => (
            <div key={i} onClick={() => setOpen(open === i ? -1 : i)} style={{ background: theme.bgRaised, borderRadius: 14, padding: 14, cursor: 'pointer', border: `1px solid ${theme.hairline}` }}>
              <SituationCloud theme={theme} style={{ marginBottom: 8 }}>{it.s}</SituationCloud>
              <div style={{ fontFamily: type.serif, fontSize: 14, color: theme.ink, lineHeight: 1.5 }}>{it.e}</div>
              {open === i && (
                <div style={{ marginTop: 10, paddingTop: 10, borderTop: `1px dashed ${theme.hairline}`, fontSize: 13, color: theme.inkSoft, lineHeight: 1.5 }}>
                  {it.t}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ─── Generate ──────────────────────────────
function GenerateScreen({ theme, type, onNav }) {
  const [level, setLevel] = React.useState('mid');
  const [count, setCount] = React.useState(10);
  const [loading, setLoading] = React.useState(false);
  const [done, setDone] = React.useState(true);
  const levels = [
    { k: 'kid', l: '유아', sub: '간단한 일상' },
    { k: 'low', l: '초등', sub: '카페 주문 수준' },
    { k: 'mid', l: '중등', sub: '친구랑 카톡 수준' },
    { k: 'high', l: '고등', sub: '의견 표현 가능' },
  ];

  return (
    <div style={{ height: '100%', overflow: 'auto', paddingBottom: 96, background: theme.bg, fontFamily: type.body }}>
      <TopBar theme={theme} title="예문 생성" onBack={() => onNav('home')}/>
      <div style={{ padding: 16 }}>
        <div style={{ background: theme.bgRaised, borderRadius: 18, padding: 16, border: `1px solid ${theme.hairline}` }}>
          <div style={{ fontSize: 12, color: theme.inkMuted, fontWeight: 600, marginBottom: 8, textTransform: 'uppercase', letterSpacing: 0.3 }}>난이도</div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
            {levels.map(L => {
              const a = L.k === level;
              return (
                <button key={L.k} onClick={() => setLevel(L.k)} style={{
                  padding: '12px 10px', borderRadius: 14,
                  background: a ? theme.primarySoft : theme.bgSoft,
                  border: `1px solid ${a ? theme.primary : theme.hairline}`,
                  color: a ? theme.primaryDeep : theme.ink,
                  fontFamily: 'inherit', cursor: 'pointer', textAlign: 'left',
                }}>
                  <div style={{ fontSize: 14, fontWeight: 600 }}>{L.l}</div>
                  <div style={{ fontSize: 11, color: a ? theme.primaryDeep : theme.inkMuted, marginTop: 2 }}>{L.sub}</div>
                </button>
              );
            })}
          </div>

          <div style={{ fontSize: 12, color: theme.inkMuted, fontWeight: 600, marginTop: 16, marginBottom: 8, textTransform: 'uppercase', letterSpacing: 0.3 }}>개수</div>
          <div style={{ display: 'flex', gap: 6 }}>
            {[10, 20, 30].map(n => {
              const a = n === count;
              return (
                <button key={n} onClick={() => setCount(n)} style={{
                  flex: 1, padding: '11px 0', borderRadius: 12,
                  background: a ? theme.primary : theme.bgSoft,
                  color: a ? '#FFFCF7' : theme.ink,
                  border: `1px solid ${a ? theme.primary : theme.hairline}`,
                  fontFamily: 'inherit', fontSize: 14, fontWeight: 600, cursor: 'pointer',
                }}>{n}개</button>
              );
            })}
          </div>

          <div style={{ marginTop: 14 }}>
            <CafeButton theme={theme} full size="lg" onClick={() => { setLoading(true); setDone(false); setTimeout(() => { setLoading(false); setDone(true); }, 1800); }}>✨ 예문 생성</CafeButton>
          </div>
        </div>

        {loading && (
          <div style={{ marginTop: 20, padding: '32px 16px', background: theme.bgRaised, borderRadius: 18, textAlign: 'center', border: `1px solid ${theme.hairline}` }}>
            <div style={{ fontSize: 36, marginBottom: 8 }}>☕</div>
            <div style={{ fontSize: 14, fontWeight: 600, color: theme.ink, fontFamily: type.heading }}>커피를 내리는 중…</div>
            <div style={{ fontSize: 12, color: theme.inkMuted, marginTop: 4 }}>Gemini가 예문을 정성껏 만들고 있어요</div>
            <div style={{ marginTop: 16, height: 4, background: theme.bgSoft, borderRadius: 2, overflow: 'hidden' }}>
              <div style={{ height: '100%', width: '60%', background: theme.primary, borderRadius: 2 }}/>
            </div>
          </div>
        )}

        {done && !loading && (
          <>
            <SectionHeader theme={theme} title="생성 결과 (10개)" action="저장" />
            <div style={{ padding: '0', display: 'flex', flexDirection: 'column', gap: 8 }}>
              {[
                { e: 'I’m about to grab a coffee, want one?', s: '점심 시간 사무실', t: '나 커피 사러 가는데, 너도 마실래?', tags: ['I’m about to ~', 'grab'] },
                { e: 'She’s about to leave for the airport.', s: '공항 가기 전', t: '걔 곧 공항으로 출발해.', tags: ['I’m about to ~', 'leave'] },
                { e: 'I was about to text you!', s: '우연한 타이밍', t: '나 너한테 막 문자하려던 참이었어!', tags: ['I’m about to ~'] },
              ].map((it, i) => (
                <div key={i} style={{ background: theme.bgRaised, borderRadius: 14, padding: 14, border: `1px solid ${theme.hairline}` }}>
                  <SituationCloud theme={theme} style={{ marginBottom: 8 }}>{it.s}</SituationCloud>
                  <div style={{ fontFamily: type.serif, fontSize: 14, color: theme.ink, lineHeight: 1.5 }}>{it.e}</div>
                  <div style={{ fontSize: 12.5, color: theme.inkSoft, marginTop: 6, paddingTop: 8, borderTop: `1px dashed ${theme.hairline}` }}>{it.t}</div>
                  <div style={{ display: 'flex', gap: 6, marginTop: 8, flexWrap: 'wrap' }}>
                    {it.tags.map((t, j) => <Chip key={j} theme={theme} variant="sage">📌 {t}</Chip>)}
                  </div>
                </div>
              ))}
            </div>
          </>
        )}
      </div>
    </div>
  );
}

// ─── History ──────────────────────────────
function HistoryScreen({ theme, type, onNav }) {
  return (
    <div style={{ height: '100%', overflow: 'auto', paddingBottom: 96, background: theme.bg, fontFamily: type.body }}>
      <TopBar theme={theme} title="학습 기록" onBack={() => onNav('home')}/>
      <div style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 14 }}>
        {[
          { d: 'Day 12', date: '5월 4일 (화)', p: ['I’m about to ~'], w: ['cozy', 'reluctant', 'hesitate'] },
          { d: 'Day 11', date: '5월 3일 (월)', p: ['It depends on ~'], w: ['serene', 'linger', 'oblige'] },
          { d: 'Day 10', date: '5월 2일 (일)', p: ['How come ~?'], w: ['ponder', 'savor'] },
          { d: 'Day 9', date: '5월 1일 (토)', p: ['I might as well ~'], w: ['embark', 'unwind'] },
        ].map((d, i) => (
          <div key={i} style={{ background: theme.bgRaised, borderRadius: 18, padding: 16, border: `1px solid ${theme.hairline}` }}>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 10 }}>
              <div style={{ fontFamily: type.serif, fontSize: 22, fontWeight: 600, color: theme.primary, letterSpacing: -0.3 }}>{d.d}</div>
              <div style={{ fontSize: 12, color: theme.inkMuted }}>{d.date}</div>
            </div>
            <div style={{ marginTop: 10, fontSize: 11, color: theme.inkMuted, fontWeight: 600, textTransform: 'uppercase', letterSpacing: 0.3 }}>등록한 패턴</div>
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginTop: 4 }}>
              {d.p.map((p, j) => <Chip key={j} theme={theme} variant="warm">{p}</Chip>)}
            </div>
            <div style={{ marginTop: 10, fontSize: 11, color: theme.inkMuted, fontWeight: 600, textTransform: 'uppercase', letterSpacing: 0.3 }}>등록한 단어</div>
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginTop: 4 }}>
              {d.w.map((w, j) => <Chip key={j} theme={theme}>{w}</Chip>)}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── Settings ──────────────────────────────
function SettingsScreen({ theme, type, onNav }) {
  const [count, setCount] = React.useState(20);
  return (
    <div style={{ height: '100%', overflow: 'auto', paddingBottom: 96, background: theme.bg, fontFamily: type.body }}>
      <TopBar theme={theme} title="설정" onBack={() => onNav('home')}/>
      <div style={{ padding: 16 }}>
        <div style={{ background: theme.bgRaised, borderRadius: 18, padding: 16, border: `1px solid ${theme.hairline}` }}>
          <div style={{ fontSize: 14, fontWeight: 600, color: theme.ink }}>하루 복습 개수</div>
          <div style={{ fontSize: 12, color: theme.inkMuted, marginTop: 4, lineHeight: 1.5 }}>타입별 N개씩 = 총 3N장의 카드를 받아요</div>
          <div style={{ display: 'flex', gap: 8, marginTop: 14 }}>
            {[10, 20, 30].map(n => {
              const a = n === count;
              return (
                <button key={n} onClick={() => setCount(n)} style={{
                  flex: 1, padding: '12px 0', borderRadius: 14,
                  background: a ? theme.primarySoft : theme.bgSoft,
                  border: `1px solid ${a ? theme.primary : theme.hairline}`,
                  color: a ? theme.primaryDeep : theme.ink,
                  fontFamily: 'inherit', fontWeight: 600, fontSize: 14, cursor: 'pointer',
                }}>
                  <div>{n}</div>
                  <div style={{ fontSize: 10, marginTop: 2, opacity: 0.8 }}>총 {n*3}장</div>
                </button>
              );
            })}
          </div>
        </div>

        <SectionHeader theme={theme} title="알림" />
        <div style={{ background: theme.bgRaised, borderRadius: 18, border: `1px solid ${theme.hairline}`, overflow: 'hidden' }}>
          {[
            { l: '복습 리마인더', r: '오전 8시' },
            { l: '연속 깨질 위험 알림', r: '켜짐' },
            { l: '주간 리포트', r: '일요일' },
          ].map((it, i, a) => (
            <div key={i} style={{ display: 'flex', alignItems: 'center', padding: '14px 16px', borderBottom: i < a.length - 1 ? `1px solid ${theme.divider}` : 'none' }}>
              <div style={{ flex: 1, fontSize: 14, color: theme.ink }}>{it.l}</div>
              <div style={{ fontSize: 13, color: theme.inkMuted }}>{it.r}</div>
              <div style={{ marginLeft: 8, color: theme.inkMuted }}>›</div>
            </div>
          ))}
        </div>

        <SectionHeader theme={theme} title="계정" />
        <div style={{ background: theme.bgRaised, borderRadius: 18, border: `1px solid ${theme.hairline}`, overflow: 'hidden' }}>
          {['데이터 백업', '데이터 내보내기', '로그아웃'].map((l, i, a) => (
            <div key={i} style={{ display: 'flex', alignItems: 'center', padding: '14px 16px', borderBottom: i < a.length - 1 ? `1px solid ${theme.divider}` : 'none' }}>
              <div style={{ flex: 1, fontSize: 14, color: i === 2 ? '#C77E47' : theme.ink }}>{l}</div>
              <div style={{ color: theme.inkMuted }}>›</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { WordsScreen, WordDetailScreen, PatternsScreen, PatternDetailScreen, GenerateScreen, HistoryScreen, SettingsScreen });
