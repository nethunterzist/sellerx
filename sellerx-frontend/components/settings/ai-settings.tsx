"use client";

import { useState } from "react";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useAiSettings, useUpdateAiSettings, useKnowledgeBase, useCreateKnowledge, useUpdateKnowledge, useDeleteKnowledge, useToggleKnowledge } from "@/hooks/queries/use-ai";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Slider } from "@/components/ui/slider";
import { Badge } from "@/components/ui/badge";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle, AlertDialogTrigger } from "@/components/ui/alert-dialog";
import { Loader2, Plus, Pencil, Trash2, Sparkles, BookOpen, Settings2 } from "lucide-react";
import { KNOWLEDGE_CATEGORIES, type KnowledgeBaseItem, type CreateKnowledgeRequest } from "@/types/ai";
import { toast } from "sonner";

export function AiSettings() {
  const { data: selectedStore } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  const { data: settings, isLoading: settingsLoading } = useAiSettings(storeId);
  const { data: knowledgeItems, isLoading: knowledgeLoading } = useKnowledgeBase(storeId);

  const updateSettings = useUpdateAiSettings();
  const createKnowledge = useCreateKnowledge();
  const updateKnowledge = useUpdateKnowledge();
  const deleteKnowledge = useDeleteKnowledge();
  const toggleKnowledge = useToggleKnowledge();

  const [isKnowledgeDialogOpen, setIsKnowledgeDialogOpen] = useState(false);
  const [editingKnowledge, setEditingKnowledge] = useState<KnowledgeBaseItem | null>(null);

  if (!storeId) {
    return (
      <Card>
        <CardContent className="py-12 text-center">
          <p className="text-muted-foreground">Lütfen bir mağaza seçin</p>
        </CardContent>
      </Card>
    );
  }

  if (settingsLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  const handleSettingChange = async (key: string, value: any) => {
    try {
      await updateSettings.mutateAsync({
        storeId,
        data: { [key]: value },
      });
      toast.success("Ayarlar güncellendi");
    } catch {
      toast.error("Ayarlar güncellenirken hata oluştu");
    }
  };

  const handleSaveKnowledge = async (data: CreateKnowledgeRequest) => {
    try {
      if (editingKnowledge) {
        await updateKnowledge.mutateAsync({
          id: editingKnowledge.id,
          data,
          storeId,
        });
        toast.success("Bilgi güncellendi");
      } else {
        await createKnowledge.mutateAsync({ storeId, data });
        toast.success("Bilgi eklendi");
      }
      setIsKnowledgeDialogOpen(false);
      setEditingKnowledge(null);
    } catch {
      toast.error("İşlem başarısız");
    }
  };

  const handleDeleteKnowledge = async (id: string) => {
    try {
      await deleteKnowledge.mutateAsync({ id, storeId });
      toast.success("Bilgi silindi");
    } catch {
      toast.error("Silme işlemi başarısız");
    }
  };

  const handleToggleKnowledge = async (id: string, active: boolean) => {
    try {
      await toggleKnowledge.mutateAsync({ id, active, storeId });
    } catch {
      toast.error("İşlem başarısız");
    }
  };

  // Group knowledge items by category
  const knowledgeByCategory = knowledgeItems?.reduce((acc, item) => {
    if (!acc[item.category]) {
      acc[item.category] = [];
    }
    acc[item.category].push(item);
    return acc;
  }, {} as Record<string, KnowledgeBaseItem[]>) || {};

  return (
    <div className="space-y-6">
      {/* AI Settings Card */}
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Settings2 className="h-5 w-5 text-[#1D70F1]" />
            <CardTitle>AI Cevap Ayarları</CardTitle>
          </div>
          <CardDescription>
            Müşteri sorularına AI destekli cevap önerisi alın
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          {/* Enable AI */}
          <div className="flex items-center justify-between">
            <div className="space-y-0.5">
              <Label>AI Cevap Önerisi</Label>
              <p className="text-sm text-muted-foreground">
                Müşteri sorularına AI destekli cevap önerisi alın
              </p>
            </div>
            <Switch
              checked={settings?.aiEnabled || false}
              onCheckedChange={(checked) => handleSettingChange("aiEnabled", checked)}
            />
          </div>

          {settings?.aiEnabled && (
            <>
              {/* Tone */}
              <div className="space-y-2">
                <Label>Cevap Tonu</Label>
                <Select
                  value={settings?.tone || "professional"}
                  onValueChange={(value) => handleSettingChange("tone", value)}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="professional">Profesyonel</SelectItem>
                    <SelectItem value="friendly">Samimi</SelectItem>
                    <SelectItem value="formal">Resmi</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              {/* Greeting */}
              <div className="flex items-center justify-between">
                <div className="space-y-0.5">
                  <Label>Selamlama Ekle</Label>
                  <p className="text-sm text-muted-foreground">
                    Cevabın başına "Merhaba," ekle
                  </p>
                </div>
                <Switch
                  checked={settings?.includeGreeting || false}
                  onCheckedChange={(checked) => handleSettingChange("includeGreeting", checked)}
                />
              </div>

              {/* Signature */}
              <div className="flex items-center justify-between">
                <div className="space-y-0.5">
                  <Label>İmza Ekle</Label>
                  <p className="text-sm text-muted-foreground">
                    Cevabın sonuna mağaza imzası ekle
                  </p>
                </div>
                <Switch
                  checked={settings?.includeSignature || false}
                  onCheckedChange={(checked) => handleSettingChange("includeSignature", checked)}
                />
              </div>

              {settings?.includeSignature && (
                <div className="space-y-2">
                  <Label>İmza Metni</Label>
                  <Input
                    value={settings?.signatureText || ""}
                    onChange={(e) => handleSettingChange("signatureText", e.target.value)}
                    placeholder="İyi günler dileriz, [Mağaza Adı]"
                  />
                </div>
              )}

              {/* Max Answer Length */}
              <div className="space-y-2">
                <Label>Maksimum Cevap Uzunluğu: {settings?.maxAnswerLength || 500} karakter</Label>
                <Slider
                  value={[settings?.maxAnswerLength || 500]}
                  onValueChange={(value) => handleSettingChange("maxAnswerLength", value[0])}
                  min={200}
                  max={1000}
                  step={50}
                />
              </div>

              {/* Confidence Threshold */}
              <div className="space-y-2">
                <Label>Minimum Güven Skoru: %{Math.round((settings?.confidenceThreshold || 0.8) * 100)}</Label>
                <p className="text-sm text-muted-foreground">
                  Bu skorun altındaki cevaplar için uyarı gösterilir
                </p>
                <Slider
                  value={[settings?.confidenceThreshold || 0.8]}
                  onValueChange={(value) => handleSettingChange("confidenceThreshold", value[0])}
                  min={0.5}
                  max={0.95}
                  step={0.05}
                />
              </div>
            </>
          )}
        </CardContent>
      </Card>

      {/* Knowledge Base Card */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <BookOpen className="h-5 w-5 text-[#1D70F1]" />
              <div>
                <CardTitle>Bilgi Bankası</CardTitle>
                <CardDescription>
                  AI'ın cevap üretirken kullanacağı mağaza bilgileri
                </CardDescription>
              </div>
            </div>
            <Dialog open={isKnowledgeDialogOpen} onOpenChange={(open) => {
              setIsKnowledgeDialogOpen(open);
              if (!open) setEditingKnowledge(null);
            }}>
              <DialogTrigger asChild>
                <Button size="sm">
                  <Plus className="h-4 w-4 mr-1" />
                  Bilgi Ekle
                </Button>
              </DialogTrigger>
              <KnowledgeFormDialog
                knowledge={editingKnowledge}
                onSave={handleSaveKnowledge}
                isLoading={createKnowledge.isPending || updateKnowledge.isPending}
              />
            </Dialog>
          </div>
        </CardHeader>
        <CardContent>
          {knowledgeLoading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
            </div>
          ) : !knowledgeItems?.length ? (
            <div className="text-center py-8 text-muted-foreground">
              <BookOpen className="h-12 w-12 mx-auto mb-4 opacity-50" />
              <p>Henüz bilgi eklenmemiş</p>
              <p className="text-sm mt-1">
                Kargo, iade, ürün bilgileri gibi bilgiler ekleyerek AI'ın daha iyi cevaplar üretmesini sağlayın
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {KNOWLEDGE_CATEGORIES.map((category) => {
                const items = knowledgeByCategory[category.value] || [];
                if (items.length === 0) return null;

                return (
                  <div key={category.value}>
                    <h4 className="text-sm font-medium text-muted-foreground mb-2">
                      {category.label} ({items.length})
                    </h4>
                    <div className="space-y-2">
                      {items.map((item) => (
                        <div
                          key={item.id}
                          className="flex items-start justify-between p-3 rounded-lg border bg-card"
                        >
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-2">
                              <h5 className="font-medium truncate">{item.title}</h5>
                              {!item.isActive && (
                                <Badge variant="secondary" className="text-xs">
                                  Pasif
                                </Badge>
                              )}
                            </div>
                            <p className="text-sm text-muted-foreground line-clamp-2 mt-1">
                              {item.content}
                            </p>
                            {item.keywords?.length > 0 && (
                              <div className="flex flex-wrap gap-1 mt-2">
                                {item.keywords.slice(0, 3).map((keyword) => (
                                  <Badge key={keyword} variant="outline" className="text-xs">
                                    {keyword}
                                  </Badge>
                                ))}
                                {item.keywords.length > 3 && (
                                  <Badge variant="outline" className="text-xs">
                                    +{item.keywords.length - 3}
                                  </Badge>
                                )}
                              </div>
                            )}
                          </div>
                          <div className="flex items-center gap-1 ml-4">
                            <Switch
                              checked={item.isActive}
                              onCheckedChange={(checked) => handleToggleKnowledge(item.id, checked)}
                              className="mr-2"
                            />
                            <Button
                              variant="ghost"
                              size="icon"
                              onClick={() => {
                                setEditingKnowledge(item);
                                setIsKnowledgeDialogOpen(true);
                              }}
                            >
                              <Pencil className="h-4 w-4" />
                            </Button>
                            <AlertDialog>
                              <AlertDialogTrigger asChild>
                                <Button variant="ghost" size="icon">
                                  <Trash2 className="h-4 w-4 text-red-500" />
                                </Button>
                              </AlertDialogTrigger>
                              <AlertDialogContent>
                                <AlertDialogHeader>
                                  <AlertDialogTitle>Bilgiyi Sil</AlertDialogTitle>
                                  <AlertDialogDescription>
                                    "{item.title}" bilgisini silmek istediğinize emin misiniz?
                                  </AlertDialogDescription>
                                </AlertDialogHeader>
                                <AlertDialogFooter>
                                  <AlertDialogCancel>İptal</AlertDialogCancel>
                                  <AlertDialogAction
                                    onClick={() => handleDeleteKnowledge(item.id)}
                                    className="bg-red-500 hover:bg-red-600"
                                  >
                                    Sil
                                  </AlertDialogAction>
                                </AlertDialogFooter>
                              </AlertDialogContent>
                            </AlertDialog>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

// Knowledge Form Dialog
function KnowledgeFormDialog({
  knowledge,
  onSave,
  isLoading,
}: {
  knowledge: KnowledgeBaseItem | null;
  onSave: (data: CreateKnowledgeRequest) => void;
  isLoading: boolean;
}) {
  const [category, setCategory] = useState(knowledge?.category || "general");
  const [title, setTitle] = useState(knowledge?.title || "");
  const [content, setContent] = useState(knowledge?.content || "");
  const [keywords, setKeywords] = useState(knowledge?.keywords?.join(", ") || "");
  const [priority, setPriority] = useState(knowledge?.priority || 0);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSave({
      category,
      title,
      content,
      keywords: keywords.split(",").map((k) => k.trim()).filter(Boolean),
      priority,
    });
  };

  return (
    <DialogContent className="sm:max-w-[500px]">
      <DialogHeader>
        <DialogTitle>
          {knowledge ? "Bilgiyi Düzenle" : "Yeni Bilgi Ekle"}
        </DialogTitle>
        <DialogDescription>
          AI'ın müşteri sorularına cevap verirken kullanacağı bilgileri ekleyin
        </DialogDescription>
      </DialogHeader>
      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="space-y-2">
          <Label>Kategori</Label>
          <Select value={category} onValueChange={setCategory}>
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {KNOWLEDGE_CATEGORIES.map((cat) => (
                <SelectItem key={cat.value} value={cat.value}>
                  {cat.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-2">
          <Label>Başlık</Label>
          <Input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Kargo süreleri"
            required
          />
        </div>
        <div className="space-y-2">
          <Label>İçerik</Label>
          <Textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="Kargo süremiz 2-3 iş günüdür. Hafta sonları gönderim yapılmamaktadır."
            rows={4}
            required
          />
        </div>
        <div className="space-y-2">
          <Label>Anahtar Kelimeler (virgülle ayırın)</Label>
          <Input
            value={keywords}
            onChange={(e) => setKeywords(e.target.value)}
            placeholder="kargo, teslimat, süre, gönderim"
          />
        </div>
        <DialogFooter>
          <Button type="submit" disabled={isLoading}>
            {isLoading && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
            {knowledge ? "Güncelle" : "Ekle"}
          </Button>
        </DialogFooter>
      </form>
    </DialogContent>
  );
}
