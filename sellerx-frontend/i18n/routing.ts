import { defineRouting } from "next-intl/routing";

export const routing = defineRouting({
  locales: ["tr", "en"],
  defaultLocale: "tr",
  localePrefix: "always",
  localeDetection: false, // Tarayıcı diline bakma, her zaman Türkçe ile başla
});
