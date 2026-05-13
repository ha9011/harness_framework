interface CoffeeSpinnerProps {
  size?: number;
  className?: string;
}

export default function CoffeeSpinner({ size = 20, className }: CoffeeSpinnerProps) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
      className={className}
      style={{ animation: "coffee-spin 1s linear infinite" }}
    >
      {/* 머그잔 몸체 */}
      <rect x="3" y="8" width="13" height="11" rx="2" fill="#A67C52" />
      {/* 커피 표면 */}
      <rect x="4" y="9" width="11" height="4" rx="1" fill="#6F4E37" />
      {/* 손잡이 */}
      <path
        d="M16 11 C19 11 19 16 16 16"
        stroke="#A67C52"
        strokeWidth="2"
        fill="none"
        strokeLinecap="round"
      />
      {/* 김 (steam) */}
      <path d="M7 6 Q8 4 7 2" stroke="#9A8676" strokeWidth="1" fill="none" strokeLinecap="round" />
      <path d="M10 6 Q11 4 10 2" stroke="#9A8676" strokeWidth="1" fill="none" strokeLinecap="round" />
      <path d="M13 6 Q14 4 13 2" stroke="#9A8676" strokeWidth="1" fill="none" strokeLinecap="round" />
    </svg>
  );
}
