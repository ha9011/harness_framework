import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import CremaLoader from "@/app/components/CremaLoader";

describe("CremaLoader", () => {
  it("기본 메시지 '불러오는 중...'이 표시된다", () => {
    render(<CremaLoader />);
    expect(screen.getByText("불러오는 중...")).toBeInTheDocument();
  });

  it("커스텀 message prop이 반영된다", () => {
    render(<CremaLoader message="예문을 만들고 있어요..." />);
    expect(screen.getByText("예문을 만들고 있어요...")).toBeInTheDocument();
  });

  it("SVG 요소가 렌더링된다", () => {
    const { container } = render(<CremaLoader />);
    const svg = container.querySelector("svg");
    expect(svg).toBeInTheDocument();
  });

  it("role='status' 속성이 적용되어 있다", () => {
    render(<CremaLoader />);
    expect(screen.getByRole("status")).toBeInTheDocument();
  });

  it("aria-live='polite' 속성이 적용되어 있다", () => {
    render(<CremaLoader />);
    const status = screen.getByRole("status");
    expect(status).toHaveAttribute("aria-live", "polite");
  });
});
