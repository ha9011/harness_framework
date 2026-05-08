import type { Metadata } from "next";
import "./globals.css";
import { AppShell } from "@/app/components/app-shell";
import { AppProviders } from "@/app/components/providers/app-providers";

export const metadata: Metadata = {
  title: "영어 패턴 학습기",
  description: "Cozy Cafe 영어 패턴 학습 앱",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body>
        <AppProviders>
          <AppShell>{children}</AppShell>
        </AppProviders>
      </body>
    </html>
  );
}
