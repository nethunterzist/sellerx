"use client";

import { useEffect, useRef, useState } from "react";
import { motion, useSpring, useTransform } from "motion/react";

interface NumberTickerProps {
  value: number;
  prefix?: string;
  suffix?: string;
  /** Number of decimal places */
  decimals?: number;
  /** Animation duration in seconds */
  duration?: number;
  className?: string;
  /** Locale for number formatting */
  locale?: string;
}

export function NumberTicker({
  value,
  prefix = "",
  suffix = "",
  decimals = 0,
  duration = 0.6,
  className,
  locale = "tr-TR",
}: NumberTickerProps) {
  const springValue = useSpring(0, {
    stiffness: 100,
    damping: 20,
    duration: duration * 1000,
  });

  const display = useTransform(springValue, (current) => {
    const formatted = current.toLocaleString(locale, {
      minimumFractionDigits: decimals,
      maximumFractionDigits: decimals,
    });
    return `${prefix}${formatted}${suffix}`;
  });

  const [displayText, setDisplayText] = useState(() => {
    const formatted = value.toLocaleString(locale, {
      minimumFractionDigits: decimals,
      maximumFractionDigits: decimals,
    });
    return `${prefix}${formatted}${suffix}`;
  });

  const prevValue = useRef(value);

  useEffect(() => {
    // Only animate if value actually changed
    if (prevValue.current !== value) {
      springValue.set(value);
      prevValue.current = value;
    }
  }, [value, springValue]);

  useEffect(() => {
    const unsubscribe = display.on("change", (v) => {
      setDisplayText(v);
    });
    return unsubscribe;
  }, [display]);

  // Set initial value without animation
  useEffect(() => {
    springValue.jump(value);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <motion.span className={className}>
      {displayText}
    </motion.span>
  );
}
