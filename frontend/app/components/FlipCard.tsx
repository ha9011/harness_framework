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
      className="perspective-1000 w-full cursor-pointer"
      onClick={handleClick}
    >
      <div
        className={`relative w-full min-h-[280px] transition-transform duration-400 ease-in-out preserve-3d ${
          isFlipped ? "rotate-y-180" : ""
        }`}
      >
        {/* 앞면 */}
        <div className="absolute inset-0 backface-hidden bg-raised rounded-[20px] border border-hairline shadow-card p-5 flex flex-col justify-center items-center">
          {front}
        </div>
        {/* 뒷면 */}
        <div className="absolute inset-0 backface-hidden rotate-y-180 bg-raised rounded-[20px] border border-hairline shadow-card p-5 flex flex-col justify-center items-center">
          {back}
        </div>
      </div>
    </div>
  );
}
