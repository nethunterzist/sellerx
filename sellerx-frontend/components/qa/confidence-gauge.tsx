"use client";

import { motion } from "motion/react";

interface ConfidenceGaugeProps {
  score: number;
  size?: "sm" | "md" | "lg";
  showLabel?: boolean;
}

const sizeConfig = {
  sm: { px: 32, strokeWidth: 3, fontSize: 8 },
  md: { px: 48, strokeWidth: 4, fontSize: 11 },
  lg: { px: 64, strokeWidth: 5, fontSize: 14 },
} as const;

function getColor(score: number): string {
  if (score < 0.5) return "#ef4444";
  if (score < 0.7) return "#f97316";
  if (score < 0.9) return "#eab308";
  return "#22c55e";
}

export function ConfidenceGauge({
  score,
  size = "md",
  showLabel = true,
}: ConfidenceGaugeProps) {
  const clamped = Math.max(0, Math.min(1, score));
  const { px, strokeWidth, fontSize } = sizeConfig[size];
  const radius = (px - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  const percent = Math.round(clamped * 100);
  const color = getColor(clamped);

  return (
    <svg width={px} height={px} viewBox={`0 0 ${px} ${px}`}>
      {/* Background circle */}
      <circle
        cx={px / 2}
        cy={px / 2}
        r={radius}
        fill="none"
        stroke="currentColor"
        strokeWidth={strokeWidth}
        className="text-muted-foreground/20"
      />
      {/* Animated fill circle */}
      <motion.circle
        cx={px / 2}
        cy={px / 2}
        r={radius}
        fill="none"
        stroke={color}
        strokeWidth={strokeWidth}
        strokeLinecap="round"
        strokeDasharray={circumference}
        initial={{ strokeDashoffset: circumference }}
        animate={{ strokeDashoffset: circumference * (1 - clamped) }}
        transition={{ duration: 0.8, ease: "easeOut" }}
        transform={`rotate(-90 ${px / 2} ${px / 2})`}
      />
      {/* Center percentage text */}
      {showLabel && (
        <text
          x={px / 2}
          y={px / 2}
          textAnchor="middle"
          dominantBaseline="central"
          fontSize={fontSize}
          fontWeight={600}
          fill="currentColor"
          className="text-foreground"
        >
          {percent}%
        </text>
      )}
    </svg>
  );
}
