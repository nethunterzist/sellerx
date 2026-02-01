"use client";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Keyboard, Command } from "lucide-react";

interface ShortcutCategory {
  title: string;
  shortcuts: {
    keys: string[];
    description: string;
  }[];
}

const shortcutCategories: ShortcutCategory[] = [
  {
    title: "Genel",
    shortcuts: [
      { keys: ["Ctrl", "K"], description: "Hızlı arama" },
      { keys: ["Ctrl", "/"], description: "Kısayolları göster" },
      { keys: ["Esc"], description: "Modal/paneli kapat" },
    ],
  },
  {
    title: "Navigasyon",
    shortcuts: [
      { keys: ["G", "D"], description: "Dashboard'a git" },
      { keys: ["G", "P"], description: "Ürünler'e git" },
      { keys: ["G", "O"], description: "Siparişler'e git" },
      { keys: ["G", "S"], description: "Ayarlar'a git" },
    ],
  },
  {
    title: "Ürünler",
    shortcuts: [
      { keys: ["N"], description: "Yeni ürün ekle" },
      { keys: ["S"], description: "Ürünleri senkronize et" },
      { keys: ["F"], description: "Ürünlerde ara" },
    ],
  },
  {
    title: "Siparişler",
    shortcuts: [
      { keys: ["R"], description: "Siparişleri yenile" },
      { keys: ["Enter"], description: "Sipariş detayını aç" },
    ],
  },
  {
    title: "Tablolar",
    shortcuts: [
      { keys: ["↑", "↓"], description: "Satırlar arası gezin" },
      { keys: ["Enter"], description: "Satırı seç/düzenle" },
      { keys: ["Ctrl", "A"], description: "Tümünü seç" },
    ],
  },
];

function KeyBadge({ children }: { children: React.ReactNode }) {
  return (
    <kbd className="inline-flex items-center justify-center min-w-[24px] h-6 px-2 text-xs font-medium text-foreground bg-muted border border-border rounded shadow-sm">
      {children}
    </kbd>
  );
}

export function ShortcutsDialog() {
  return (
    <Dialog>
      <DialogTrigger asChild>
        <Button variant="outline" className="w-full justify-start gap-3">
          <Keyboard className="h-4 w-4" />
          Klavye Kısayollarını Görüntüle
        </Button>
      </DialogTrigger>
      <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Keyboard className="h-5 w-5" />
            Klavye Kısayolları
          </DialogTitle>
          <DialogDescription>
            Uygulamada hızlı gezinmek için bu kısayolları kullanabilirsiniz
          </DialogDescription>
        </DialogHeader>

        <div className="mt-4 space-y-6">
          {shortcutCategories.map((category) => (
            <div key={category.title}>
              <h3 className="text-sm font-semibold text-foreground mb-3">
                {category.title}
              </h3>
              <div className="space-y-2">
                {category.shortcuts.map((shortcut, index) => (
                  <div
                    key={index}
                    className="flex items-center justify-between py-2 px-3 rounded-lg hover:bg-accent"
                  >
                    <span className="text-sm text-muted-foreground">
                      {shortcut.description}
                    </span>
                    <div className="flex items-center gap-1">
                      {shortcut.keys.map((key, keyIndex) => (
                        <span key={keyIndex} className="flex items-center">
                          <KeyBadge>{key}</KeyBadge>
                          {keyIndex < shortcut.keys.length - 1 && (
                            <span className="mx-1 text-muted-foreground text-xs">+</span>
                          )}
                        </span>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>

        <div className="mt-6 p-4 rounded-lg bg-blue-50 border border-blue-200">
          <div className="flex items-start gap-3">
            <Command className="h-5 w-5 text-blue-600 mt-0.5" />
            <div>
              <p className="font-medium text-blue-900">Mac kullanıcıları için</p>
              <p className="text-sm text-blue-700 mt-1">
                Mac'te <KeyBadge>Ctrl</KeyBadge> yerine{" "}
                <KeyBadge>⌘ Cmd</KeyBadge> tuşunu kullanabilirsiniz.
              </p>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
