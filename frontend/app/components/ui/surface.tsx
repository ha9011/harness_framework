import type { HTMLAttributes } from "react";
import { cafeColors } from "@/app/lib/design-tokens";
import { cn } from "@/app/lib/utils";

type SurfaceProps = HTMLAttributes<HTMLDivElement> & {
  tone?: "raised" | "kraft" | "soft";
};

const surfaceTone: Record<NonNullable<SurfaceProps["tone"]>, string> = {
  raised: "bg-cafe-raised",
  kraft: "bg-cafe-kraft",
  soft: "bg-cafe-soft",
};

export function Surface({ className, tone = "raised", style, ...props }: SurfaceProps) {
  return (
    <div
      className={cn(
        "rounded-[18px] border border-[rgba(61,46,34,0.10)] shadow-cafe",
        surfaceTone[tone],
        className,
      )}
      style={{ borderColor: cafeColors.hairline, ...style }}
      {...props}
    />
  );
}
