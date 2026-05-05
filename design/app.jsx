// app.jsx — main app: device frames + design canvas + tweaks panel.

const TWEAKS_DEFAULTS = /*EDITMODE-BEGIN*/{
  "dark": false,
  "palette": "latte",
  "typography": "warm",
  "cardStyle": "menu",
  "treeStage": 5,
  "completedTrees": 2,
  "dayInCycle": 16,
  "wilt": 0,
  "recovering": false
}/*EDITMODE-END*/;

// Mobile shell: a phone-shaped frame with our content
function Phone({ theme, type, children, label, height = 760 }) {
  const dark = theme.bg.startsWith('#1') || theme.bg.startsWith('#2');
  return (
    <div style={{
      width: 340, height, borderRadius: 44, position: 'relative',
      background: '#0a0a0a', padding: 9,
      boxShadow: '0 30px 60px -10px rgba(60,40,20,0.25), 0 0 0 1.5px rgba(0,0,0,0.5)',
    }}>
      <div style={{
        width: '100%', height: '100%', borderRadius: 36, overflow: 'hidden',
        background: theme.bg, position: 'relative',
      }}>
        {/* fake notch / dynamic island */}
        <div style={{ position: 'absolute', top: 8, left: '50%', transform: 'translateX(-50%)', width: 96, height: 26, borderRadius: 16, background: '#000', zIndex: 100 }}/>
        {/* status bar */}
        <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: 40, display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 24px', zIndex: 50, color: dark ? '#fff' : '#000', fontSize: 13, fontWeight: 600 }}>
          <span style={{ paddingTop: 14 }}>9:41</span>
          <span style={{ paddingTop: 14, display: 'flex', gap: 4, alignItems: 'center' }}>
            <svg width="16" height="10" viewBox="0 0 16 10"><rect x="0" y="6" width="2.5" height="4" rx="0.5" fill="currentColor"/><rect x="3.7" y="4" width="2.5" height="6" rx="0.5" fill="currentColor"/><rect x="7.4" y="2" width="2.5" height="8" rx="0.5" fill="currentColor"/><rect x="11.1" y="0" width="2.5" height="10" rx="0.5" fill="currentColor"/></svg>
          </span>
        </div>

        <div style={{ paddingTop: 40, height: '100%', boxSizing: 'border-box', display: 'flex', flexDirection: 'column' }}>
          {children}
        </div>
      </div>
    </div>
  );
}

function App() {
  const [tw, setTweak] = useTweaks(TWEAKS_DEFAULTS);
  const theme = makeTheme(tw.palette, tw.dark);
  const type = TYPE_PRESETS[tw.typography];
  // attach serif to theme for handy access
  theme.serif = type.serif;

  // app body bg
  React.useEffect(() => {
    document.body.style.background = tw.dark ? '#16110C' : '#EFEAE0';
  }, [tw.dark]);

  // controlled "live preview" — track navigation in one mock phone
  const [live, setLive] = React.useState('login');
  const renderLive = () => {
    const props = { theme, type, onNav: setLive };
    switch (live) {
      case 'login': return <Login {...props} onSignup={() => setLive('signup')} />;
      case 'signup': return <Signup {...props} onLogin={() => setLive('login')} />;
      case 'home': return <HomeA {...props} treeStage={tw.treeStage} completedCount={tw.completedTrees} wilt={tw.wilt} recovering={tw.recovering} dayInCycle={tw.dayInCycle} onStart={() => setLive('review')} />;
      case 'review': return <ReviewScreen {...props} cardStyle={tw.cardStyle} />;
      case 'words': return <WordsScreen {...props} />;
      case 'wordDetail': return <WordDetailScreen {...props} />;
      case 'patterns': return <PatternsScreen {...props} />;
      case 'patternDetail': return <PatternDetailScreen {...props} />;
      case 'generate': return <GenerateScreen {...props} />;
      case 'history': return <HistoryScreen {...props} />;
      case 'settings': return <SettingsScreen {...props} />;
      default: return <HomeA {...props} treeStage={tw.treeStage} onStart={() => setLive('review')} />;
    }
  };
  const isAuth = live === 'login' || live === 'signup';
  // for the live phone, decide which nav tab is active
  const liveActiveNav = ({ home: 'home', review: 'review', words: 'words', wordDetail: 'words', patterns: 'patterns', patternDetail: 'patterns', generate: 'generate', history: 'home', settings: 'home' })[live] || 'home';

  return (
    <>
      <DesignCanvas>
        {/* SECTION 1 — Live interactive preview */}
        <DCSection id="live" title="🔴 인터랙티브 프로토타입" subtitle="9개 화면 모두 실제로 동작합니다 — 탭을 눌러 이동, 카드 탭하면 뒤집어짐">
          <DCArtboard id="live-phone" label="라이브 데모 · 모든 화면 클릭 가능" width={340} height={760}>
            <Phone theme={theme} type={type}>
              <div style={{ flex: 1, position: 'relative', minHeight: 0 }}>
                {renderLive()}
              </div>
              {!isAuth && <BottomNav theme={theme} active={liveActiveNav} onNav={(k) => {
                if (k === 'home') setLive('home');
                else if (k === 'words') setLive('words');
                else if (k === 'patterns') setLive('patterns');
                else if (k === 'generate') setLive('generate');
                else if (k === 'review') setLive('review');
              }} />}
            </Phone>
          </DCArtboard>
        </DCSection>

        {/* SECTION — Auth screens (Login / Signup) */}
        <DCSection id="auth" title="🔑 로그인 · 회원가입" subtitle="창가 자리 무드 · 깔끔한 4필드 가입">
          <DCArtboard id="login" label="로그인 · 창가 자리" width={340} height={760}>
            <Phone theme={theme} type={type}>
              <div style={{ flex: 1, position: 'relative', minHeight: 0 }}>
                <Login theme={theme} type={type} onSignup={() => {}}/>
              </div>
            </Phone>
          </DCArtboard>
          <DCArtboard id="signup" label="회원가입 · 기본 정보" width={340} height={760}>
            <Phone theme={theme} type={type}>
              <div style={{ flex: 1, position: 'relative', minHeight: 0 }}>
                <Signup theme={theme} type={type} onLogin={() => {}}/>
              </div>
            </Phone>
          </DCArtboard>
        </DCSection>

        {/* SECTION 2 — Home dashboard variations */}
        <DCSection id="home-variants" title="🏠 홈 대시보드 — 4가지 방향" subtitle="(A) 클래식 / (B) 카페 영수증 / (C) 미니멀 / (D) 정원">
          <DCArtboard id="home-a" label="A · 클래식 (창가 정원)" width={340} height={760}>
            <Phone theme={theme} type={type}>
              <div style={{ flex: 1, position: 'relative', minHeight: 0 }}>
                <HomeA theme={theme} type={type} treeStage={tw.treeStage} completedCount={tw.completedTrees} wilt={tw.wilt} recovering={tw.recovering} dayInCycle={tw.dayInCycle} onNav={() => {}} onStart={() => {}} />
              </div>
              <BottomNav theme={theme} active="home" onNav={() => {}} />
            </Phone>
          </DCArtboard>
          <DCArtboard id="home-b" label="B · 카페 영수증" width={340} height={760}>
            <Phone theme={theme} type={type}>
              <div style={{ flex: 1, position: 'relative', minHeight: 0 }}>
                <HomeB theme={theme} type={type} treeStage={tw.treeStage} onStart={() => {}} />
              </div>
              <BottomNav theme={theme} active="home" onNav={() => {}} />
            </Phone>
          </DCArtboard>
          <DCArtboard id="home-c" label="C · 미니멀" width={340} height={760}>
            <Phone theme={theme} type={type}>
              <div style={{ flex: 1, position: 'relative', minHeight: 0 }}>
                <HomeC theme={theme} type={type} treeStage={tw.treeStage} completedCount={tw.completedTrees} wilt={tw.wilt} recovering={tw.recovering} onStart={() => {}} />
              </div>
              <BottomNav theme={theme} active="home" onNav={() => {}} />
            </Phone>
          </DCArtboard>
          <DCArtboard id="home-d" label="D · 정원" width={340} height={760}>
            <Phone theme={theme} type={type}>
              <div style={{ flex: 1, position: 'relative', minHeight: 0 }}>
                <HomeD theme={theme} type={type} treeStage={tw.treeStage} onStart={() => {}} />
              </div>
              <BottomNav theme={theme} active="home" onNav={() => {}} />
            </Phone>
          </DCArtboard>
        </DCSection>

        {/* SECTION 3 — Review card style variations */}
        <DCSection id="review-variants" title="🃏 복습 플립 카드 — 4가지 카드 스타일" subtitle="카드를 탭하면 뒤집어집니다. 탭 전환도 가능">
          {[
            { k: 'menu', label: '메뉴판 (크라프트지 + 라인)' },
            { k: 'napkin', label: '냅킨 (가로 텍스처)' },
            { k: 'modern', label: '모던 (오트밀 + 컬러 백)' },
            { k: 'kraft', label: '크라프트 + 크림' },
          ].map(s => (
            <DCArtboard key={s.k} id={`rev-${s.k}`} label={s.label} width={340} height={760}>
              <Phone theme={theme} type={type}>
                <div style={{ flex: 1, position: 'relative', minHeight: 0 }}>
                  <ReviewScreen theme={theme} type={type} cardStyle={s.k} onNav={() => {}} />
                </div>
              </Phone>
            </DCArtboard>
          ))}
        </DCSection>

        {/* SECTION 4 — All other screens */}
        <DCSection id="other-screens" title="📱 나머지 화면" subtitle="단어 / 패턴 / 예문 생성 / 학습 기록 / 설정">
          <DCArtboard id="words" label="단어 목록 + 등록" width={340} height={760}>
            <Phone theme={theme} type={type}>
              <div style={{ flex: 1, position: 'relative', minHeight: 0 }}><WordsScreen theme={theme} type={type} onNav={() => {}} /></div>
              <BottomNav theme={theme} active="words" onNav={() => {}} />
            </Phone>
          </DCArtboard>
          <DCArtboard id="word-detail" label="단어 상세" width={340} height={760}>
            <Phone theme={theme} type={type}>
              <div style={{ flex: 1, position: 'relative', minHeight: 0 }}><WordDetailScreen theme={theme} type={type} onNav={() => {}} /></div>
              <BottomNav theme={theme} active="words" onNav={() => {}} />
            </Phone>
          </DCArtboard>
          <DCArtboard id="patterns" label="패턴 목록 + 등록" width={340} height={760}>
            <Phone theme={theme} type={type}>
              <div style={{ flex: 1, position: 'relative', minHeight: 0 }}><PatternsScreen theme={theme} type={type} onNav={() => {}} /></div>
              <BottomNav theme={theme} active="patterns" onNav={() => {}} />
            </Phone>
          </DCArtboard>
          <DCArtboard id="pattern-detail" label="패턴 상세" width={340} height={760}>
            <Phone theme={theme} type={type}>
              <div style={{ flex: 1, position: 'relative', minHeight: 0 }}><PatternDetailScreen theme={theme} type={type} onNav={() => {}} /></div>
              <BottomNav theme={theme} active="patterns" onNav={() => {}} />
            </Phone>
          </DCArtboard>
          <DCArtboard id="generate" label="예문 생성" width={340} height={760}>
            <Phone theme={theme} type={type}>
              <div style={{ flex: 1, position: 'relative', minHeight: 0 }}><GenerateScreen theme={theme} type={type} onNav={() => {}} /></div>
              <BottomNav theme={theme} active="generate" onNav={() => {}} />
            </Phone>
          </DCArtboard>
          <DCArtboard id="history" label="학습 기록" width={340} height={760}>
            <Phone theme={theme} type={type}>
              <div style={{ flex: 1, position: 'relative', minHeight: 0 }}><HistoryScreen theme={theme} type={type} onNav={() => {}} /></div>
              <BottomNav theme={theme} active="home" onNav={() => {}} />
            </Phone>
          </DCArtboard>
          <DCArtboard id="settings" label="설정" width={340} height={760}>
            <Phone theme={theme} type={type}>
              <div style={{ flex: 1, position: 'relative', minHeight: 0 }}><SettingsScreen theme={theme} type={type} onNav={() => {}} /></div>
              <BottomNav theme={theme} active="home" onNav={() => {}} />
            </Phone>
          </DCArtboard>
        </DCSection>

        {/* SECTION 5 — Coffee tree growth showcase */}
        <DCSection id="tree-stages" title="🌱 30일 주기 + 정원 성장" subtitle="한 그루 = 30일. 완성되면 뒤로 보내고 새 화분 시작.">
          <DCArtboard id="all-stages" label="한 그루의 30일 주기" width={780} height={260}>
            <div style={{ display: 'flex', padding: 16, gap: 0, background: theme.bgRaised, height: '100%', alignItems: 'center', justifyContent: 'space-around' }}>
              {[0,1,2,3,4,5,6,7].map(s => (
                <div key={s} style={{ textAlign: 'center' }}>
                  <CoffeePot stage={s} size={84} dark={tw.dark} />
                  <div style={{ fontSize: 10, color: theme.inkMuted, marginTop: 4, fontFamily: type.body }}>{['빈 화분','씨앗','새싹','어린잎','자라는 중','풍성','꽃봉오리','수확'][s]}</div>
                  <div style={{ fontSize: 9, color: theme.primary, fontWeight: 600 }}>Day {Math.round(s * 30 / 7)}</div>
                </div>
              ))}
            </div>
          </DCArtboard>
          <DCArtboard id="garden-grow" label="정원 — 1·2·3·4그루 비교" width={760} height={300}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8, padding: 12, background: theme.bg, height: '100%' }}>
              {[0, 1, 2, 3].map(c => (
                <div key={c} style={{ background: theme.bgRaised, borderRadius: 16, overflow: 'hidden', border: `1px solid ${theme.hairline}` }}>
                  <CoffeeGarden completedCount={c} currentStage={5} wilt={0} dark={tw.dark} size={170}/>
                  <div style={{ padding: '6px 10px 8px', fontSize: 11, color: theme.inkMuted, textAlign: 'center' }}>{c}그루 완성 + 자라는 중</div>
                </div>
              ))}
            </div>
          </DCArtboard>
          <DCArtboard id="wilt-states" label="시들음 → 회복 단계" width={760} height={300}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8, padding: 12, background: theme.bg, height: '100%' }}>
              {[{w: 0, l: '건강', r: false}, {w: 0.4, l: '시들기 시작', r: false}, {w: 0.8, l: '많이 시듦', r: false}, {w: 0.3, l: '회복 중', r: true}].map((s, i) => (
                <div key={i} style={{ background: theme.bgRaised, borderRadius: 16, overflow: 'hidden', border: `1px solid ${theme.hairline}` }}>
                  <CoffeeGarden completedCount={2} currentStage={5} wilt={s.w} recovering={s.r} dark={tw.dark} size={170}/>
                  <div style={{ padding: '6px 10px 8px', fontSize: 11, color: s.r ? theme.sage : (s.w > 0.5 ? '#C77E47' : theme.inkMuted), textAlign: 'center', fontWeight: 600 }}>{s.l}</div>
                </div>
              ))}
            </div>
          </DCArtboard>
        </DCSection>
      </DesignCanvas>

      {/* Tweaks Panel */}
      <TweaksPanel title="Tweaks">
        <TweakSection label="테마">
          <TweakToggle label="다크 모드 (저녁 카페)" value={tw.dark} onChange={(v) => setTweak('dark', v)} />
          <TweakRadio label="포인트 컬러" value={tw.palette} onChange={(v) => setTweak('palette', v)} options={[
            { value: 'latte', label: '라떼' },
            { value: 'mocha', label: '모카' },
            { value: 'sage', label: '세이지' },
          ]}/>
          <TweakRadio label="타이포그래피" value={tw.typography} onChange={(v) => setTweak('typography', v)} options={[
            { value: 'warm', label: '손글씨' },
            { value: 'modern', label: '모던' },
          ]}/>
        </TweakSection>
        <TweakSection label="카드">
          <TweakSelect label="복습 카드 스타일" value={tw.cardStyle} onChange={(v) => setTweak('cardStyle', v)} options={[
            { value: 'menu', label: '메뉴판 (크라프트 + 라인)' },
            { value: 'napkin', label: '냅킨 (사선 텍스처)' },
            { value: 'modern', label: '모던 (오트밀)' },
            { value: 'kraft', label: '크라프트 + 크림' },
          ]}/>
        </TweakSection>
        <TweakSection label="커피 정원">
          <TweakSlider label="현재 나무 성장" value={tw.treeStage} onChange={(v) => setTweak('treeStage', v)} min={0} max={7} step={1} />
          <div style={{ fontSize: 10.5, color: '#888', marginTop: -4 }}>
            {['빈 화분','씨앗','새싹','어린잎','자라는 중','풍성한 잎','꽃봉오리','원두 수확'][tw.treeStage]}
          </div>
          <TweakSlider label="30일 주기 (Day)" value={tw.dayInCycle} onChange={(v) => setTweak('dayInCycle', v)} min={1} max={30} step={1} />
          <TweakSlider label="완성된 나무 수" value={tw.completedTrees} onChange={(v) => setTweak('completedTrees', v)} min={0} max={4} step={1} />
          <TweakSlider label="시들음 정도" value={tw.wilt} onChange={(v) => setTweak('wilt', v)} min={0} max={1} step={0.1} />
          <TweakToggle label="회복 중 (반짝임)" value={tw.recovering} onChange={(v) => setTweak('recovering', v)} />
        </TweakSection>
      </TweaksPanel>
    </>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App />);
