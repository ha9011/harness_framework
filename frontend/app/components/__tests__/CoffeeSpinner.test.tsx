import { render } from "@testing-library/react";
import "@testing-library/jest-dom";
import CoffeeSpinner from "@/app/components/CoffeeSpinner";

describe("CoffeeSpinner", () => {
  it("SVG 요소가 렌더링된다", () => {
    const { container } = render(<CoffeeSpinner />);
    const svg = container.querySelector("svg");
    expect(svg).toBeInTheDocument();
  });

  it("기본 size가 20이다", () => {
    const { container } = render(<CoffeeSpinner />);
    const svg = container.querySelector("svg");
    expect(svg).toHaveAttribute("width", "20");
    expect(svg).toHaveAttribute("height", "20");
  });

  it("커스텀 size prop이 반영된다", () => {
    const { container } = render(<CoffeeSpinner size={32} />);
    const svg = container.querySelector("svg");
    expect(svg).toHaveAttribute("width", "32");
    expect(svg).toHaveAttribute("height", "32");
  });

  it("className prop이 SVG 래퍼에 전달된다", () => {
    const { container } = render(<CoffeeSpinner className="my-custom-class" />);
    const svg = container.querySelector("svg");
    expect(svg).toHaveClass("my-custom-class");
  });

  it("aria-hidden='true'가 적용되어 있다", () => {
    const { container } = render(<CoffeeSpinner />);
    const svg = container.querySelector("svg");
    expect(svg).toHaveAttribute("aria-hidden", "true");
  });
});
