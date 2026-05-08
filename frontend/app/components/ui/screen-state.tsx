import type { ReactNode } from "react";
import { Button } from "@/app/components/ui/button";
import { Surface } from "@/app/components/ui/surface";

type ScreenStateProps = {
  title: string;
  description: string;
  action?: ReactNode;
};

export function LoadingState({ title = "불러오는 중입니다" }: Partial<Pick<ScreenStateProps, "title">>) {
  return (
    <Surface className="p-5">
      <p className="text-sm font-medium text-cafe-ink-soft">{title}</p>
    </Surface>
  );
}

export function EmptyState({ title, description, action }: ScreenStateProps) {
  return (
    <Surface className="space-y-3 p-5 text-center">
      <h2 className="text-base font-semibold text-cafe-ink">{title}</h2>
      <p className="text-sm leading-relaxed text-cafe-ink-soft">{description}</p>
      {action}
    </Surface>
  );
}

export function ErrorState({
  title = "요청을 처리하지 못했습니다",
  description,
  onRetry,
}: {
  title?: string;
  description: string;
  onRetry?: () => void;
}) {
  return (
    <Surface className="space-y-3 p-5">
      <h2 className="text-base font-semibold text-cafe-warning">{title}</h2>
      <p className="text-sm leading-relaxed text-cafe-ink-soft">{description}</p>
      {onRetry ? (
        <Button type="button" variant="secondary" onClick={onRetry}>
          다시 시도
        </Button>
      ) : null}
    </Surface>
  );
}
