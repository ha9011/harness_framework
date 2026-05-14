import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  // 부모 디렉토리의 lockfile 감지로 인한 standalone 출력 중첩 방지 (Next 16 + Turbopack)
  turbopack: {
    root: __dirname,
  },
};

export default nextConfig;
