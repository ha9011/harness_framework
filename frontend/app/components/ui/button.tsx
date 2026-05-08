import { Slot } from "@radix-ui/react-slot";
import { cva, type VariantProps } from "class-variance-authority";
import * as React from "react";
import { cn } from "@/app/lib/utils";

const buttonVariants = cva(
  "inline-flex items-center justify-center gap-2 whitespace-nowrap rounded-[14px] text-sm font-medium transition-transform active:translate-y-px disabled:pointer-events-none disabled:opacity-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cafe-latte focus-visible:ring-offset-2 focus-visible:ring-offset-cafe-bg",
  {
    variants: {
      variant: {
        default: "bg-cafe-latte text-cafe-cta shadow-cafe-button hover:bg-cafe-latte-deep",
        secondary:
          "border border-[rgba(61,46,34,0.10)] bg-cafe-raised text-cafe-ink shadow-cafe hover:bg-cafe-soft",
        ghost:
          "border border-[rgba(61,46,34,0.10)] bg-transparent text-cafe-ink hover:bg-cafe-soft",
        sage: "bg-cafe-sage text-cafe-cta shadow-[0_6px_14px_-4px_rgba(122,143,107,0.4)] hover:bg-cafe-sage-deep",
        danger:
          "bg-cafe-warning text-cafe-cta shadow-[0_6px_14px_-4px_rgba(199,126,71,0.35)]",
      },
      size: {
        sm: "h-[34px] px-3 text-xs",
        default: "h-[42px] px-4",
        lg: "h-[52px] px-5 text-base",
        icon: "h-[42px] w-[42px]",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  },
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean;
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, asChild = false, ...props }, ref) => {
    const Comp = asChild ? Slot : "button";

    return <Comp className={cn(buttonVariants({ variant, size, className }))} ref={ref} {...props} />;
  },
);

Button.displayName = "Button";

export { Button, buttonVariants };
