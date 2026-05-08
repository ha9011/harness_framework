"use client";

import { BookOpen, Home, PanelTop, Rows3, Sparkles } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/app/lib/utils";

const navItems = [
  { href: "/", label: "홈", icon: Home },
  { href: "/words", label: "단어", icon: BookOpen },
  { href: "/patterns", label: "패턴", icon: Rows3 },
  { href: "/generate", label: "생성", icon: Sparkles },
  { href: "/review", label: "복습", icon: PanelTop },
] as const;

function isActive(pathname: string, href: string) {
  if (href === "/") {
    return pathname === "/";
  }

  return pathname === href || pathname.startsWith(`${href}/`);
}

export function BottomNav() {
  const pathname = usePathname();

  return (
    <nav
      aria-label="주요 화면"
      className="fixed inset-x-0 bottom-0 z-40 mx-auto max-w-lg border-t border-[rgba(61,46,34,0.10)] bg-cafe-raised px-2 pb-6 pt-2"
    >
      <ul className="grid grid-cols-5 gap-1">
        {navItems.map((item) => {
          const active = isActive(pathname, item.href);
          const Icon = item.icon;

          return (
            <li key={item.href}>
              <Link
                href={item.href}
                aria-current={active ? "page" : undefined}
                className={cn(
                  "flex min-h-14 flex-col items-center justify-center gap-1 rounded-[14px] px-2 text-[11px] font-medium transition-colors",
                  active
                    ? "bg-cafe-latte-soft text-cafe-latte-deep"
                    : "text-cafe-ink-muted hover:bg-cafe-soft hover:text-cafe-ink",
                )}
              >
                <Icon className="h-5 w-5" aria-hidden="true" />
                <span>{item.label}</span>
              </Link>
            </li>
          );
        })}
      </ul>
    </nav>
  );
}
