"use client";

import { useMemo } from "react";
import { motion } from "motion/react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import { Eye } from "lucide-react";

interface RulesLivePreviewProps {
  settings: {
    tone: "professional" | "friendly" | "formal";
    language: string;
    maxAnswerLength: number;
    includeGreeting: boolean;
    includeSignature: boolean;
    signatureText: string;
  };
}

const TONE_GREETINGS = {
  professional: { tr: "Merhaba,", en: "Hello," },
  friendly: { tr: "Selam!", en: "Hi there!" },
  formal: { tr: "Sayın Müşterimiz,", en: "Dear Customer," },
};

const TONE_LABELS = {
  professional: { tr: "Profesyonel", en: "Professional" },
  friendly: { tr: "Samimi", en: "Friendly" },
  formal: { tr: "Resmi", en: "Formal" },
};

const EXAMPLE_ANSWERS = {
  professional: {
    tr: "Ürünümüz hakkında sorunuz için teşekkür ederiz. Bu ürün, yüksek kaliteli malzemeden üretilmiştir ve uzun ömürlü kullanım için tasarlanmıştır. Ürün stoklarımızda mevcuttur ve hızlı kargo seçeneği ile 2-3 iş günü içinde elinizde olacaktır.",
    en: "Thank you for your question about our product. This product is made from high-quality materials and designed for long-lasting use. The product is in stock and will be delivered to you within 2-3 business days with our express shipping option.",
  },
  friendly: {
    tr: "Hey! Ürünle ilgili soruna çok teşekkürler 😊 Bu ürün gerçekten harika, süper kaliteli malzemeden yapılmış ve uzun süre dayanıyor. Stokta var, istersen hızlı kargo ile 2-3 güne gelir!",
    en: "Hey! Thanks so much for your question about the product 😊 This product is really great, made from super quality materials and lasts a long time. It's in stock, and if you want, it'll arrive in 2-3 days with express shipping!",
  },
  formal: {
    tr: "Saygıdeğer müşterimiz, ürünümüz hakkındaki sorunuz için size teşekkür ederiz. Söz konusu ürün, yüksek standartlarda kaliteli hammaddelerden imal edilmiştir ve uzun ömürlü kullanım için özel olarak tasarlanmıştır. Ürün stoklarımızda bulunmaktadır ve ekspres kargo seçeneği ile 2-3 iş günü içerisinde tarafınıza ulaştırılacaktır.",
    en: "Dear valued customer, we thank you for your inquiry about our product. The product in question is manufactured from high-standard quality raw materials and specially designed for long-lasting use. The product is available in our inventory and will be delivered to you within 2-3 business days via our express shipping option.",
  },
};

function getToneGreeting(tone: "professional" | "friendly" | "formal", lang: string): string {
  return TONE_GREETINGS[tone][lang as keyof typeof TONE_GREETINGS.professional] || TONE_GREETINGS[tone].tr;
}

function getExampleAnswer(tone: "professional" | "friendly" | "formal", lang: string): string {
  return EXAMPLE_ANSWERS[tone][lang as keyof typeof EXAMPLE_ANSWERS.professional] || EXAMPLE_ANSWERS[tone].tr;
}

function getToneLabel(tone: "professional" | "friendly" | "formal", lang: string): string {
  return TONE_LABELS[tone][lang as keyof typeof TONE_LABELS.professional] || TONE_LABELS[tone].tr;
}

export function RulesLivePreview({ settings }: RulesLivePreviewProps) {
  const exampleMessage = useMemo(() => {
    let message = "";

    // Greeting
    if (settings.includeGreeting) {
      message += getToneGreeting(settings.tone, settings.language);
    }

    // Body
    message += message ? "\n\n" : "";
    message += getExampleAnswer(settings.tone, settings.language);

    // Signature
    if (settings.includeSignature && settings.signatureText) {
      message += "\n\n";
      message += settings.signatureText;
    }

    // Truncate to max length
    if (message.length > settings.maxAnswerLength) {
      message = message.substring(0, settings.maxAnswerLength) + "...";
    }

    return message;
  }, [settings]);

  const charCount = exampleMessage.length;
  const isOverLimit = charCount > settings.maxAnswerLength;
  const usagePercent = Math.round((charCount / settings.maxAnswerLength) * 100);

  return (
    <Card className="glass-card sticky top-24">
      <CardHeader className="border-b glass-header">
        <CardTitle className="flex items-center justify-between text-base">
          <div className="flex items-center gap-2">
            <Eye className="h-4 w-4" />
            <span>Canlı Önizleme</span>
          </div>
          <Badge variant={isOverLimit ? "destructive" : "secondary"} className="text-xs">
            {charCount}/{settings.maxAnswerLength}
          </Badge>
        </CardTitle>
      </CardHeader>
      <CardContent className="p-4 space-y-4">
        {/* Settings badges */}
        <div className="flex flex-wrap gap-2">
          <Badge variant="outline" className="text-xs">
            {getToneLabel(settings.tone, settings.language)}
          </Badge>
          <Badge variant="outline" className="text-xs">
            {settings.language.toUpperCase()}
          </Badge>
          {settings.includeGreeting && (
            <Badge variant="outline" className="text-xs bg-blue-50 text-blue-700 dark:bg-blue-900/20 dark:text-blue-400">
              Selamlama ✓
            </Badge>
          )}
          {settings.includeSignature && (
            <Badge variant="outline" className="text-xs bg-purple-50 text-purple-700 dark:bg-purple-900/20 dark:text-purple-400">
              İmza ✓
            </Badge>
          )}
        </div>

        {/* Message preview with animation */}
        <motion.div
          key={exampleMessage} // Re-animate on change
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3 }}
          className={cn(
            "rounded-lg p-4 whitespace-pre-wrap text-sm leading-relaxed",
            "bg-gradient-to-br from-muted/50 to-muted/30 border"
          )}
        >
          {exampleMessage}
        </motion.div>

        {/* Character limit progress bar */}
        <div className="space-y-1.5">
          <div className="flex items-center justify-between text-xs">
            <span className="text-muted-foreground">Karakter Kullanımı</span>
            <span className={cn(
              "font-medium",
              isOverLimit ? "text-destructive" : usagePercent > 90 ? "text-yellow-600" : "text-muted-foreground"
            )}>
              {usagePercent}%
            </span>
          </div>
          <div className="relative w-full h-2 bg-muted rounded-full overflow-hidden">
            <motion.div
              className={cn(
                "absolute inset-y-0 left-0 rounded-full transition-colors",
                isOverLimit
                  ? "bg-destructive"
                  : usagePercent > 90
                    ? "bg-yellow-500"
                    : "bg-primary"
              )}
              initial={{ width: 0 }}
              animate={{ width: `${Math.min(usagePercent, 100)}%` }}
              transition={{ duration: 0.4, ease: "easeOut" }}
            />
          </div>
        </div>

        {/* Warning */}
        {isOverLimit && (
          <motion.p
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="text-xs text-destructive bg-destructive/10 p-2 rounded"
          >
            ⚠️ Mesaj maksimum uzunluğu aşıyor
          </motion.p>
        )}
      </CardContent>
    </Card>
  );
}
