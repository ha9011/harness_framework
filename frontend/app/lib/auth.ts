import { apiFetch } from "@/app/lib/api";

export type AuthUser = {
  id: number;
  email: string;
  nickname: string;
};

export type LoginCredentials = {
  email: string;
  password: string;
};

export type SignupCredentials = LoginCredentials & {
  nickname: string;
};

export const authMeQueryKey = ["auth", "me"] as const;

export function fetchCurrentUser() {
  return apiFetch<AuthUser>("/api/auth/me");
}

export function loginUser(credentials: LoginCredentials) {
  return apiFetch<AuthUser>("/api/auth/login", {
    method: "POST",
    json: {
      email: credentials.email,
      password: credentials.password,
    },
  });
}

export function signupUser(credentials: SignupCredentials) {
  return apiFetch<AuthUser>("/api/auth/signup", {
    method: "POST",
    json: {
      email: credentials.email,
      password: credentials.password,
      nickname: credentials.nickname,
    },
  });
}

export function logoutUser() {
  return apiFetch<void>("/api/auth/logout", {
    method: "POST",
  });
}
