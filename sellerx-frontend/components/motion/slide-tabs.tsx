"use client";

import { motion, AnimatePresence } from "motion/react";
import { type ReactNode } from "react";

interface SlideTabsProps {
  activeTab: string;
  /** 1 for forward, -1 for backward */
  direction: number;
  children: ReactNode;
  className?: string;
  duration?: number;
}

const slideVariants = {
  enter: (direction: number) => ({
    x: direction > 0 ? 80 : -80,
    opacity: 0,
  }),
  center: {
    x: 0,
    opacity: 1,
  },
  exit: (direction: number) => ({
    x: direction > 0 ? -80 : 80,
    opacity: 0,
  }),
};

export function SlideTabs({
  activeTab,
  direction,
  children,
  className,
  duration = 0.2,
}: SlideTabsProps) {
  return (
    <div className={className} style={{ overflow: "hidden" }}>
      <AnimatePresence mode="wait" custom={direction}>
        <motion.div
          key={activeTab}
          custom={direction}
          variants={slideVariants}
          initial="enter"
          animate="center"
          exit="exit"
          transition={{ duration, ease: "easeInOut" }}
        >
          {children}
        </motion.div>
      </AnimatePresence>
    </div>
  );
}
