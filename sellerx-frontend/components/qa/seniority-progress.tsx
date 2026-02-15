"use client";

import { motion } from "motion/react";
import { cn } from "@/lib/utils";

export type SeniorityLevel = "JUNIOR" | "LEARNING" | "SENIOR" | "EXPERT";

interface SeniorityProgressProps {
  level: SeniorityLevel;
  progress: number; // 0-100
  variant?: "ring" | "bar";
  size?: "sm" | "md" | "lg";
  showLabel?: boolean;
}

const levelConfig = {
  JUNIOR: { color: "#ef4444", label: "Junior", range: [0, 25] },
  LEARNING: { color: "#f97316", label: "Öğreniyor", range: [25, 50] },
  SENIOR: { color: "#eab308", label: "Senior", range: [50, 75] },
  EXPERT: { color: "#22c55e", label: "Uzman", range: [75, 100] },
};

const sizeConfig = {
  sm: { ring: 40, strokeWidth: 3, fontSize: 10 },
  md: { ring: 56, strokeWidth: 4, fontSize: 12 },
  lg: { ring: 72, strokeWidth: 5, fontSize: 14 },
};

export function SeniorityProgress({
  level,
  progress,
  variant = "ring",
  size = "md",
  showLabel = true,
}: SeniorityProgressProps) {
  const config = levelConfig[level];
  const sizes = sizeConfig[size];
  const clampedProgress = Math.max(0, Math.min(100, progress));

  if (variant === "bar") {
    return (
      <div className="space-y-1.5">
        {showLabel && (
          <div className="flex items-center justify-between text-xs">
            <span className="font-medium">{config.label}</span>
            <span className="text-muted-foreground">{Math.round(clampedProgress)}%</span>
          </div>
        )}
        <div className="relative w-full h-2 bg-muted rounded-full overflow-hidden">
          <motion.div
            className="absolute inset-y-0 left-0 rounded-full"
            style={{ backgroundColor: config.color }}
            initial={{ width: 0 }}
            animate={{ width: `${clampedProgress}%` }}
            transition={{ duration: 0.6, ease: "easeOut" }}
          />
        </div>
      </div>
    );
  }

  // Ring variant
  const px = sizes.ring;
  const radius = (px - sizes.strokeWidth * 2) / 2;
  const circumference = 2 * Math.PI * radius;
  const offset = circumference * (1 - clampedProgress / 100);

  return (
    <div className="flex flex-col items-center gap-1">
      <svg width={px} height={px} viewBox={`0 0 ${px} ${px}`}>
        {/* Background circle */}
        <circle
          cx={px / 2}
          cy={px / 2}
          r={radius}
          fill="none"
          stroke="currentColor"
          strokeWidth={sizes.strokeWidth}
          className="text-muted/30"
        />
        {/* Animated progress circle */}
        <motion.circle
          cx={px / 2}
          cy={px / 2}
          r={radius}
          fill="none"
          stroke={config.color}
          strokeWidth={sizes.strokeWidth}
          strokeLinecap="round"
          strokeDasharray={circumference}
          initial={{ strokeDashoffset: circumference }}
          animate={{ strokeDashoffset: offset }}
          transition={{ duration: 0.8, ease: "easeOut" }}
          transform={`rotate(-90 ${px / 2} ${px / 2})`}
        />
        {/* Center percentage */}
        {showLabel && (
          <text
            x={px / 2}
            y={px / 2}
            textAnchor="middle"
            dominantBaseline="central"
            fontSize={sizes.fontSize}
            fontWeight={600}
            fill={config.color}
          >
            {Math.round(clampedProgress)}%
          </text>
        )}
      </svg>
      {showLabel && (
        <span className="text-xs font-medium" style={{ color: config.color }}>
          {config.label}
        </span>
      )}
    </div>
  );
}

// Helper function to determine level based on progress
export function getSeniorityLevel(progress: number): SeniorityLevel {
  if (progress < 25) return "JUNIOR";
  if (progress < 50) return "LEARNING";
  if (progress < 75) return "SENIOR";
  return "EXPERT";
}
