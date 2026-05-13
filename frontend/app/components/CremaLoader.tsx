interface CremaLoaderProps {
  message?: string;
}

export default function CremaLoader({ message = "불러오는 중..." }: CremaLoaderProps) {
  return (
    <div
      className="flex flex-col items-center justify-center gap-3"
      role="status"
      aria-live="polite"
    >
      <svg
        width={80}
        height={80}
        viewBox="0 0 120 120"
        fill="none"
        aria-hidden="true"
      >
        {/* 소서 (받침 접시) */}
        <circle cx="55" cy="60" r="55" fill="#E8DCC8" />
        {/* 머그잔 외벽 */}
        <circle cx="55" cy="60" r="42" fill="#A67C52" />
        {/* 머그잔 내벽 (rim) */}
        <circle cx="55" cy="60" r="36" fill="#8C6440" />
        {/* 커피 액면 */}
        <circle cx="55" cy="60" r="32" fill="#523926" />
        {/* 크레마 나선 */}
        <path
          d="M55 40 C65 45, 70 55, 60 60 C50 65, 45 58, 50 50 C55 42, 65 48, 58 55 C51 62, 42 55, 48 48"
          stroke="#E8D5BE"
          strokeWidth="3"
          fill="none"
          strokeLinecap="round"
          style={{
            animation: "crema-swirl 3s linear infinite",
            transformOrigin: "center",
          }}
        />
        {/* 손잡이 */}
        <path
          d="M97 48 C110 48, 112 72, 97 72"
          stroke="#A67C52"
          strokeWidth="6"
          fill="none"
          strokeLinecap="round"
        />
      </svg>
      <span className="text-sm text-ink-muted">{message}</span>
    </div>
  );
}
