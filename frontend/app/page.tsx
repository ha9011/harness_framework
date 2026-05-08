import { Coffee } from "lucide-react";
import { Chip } from "@/app/components/ui/chip";
import { Surface } from "@/app/components/ui/surface";

export default function HomePage() {
  return (
    <div className="space-y-5 px-4 py-5">
      <header className="space-y-2">
        <Chip variant="warm">Cozy Cafe</Chip>
        <div>
          <h1 className="text-2xl font-semibold text-cafe-ink">영어 패턴 학습기</h1>
          <p className="mt-1 text-sm leading-relaxed text-cafe-ink-soft">
            오늘의 단어, 패턴, 문장을 차분하게 쌓아가는 학습 공간입니다.
          </p>
        </div>
      </header>

      <Surface className="space-y-4 p-5">
        <div className="flex items-start gap-3">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-[16px] bg-cafe-sage-bg text-cafe-sage">
            <Coffee className="h-5 w-5" aria-hidden="true" />
          </div>
          <div className="min-w-0">
            <h2 className="text-base font-semibold text-cafe-ink">오늘 학습 데이터가 아직 없습니다</h2>
            <p className="mt-1 text-sm leading-relaxed text-cafe-ink-soft">
              첫 학습 기록이 생기면 이곳에 오늘의 현황이 표시됩니다.
            </p>
          </div>
        </div>

        <div className="grid grid-cols-3 gap-2">
          <Chip>Query</Chip>
          <Chip>Tailwind</Chip>
          <Chip>Vitest</Chip>
        </div>
      </Surface>
    </div>
  );
}
