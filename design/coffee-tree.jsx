// coffee-tree.jsx — Coffee plant pot + windowsill garden.
// 30-day cycle: each pot grows seed → harvest, then a new pot is added,
// and the front pot resets. Wilt state when streak breaks.

function CoffeePot({ stage = 4, size = 200, dark = false, wilt = 0, completed = false, recoveryGlow = false, empty = false }) {
  const s = completed ? 7 : Math.max(0, Math.min(7, stage));
  const w = Math.max(0, Math.min(1, wilt));

  const pot = dark ? '#8C6440' : '#A67C52';
  const potDark = dark ? '#523926' : '#6F4E37';
  const potRim = dark ? '#6F4E37' : '#8C6440';
  const soil = dark ? '#2A2018' : '#3D2E22';
  const leafBase = dark ? '#9CB28A' : '#7A8F6B';
  const leafDarkBase = dark ? '#6B8059' : '#5A6E4F';
  const wiltColor = '#B58A3A';
  const wiltDark = '#7A5A1F';
  const blend = (a, b, t) => {
    const pa = a.match(/\w\w/g).map(h => parseInt(h, 16));
    const pb = b.match(/\w\w/g).map(h => parseInt(h, 16));
    const r = pa.map((v, i) => Math.round(v * (1 - t) + pb[i] * t));
    return '#' + r.map(v => v.toString(16).padStart(2, '0')).join('');
  };
  const leaf = blend(leafBase.slice(1), wiltColor.slice(1), w * 0.7);
  const leafDark = blend(leafDarkBase.slice(1), wiltDark.slice(1), w * 0.7);
  const stem = blend((dark ? '#7A8557' : '#6B7A52').slice(1), '#8A6F3A', w * 0.6);
  const flower = dark ? '#FFFCF7' : '#FFFFFF';
  const cherry = '#C44E3F';
  const cherryDark = '#8E3328';

  const droop = w * 12;
  const tilt = w * -6;

  const jit = (i, amp = 0.5) => {
    const x = Math.sin(i * 12.9898) * 43758.5453;
    return ((x - Math.floor(x)) - 0.5) * 2 * amp;
  };

  // POT GEOMETRY
  // pot top y=160, bottom y=200, rim center y=160
  return (
    <svg viewBox="0 0 200 240" width={size} height={size * 1.2} style={{ display: 'block', overflow: 'visible' }}>
      {recoveryGlow && (
        <ellipse cx="100" cy="200" rx="70" ry="12" fill="#7A8F6B" opacity="0.2">
          <animate attributeName="ry" values="12;20;12" dur="2s" repeatCount="indefinite"/>
          <animate attributeName="opacity" values="0.2;0.35;0.2" dur="2s" repeatCount="indefinite"/>
        </ellipse>
      )}

      {/* pot shadow */}
      <ellipse cx="100" cy="208" rx="44" ry="5" fill="rgba(0,0,0,0.2)" />

      <g transform={`rotate(${tilt} 100 200)`}>
        {/* pot body — terracotta, slightly trapezoidal */}
        <path d="M 66 162 L 72 200 Q 72 204 76 204 L 124 204 Q 128 204 128 200 L 134 162 Z" fill={pot} />
        {/* pot left shading */}
        <path d="M 66 162 L 72 200 Q 72 204 76 204 L 82 204 L 78 162 Z" fill="rgba(0,0,0,0.12)" />
        {/* pot right highlight */}
        <path d="M 122 162 L 124 204 L 128 204 Q 128 204 128 200 L 134 162 Z" fill="rgba(255,255,255,0.08)" />
        {/* rim ring */}
        <ellipse cx="100" cy="162" rx="34" ry="5" fill={potRim} />
        <ellipse cx="100" cy="161" rx="32" ry="3.8" fill={pot} />
        {/* soil */}
        <ellipse cx="100" cy="161" rx="30" ry="3.2" fill={soil} />

        {empty && (
          <g>
            {/* hint of fresh soil mound */}
            <ellipse cx="100" cy="159" rx="14" ry="2" fill={dark ? '#3A2D22' : '#5C4530'} opacity="0.7"/>
            <text x="100" y="148" fontFamily="serif" fontSize="9" fill={dark ? '#7A6A5A' : '#9A8676'} textAnchor="middle" fontStyle="italic" opacity="0.7">곧 시작…</text>
          </g>
        )}

        {/* growth */}
        {!empty && s === 0 && (
          <g>
            <ellipse cx="100" cy="158" rx="12" ry="2" fill={dark ? '#3A2D22' : '#5C4530'} opacity="0.8"/>
            <text x="100" y="148" fontFamily="serif" fontSize="9" fill={dark ? '#9A8676' : '#9A8676'} textAnchor="middle" fontStyle="italic" opacity="0.7">씨앗</text>
          </g>
        )}

        {!empty && s === 1 && (
          <g>
            {/* tiny sprout */}
            <path d="M 100 158 L 100 150" stroke={stem} strokeWidth="1.5" strokeLinecap="round"/>
            <ellipse cx="96" cy="150" rx="3" ry="2" fill={leaf} transform="rotate(-30 96 150)"/>
            <ellipse cx="104" cy="150" rx="3" ry="2" fill={leaf} transform="rotate(30 104 150)"/>
          </g>
        )}

        {!empty && s >= 2 && (
          <path
            d={`M 100 ${160} Q ${100 + jit(2, 0.8)} ${140 - (s - 2) * 8 + droop * 0.6} ${100 + droop * 0.3} ${Math.max(60, 142 - s * 12) + droop}`}
            stroke={stem} strokeWidth={2 + s * 0.3} strokeLinecap="round" fill="none"
          />
        )}

        {!empty && s === 2 && (
          <g transform={`translate(${droop * 0.3} ${droop})`}>
            <path d="M 100 132 Q 90 126 82 130 Q 88 138 100 134" fill={leaf} />
            <path d="M 100 132 Q 110 126 118 130 Q 112 138 100 134" fill={leaf} />
            <path d="M 100 124 Q 95 118 92 122 Q 96 128 100 126" fill={leafDark} opacity="0.85"/>
            <path d="M 100 124 Q 105 118 108 122 Q 104 128 100 126" fill={leafDark} opacity="0.85"/>
          </g>
        )}

        {!empty && s === 3 && (
          <g>
            {[0, 1, 2].map((i) => {
              const y = 134 - i * 12 + droop * (1 - i * 0.3);
              const dir = i % 2 === 0 ? 1 : -1;
              return (
                <g key={i}>
                  <path d={`M 100 ${y} Q ${100 - 14 * dir} ${y - 4} ${100 - 22 * dir} ${y + 2} Q ${100 - 14 * dir} ${y + 6} 100 ${y + 2} Z`} fill={leaf} />
                  <path d={`M 100 ${y} Q ${100 + 14 * dir} ${y - 4} ${100 + 22 * dir} ${y + 2} Q ${100 + 14 * dir} ${y + 6} 100 ${y + 2} Z`} fill={leaf} opacity="0.92" />
                </g>
              );
            })}
          </g>
        )}

        {!empty && s >= 4 && (
          <g>
            {[0, 1, 2, 3, 4].slice(0, s - 2).map((i) => {
              const y = 142 - i * 12 + droop * (1 - i * 0.2);
              const len = 18 + i * 1.5;
              return (
                <g key={i}>
                  <path d={`M 100 ${y} Q ${100 - len * 0.6} ${y - 3} ${100 - len} ${y + jit(i + 9, 2) + droop * 0.4}`} stroke={stem} strokeWidth="1.4" fill="none" strokeLinecap="round" />
                  <path d={`M 100 ${y} Q ${100 + len * 0.6} ${y - 3} ${100 + len} ${y + jit(i + 7, 2) + droop * 0.4}`} stroke={stem} strokeWidth="1.4" fill="none" strokeLinecap="round" />
                  <path d={`M ${100 - len + 2} ${y + jit(i + 9, 2) + droop * 0.4} q -8 -4 -14 -1 q 2 6 14 4 z`} fill={i % 2 ? leaf : leafDark} />
                  <path d={`M ${100 + len - 2} ${y + jit(i + 7, 2) + droop * 0.4} q 8 -4 14 -1 q -2 6 -14 4 z`} fill={i % 2 ? leafDark : leaf} />
                </g>
              );
            })}
            {/* crown */}
            <g transform={`translate(0 ${droop})`}>
              <ellipse cx="100" cy={Math.max(60, 140 - s * 12)} rx="14" ry="10" fill={leaf} />
              <ellipse cx="92" cy={Math.max(62, 142 - s * 12)} rx="9" ry="7" fill={leafDark} opacity="0.7" />
              <ellipse cx="108" cy={Math.max(62, 142 - s * 12)} rx="9" ry="7" fill={leaf} opacity="0.85" />
            </g>
          </g>
        )}

        {!empty && s >= 5 && (
          <g transform={`translate(0 ${droop * 0.7})`}>
            {[...Array(8)].map((_, i) => {
              const angle = (i / 8) * Math.PI * 2;
              const cx = 100 + Math.cos(angle) * (16 + jit(i, 4));
              const cy = 100 + Math.sin(angle) * (18 + jit(i + 3, 4)) - 10;
              return (
                <ellipse key={i} cx={cx} cy={cy} rx="8" ry="5"
                  fill={i % 2 ? leaf : leafDark}
                  transform={`rotate(${(i / 8) * 360} ${cx} ${cy})`}
                  opacity="0.92"
                />
              );
            })}
          </g>
        )}

        {!empty && s === 6 && (
          <g>
            {[{ x: 86, y: 96 }, { x: 116, y: 94 }, { x: 100, y: 80 }, { x: 90, y: 110 }, { x: 114, y: 112 }].map((p, i) => (
              <g key={i} transform={`translate(${p.x} ${p.y + droop * 0.5})`}>
                {[0, 72, 144, 216, 288].map((r, j) => (
                  <ellipse key={j} cx="0" cy="-3" rx="2" ry="3.2" fill={flower}
                    transform={`rotate(${r})`} stroke={dark ? '#C8B59E' : '#E8D5BE'} strokeWidth="0.4" />
                ))}
                <circle cx="0" cy="0" r="1.4" fill="#E8C97B" />
              </g>
            ))}
          </g>
        )}

        {!empty && s >= 7 && (
          <g>
            {[
              { x: 84, y: 98 }, { x: 116, y: 96 }, { x: 100, y: 82 },
              { x: 90, y: 112 }, { x: 114, y: 114 }, { x: 78, y: 122 },
              { x: 122, y: 126 }, { x: 100, y: 130 }, { x: 92, y: 90 },
            ].map((p, i) => (
              <g key={i}>
                <circle cx={p.x} cy={p.y} r="3.6" fill={cherryDark} />
                <circle cx={p.x - 0.8} cy={p.y - 0.8} r="3" fill={cherry} />
                <circle cx={p.x - 1.4} cy={p.y - 1.4} r="0.8" fill="#FFD2B0" opacity="0.8" />
              </g>
            ))}
          </g>
        )}

        {/* completed marker */}
        {completed && (
          <g transform="translate(132 76)">
            <circle r="9" fill="#E8C97B" stroke="#FFFCF7" strokeWidth="1.5"/>
            <text y="3" fontSize="10" textAnchor="middle" fill="#6F4E37" fontWeight="700">✓</text>
          </g>
        )}
      </g>
    </svg>
  );
}

// CoffeeGarden — windowsill scene with N pots arranged left-to-right.
// Layout strategy:
//   - Total of 4 slots on the windowsill: [done][done][CURRENT][next]
//   - Completed pots fill from the left (smaller, slightly faded)
//   - Current (active) pot is biggest, in slot 3
//   - The slot after current shows an empty pot ("next cycle")
function CoffeeGarden({ completedCount = 2, currentStage = 4, wilt = 0, dark = false, size = 320, recovering = false, showWindow = true }) {
  const completed = Math.max(0, Math.min(3, completedCount));
  // Display: completed pots (small, faded) on left, current pot (BIG) on right
  // No "next empty" pot — the current pot itself IS the next cycle when empty
  const VW = 360;
  const VH = 220;
  const groundY = 168;

  const sillColor = dark ? '#3A2D22' : '#D8C4A8';
  const sillEdge = dark ? '#523926' : '#A67C52';
  const windowFrame = dark ? '#4A3828' : '#C8B091';
  const windowGlass = dark ? '#1F1812' : '#EAE1D0';
  const skyTop = dark ? '#2A2018' : '#F5EBD9';
  const skyBot = dark ? '#1F1812' : '#FFF8E8';

  // Layout: current pot is hero on the RIGHT.
  // Completed pots line up on the LEFT, sized smaller.
  const heroPotSize = 130;
  const smallPotSize = 56;
  const heroX = VW - 80; // hero pot center x
  // Completed pots: spaced from leftStart to leftEnd
  const leftStart = 38;
  const leftEnd = heroX - heroPotSize / 2 - 30; // give 30px gap before hero
  const completedPositions = Array.from({ length: completed }, (_, i) => {
    if (completed === 1) return leftStart + 30;
    if (completed === 2) return leftStart + i * 64;
    // 3 pots: spread evenly
    return leftStart + (i * (leftEnd - leftStart)) / (completed - 1);
  });

  return (
    <div style={{ position: 'relative', width: '100%', aspectRatio: `${VW} / ${VH}`, overflow: 'hidden' }}>
      {/* Window backdrop */}
      {showWindow && (
        <svg viewBox={`0 0 ${VW} ${VH}`} width="100%" height="100%" preserveAspectRatio="none" style={{ position: 'absolute', inset: 0 }}>
          <defs>
            <linearGradient id="cg-sky" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor={skyTop}/>
              <stop offset="100%" stopColor={skyBot}/>
            </linearGradient>
          </defs>
          <rect x="0" y="0" width={VW} height={VH} fill="url(#cg-sky)"/>
          {/* window frame — wider than tall */}
          <rect x="10" y="6" width={VW - 20} height={groundY - 14} rx="4" fill={windowGlass} stroke={windowFrame} strokeWidth="2.5"/>
          {/* mullions */}
          <line x1={VW / 2} y1="8" x2={VW / 2} y2={groundY - 8} stroke={windowFrame} strokeWidth="2"/>
          <line x1="12" y1={(groundY - 14) / 2 + 6} x2={VW - 12} y2={(groundY - 14) / 2 + 6} stroke={windowFrame} strokeWidth="2"/>
          {/* sun */}
          <circle cx={VW * 0.78} cy="46" r="18" fill="#FFE6B5" opacity={dark ? 0.18 : 0.55}/>
          <circle cx={VW * 0.78} cy="46" r="32" fill="#FFE6B5" opacity={dark ? 0.06 : 0.22}/>
          {/* hills */}
          <path d={`M 0 ${groundY - 18} Q 50 ${groundY - 38} 100 ${groundY - 22} Q 160 ${groundY - 44} 220 ${groundY - 24} Q 280 ${groundY - 38} ${VW} ${groundY - 22} L ${VW} ${groundY - 14} L 0 ${groundY - 14} Z`}
            fill={dark ? '#332821' : '#D4DCC4'} opacity="0.55"/>
          {/* sill */}
          <rect x="0" y={groundY} width={VW} height="18" fill={sillColor}/>
          <rect x="0" y={groundY - 2} width={VW} height="3" fill={sillEdge}/>
          <rect x="0" y={groundY + 16} width={VW} height="2" fill={sillEdge} opacity="0.5"/>
        </svg>
      )}

      {/* Pots */}
      <div style={{ position: 'absolute', inset: 0 }}>
        {/* Completed pots — left cluster, smaller */}
        {completedPositions.map((x, i) => (
          <div key={`c${i}`} style={{
            position: 'absolute',
            left: `${(x / VW) * 100}%`,
            bottom: `${((VH - groundY - 4) / VH) * 100}%`,
            transform: 'translateX(-50%)',
            width: smallPotSize,
            height: smallPotSize * 1.2,
            filter: 'saturate(0.9)',
            opacity: 0.94,
            zIndex: 1,
          }}>
            <MiniCompletedTree size={smallPotSize} dark={dark} />
          </div>
        ))}

        {/* Current pot — hero, right side */}
        <div style={{
          position: 'absolute',
          left: `${(heroX / VW) * 100}%`,
          bottom: `${((VH - groundY - 4) / VH) * 100}%`,
          transform: 'translateX(-50%)',
          width: heroPotSize,
          height: heroPotSize * 1.2,
          zIndex: 10,
          transition: 'all 0.4s cubic-bezier(0.4, 0, 0.2, 1)',
        }}>
          <CoffeePot
            stage={currentStage}
            wilt={wilt}
            recoveryGlow={recovering}
            size={heroPotSize}
            dark={dark}
          />
        </div>
      </div>

      {/* Status pill */}
      <div style={{
        position: 'absolute', top: 10, right: 12,
        padding: '4px 10px', borderRadius: 999,
        background: dark ? 'rgba(42,32,24,0.85)' : 'rgba(255,252,247,0.92)',
        border: `1px solid ${dark ? 'rgba(245,235,217,0.12)' : 'rgba(61,46,34,0.1)'}`,
        fontSize: 10.5, fontWeight: 600,
        color: wilt > 0.3 ? '#C77E47' : (dark ? '#9CB28A' : '#7A8F6B'),
        boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
        backdropFilter: 'blur(6px)',
      }}>
        {wilt > 0.6 ? '😢 시들고 있어요'
          : wilt > 0.2 ? '💧 물이 필요해요'
          : recovering ? '✨ 회복 중'
          : `🌱 ${currentStage}/7`}
      </div>
    </div>
  );
}

// Legacy single-pot wrapper
function CoffeeTree({ stage = 4, size = 200, dark = false }) {
  return <CoffeePot stage={stage} size={size} dark={dark} />;
}

// MiniCompletedTree — dense little harvested coffee tree for the garden's left side.
// Reads clearly at tiny sizes (~50-60px) where CoffeePot details get lost.
function MiniCompletedTree({ size = 56, dark = false }) {
  const pot = dark ? '#8C6440' : '#A67C52';
  const potDark = dark ? '#523926' : '#6F4E37';
  const leaf = dark ? '#9CB28A' : '#7A8F6B';
  const leafDark = dark ? '#6B8059' : '#5A6E4F';
  const stem = dark ? '#7A8557' : '#6B7A52';
  const cherry = '#C44E3F';
  const cherryDark = '#8E3328';
  return (
    <svg viewBox="0 0 100 120" width={size} height={size * 1.2} style={{ display: 'block', overflow: 'visible' }}>
      {/* shadow */}
      <ellipse cx="50" cy="108" rx="22" ry="2.5" fill="rgba(0,0,0,0.18)"/>
      {/* pot */}
      <path d="M 32 86 L 35 104 Q 35 106 37 106 L 63 106 Q 65 106 65 104 L 68 86 Z" fill={pot}/>
      <path d="M 32 86 L 35 104 Q 35 106 37 106 L 41 106 L 39 86 Z" fill="rgba(0,0,0,0.12)"/>
      <ellipse cx="50" cy="86" rx="18" ry="2.6" fill={potDark}/>
      <ellipse cx="50" cy="85.5" rx="16.5" ry="1.8" fill={pot}/>
      {/* trunk */}
      <path d="M 50 86 Q 49 70 50 50" stroke={stem} strokeWidth="2.2" fill="none" strokeLinecap="round"/>
      {/* leafy crown — bushy ellipses */}
      <g>
        <ellipse cx="50" cy="50" rx="22" ry="20" fill={leafDark}/>
        <ellipse cx="44" cy="46" rx="14" ry="13" fill={leaf}/>
        <ellipse cx="58" cy="52" rx="13" ry="12" fill={leaf} opacity="0.92"/>
        <ellipse cx="50" cy="40" rx="11" ry="10" fill={leaf} opacity="0.88"/>
        <ellipse cx="40" cy="56" rx="9" ry="8" fill={leafDark} opacity="0.85"/>
        <ellipse cx="62" cy="44" rx="8" ry="7" fill={leafDark} opacity="0.85"/>
      </g>
      {/* cherries */}
      {[
        {x: 42, y: 50}, {x: 56, y: 47}, {x: 50, y: 56},
        {x: 38, y: 44}, {x: 60, y: 54}, {x: 47, y: 42},
      ].map((p, i) => (
        <g key={i}>
          <circle cx={p.x} cy={p.y} r="2.6" fill={cherryDark}/>
          <circle cx={p.x - 0.5} cy={p.y - 0.5} r="2.1" fill={cherry}/>
          <circle cx={p.x - 0.9} cy={p.y - 0.9} r="0.6" fill="#FFD2B0" opacity="0.9"/>
        </g>
      ))}
      {/* check mark */}
      <g transform="translate(74 36)">
        <circle r="7" fill="#E8C97B" stroke="#FFFCF7" strokeWidth="1.3"/>
        <text y="2.6" fontSize="8" textAnchor="middle" fill="#6F4E37" fontWeight="700">✓</text>
      </g>
    </svg>
  );
}

function Bean({ size = 14, color = 'currentColor' }) {
  return (
    <svg width={size} height={size} viewBox="0 0 16 16" style={{ display: 'inline-block', verticalAlign: 'middle' }}>
      <ellipse cx="8" cy="8" rx="5" ry="6.5" fill={color} transform="rotate(-20 8 8)" />
      <path d="M 5 4 Q 9 8 11 12" stroke="rgba(255,252,247,0.4)" strokeWidth="1.2" fill="none" strokeLinecap="round" transform="rotate(-20 8 8)" />
    </svg>
  );
}

Object.assign(window, { CoffeeTree, CoffeePot, CoffeeGarden, MiniCompletedTree, Bean });
