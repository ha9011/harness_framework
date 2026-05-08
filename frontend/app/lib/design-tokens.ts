export const cafeColors = {
  bg: "#FAF6F0",
  bgRaised: "#FFFCF7",
  bgKraft: "#E8DCC8",
  bgSoft: "#F2EADD",
  ink: "#3D2E22",
  inkSoft: "#6B5644",
  inkMuted: "#9A8676",
  hairline: "rgba(61,46,34,0.10)",
  divider: "rgba(61,46,34,0.06)",
  sage: "#7A8F6B",
  sageDeep: "#5A6E4F",
  sageSoft: "#D4DCC4",
  sageBg: "#EBF0E2",
  warning: "#C77E47",
  important: "#E8C97B",
  ctaText: "#FFFCF7",
} as const;

export const cafePalettes = {
  latte: {
    name: "latte",
    primary: "#A67C52",
    primaryDeep: "#8C6440",
    primarySoft: "#E8D5BE",
  },
  mocha: {
    name: "mocha",
    primary: "#6F4E37",
    primaryDeep: "#523926",
    primarySoft: "#D9C5B0",
  },
  sage: {
    name: "sage",
    primary: "#7A8F6B",
    primaryDeep: "#5A6E4F",
    primarySoft: "#D4DCC4",
  },
} as const;

export const cafeTheme = {
  ...cafeColors,
  ...cafePalettes.latte,
  cardShadow: "0 2px 6px rgba(120,90,60,0.08), 0 12px 28px -8px rgba(120,90,60,0.12)",
  cardShadowHover: "0 4px 10px rgba(120,90,60,0.12), 0 18px 36px -8px rgba(120,90,60,0.18)",
} as const;

export type CafeColorToken = keyof typeof cafeColors;
export type CafePaletteName = keyof typeof cafePalettes;
