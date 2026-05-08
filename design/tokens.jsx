// tokens.jsx — Cozy Cafe design tokens
// Color palette inspired by latte, oat, sage and mocha tones.
// All exports are pushed onto window so other Babel scripts can read them.

const PALETTES = {
  latte: {
    name: '라떼 브라운',
    primary: '#A67C52',
    primaryDeep: '#8C6440',
    primarySoft: '#E8D5BE',
  },
  mocha: {
    name: '모카',
    primary: '#6F4E37',
    primaryDeep: '#523926',
    primarySoft: '#D9C5B0',
  },
  sage: {
    name: '세이지',
    primary: '#7A8F6B',
    primaryDeep: '#5A6E4F',
    primarySoft: '#D4DCC4',
  },
};

// Light theme — warm, paper-like
const lightTheme = {
  bg: '#FAF6F0',         // cream white
  bgRaised: '#FFFCF7',   // raised paper
  bgKraft: '#E8DCC8',    // kraft paper
  bgSoft: '#F2EADD',     // oat
  ink: '#3D2E22',        // mocha brown text
  inkSoft: '#6B5644',    // softer body
  inkMuted: '#9A8676',   // muted
  hairline: 'rgba(61,46,34,0.10)',
  divider: 'rgba(61,46,34,0.06)',
  sage: '#7A8F6B',
  sageSoft: '#D4DCC4',
  sageBg: '#EBF0E2',
  cream: '#F5EBD9',
  warning: '#C77E47',
  cardShadow: '0 2px 6px rgba(120,90,60,0.08), 0 12px 28px -8px rgba(120,90,60,0.12)',
  cardShadowHover: '0 4px 10px rgba(120,90,60,0.12), 0 18px 36px -8px rgba(120,90,60,0.18)',
  pillBg: 'rgba(255,252,247,0.78)',
  pillBorder: 'rgba(61,46,34,0.06)',
};

// Dark theme — evening cafe
const darkTheme = {
  bg: '#1F1812',
  bgRaised: '#2A2018',
  bgKraft: '#3A2D22',
  bgSoft: '#332821',
  ink: '#F5EBD9',
  inkSoft: '#C8B59E',
  inkMuted: '#8C7A66',
  hairline: 'rgba(245,235,217,0.10)',
  divider: 'rgba(245,235,217,0.06)',
  sage: '#9CB28A',
  sageSoft: '#4A5840',
  sageBg: '#2D3326',
  cream: '#3A2D22',
  warning: '#D89465',
  cardShadow: '0 2px 6px rgba(0,0,0,0.4), 0 12px 28px -8px rgba(0,0,0,0.5)',
  cardShadowHover: '0 4px 10px rgba(0,0,0,0.5), 0 18px 36px -8px rgba(0,0,0,0.6)',
  pillBg: 'rgba(42,32,24,0.78)',
  pillBorder: 'rgba(245,235,217,0.06)',
};

// Resolve theme — return merged palette + theme
function makeTheme(paletteKey, dark) {
  const pal = PALETTES[paletteKey] || PALETTES.latte;
  const base = dark ? darkTheme : lightTheme;
  return { ...base, ...pal };
}

// Typography presets
const TYPE_PRESETS = {
  warm: {
    name: '손글씨 + 세리프',
    heading: '"Gowun Dodum", "Fraunces", serif',
    body: '"Gowun Dodum", system-ui, sans-serif',
    serif: '"Fraunces", Georgia, serif',
    mono: '"JetBrains Mono", "Courier New", monospace',
    headingWeight: 400,
    bodyWeight: 400,
  },
  modern: {
    name: '모던',
    heading: '"Pretendard", "Inter", system-ui, sans-serif',
    body: '"Pretendard", "Inter", system-ui, sans-serif',
    serif: '"Fraunces", Georgia, serif',
    mono: '"JetBrains Mono", "Courier New", monospace',
    headingWeight: 700,
    bodyWeight: 400,
  },
};

Object.assign(window, { PALETTES, makeTheme, TYPE_PRESETS, lightTheme, darkTheme });
