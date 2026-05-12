import type { Metadata } from "next";
import { Gowun_Dodum } from "next/font/google";
import "./globals.css";
import BottomNav from "./components/BottomNav";
import { AuthProvider } from "@/lib/auth-context";

const gowun = Gowun_Dodum({
  weight: "400",
  variable: "--font-gowun",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Cozy Cafe - 영어 패턴 학습기",
  description: "단어×패턴 AI 예문 생성 + SM-2 간격 반복 복습",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" className={`${gowun.variable} h-full antialiased`}>
      <body className="min-h-full flex flex-col font-sans" suppressHydrationWarning>
        <AuthProvider>
          <main className="flex-1 w-full max-w-md mx-auto pb-20 pt-10 px-4">
            {children}
          </main>
          <BottomNav />
        </AuthProvider>
      </body>
    </html>
  );
}
