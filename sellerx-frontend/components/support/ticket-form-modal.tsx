'use client';

import { useState } from 'react';
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
import { useCreateTicket } from '@/hooks/queries/use-support';
import type { TicketCategory, TicketPriority } from '@/types/support';

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

export function TicketFormModal({ open, onOpenChange }: TicketFormModalProps) {
  const createTicket = useCreateTicket();
  const [selectedCategory, setSelectedCategory] = useState<TicketCategory>('OTHER');
  const [selectedPriority, setSelectedPriority] = useState<TicketPriority>('MEDIUM');

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

  const onSubmit = async (data: TicketFormData) => {
    try {
      await createTicket.mutateAsync({
        subject: data.subject || "",
        message: data.message || "",
        category: selectedCategory,
        priority: selectedPriority,
      });
      reset();
      onOpenChange(false);
    } catch (error) {
      console.error('Ticket creation failed:', error);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
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
                <SelectTrigger>
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
                <SelectTrigger>
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
