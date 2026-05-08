import type { HTMLAttributes } from "react";
import { cn } from "@/app/lib/utils";

type ChipVariant = "default" | "warm" | "sage" | "outline";

const chipVariants: Record<ChipVariant, string> = {
  default: "bg-cafe-soft text-cafe-ink-soft border-[rgba(61,46,34,0.10)]",
  warm: "bg-cafe-latte-soft text-cafe-latte-deep border-transparent",
  sage: "bg-cafe-sage-bg text-cafe-sage border-transparent",
  outline: "bg-transparent text-cafe-ink-soft border-[rgba(61,46,34,0.10)]",
};

type ChipProps = HTMLAttributes<HTMLSpanElement> & {
  variant?: ChipVariant;
};

export function Chip({ className, variant = "default", ...props }: ChipProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full border px-2.5 py-1 text-[11px] font-medium leading-none",
        chipVariants[variant],
        className,
      )}
      {...props}
    />
  );
}
