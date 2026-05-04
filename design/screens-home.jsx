// screens-home.jsx — Home dashboard, 4 variations.

const HomeStats = ({ theme, t }) => (
  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8, padding: '0 16px' }}>
    {[
      { n: 248, l: '단어' },
      { n: 36, l: '패턴' },
      { n: 412, l: '예문' },
      { n: 12, l: '연속일', accent: true },
    ].map((s, i) => (
      <div key={i} style={{
        background: s.accent ? theme.primarySoft : theme.bgRaised,
        border: `1px solid ${theme.hairline}`,
        borderRadius: 14, padding: '12px 6px', textAlign: 'center',
      }}>
        <div style={{ fontFamily: theme.serif || 'Fraunces, serif', fontSize: 22, fontWeight: 600, color: s.accent ? theme.primaryDeep : theme.ink, lineHeight: 1 }}>
          {s.accent ? '🔥' : ''}{s.n}
        </div>
        <div style={{ fontSize: 11, color: s.accent ? theme.primaryDeep : theme.inkMuted, marginTop: 4 }}>{s.l}</div>
      </div>
    ))}
  </div>
);

// VARIATION A — Classic: garden front and center, big CTA, type breakdown
function HomeA({ theme, type, treeStage, completedCount = 2, wilt = 0, recovering = false, dayInCycle = 16, onNav, onStart }) {
  const dark = theme.bg.startsWith('#1') || theme.bg.startsWith('#2');
  return (
    <div style={{ height: '100%', overflow: 'auto', paddingBottom: 96, fontFamily: type.body }}>
      {/* greeting */}
      <div style={{ padding: '14px 16px 10px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <div style={{ fontSize: 12, color: theme.inkMuted }}>좋은 아침이에요 ☀️</div>
          <div style={{ fontSize: 20, fontFamily: type.heading, fontWeight: type.headingWeight, color: theme.ink, letterSpacing: -0.3, marginTop: 2 }}>오늘도 한 모금 천천히</div>
        </div>
        <div style={{ width: 36, height: 36, borderRadius: 18, background: theme.bgKraft, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 14 }}>🍵</div>
      </div>

      {/* Garden hero — windowsill with multiple pots */}
      <div style={{ margin: '8px 16px 0', borderRadius: 24,
        background: theme.bgRaised,
        border: `1px solid ${theme.hairline}`,
        boxShadow: theme.cardShadow,
        overflow: 'hidden',
      }}>
        <div style={{ padding: '12px 16px 4px', display: 'flex', alignItems: 'baseline', justifyContent: 'space-between' }}>
          <div style={{ fontSize: 11, color: theme.sage, fontWeight: 600, letterSpacing: 0.4, textTransform: 'uppercase' }}>나의 카페 정원</div>
          <div style={{ fontSize: 11, color: theme.inkMuted }}>완성된 나무 <b style={{ color: theme.primary }}>{completedCount}그루</b></div>
        </div>
        <CoffeeGarden completedCount={completedCount} currentStage={treeStage} wilt={wilt} dark={dark} recovering={recovering} size={300}/>
        {/* Cycle progress bar */}
        <div style={{ padding: '10px 16px 14px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, color: theme.inkMuted, marginBottom: 5 }}>
            <span>현재 나무 · Day {dayInCycle}/30</span>
            <span style={{ color: wilt > 0.3 ? '#C77E47' : theme.sage, fontWeight: 600 }}>
              {wilt > 0.3 ? `🥀 ${Math.round(wilt * 100)}% 시들음` : `🔥 ${dayInCycle}일째`}
            </span>
          </div>
          <div style={{ height: 6, background: theme.bgSoft, borderRadius: 3, overflow: 'hidden', position: 'relative' }}>
            <div style={{ height: '100%', width: `${(dayInCycle / 30) * 100}%`, background: `linear-gradient(90deg, ${theme.sage}, ${theme.primary})`, borderRadius: 3, transition: 'width 0.5s ease' }}/>
            {/* milestones */}
            {[7, 14, 21].map(m => (
              <div key={m} style={{ position: 'absolute', top: 0, left: `${(m / 30) * 100}%`, width: 1, height: '100%', background: theme.bgRaised, opacity: 0.6 }}/>
            ))}
          </div>
          <div style={{ fontSize: 11, color: theme.inkMuted, marginTop: 6, fontStyle: 'italic', textAlign: 'center' }}>
            {wilt > 0.5 ? '복습을 다시 시작하면 나무가 회복돼요 🌿'
              : wilt > 0.2 ? '오늘 복습으로 다시 살릴 수 있어요'
              : dayInCycle >= 28 ? '곧 원두 수확! 새 화분이 도착할 거예요 ☕'
              : `${30 - dayInCycle}일 후 원두 수확`}
          </div>
        </div>
      </div>

      {/* Today's review breakdown */}
      <SectionHeader title="오늘의 복습" theme={theme} action="설정" />
      <div style={{ padding: '0 16px', display: 'flex', flexDirection: 'column', gap: 8 }}>
        {[
          { k: 'word', label: '단어', n: 15, hint: '영→한 / 한→영' },
          { k: 'pattern', label: '패턴', n: 8, hint: '교재 예문 포함' },
          { k: 'sentence', label: '문장', n: 20, hint: '상황과 함께' },
        ].map(r => (
          <div key={r.k} style={{
            background: theme.bgRaised, borderRadius: 16, padding: '12px 14px',
            border: `1px solid ${theme.hairline}`,
            display: 'flex', alignItems: 'center', gap: 12,
          }}>
            <div style={{
              width: 38, height: 38, borderRadius: 12,
              background: theme.bgSoft, display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontFamily: type.serif, fontSize: 17, fontWeight: 600, color: theme.primary,
            }}>{r.n}</div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 14, fontWeight: 600, color: theme.ink }}>{r.label}</div>
              <div style={{ fontSize: 11.5, color: theme.inkMuted, marginTop: 1 }}>{r.hint}</div>
            </div>
            <div style={{ fontSize: 12, color: theme.primary, fontWeight: 600 }}>{r.n}장 →</div>
          </div>
        ))}
      </div>

      {/* CTA */}
      <div style={{ padding: '16px 16px 0' }}>
        <CafeButton theme={theme} size="lg" full onClick={onStart}>
          <span style={{ fontSize: 17 }}>오늘의 복습 시작</span>
          <span style={{ marginLeft: 6, opacity: 0.85 }}>·  43장</span>
        </CafeButton>
      </div>

      {/* Stats */}
      <SectionHeader title="누적 학습" theme={theme} />
      <HomeStats theme={theme} t={type} />

      {/* Recent */}
      <SectionHeader title="최근 학습" theme={theme} action="모두 보기" />
      <div style={{ padding: '0 16px', display: 'flex', flexDirection: 'column', gap: 6 }}>
        {[
          { d: 'Day 12', t: 'I’m about to ~ (막 ~하려는 참이야)' },
          { d: 'Day 11', t: 'It depends on ~ (~에 달렸어)' },
          { d: 'Day 10', t: 'How come ~? (어쩌다 ~?)' },
        ].map((r, i) => (
          <div key={i} style={{ background: theme.bgRaised, borderRadius: 12, padding: '10px 14px', display: 'flex', alignItems: 'center', gap: 10, border: `1px solid ${theme.hairline}` }}>
            <div style={{ fontFamily: type.serif, fontSize: 12, color: theme.primary, fontWeight: 600, width: 46 }}>{r.d}</div>
            <div style={{ fontSize: 13, color: theme.ink, flex: 1, letterSpacing: -0.2 }}>{r.t}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

// VARIATION B — Cafe receipt vibe (kraft paper texture, ticket numbers)
function HomeB({ theme, type, treeStage, onStart }) {
  const dark = theme.bg.startsWith('#1') || theme.bg.startsWith('#2');
  return (
    <div style={{ height: '100%', overflow: 'auto', paddingBottom: 96, fontFamily: type.body, background: theme.bg }}>
      <div style={{ padding: '14px 16px 0' }}>
        <div style={{ fontSize: 11, color: theme.inkMuted, fontWeight: 600, letterSpacing: 1.5, textTransform: 'uppercase' }}>오늘의 메뉴</div>
        <div style={{ fontFamily: type.serif, fontSize: 28, color: theme.ink, fontWeight: 600, marginTop: 4, letterSpacing: -0.5 }}>Cafe English</div>
        <div style={{ fontSize: 12, color: theme.inkMuted, marginTop: 2, fontStyle: 'italic' }}>5월 4일 · 화요일 · 흐림</div>
      </div>

      {/* Receipt card */}
      <div style={{ margin: '16px 16px 0', position: 'relative' }}>
        <div style={{
          background: theme.bgKraft, borderRadius: 4,
          padding: '20px 18px 24px',
          boxShadow: theme.cardShadow,
          backgroundImage: `repeating-linear-gradient(45deg, transparent 0 12px, ${theme.hairline} 12px 13px)`,
          backgroundBlendMode: 'multiply',
        }}>
          {/* receipt header */}
          <div style={{ textAlign: 'center', borderBottom: `1.5px dashed ${theme.ink}33`, paddingBottom: 12 }}>
            <div style={{ fontFamily: type.serif, fontSize: 16, color: theme.ink, fontWeight: 600 }}>오늘의 주문</div>
            <div style={{ fontSize: 10.5, color: theme.inkMuted, marginTop: 2 }}>ORDER #00012 · 12일째</div>
          </div>
          {/* lines */}
          <div style={{ padding: '12px 4px 0', display: 'flex', flexDirection: 'column', gap: 9 }}>
            {[
              { n: '단어 복습', q: 15 },
              { n: '패턴 복습', q: 8 },
              { n: '문장 복습', q: 20 },
            ].map((it, i) => (
              <div key={i} style={{ display: 'flex', alignItems: 'baseline', fontSize: 14, color: theme.ink, fontFamily: type.serif }}>
                <span style={{ flexShrink: 0 }}>{it.n}</span>
                <span style={{ flex: 1, borderBottom: `1px dotted ${theme.ink}55`, margin: '0 6px', height: 1, position: 'relative', top: -3 }} />
                <span style={{ flexShrink: 0, fontWeight: 600 }}>x {it.q}</span>
              </div>
            ))}
            <div style={{ borderTop: `1.5px dashed ${theme.ink}33`, marginTop: 8, paddingTop: 10, display: 'flex', justifyContent: 'space-between', fontFamily: type.serif, fontSize: 16, fontWeight: 600, color: theme.ink }}>
              <span>합계</span>
              <span>43 장</span>
            </div>
          </div>
          {/* tree corner */}
          <div style={{ position: 'absolute', right: -6, bottom: -10, transform: 'rotate(8deg)' }}>
            <CoffeeTree stage={treeStage} size={86} dark={dark} />
          </div>
        </div>
        {/* receipt edge */}
        <div style={{ height: 12, background: `radial-gradient(circle at 8px 0, ${theme.bg} 7px, ${theme.bgKraft} 8px)`, backgroundSize: '16px 12px', backgroundRepeat: 'repeat-x' }} />
      </div>

      <div style={{ padding: '20px 16px 0' }}>
        <CafeButton theme={theme} size="lg" full onClick={onStart}>
          <span>주문 받기 (복습 시작)</span>
        </CafeButton>
      </div>

      <SectionHeader title="누적 적립" theme={theme} />
      <HomeStats theme={theme} t={type} />

      <SectionHeader title="오늘의 한 줄" theme={theme} />
      <div style={{ padding: '0 16px' }}>
        <div style={{
          background: theme.bgRaised, borderRadius: 16, padding: 16,
          border: `1px dashed ${theme.primary}66`,
          fontFamily: type.serif, fontStyle: 'italic',
          color: theme.ink, fontSize: 14, lineHeight: 1.6,
        }}>“A small cup, taken slowly, makes a long day shorter.”</div>
      </div>
    </div>
  );
}

// VARIATION C — Minimal: focus on streak + single CTA
function HomeC({ theme, type, treeStage, completedCount = 2, wilt = 0, recovering = false, onStart }) {
  const dark = theme.bg.startsWith('#1') || theme.bg.startsWith('#2');
  return (
    <div style={{ height: '100%', overflow: 'auto', paddingBottom: 96, fontFamily: type.body, background: theme.bg }}>
      <div style={{ padding: '20px 20px 0', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ fontSize: 13, color: theme.inkMuted, fontWeight: 500 }}>5월 4일</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '6px 12px', background: theme.primarySoft, borderRadius: 999, fontSize: 12, color: theme.primaryDeep, fontWeight: 600 }}>🔥 12일</div>
      </div>

      {/* Hero: garden */}
      <div style={{ padding: '12px 16px 0' }}>
        <CoffeeGarden completedCount={completedCount} currentStage={treeStage} wilt={wilt} dark={dark} recovering={recovering} size={300} showWindow={false}/>
      </div>
      <div style={{ textAlign: 'center', padding: '0 20px' }}>
        <div style={{ fontFamily: type.serif, fontSize: 30, fontWeight: 600, color: theme.ink, letterSpacing: -0.6 }}>43</div>
        <div style={{ fontSize: 13, color: theme.inkMuted, marginTop: 2 }}>장 남음 · 오늘의 복습</div>
      </div>

      {/* breakdown row */}
      <div style={{ padding: '20px 20px 0', display: 'flex', justifyContent: 'center', gap: 6 }}>
        {[{ l: '단어', n: 15 },{ l: '패턴', n: 8 },{ l: '문장', n: 20 }].map((r, i) => (
          <div key={i} style={{
            flex: 1, padding: '10px 8px', borderRadius: 14,
            background: theme.bgRaised, border: `1px solid ${theme.hairline}`,
            textAlign: 'center',
          }}>
            <div style={{ fontFamily: type.serif, fontSize: 18, fontWeight: 600, color: theme.ink }}>{r.n}</div>
            <div style={{ fontSize: 11, color: theme.inkMuted, marginTop: 2 }}>{r.l}</div>
          </div>
        ))}
      </div>

      <div style={{ padding: '20px 20px 0' }}>
        <CafeButton theme={theme} size="lg" full onClick={onStart}>오늘의 복습 시작</CafeButton>
      </div>

      <SectionHeader title="최근" theme={theme} />
      <div style={{ padding: '0 20px', display: 'flex', flexDirection: 'column', gap: 4 }}>
        {[
          { d: '12', t: 'I’m about to ~' },
          { d: '11', t: 'It depends on ~' },
          { d: '10', t: 'How come ~?' },
        ].map((r, i) => (
          <div key={i} style={{ display: 'flex', gap: 14, padding: '10px 0', borderBottom: i < 2 ? `1px solid ${theme.divider}` : 'none' }}>
            <div style={{ fontFamily: type.serif, color: theme.primary, fontWeight: 600, width: 32, fontSize: 14 }}>D{r.d}</div>
            <div style={{ fontSize: 13, color: theme.ink, flex: 1, letterSpacing: -0.2 }}>{r.t}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

// VARIATION D — Garden: two-column landscape with widgets
function HomeD({ theme, type, treeStage, onStart }) {
  const dark = theme.bg.startsWith('#1') || theme.bg.startsWith('#2');
  return (
    <div style={{ height: '100%', overflow: 'auto', paddingBottom: 96, fontFamily: type.body }}>
      <div style={{ padding: '14px 16px 8px' }}>
        <div style={{ fontSize: 12, color: theme.inkMuted }}>안녕, 지원!</div>
        <div style={{ fontFamily: type.heading, fontSize: 22, fontWeight: type.headingWeight, color: theme.ink, marginTop: 2, letterSpacing: -0.4 }}>나의 작은 카페</div>
      </div>

      {/* big tree with floor */}
      <div style={{ position: 'relative', margin: '0 16px', borderRadius: 22,
        background: `linear-gradient(180deg, ${theme.sageBg} 0%, ${theme.bgRaised} 80%)`,
        border: `1px solid ${theme.hairline}`,
        height: 220, overflow: 'hidden',
      }}>
        <div style={{ position: 'absolute', top: 12, left: 14, fontSize: 11, color: theme.sage, fontWeight: 600, letterSpacing: 0.3, textTransform: 'uppercase' }}>Day 12</div>
        <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0 }}>
          <CoffeeGarden completedCount={2} currentStage={treeStage} wilt={0} dark={dark} size={300} showWindow={false}/>
        </div>
        <div style={{ position: 'absolute', top: 14, right: 14, fontSize: 11, color: theme.inkMuted }}>
          <span style={{ color: theme.primary, fontWeight: 600 }}>+1</span> 다음 단계까지
        </div>
        {/* hovering badge */}
        <div style={{ position: 'absolute', bottom: 14, left: 14, padding: '6px 10px', background: theme.bgRaised, borderRadius: 12, fontSize: 11, fontWeight: 600, color: theme.ink, boxShadow: theme.cardShadow }}>
          ☕ 원두 14알 수확
        </div>
      </div>

      {/* widget grid */}
      <div style={{ padding: '12px 16px 0', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
        {[
          { l: '단어', n: 15, c: theme.primary },
          { l: '패턴', n: 8, c: theme.sage },
          { l: '문장', n: 20, c: theme.warning },
          { l: '연속', n: '12d', c: theme.primaryDeep },
        ].map((w, i) => (
          <div key={i} style={{
            background: theme.bgRaised, borderRadius: 16, padding: '12px 14px',
            border: `1px solid ${theme.hairline}`,
          }}>
            <div style={{ fontSize: 11, color: theme.inkMuted, marginBottom: 6 }}>{w.l}</div>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 4 }}>
              <div style={{ fontFamily: type.serif, fontSize: 22, fontWeight: 600, color: w.c, letterSpacing: -0.3 }}>{w.n}</div>
              <div style={{ fontSize: 11, color: theme.inkMuted }}>장 남음</div>
            </div>
          </div>
        ))}
      </div>

      <div style={{ padding: '14px 16px 0' }}>
        <CafeButton theme={theme} size="lg" full onClick={onStart}>오늘의 복습 시작 →</CafeButton>
      </div>
    </div>
  );
}

Object.assign(window, { HomeA, HomeB, HomeC, HomeD });
