'use client';

import { useState, useRef } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useCreateTicket, useUploadTicketAttachment } from '@/hooks/queries/use-support';
import type { TicketCategory, TicketPriority } from '@/types/support';
import { Paperclip, X } from 'lucide-react';
import { useToast } from '@/hooks/use-toast';

const ticketSchema = z.object({
  subject: z.string().min(5, 'Konu en az 5 karakter olmalı').max(255),
  message: z.string().min(10, 'Mesaj en az 10 karakter olmalı'),
  category: z.enum(['TECHNICAL', 'BILLING', 'ORDER', 'PRODUCT', 'INTEGRATION', 'OTHER']),
  priority: z.enum(['LOW', 'MEDIUM', 'HIGH', 'URGENT']).optional(),
});

type TicketFormData = z.infer<typeof ticketSchema>;

interface TicketFormModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const categories: { value: TicketCategory; label: string }[] = [
  { value: 'TECHNICAL', label: 'Teknik Sorun' },
  { value: 'BILLING', label: 'Faturalama' },
  { value: 'ORDER', label: 'Sipariş' },
  { value: 'PRODUCT', label: 'Ürün' },
  { value: 'INTEGRATION', label: 'Entegrasyon' },
  { value: 'OTHER', label: 'Diğer' },
];

const priorities: { value: TicketPriority; label: string }[] = [
  { value: 'LOW', label: 'Düşük' },
  { value: 'MEDIUM', label: 'Normal' },
  { value: 'HIGH', label: 'Yüksek' },
  { value: 'URGENT', label: 'Acil' },
];

const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
const ALLOWED_FILE_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'application/pdf'];

export function TicketFormModal({ open, onOpenChange }: TicketFormModalProps) {
  const { toast } = useToast();
  const createTicket = useCreateTicket();
  const uploadAttachment = useUploadTicketAttachment();
  const [selectedCategory, setSelectedCategory] = useState<TicketCategory>('OTHER');
  const [selectedPriority, setSelectedPriority] = useState<TicketPriority>('MEDIUM');
  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<TicketFormData>({
    resolver: zodResolver(ticketSchema),
    defaultValues: {
      priority: 'MEDIUM',
      category: 'OTHER',
    },
  });

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []);

    // Validate files
    const invalidFiles = files.filter(file =>
      !ALLOWED_FILE_TYPES.includes(file.type) || file.size > MAX_FILE_SIZE
    );

    if (invalidFiles.length > 0) {
      toast({
        title: "Geçersiz Dosya",
        description: "Sadece JPG, PNG ve PDF dosyaları (max 10MB) yüklenebilir.",
        variant: "destructive",
      });
      return;
    }

    setSelectedFiles(prev => [...prev, ...files]);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const removeFile = (index: number) => {
    setSelectedFiles(prev => prev.filter((_, i) => i !== index));
  };

  const onSubmit = async (data: TicketFormData) => {
    try {
      // Create ticket first
      const ticket = await createTicket.mutateAsync({
        subject: data.subject || "",
        message: data.message || "",
        category: selectedCategory,
        priority: selectedPriority,
      });

      // Upload attachments if any
      if (selectedFiles.length > 0) {
        for (const file of selectedFiles) {
          try {
            await uploadAttachment.mutateAsync({ ticketId: ticket.id, file });
          } catch (error) {
            console.error('Failed to upload file:', file.name, error);
            // Continue uploading other files even if one fails
          }
        }
      }

      reset();
      setSelectedFiles([]);
      onOpenChange(false);
    } catch (error) {
      console.error('Ticket creation failed:', error);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[750px] max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Yeni Destek Talebi</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="subject">Konu</Label>
            <Input
              id="subject"
              {...register('subject')}
              placeholder="Sorununuzu kısaca özetleyin"
            />
            {errors.subject && (
              <p className="text-sm text-destructive">{errors.subject.message}</p>
            )}
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>Kategori</Label>
              <Select value={selectedCategory} onValueChange={(v) => setSelectedCategory(v as TicketCategory)}>
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="Kategori seçin" />
                </SelectTrigger>
                <SelectContent>
                  {categories.map((cat) => (
                    <SelectItem key={cat.value} value={cat.value}>
                      {cat.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label>Öncelik</Label>
              <Select value={selectedPriority} onValueChange={(v) => setSelectedPriority(v as TicketPriority)}>
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="Öncelik seçin" />
                </SelectTrigger>
                <SelectContent>
                  {priorities.map((pri) => (
                    <SelectItem key={pri.value} value={pri.value}>
                      {pri.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="message">Mesaj</Label>
            <Textarea
              id="message"
              {...register('message')}
              placeholder="Sorununuzu detaylı olarak açıklayın..."
              rows={5}
            />
            {errors.message && (
              <p className="text-sm text-destructive">{errors.message.message}</p>
            )}
          </div>

          {/* File Upload */}
          <div className="space-y-2">
            <Label>Dosya Ekle (Opsiyonel)</Label>
            <div className="flex items-center gap-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => fileInputRef.current?.click()}
              >
                <Paperclip className="h-4 w-4 mr-2" />
                Dosya Seç
              </Button>
              <span className="text-xs text-muted-foreground">
                JPG, PNG, PDF (max 10MB)
              </span>
            </div>
            <input
              ref={fileInputRef}
              type="file"
              accept=".jpg,.jpeg,.png,.pdf"
              multiple
              onChange={handleFileSelect}
              className="hidden"
            />

            {/* Selected Files */}
            {selectedFiles.length > 0 && (
              <div className="space-y-2 mt-2">
                {selectedFiles.map((file, index) => (
                  <div
                    key={index}
                    className="flex items-center justify-between bg-muted p-2 rounded-md"
                  >
                    <div className="flex items-center gap-2 overflow-hidden">
                      <Paperclip className="h-4 w-4 text-muted-foreground flex-shrink-0" />
                      <span className="text-sm truncate">{file.name}</span>
                      <span className="text-xs text-muted-foreground flex-shrink-0">
                        ({(file.size / 1024 / 1024).toFixed(2)} MB)
                      </span>
                    </div>
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() => removeFile(index)}
                    >
                      <X className="h-4 w-4" />
                    </Button>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="flex justify-end gap-3">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
            >
              İptal
            </Button>
            <Button type="submit" disabled={createTicket.isPending}>
              {createTicket.isPending ? 'Gönderiliyor...' : 'Gönder'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
