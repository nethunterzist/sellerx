"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Loader2, Receipt } from "lucide-react";
import {
  useExpenseCategories,
  useCreateExpense,
  useUpdateExpense,
} from "@/hooks/queries/use-expenses";
import type { StoreExpense, ExpenseFrequency } from "@/types/expense";
import { frequencyLabels } from "@/types/expense";

const formSchema = z.object({
  categoryId: z.coerce.number().min(1, "Kategori seçiniz"),
  amount: z.coerce.number().min(0.01, "Tutar 0'dan büyük olmalıdır"),
  frequency: z.enum(["DAILY", "WEEKLY", "MONTHLY", "YEARLY", "ONE_TIME"]),
  description: z.string().optional(),
  startDate: z.string().min(1, "Başlangıç tarihi gereklidir"),
  endDate: z.string().optional(),
});

type FormValues = z.infer<typeof formSchema>;

interface ExpenseFormModalProps {
  storeId: string;
  expense?: StoreExpense | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function ExpenseFormModal({
  storeId,
  expense,
  open,
  onOpenChange,
}: ExpenseFormModalProps) {
  const isEditing = !!expense;
  const { data: categories, isLoading: categoriesLoading } = useExpenseCategories();
  const createMutation = useCreateExpense();
  const updateMutation = useUpdateExpense();

  const today = new Date().toISOString().split("T")[0];

  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      categoryId: 0,
      amount: 0,
      frequency: "MONTHLY",
      description: "",
      startDate: today,
      endDate: "",
    },
  });

  // Reset form when modal opens or expense changes
  useEffect(() => {
    if (open) {
      if (expense) {
        form.reset({
          categoryId: expense.category.id,
          amount: expense.amount,
          frequency: expense.frequency,
          description: expense.description || "",
          startDate: expense.startDate,
          endDate: expense.endDate || "",
        });
      } else {
        form.reset({
          categoryId: 0,
          amount: 0,
          frequency: "MONTHLY",
          description: "",
          startDate: today,
          endDate: "",
        });
      }
    }
  }, [open, expense, form, today]);

  const onSubmit = (values: FormValues) => {
    const data = {
      categoryId: values.categoryId,
      amount: values.amount,
      frequency: values.frequency as ExpenseFrequency,
      description: values.description || undefined,
      startDate: values.startDate,
      endDate: values.endDate || undefined,
    };

    if (isEditing && expense) {
      updateMutation.mutate(
        { storeId, expenseId: expense.id, data },
        {
          onSuccess: () => {
            onOpenChange(false);
          },
        }
      );
    } else {
      createMutation.mutate(
        { storeId, data },
        {
          onSuccess: () => {
            onOpenChange(false);
          },
        }
      );
    }
  };

  const isPending = createMutation.isPending || updateMutation.isPending;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Receipt className="h-5 w-5" />
            {isEditing ? "Gideri Düzenle" : "Yeni Gider Ekle"}
          </DialogTitle>
          <DialogDescription>
            {isEditing
              ? "Gider bilgilerini güncelleyin"
              : "Mağazanız için yeni bir gider ekleyin"}
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="categoryId"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Kategori</FormLabel>
                  <Select
                    onValueChange={field.onChange}
                    value={field.value ? String(field.value) : ""}
                    disabled={categoriesLoading}
                  >
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Kategori seçin" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {categories?.map((cat) => (
                        <SelectItem key={cat.id} value={String(cat.id)}>
                          {cat.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="amount"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Tutar (TL)</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        min="0"
                        step="0.01"
                        placeholder="0.00"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="frequency"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Sıklık</FormLabel>
                    <Select onValueChange={field.onChange} value={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="Sıklık seçin" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        {(
                          Object.entries(frequencyLabels) as [
                            ExpenseFrequency,
                            string
                          ][]
                        ).map(([value, label]) => (
                          <SelectItem key={value} value={value}>
                            {label}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <FormField
                control={form.control}
                name="startDate"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Başlangıç Tarihi</FormLabel>
                    <FormControl>
                      <Input type="date" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="endDate"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Bitiş Tarihi (Opsiyonel)</FormLabel>
                    <FormControl>
                      <Input type="date" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Açıklama (Opsiyonel)</FormLabel>
                  <FormControl>
                    <Textarea
                      placeholder="Gider hakkında not ekleyin..."
                      className="resize-none"
                      rows={2}
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="flex justify-end gap-2 pt-4">
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                disabled={isPending}
              >
                İptal
              </Button>
              <Button type="submit" disabled={isPending}>
                {isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                {isPending ? "Kaydediliyor..." : isEditing ? "Güncelle" : "Ekle"}
              </Button>
            </div>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
