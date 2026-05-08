"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { LogIn } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { useAuth } from "@/app/components/providers/auth-provider";
import { Button } from "@/app/components/ui/button";
import { Chip } from "@/app/components/ui/chip";
import { Surface } from "@/app/components/ui/surface";
import { ApiError } from "@/app/lib/api";

const loginSchema = z.object({
  email: z.string().min(1, "이메일을 입력해주세요").email("이메일 형식을 확인해주세요"),
  password: z.string().min(1, "비밀번호를 입력해주세요"),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuth();
  const [serverError, setServerError] = useState<string | null>(null);
  const {
    formState: { errors, isSubmitting },
    handleSubmit,
    register,
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: "",
      password: "",
    },
  });

  async function onSubmit(values: LoginFormValues) {
    setServerError(null);

    try {
      await login(values);
      router.push("/");
    } catch (error) {
      setServerError(error instanceof ApiError ? error.message : "로그인에 실패했습니다");
    }
  }

  return (
    <main className="min-h-dvh bg-cafe-bg px-4 py-8 text-cafe-ink">
      <div className="mx-auto flex min-h-[calc(100dvh-4rem)] max-w-md flex-col justify-center gap-6">
        <header className="space-y-3">
          <Chip variant="warm">Cozy Cafe</Chip>
          <div className="space-y-2">
            <h1 className="text-2xl font-semibold text-cafe-ink">로그인</h1>
            <p className="text-sm leading-relaxed text-cafe-ink-soft">
              오늘의 복습을 이어가려면 계정으로 들어오세요.
            </p>
          </div>
        </header>

        <Surface className="p-5">
          <form className="space-y-4" noValidate onSubmit={handleSubmit(onSubmit)}>
            <div className="space-y-2">
              <label htmlFor="login-email" className="text-sm font-semibold text-cafe-ink">
                이메일
              </label>
              <input
                id="login-email"
                type="email"
                autoComplete="email"
                aria-invalid={Boolean(errors.email)}
                className="w-full rounded-[12px] border border-[rgba(61,46,34,0.10)] bg-cafe-soft px-4 py-3 text-sm text-cafe-ink placeholder:text-cafe-ink-muted focus:outline-none focus:ring-2 focus:ring-cafe-latte"
                placeholder="user@example.com"
                {...register("email")}
              />
              {errors.email ? (
                <p className="text-xs font-medium text-cafe-warning" role="alert">
                  {errors.email.message}
                </p>
              ) : null}
            </div>

            <div className="space-y-2">
              <label htmlFor="login-password" className="text-sm font-semibold text-cafe-ink">
                비밀번호
              </label>
              <input
                id="login-password"
                type="password"
                autoComplete="current-password"
                aria-invalid={Boolean(errors.password)}
                className="w-full rounded-[12px] border border-[rgba(61,46,34,0.10)] bg-cafe-soft px-4 py-3 text-sm text-cafe-ink placeholder:text-cafe-ink-muted focus:outline-none focus:ring-2 focus:ring-cafe-latte"
                placeholder="비밀번호"
                {...register("password")}
              />
              {errors.password ? (
                <p className="text-xs font-medium text-cafe-warning" role="alert">
                  {errors.password.message}
                </p>
              ) : null}
            </div>

            {serverError ? (
              <p className="rounded-[12px] bg-cafe-soft px-3 py-2 text-xs font-medium text-cafe-warning" role="alert">
                {serverError}
              </p>
            ) : null}

            <Button type="submit" size="lg" className="w-full" disabled={isSubmitting}>
              <LogIn className="h-4 w-4" aria-hidden="true" />
              로그인
            </Button>
          </form>
        </Surface>

        <div className="text-center text-sm text-cafe-ink-soft">
          계정이 없나요?{" "}
          <Link href="/signup" className="font-semibold text-cafe-latte-deep underline-offset-4 hover:underline">
            회원가입
          </Link>
        </div>
      </div>
    </main>
  );
}
