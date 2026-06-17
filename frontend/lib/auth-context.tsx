"use client";

import { createContext, useContext, useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { api } from "@/lib/api";
import { setToken, clearToken } from "@/lib/auth-token";
import type { AuthUser, AuthLoginResponse } from "@/lib/types";

interface AuthContextType {
  user: AuthUser | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  signup: (email: string, password: string, nickname: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);
  const router = useRouter();

  useEffect(() => {
    api
      .get<AuthUser>("/auth/me")
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    const handleUnauthorized = () => {
      clearToken();
      setUser(null);
      router.push("/login");
    };
    window.addEventListener("unauthorized", handleUnauthorized);
    return () => window.removeEventListener("unauthorized", handleUnauthorized);
  }, [router]);

  const login = useCallback(async (email: string, password: string) => {
    const data = await api.post<AuthLoginResponse>("/auth/login", { email, password });
    setToken(data.token);
    setUser({ id: data.id, email: data.email, nickname: data.nickname });
  }, []);

  const signup = useCallback(async (email: string, password: string, nickname: string) => {
    const data = await api.post<AuthLoginResponse>("/auth/signup", { email, password, nickname });
    setToken(data.token);
    setUser({ id: data.id, email: data.email, nickname: data.nickname });
  }, []);

  const logout = useCallback(async () => {
    await api.post("/auth/logout");
    clearToken();
    setUser(null);
    router.push("/login");
  }, [router]);

  return (
    <AuthContext.Provider value={{ user, loading, login, signup, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
