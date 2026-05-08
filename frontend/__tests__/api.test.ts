import { http, HttpResponse } from "msw";
import { API_BASE_URL, ApiError, apiFetch } from "@/app/lib/api";
import { server } from "@/test/msw/server";

describe("apiFetch", () => {
  it("uses the configured API base URL and includes credentials by default", async () => {
    let credentials: RequestCredentials | undefined;

    server.use(
      http.get(`${API_BASE_URL}/api/health`, ({ request }) => {
        credentials = request.credentials;
        return HttpResponse.json({ status: "UP" });
      }),
    );

    await expect(apiFetch<{ status: string }>("/api/health")).resolves.toEqual({ status: "UP" });
    expect(credentials).toBe("include");
  });

  it("serializes json bodies and exposes backend error messages", async () => {
    server.use(
      http.post(`${API_BASE_URL}/api/example`, async ({ request }) => {
        const body = await request.json();

        if (body && typeof body === "object" && "name" in body) {
          return HttpResponse.json({ message: "invalid example" }, { status: 400 });
        }

        return HttpResponse.json({ ok: true });
      }),
    );

    await expect(apiFetch("/api/example", { method: "POST", json: { name: "test" } })).rejects.toEqual(
      expect.objectContaining<ApiError>({
        name: "ApiError",
        status: 400,
        message: "invalid example",
      }),
    );
  });
});
