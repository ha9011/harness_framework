"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { createContext, useCallback, useContext, useMemo } from "react";
import type { PropsWithChildren } from "react";
import {
  authMeQueryKey,
  fetchCurrentUser,
  loginUser,
  logoutUser,
  signupUser,
} from "@/app/lib/auth";
import type { AuthUser, LoginCredentials, SignupCredentials } from "@/app/lib/auth";

type AuthStatus = "loading" | "authenticated" | "unauthenticated";

type AuthContextValue = {
  user: AuthUser | null;
  status: AuthStatus;
  isAuthenticated: boolean;
  login: (credentials: LoginCredentials) => Promise<void>;
  signup: (credentials: SignupCredentials) => Promise<void>;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
  const queryClient = useQueryClient();
  const currentUserQuery = useQuery<AuthUser | null>({
    queryKey: authMeQueryKey,
    queryFn: fetchCurrentUser,
  });

  const user = currentUserQuery.data ?? null;
  const status: AuthStatus = currentUserQuery.isPending
    ? "loading"
    : user
      ? "authenticated"
      : "unauthenticated";

  const login = useCallback(
    async (credentials: LoginCredentials) => {
      const authenticatedUser = await loginUser(credentials);
      queryClient.setQueryData(authMeQueryKey, authenticatedUser);
    },
    [queryClient],
  );

  const signup = useCallback(
    async (credentials: SignupCredentials) => {
      const authenticatedUser = await signupUser(credentials);
      queryClient.setQueryData(authMeQueryKey, authenticatedUser);
    },
    [queryClient],
  );

  const logout = useCallback(async () => {
    await logoutUser();
    queryClient.setQueryData(authMeQueryKey, null);
  }, [queryClient]);

  const refresh = useCallback(async () => {
    await queryClient.invalidateQueries({ queryKey: authMeQueryKey });
  }, [queryClient]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      status,
      isAuthenticated: status === "authenticated",
      login,
      signup,
      logout,
      refresh,
    }),
    [login, logout, refresh, signup, status, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);

  if (!value) {
    throw new Error("useAuth must be used within AuthProvider");
  }

  return value;
}
