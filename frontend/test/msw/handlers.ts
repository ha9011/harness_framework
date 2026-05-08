import { http, HttpResponse } from "msw";
import { API_BASE_URL } from "@/app/lib/api";

export const handlers = [
  http.get(`${API_BASE_URL}/api/health`, () => {
    return HttpResponse.json({ status: "UP" });
  }),
  http.get(`${API_BASE_URL}/api/auth/me`, () => {
    return HttpResponse.json(
      { error: "UNAUTHORIZED", message: "인증이 필요합니다" },
      { status: 401 },
    );
  }),
  http.post(`${API_BASE_URL}/api/auth/logout`, () => {
    return new HttpResponse(null, { status: 204 });
  }),
];
