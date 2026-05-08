import { http, HttpResponse } from "msw";
import { API_BASE_URL } from "@/app/lib/api";

export const handlers = [
  http.get(`${API_BASE_URL}/api/health`, () => {
    return HttpResponse.json({ status: "UP" });
  }),
];
