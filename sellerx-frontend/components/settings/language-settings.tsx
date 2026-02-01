"use client";

import { useRouter, usePathname } from "next/navigation";
import { useLocale } from "next-intl";
import { SettingsSection } from "./settings-section";
import { CheckCircle2, Globe } from "lucide-react";
import { cn } from "@/lib/utils";

const languages = [
  {
    code: "tr",
    label: "Turkish",
    nativeLabel: "Türkçe",
    flag: "🇹🇷",
    description: "Varsayılan dil",
  },
  {
    code: "en",
    label: "English",
    nativeLabel: "English",
    flag: "🇬🇧",
    description: "English language",
  },
];

export function LanguageSettings() {
  const router = useRouter();
  const pathname = usePathname();
  const currentLocale = useLocale();

  const handleLanguageChange = (newLocale: string) => {
    if (newLocale === currentLocale) return;

    // Replace the locale in the pathname
    const segments = pathname.split("/");
    segments[1] = newLocale; // Replace locale segment
    const newPath = segments.join("/");

    router.push(newPath);
  };

  return (
    <SettingsSection
      title="Dil Tercihi"
      description="Uygulama dilini seçin. Sayfa yenilenerek yeni dil aktif olacaktır."
    >
      <div className="grid gap-4 sm:grid-cols-2">
        {languages.map((lang) => {
          const isActive = currentLocale === lang.code;

          return (
            <button
              key={lang.code}
              onClick={() => handleLanguageChange(lang.code)}
              className={cn(
                "relative flex items-center gap-4 p-5 rounded-xl border-2 transition-all text-left",
                isActive
                  ? "border-primary bg-primary/10 dark:bg-primary/20 shadow-sm"
                  : "border-border hover:border-border hover:bg-accent"
              )}
            >
              <span className="text-4xl">{lang.flag}</span>
              <div className="flex-1">
                <p
                  className={cn(
                    "font-semibold text-lg",
                    isActive ? "text-primary" : "text-foreground"
                  )}
                >
                  {lang.nativeLabel}
                </p>
                <p className="text-sm text-muted-foreground">{lang.description}</p>
              </div>
              {isActive && (
                <CheckCircle2 className="h-6 w-6 text-[#1D70F1]" />
              )}
            </button>
          );
        })}
      </div>

      <div className="mt-6 p-4 rounded-lg bg-amber-50 border border-amber-200">
        <div className="flex items-start gap-3">
          <Globe className="h-5 w-5 text-amber-600 mt-0.5" />
          <div>
            <p className="font-medium text-amber-900">Dil desteği hakkında</p>
            <p className="text-sm text-amber-700 mt-1">
              Şu anda Türkçe ve İngilizce dil desteği sunulmaktadır. Dil değiştirdiğinizde
              tüm menüler, butonlar ve sistem mesajları seçilen dilde görüntülenecektir.
            </p>
          </div>
        </div>
      </div>
    </SettingsSection>
  );
}
