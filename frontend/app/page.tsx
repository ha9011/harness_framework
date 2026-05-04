import Link from "next/link";

export default function Home() {
  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-lg font-semibold text-ink tracking-tight">
        Cozy Cafe
      </h1>
      <p className="text-sm text-ink-soft leading-relaxed">
        영어 패턴 학습기에 오신 것을 환영합니다.
      </p>
      <div className="flex flex-col gap-3">
        <Link
          href="/words"
          className="bg-raised rounded-[20px] border border-hairline p-4 shadow-sm"
        >
          <p className="text-sm font-semibold text-ink">📖 단어 관리</p>
          <p className="text-xs text-ink-muted mt-1">단어를 등록하고 관리하세요</p>
        </Link>
      </div>
    </div>
  );
}
