'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { CreditCard, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import { useAddPaymentMethod } from '@/hooks/queries/use-billing';

const cardSchema = z.object({
  cardAlias: z.string().optional(),
  cardHolderName: z.string().min(3, 'Kart sahibi adı gerekli'),
  cardNumber: z
    .string()
    .min(16, 'Geçerli bir kart numarası girin')
    .max(19, 'Geçerli bir kart numarası girin')
    .regex(/^[\d\s]+$/, 'Sadece rakam girin'),
  expireMonth: z
    .string()
    .min(2, 'Ay gerekli')
    .max(2, 'Geçerli bir ay girin')
    .regex(/^(0[1-9]|1[0-2])$/, 'Geçerli bir ay girin (01-12)'),
  expireYear: z
    .string()
    .min(2, 'Yıl gerekli')
    .max(2, 'Geçerli bir yıl girin')
    .regex(/^\d{2}$/, 'Geçerli bir yıl girin'),
});

type CardFormValues = z.infer<typeof cardSchema>;

interface AddCardModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

export function AddCardModal({ open, onClose, onSuccess }: AddCardModalProps) {
  const [error, setError] = useState<string | null>(null);
  const addCard = useAddPaymentMethod();

  const form = useForm<CardFormValues>({
    resolver: zodResolver(cardSchema),
    defaultValues: {
      cardAlias: '',
      cardHolderName: '',
      cardNumber: '',
      expireMonth: '',
      expireYear: '',
    },
  });

  const onSubmit = async (values: CardFormValues) => {
    setError(null);

    try {
      await addCard.mutateAsync({
        cardAlias: values.cardAlias || undefined,
        cardHolderName: values.cardHolderName,
        cardNumber: values.cardNumber.replace(/\s/g, ''),
        expireMonth: values.expireMonth,
        expireYear: '20' + values.expireYear,
      });

      form.reset();
      onSuccess?.();
      onClose();
    } catch (err: any) {
      setError(err.message || 'Kart eklenirken bir hata oluştu');
    }
  };

  const formatCardNumber = (value: string) => {
    const v = value.replace(/\s+/g, '').replace(/[^0-9]/gi, '');
    const matches = v.match(/\d{4,16}/g);
    const match = (matches && matches[0]) || '';
    const parts = [];

    for (let i = 0, len = match.length; i < len; i += 4) {
      parts.push(match.substring(i, i + 4));
    }

    if (parts.length) {
      return parts.join(' ');
    } else {
      return value;
    }
  };

  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <CreditCard className="h-5 w-5" />
            Yeni Kart Ekle
          </DialogTitle>
          <DialogDescription>
            Ödeme yöntemi olarak kullanmak üzere kart bilgilerinizi girin.
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="cardAlias"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Kart Adı (Opsiyonel)</FormLabel>
                  <FormControl>
                    <Input placeholder="Örn: İş Kartım" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="cardHolderName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Kart Sahibi Adı</FormLabel>
                  <FormControl>
                    <Input placeholder="Kartın üzerindeki isim" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="cardNumber"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Kart Numarası</FormLabel>
                  <FormControl>
                    <Input
                      placeholder="1234 5678 9012 3456"
                      maxLength={19}
                      {...field}
                      onChange={(e) => {
                        field.onChange(formatCardNumber(e.target.value));
                      }}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="expireMonth"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Ay</FormLabel>
                    <FormControl>
                      <Input placeholder="MM" maxLength={2} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="expireYear"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Yıl</FormLabel>
                    <FormControl>
                      <Input placeholder="YY" maxLength={2} {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            {error && (
              <p className="text-sm text-destructive">{error}</p>
            )}

            <DialogFooter className="gap-2 sm:gap-0">
              <Button type="button" variant="outline" onClick={onClose}>
                İptal
              </Button>
              <Button type="submit" disabled={addCard.isPending}>
                {addCard.isPending && (
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                )}
                Kartı Ekle
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
