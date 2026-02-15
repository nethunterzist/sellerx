"use client";

import { useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import {
  useEmailTemplate,
  useUpdateEmailTemplate,
  usePreviewEmail,
  useSendTestEmail,
  useEmailVariables,
} from "@/hooks/queries/use-admin-email-templates";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import {
  ArrowLeft,
  Mail,
  Save,
  Eye,
  Send,
  Variable,
  Copy,
  CheckCircle2,
  Loader2,
  Info,
} from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { toast } from "sonner";

export default function AdminEmailTemplateEditPage() {
  const params = useParams();
  const router = useRouter();
  const type = params.type as string;

  const { data: template, isLoading } = useEmailTemplate(type);
  const { data: variablesData } = useEmailVariables();
  const updateMutation = useUpdateEmailTemplate();
  const previewMutation = usePreviewEmail();
  const testEmailMutation = useSendTestEmail();

  const [subjectTemplate, setSubjectTemplate] = useState("");
  const [bodyTemplate, setBodyTemplate] = useState("");
  const [description, setDescription] = useState("");
  const [isActive, setIsActive] = useState(true);
  const [testEmail, setTestEmail] = useState("");
  const [previewHtml, setPreviewHtml] = useState<string | null>(null);
  const [previewSubject, setPreviewSubject] = useState<string | null>(null);
  const [showPreview, setShowPreview] = useState(false);
  const [showTestDialog, setShowTestDialog] = useState(false);
  const [copiedVar, setCopiedVar] = useState<string | null>(null);

  // Initialize form when template loads
  useEffect(() => {
    if (template) {
      setSubjectTemplate(template.subjectTemplate);
      setBodyTemplate(template.bodyTemplate);
      setDescription(template.description || "");
      setIsActive(template.isActive);
    }
  }, [template]);

  const variables = variablesData?.variablesByType?.[type] || [];

  const handleCopyVariable = (varName: string) => {
    navigator.clipboard.writeText(`{{${varName}}}`);
    setCopiedVar(varName);
    setTimeout(() => setCopiedVar(null), 2000);
    toast.success(`{{${varName}}} kopyalandı`);
  };

  const handleSave = async () => {
    try {
      await updateMutation.mutateAsync({
        type,
        data: {
          subjectTemplate,
          bodyTemplate,
          description,
          isActive,
        },
      });
      toast.success("Şablon başarıyla kaydedildi");
    } catch {
      toast.error("Şablon kaydedilemedi");
    }
  };

  const handlePreview = async () => {
    try {
      const result = await previewMutation.mutateAsync({
        type,
        data: {
          subjectTemplate,
          bodyTemplate,
        },
      });
      setPreviewSubject(result.subject);
      setPreviewHtml(result.body);
      setShowPreview(true);
    } catch {
      toast.error("Önizleme oluşturulamadı");
    }
  };

  const handleSendTestEmail = async () => {
    if (!testEmail) {
      toast.error("Lütfen bir email adresi girin");
      return;
    }

    try {
      const result = await testEmailMutation.mutateAsync({
        type,
        data: {
          recipientEmail: testEmail,
        },
      });
      if (result.success) {
        toast.success(result.message);
        setShowTestDialog(false);
        setTestEmail("");
      } else {
        toast.error(result.message);
      }
    } catch {
      toast.error("Test email gönderilemedi");
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-96 w-full" />
      </div>
    );
  }

  if (!template) {
    return (
      <div className="flex flex-col items-center justify-center py-12">
        <p className="text-slate-500">Şablon bulunamadı</p>
        <Button variant="outline" className="mt-4" asChild>
          <Link href="/admin/email-templates">
            <ArrowLeft className="h-4 w-4 mr-2" />
            Geri Dön
          </Link>
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" asChild>
            <Link href="/admin/email-templates">
              <ArrowLeft className="h-5 w-5" />
            </Link>
          </Button>
          <div>
            <h1 className="text-2xl font-bold text-slate-900 dark:text-white">
              {template.name}
            </h1>
            <p className="text-sm text-slate-500 dark:text-slate-400 mt-1 font-mono">
              {template.emailType}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={handlePreview} disabled={previewMutation.isPending}>
            {previewMutation.isPending ? (
              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
            ) : (
              <Eye className="h-4 w-4 mr-2" />
            )}
            Önizle
          </Button>
          <Dialog open={showTestDialog} onOpenChange={setShowTestDialog}>
            <DialogTrigger asChild>
              <Button variant="outline">
                <Send className="h-4 w-4 mr-2" />
                Test Gönder
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Test Email Gönder</DialogTitle>
                <DialogDescription>
                  Bu şablonu belirtilen email adresine test olarak gönder.
                </DialogDescription>
              </DialogHeader>
              <div className="space-y-4 py-4">
                <div className="space-y-2">
                  <Label htmlFor="testEmail">Email Adresi</Label>
                  <Input
                    id="testEmail"
                    type="email"
                    placeholder="test@example.com"
                    value={testEmail}
                    onChange={(e) => setTestEmail(e.target.value)}
                  />
                </div>
                <Button
                  onClick={handleSendTestEmail}
                  disabled={testEmailMutation.isPending}
                  className="w-full"
                >
                  {testEmailMutation.isPending ? (
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  ) : (
                    <Send className="h-4 w-4 mr-2" />
                  )}
                  Gönder
                </Button>
              </div>
            </DialogContent>
          </Dialog>
          <Button onClick={handleSave} disabled={updateMutation.isPending}>
            {updateMutation.isPending ? (
              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
            ) : (
              <Save className="h-4 w-4 mr-2" />
            )}
            Kaydet
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Editor Section */}
        <div className="lg:col-span-2 space-y-6">
          {/* Subject */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Email Konusu</CardTitle>
              <CardDescription>
                Değişkenler kullanılabilir: {"{{"}<code>userName</code>{"}}"}
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Input
                value={subjectTemplate}
                onChange={(e) => setSubjectTemplate(e.target.value)}
                placeholder="Email konusunu girin..."
              />
            </CardContent>
          </Card>

          {/* Body */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Email İçeriği (HTML)</CardTitle>
              <CardDescription>
                HTML formatında email içeriği. Değişkenler kullanılabilir.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Textarea
                value={bodyTemplate}
                onChange={(e) => setBodyTemplate(e.target.value)}
                placeholder="<p>Merhaba {{userName}},</p>..."
                className="min-h-[400px] font-mono text-sm"
              />
            </CardContent>
          </Card>

          {/* Description */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Açıklama</CardTitle>
            </CardHeader>
            <CardContent>
              <Input
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Bu şablonun kullanım amacı..."
              />
            </CardContent>
          </Card>

          {/* Active Toggle */}
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center justify-between">
                <div className="space-y-0.5">
                  <Label htmlFor="active">Aktif Durum</Label>
                  <p className="text-sm text-slate-500">
                    Pasif şablonlar sistem tarafından kullanılmaz
                  </p>
                </div>
                <Switch
                  id="active"
                  checked={isActive}
                  onCheckedChange={setIsActive}
                />
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Variables Panel */}
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2">
                <Variable className="h-4 w-4" />
                Kullanılabilir Değişkenler
              </CardTitle>
              <CardDescription>
                Tıklayarak kopyala
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {variables.length > 0 ? (
                  variables.map((v) => (
                    <div
                      key={v.name}
                      className="flex items-center justify-between p-3 rounded-lg bg-slate-50 dark:bg-slate-800/50 hover:bg-slate-100 dark:hover:bg-slate-800 cursor-pointer transition-colors"
                      onClick={() => handleCopyVariable(v.name)}
                    >
                      <div>
                        <code className="text-sm font-semibold text-blue-600 dark:text-blue-400">
                          {`{{${v.name}}}`}
                        </code>
                        <p className="text-xs text-slate-500 mt-0.5">
                          {v.description}
                        </p>
                        <p className="text-xs text-slate-400 mt-0.5">
                          Örnek: {v.sampleValue}
                        </p>
                      </div>
                      {copiedVar === v.name ? (
                        <CheckCircle2 className="h-4 w-4 text-green-500" />
                      ) : (
                        <Copy className="h-4 w-4 text-slate-400" />
                      )}
                    </div>
                  ))
                ) : (
                  <p className="text-sm text-slate-500 text-center py-4">
                    Bu şablon için değişken tanımlı değil
                  </p>
                )}
              </div>
            </CardContent>
          </Card>

          {/* Info Card */}
          <Card>
            <CardContent className="pt-6">
              <div className="flex gap-3">
                <Info className="h-5 w-5 text-blue-500 flex-shrink-0" />
                <div className="text-sm text-slate-600 dark:text-slate-400">
                  <p className="font-medium text-slate-900 dark:text-white mb-1">
                    Base Layout
                  </p>
                  <p>
                    Tüm emailler otomatik olarak header ve footer ile sarmalanır.
                    Genel ayarlardan değiştirebilirsiniz.
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Preview Modal */}
      <Dialog open={showPreview} onOpenChange={setShowPreview}>
        <DialogContent className="max-w-4xl max-h-[90vh] overflow-hidden flex flex-col">
          <DialogHeader>
            <DialogTitle>Email Önizleme</DialogTitle>
            <DialogDescription>
              {previewSubject && (
                <span className="block mt-2">
                  <strong>Konu:</strong> {previewSubject}
                </span>
              )}
            </DialogDescription>
          </DialogHeader>
          <div className="flex-1 overflow-auto border rounded-lg">
            {previewHtml && (
              <iframe
                srcDoc={previewHtml}
                className="w-full h-[600px] bg-white"
                title="Email Preview"
              />
            )}
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
