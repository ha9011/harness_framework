import { getToken, clearToken } from "@/lib/auth-token";

const BASE_URL = "/api";

// localStorage에 토큰이 있으면 Authorization 헤더로 반환 (PWA 폴백, ADR-020)
function authHeader(): Record<string, string> {
  const token = getToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

interface ErrorResponse {
  code: string;
  message: string;
}

class ApiError extends Error {
  code: string;
  status: number;

  constructor(code: string, message: string, status: number) {
    super(message);
    this.code = code;
    this.status = status;
  }
}

async function request<T>(
  path: string,
  options?: RequestInit
): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    credentials: "include",
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...authHeader(),
      ...options?.headers,
    },
  });

  if (!res.ok) {
    if (res.status === 401) {
      // 만료/무효 토큰이 매 요청 재전송되는 루프 방지 (ADR-020)
      clearToken();
      if (!path.includes('/auth/me')) {
        window.dispatchEvent(new Event('unauthorized'));
      }
      throw new ApiError('UNAUTHORIZED', '인증이 필요합니다', 401);
    }

    const error: ErrorResponse = await res.json().catch(() => ({
      code: "UNKNOWN",
      message: res.statusText,
    }));
    throw new ApiError(error.code, error.message, res.status);
  }

  if (res.status === 204) {
    return undefined as T;
  }

  return res.json();
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: "POST", body: JSON.stringify(body) }),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: "PUT", body: JSON.stringify(body) }),
  patch: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: "PATCH", body: JSON.stringify(body) }),
  delete: <T>(path: string) => request<T>(path, { method: "DELETE" }),
  upload: <T>(path: string, formData: FormData) =>
    uploadRequest<T>(path, formData),
};

async function uploadRequest<T>(path: string, formData: FormData): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    method: "POST",
    credentials: "include",
    headers: {
      ...authHeader(),
    },
    body: formData,
  });

  if (!res.ok) {
    if (res.status === 401) {
      // 만료/무효 토큰이 매 요청 재전송되는 루프 방지 (ADR-020)
      clearToken();
      window.dispatchEvent(new Event('unauthorized'));
      throw new ApiError('UNAUTHORIZED', '인증이 필요합니다', 401);
    }

    const error: ErrorResponse = await res.json().catch(() => ({
      code: "UNKNOWN",
      message: res.statusText,
    }));
    throw new ApiError(error.code, error.message, res.status);
  }

  return res.json();
}

export { ApiError };
