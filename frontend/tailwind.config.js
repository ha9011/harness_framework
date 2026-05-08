/** @type {import('tailwindcss').Config} */
const config = {
  darkMode: ["class"],
  content: ["./app/**/*.{ts,tsx}", "./test/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
        },
        secondary: {
          DEFAULT: "hsl(var(--secondary))",
          foreground: "hsl(var(--secondary-foreground))",
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        accent: {
          DEFAULT: "hsl(var(--accent))",
          foreground: "hsl(var(--accent-foreground))",
        },
        destructive: {
          DEFAULT: "hsl(var(--destructive))",
          foreground: "hsl(var(--destructive-foreground))",
        },
        cafe: {
          bg: "#FAF6F0",
          raised: "#FFFCF7",
          kraft: "#E8DCC8",
          soft: "#F2EADD",
          ink: "#3D2E22",
          "ink-soft": "#6B5644",
          "ink-muted": "#9A8676",
          latte: "#A67C52",
          "latte-deep": "#8C6440",
          "latte-soft": "#E8D5BE",
          mocha: "#6F4E37",
          sage: "#7A8F6B",
          "sage-deep": "#5A6E4F",
          "sage-soft": "#D4DCC4",
          "sage-bg": "#EBF0E2",
          warning: "#C77E47",
          important: "#E8C97B",
          cta: "#FFFCF7",
        },
      },
      boxShadow: {
        cafe: "0 2px 6px rgba(120,90,60,0.08), 0 12px 28px -8px rgba(120,90,60,0.12)",
        "cafe-button": "0 6px 14px -4px rgba(166,124,82,0.4)",
      },
      borderRadius: {
        xl: "0.875rem",
        "2xl": "1.125rem",
      },
      fontFamily: {
        sans: ["Pretendard", "Inter", "system-ui", "sans-serif"],
        serif: ["Fraunces", "Georgia", "serif"],
        mono: ["JetBrains Mono", "Courier New", "monospace"],
      },
    },
  },
  plugins: [],
};

module.exports = config;
