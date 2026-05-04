"use client";

// coffee-tree.jsx 기반 커피나무 성장 SVG
// streak에 따른 성장 단계:
// 0: 씨앗, 1-3: 새싹, 4-7: 작은 나무, 8-14: 꽃 핀 나무, 15+: 열매 맺은 나무

function blend(a: string, b: string, t: number): string {
  const pa = a.match(/\w\w/g)!.map((h) => parseInt(h, 16));
  const pb = b.match(/\w\w/g)!.map((h) => parseInt(h, 16));
  const r = pa.map((v, i) => Math.round(v * (1 - t) + pb[i] * t));
  return "#" + r.map((v) => v.toString(16).padStart(2, "0")).join("");
}

function jit(i: number, amp = 0.5): number {
  const x = Math.sin(i * 12.9898) * 43758.5453;
  return (x - Math.floor(x) - 0.5) * 2 * amp;
}

interface CoffeePotProps {
  stage?: number;
  size?: number;
  wilt?: number;
}

function CoffeePot({ stage = 4, size = 200, wilt = 0 }: CoffeePotProps) {
  const s = Math.max(0, Math.min(7, stage));
  const w = Math.max(0, Math.min(1, wilt));

  const pot = "#A67C52";
  const potRim = "#8C6440";
  const soil = "#3D2E22";
  const leafBase = "#7A8F6B";
  const leafDarkBase = "#5A6E4F";
  const wiltColor = "#B58A3A";
  const wiltDark = "#7A5A1F";

  const leaf = blend(leafBase.slice(1), wiltColor.slice(1), w * 0.7);
  const leafDark = blend(leafDarkBase.slice(1), wiltDark.slice(1), w * 0.7);
  const stem = blend("6B7A52", "8A6F3A", w * 0.6);
  const cherry = "#C44E3F";
  const cherryDark = "#8E3328";

  const droop = w * 12;
  const tilt = w * -6;

  return (
    <svg
      viewBox="0 0 200 240"
      width={size}
      height={size * 1.2}
      style={{ display: "block", overflow: "visible" }}
    >
      {/* pot shadow */}
      <ellipse cx="100" cy="208" rx="44" ry="5" fill="rgba(0,0,0,0.2)" />

      <g transform={`rotate(${tilt} 100 200)`}>
        {/* pot body */}
        <path
          d="M 66 162 L 72 200 Q 72 204 76 204 L 124 204 Q 128 204 128 200 L 134 162 Z"
          fill={pot}
        />
        <path
          d="M 66 162 L 72 200 Q 72 204 76 204 L 82 204 L 78 162 Z"
          fill="rgba(0,0,0,0.12)"
        />
        {/* rim */}
        <ellipse cx="100" cy="162" rx="34" ry="5" fill={potRim} />
        <ellipse cx="100" cy="161" rx="32" ry="3.8" fill={pot} />
        {/* soil */}
        <ellipse cx="100" cy="161" rx="30" ry="3.2" fill={soil} />

        {/* stage 0: 씨앗 */}
        {s === 0 && (
          <g>
            <ellipse
              cx="100"
              cy="158"
              rx="12"
              ry="2"
              fill="#5C4530"
              opacity="0.8"
            />
          </g>
        )}

        {/* stage 1: 새싹 */}
        {s === 1 && (
          <g>
            <path
              d="M 100 158 L 100 150"
              stroke={stem}
              strokeWidth="1.5"
              strokeLinecap="round"
            />
            <ellipse
              cx="96"
              cy="150"
              rx="3"
              ry="2"
              fill={leaf}
              transform="rotate(-30 96 150)"
            />
            <ellipse
              cx="104"
              cy="150"
              rx="3"
              ry="2"
              fill={leaf}
              transform="rotate(30 104 150)"
            />
          </g>
        )}

        {/* stage 2+: 줄기 */}
        {s >= 2 && (
          <path
            d={`M 100 160 Q ${100 + jit(2, 0.8)} ${140 - (s - 2) * 8 + droop * 0.6} ${100 + droop * 0.3} ${Math.max(60, 142 - s * 12) + droop}`}
            stroke={stem}
            strokeWidth={2 + s * 0.3}
            strokeLinecap="round"
            fill="none"
          />
        )}

        {/* stage 2: 잎 2쌍 */}
        {s === 2 && (
          <g transform={`translate(${droop * 0.3} ${droop})`}>
            <path
              d="M 100 132 Q 90 126 82 130 Q 88 138 100 134"
              fill={leaf}
            />
            <path
              d="M 100 132 Q 110 126 118 130 Q 112 138 100 134"
              fill={leaf}
            />
            <path
              d="M 100 124 Q 95 118 92 122 Q 96 128 100 126"
              fill={leafDark}
              opacity="0.85"
            />
            <path
              d="M 100 124 Q 105 118 108 122 Q 104 128 100 126"
              fill={leafDark}
              opacity="0.85"
            />
          </g>
        )}

        {/* stage 3: 잎 3층 */}
        {s === 3 &&
          [0, 1, 2].map((i) => {
            const y = 134 - i * 12 + droop * (1 - i * 0.3);
            const dir = i % 2 === 0 ? 1 : -1;
            return (
              <g key={i}>
                <path
                  d={`M 100 ${y} Q ${100 - 14 * dir} ${y - 4} ${100 - 22 * dir} ${y + 2} Q ${100 - 14 * dir} ${y + 6} 100 ${y + 2} Z`}
                  fill={leaf}
                />
                <path
                  d={`M 100 ${y} Q ${100 + 14 * dir} ${y - 4} ${100 + 22 * dir} ${y + 2} Q ${100 + 14 * dir} ${y + 6} 100 ${y + 2} Z`}
                  fill={leaf}
                  opacity="0.92"
                />
              </g>
            );
          })}

        {/* stage 4+: 가지 + 수관 */}
        {s >= 4 && (
          <g>
            {[0, 1, 2, 3, 4].slice(0, s - 2).map((i) => {
              const y = 142 - i * 12 + droop * (1 - i * 0.2);
              const len = 18 + i * 1.5;
              return (
                <g key={i}>
                  <path
                    d={`M 100 ${y} Q ${100 - len * 0.6} ${y - 3} ${100 - len} ${y + jit(i + 9, 2) + droop * 0.4}`}
                    stroke={stem}
                    strokeWidth="1.4"
                    fill="none"
                    strokeLinecap="round"
                  />
                  <path
                    d={`M 100 ${y} Q ${100 + len * 0.6} ${y - 3} ${100 + len} ${y + jit(i + 7, 2) + droop * 0.4}`}
                    stroke={stem}
                    strokeWidth="1.4"
                    fill="none"
                    strokeLinecap="round"
                  />
                  <path
                    d={`M ${100 - len + 2} ${y + jit(i + 9, 2) + droop * 0.4} q -8 -4 -14 -1 q 2 6 14 4 z`}
                    fill={i % 2 ? leaf : leafDark}
                  />
                  <path
                    d={`M ${100 + len - 2} ${y + jit(i + 7, 2) + droop * 0.4} q 8 -4 14 -1 q -2 6 -14 4 z`}
                    fill={i % 2 ? leafDark : leaf}
                  />
                </g>
              );
            })}
            {/* crown */}
            <g transform={`translate(0 ${droop})`}>
              <ellipse
                cx="100"
                cy={Math.max(60, 140 - s * 12)}
                rx="14"
                ry="10"
                fill={leaf}
              />
              <ellipse
                cx="92"
                cy={Math.max(62, 142 - s * 12)}
                rx="9"
                ry="7"
                fill={leafDark}
                opacity="0.7"
              />
              <ellipse
                cx="108"
                cy={Math.max(62, 142 - s * 12)}
                rx="9"
                ry="7"
                fill={leaf}
                opacity="0.85"
              />
            </g>
          </g>
        )}

        {/* stage 6: 꽃 */}
        {s === 6 &&
          [
            { x: 86, y: 96 },
            { x: 116, y: 94 },
            { x: 100, y: 80 },
            { x: 90, y: 110 },
            { x: 114, y: 112 },
          ].map((p, i) => (
            <g
              key={i}
              transform={`translate(${p.x} ${p.y + droop * 0.5})`}
            >
              {[0, 72, 144, 216, 288].map((r, j) => (
                <ellipse
                  key={j}
                  cx="0"
                  cy="-3"
                  rx="2"
                  ry="3.2"
                  fill="#FFFFFF"
                  transform={`rotate(${r})`}
                  stroke="#E8D5BE"
                  strokeWidth="0.4"
                />
              ))}
              <circle cx="0" cy="0" r="1.4" fill="#E8C97B" />
            </g>
          ))}

        {/* stage 7: 열매 */}
        {s >= 7 &&
          [
            { x: 84, y: 98 },
            { x: 116, y: 96 },
            { x: 100, y: 82 },
            { x: 90, y: 112 },
            { x: 114, y: 114 },
            { x: 78, y: 122 },
            { x: 122, y: 126 },
            { x: 100, y: 130 },
            { x: 92, y: 90 },
          ].map((p, i) => (
            <g key={i}>
              <circle cx={p.x} cy={p.y} r="3.6" fill={cherryDark} />
              <circle cx={p.x - 0.8} cy={p.y - 0.8} r="3" fill={cherry} />
              <circle
                cx={p.x - 1.4}
                cy={p.y - 1.4}
                r="0.8"
                fill="#FFD2B0"
                opacity="0.8"
              />
            </g>
          ))}
      </g>
    </svg>
  );
}

// streak → 성장 단계 매핑
function streakToStage(streak: number): number {
  if (streak === 0) return 0;
  if (streak <= 3) return 1;
  if (streak <= 5) return 2;
  if (streak <= 7) return 3;
  if (streak <= 10) return 4;
  if (streak <= 14) return 5;
  if (streak <= 20) return 6;
  return 7;
}

interface CoffeeTreeProps {
  streak: number;
  size?: number;
}

export default function CoffeeTree({ streak, size = 160 }: CoffeeTreeProps) {
  const stage = streakToStage(streak);

  return (
    <div className="flex flex-col items-center gap-2">
      <CoffeePot stage={stage} size={size} wilt={0} />
      <div className="text-xs text-ink-muted">
        {streak === 0 && "씨앗을 심었어요"}
        {streak >= 1 && streak <= 3 && "새싹이 올라왔어요"}
        {streak >= 4 && streak <= 7 && "나무가 자라고 있어요"}
        {streak >= 8 && streak <= 14 && "꽃이 피기 시작해요"}
        {streak >= 15 && "원두가 열렸어요!"}
      </div>
    </div>
  );
}
