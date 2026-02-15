"use client";

import { motion, AnimatePresence } from "motion/react";
import { type ReactNode } from "react";

interface AnimateHeightProps {
  isOpen: boolean;
  children: ReactNode;
  className?: string;
  duration?: number;
}

export function AnimateHeight({
  isOpen,
  children,
  className,
  duration = 0.2,
}: AnimateHeightProps) {
  return (
    <AnimatePresence initial={false}>
      {isOpen && (
        <motion.div
          initial={{ height: 0, opacity: 0 }}
          animate={{ height: "auto", opacity: 1 }}
          exit={{ height: 0, opacity: 0 }}
          transition={{ duration, ease: [0.25, 0.1, 0.25, 1] as const }}
          style={{ overflow: "hidden" }}
          className={className}
        >
          {children}
        </motion.div>
      )}
    </AnimatePresence>
  );
}
