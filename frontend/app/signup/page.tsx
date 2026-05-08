"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { UserPlus } from "lucide-react";
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

const signupSchema = z.object({
  email: z.string().min(1, "이메일을 입력해주세요").email("이메일 형식을 확인해주세요"),
  nickname: z.string().min(1, "닉네임을 입력해주세요"),
  password: z.string().min(8, "비밀번호는 8자 이상이어야 합니다"),
});

type SignupFormValues = z.infer<typeof signupSchema>;

export default function SignupPage() {
  const router = useRouter();
  const { signup } = useAuth();
  const [serverError, setServerError] = useState<string | null>(null);
  const {
    formState: { errors, isSubmitting },
    handleSubmit,
    register,
  } = useForm<SignupFormValues>({
    resolver: zodResolver(signupSchema),
    defaultValues: {
      email: "",
      nickname: "",
      password: "",
    },
  });

  async function onSubmit(values: SignupFormValues) {
    setServerError(null);

    try {
      await signup(values);
      router.push("/");
    } catch (error) {
      setServerError(error instanceof ApiError ? error.message : "회원가입에 실패했습니다");
    }
  }

  return (
    <main className="min-h-dvh bg-cafe-bg px-4 py-8 text-cafe-ink">
      <div className="mx-auto flex min-h-[calc(100dvh-4rem)] max-w-md flex-col justify-center gap-6">
        <header className="space-y-3">
          <Chip variant="sage">Coffee Tree</Chip>
          <div className="space-y-2">
            <h1 className="text-2xl font-semibold text-cafe-ink">회원가입</h1>
            <p className="text-sm leading-relaxed text-cafe-ink-soft">
              단어와 패턴을 내 계정에 안전하게 쌓기 시작하세요.
            </p>
          </div>
        </header>

        <Surface className="p-5">
          <form className="space-y-4" noValidate onSubmit={handleSubmit(onSubmit)}>
            <div className="space-y-2">
              <label htmlFor="signup-email" className="text-sm font-semibold text-cafe-ink">
                이메일
              </label>
              <input
                id="signup-email"
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
              <label htmlFor="signup-nickname" className="text-sm font-semibold text-cafe-ink">
                닉네임
              </label>
              <input
                id="signup-nickname"
                type="text"
                autoComplete="nickname"
                aria-invalid={Boolean(errors.nickname)}
                className="w-full rounded-[12px] border border-[rgba(61,46,34,0.10)] bg-cafe-soft px-4 py-3 text-sm text-cafe-ink placeholder:text-cafe-ink-muted focus:outline-none focus:ring-2 focus:ring-cafe-latte"
                placeholder="혜진"
                {...register("nickname")}
              />
              {errors.nickname ? (
                <p className="text-xs font-medium text-cafe-warning" role="alert">
                  {errors.nickname.message}
                </p>
              ) : null}
            </div>

            <div className="space-y-2">
              <label htmlFor="signup-password" className="text-sm font-semibold text-cafe-ink">
                비밀번호
              </label>
              <input
                id="signup-password"
                type="password"
                autoComplete="new-password"
                aria-invalid={Boolean(errors.password)}
                className="w-full rounded-[12px] border border-[rgba(61,46,34,0.10)] bg-cafe-soft px-4 py-3 text-sm text-cafe-ink placeholder:text-cafe-ink-muted focus:outline-none focus:ring-2 focus:ring-cafe-latte"
                placeholder="8자 이상"
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
              <UserPlus className="h-4 w-4" aria-hidden="true" />
              회원가입
            </Button>
          </form>
        </Surface>

        <div className="text-center text-sm text-cafe-ink-soft">
          이미 계정이 있나요?{" "}
          <Link href="/login" className="font-semibold text-cafe-latte-deep underline-offset-4 hover:underline">
            로그인
          </Link>
        </div>
      </div>
    </main>
  );
}
