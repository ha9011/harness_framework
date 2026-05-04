"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const navItems = [
  { href: "/", label: "홈", icon: "🏠" },
  { href: "/words", label: "단어", icon: "📖" },
  { href: "/patterns", label: "패턴", icon: "🔤" },
  { href: "/generate", label: "생성", icon: "✨" },
  { href: "/review", label: "복습", icon: "🃏" },
];

export default function BottomNav() {
  const pathname = usePathname();

  return (
    <nav className="fixed bottom-0 left-0 right-0 bg-raised border-t border-hairline pb-7">
      <div className="max-w-md mx-auto flex justify-around items-center h-14">
        {navItems.map((item) => {
          const isActive =
            item.href === "/"
              ? pathname === "/"
              : pathname.startsWith(item.href);

          return (
            <Link
              key={item.href}
              href={item.href}
              className={`flex flex-col items-center gap-0.5 text-xs transition-colors ${
                isActive
                  ? "text-primary font-semibold"
                  : "text-ink-muted"
              }`}
            >
              <span className="text-lg" suppressHydrationWarning>{item.icon}</span>
              <span>{item.label}</span>
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
