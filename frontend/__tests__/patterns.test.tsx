import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import PatternsPage from "@/app/patterns/page";
import { AppProviders } from "@/app/components/providers/app-providers";
import { API_BASE_URL } from "@/app/lib/api";
import { server } from "@/test/msw/server";

const pattern = {
  id: 1,
  template: "I'm afraid that...",
  description: "유감스럽게도 ~인 것 같아요",
  examples: [{ id: 1, sortOrder: 1, sentence: "I'm afraid that we'll be late.", translation: "늦을 것 같아요." }],
};

describe("patterns", () => {
  it("creates a manual pattern with ordered examples", async () => {
    const user = userEvent.setup();
    let createBody: unknown;

    server.use(
      http.get(`${API_BASE_URL}/api/patterns`, () =>
        HttpResponse.json({ content: [pattern], totalElements: 1, totalPages: 1, page: 0, size: 10 }),
      ),
      http.post(`${API_BASE_URL}/api/patterns`, async ({ request }) => {
        createBody = await request.json();
        return HttpResponse.json(pattern, { status: 201 });
      }),
    );

    render(
      <AppProviders>
        <PatternsPage />
      </AppProviders>,
    );

    expect(await screen.findByText("I'm afraid that...")).toBeInTheDocument();
    await user.clear(screen.getByLabelText("패턴"));
    await user.type(screen.getByLabelText("패턴"), "I used to...");
    await user.type(screen.getByLabelText("설명"), "예전에는 ~하곤 했어요");
    await user.type(screen.getByLabelText("예문 1"), "I used to drink coffee.");
    await user.type(screen.getByLabelText("예문 해석 1"), "나는 커피를 마시곤 했어요.");
    await user.click(screen.getByRole("button", { name: "패턴 저장" }));

    await waitFor(() =>
      expect(createBody).toEqual({
        template: "I used to...",
        description: "예전에는 ~하곤 했어요",
        examples: [{ sentence: "I used to drink coffee.", translation: "나는 커피를 마시곤 했어요." }],
      }),
    );
  });

  it("extracts a pattern from image and saves after confirmation", async () => {
    const user = userEvent.setup();
    let createBody: unknown;

    server.use(
      http.get(`${API_BASE_URL}/api/patterns`, () =>
        HttpResponse.json({ content: [], totalElements: 0, totalPages: 0, page: 0, size: 10 }),
      ),
      http.post(`${API_BASE_URL}/api/patterns/extract`, () => HttpResponse.json(pattern)),
      http.post(`${API_BASE_URL}/api/patterns`, async ({ request }) => {
        createBody = await request.json();
        return HttpResponse.json(pattern, { status: 201 });
      }),
    );

    render(
      <AppProviders>
        <PatternsPage />
      </AppProviders>,
    );

    await user.click(screen.getByRole("tab", { name: "이미지 추출" }));
    await user.upload(screen.getByLabelText("패턴 이미지"), new File(["image"], "pattern.png", { type: "image/png" }));
    expect(await screen.findByDisplayValue("I'm afraid that...")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "패턴 저장" }));
    await waitFor(() => expect(createBody).toEqual(expect.objectContaining({ template: "I'm afraid that..." })));
  });
});
