const BASE_URL = "http://localhost:8080/api";

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
    headers: {
      "Content-Type": "application/json",
    },
    ...options,
  });

  if (!res.ok) {
    if (res.status === 401) {
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
    body: formData,
  });

  if (!res.ok) {
    if (res.status === 401) {
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
