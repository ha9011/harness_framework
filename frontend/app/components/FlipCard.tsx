"use client";

import { useState } from "react";

interface FlipCardProps {
  front: React.ReactNode;
  back: React.ReactNode;
  flipped?: boolean;
  onFlip?: () => void;
}

export default function FlipCard({ front, back, flipped, onFlip }: FlipCardProps) {
  const [internalFlipped, setInternalFlipped] = useState(false);
  const isFlipped = flipped !== undefined ? flipped : internalFlipped;

  const handleClick = () => {
    if (onFlip) {
      onFlip();
    } else {
      setInternalFlipped(!internalFlipped);
    }
  };

  return (
    <div
      className="w-full cursor-pointer"
      style={{ perspective: "1000px" }}
      onClick={handleClick}
    >
      <div
        className="relative w-full min-h-[280px]"
        style={{
          transformStyle: "preserve-3d",
          transition: "transform 0.4s ease",
          transform: isFlipped ? "rotateY(180deg)" : "rotateY(0deg)",
        }}
      >
        {/* 앞면 */}
        <div
          className="absolute inset-0 bg-raised rounded-[20px] border border-hairline shadow-card p-5 flex flex-col justify-center items-center"
          style={{ backfaceVisibility: "hidden" }}
        >
          {front}
        </div>
        {/* 뒷면 */}
        <div
          className="absolute inset-0 bg-raised rounded-[20px] border border-hairline shadow-card p-5 flex flex-col justify-center items-center"
          style={{ backfaceVisibility: "hidden", transform: "rotateY(180deg)" }}
        >
          {back}
        </div>
      </div>
    </div>
  );
}
