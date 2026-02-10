"use client";

import { useEffect, useMemo } from "react";
import { useForm, useWatch } from "react-hook-form";
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
  useStoreExpenseCategories,
  useCreateExpense,
  useUpdateExpense,
} from "@/hooks/queries/use-expenses";
import type { StoreExpense, ExpenseFrequency } from "@/types/expense";
import { frequencyLabels } from "@/types/expense";
import { useCurrency } from "@/lib/contexts/currency-context";

const VAT_OPTIONS = [
  { value: "none", label: "Faturasız (KDV Yok)", rate: null },
  { value: "0", label: "%0 KDV", rate: 0 },
  { value: "1", label: "%1 KDV", rate: 1 },
  { value: "10", label: "%10 KDV", rate: 10 },
  { value: "20", label: "%20 KDV", rate: 20 },
  { value: "custom", label: "Özel Oran", rate: null },
];

const formSchema = z.object({
  name: z.string().min(1, "Gider adı gereklidir"),
  categoryId: z.string().min(1, "Kategori seçiniz"),
  amount: z.coerce.number().min(0.01, "Tutar 0'dan büyük olmalıdır"),
  frequency: z.enum(["DAILY", "WEEKLY", "MONTHLY", "YEARLY", "ONE_TIME"]),
  description: z.string().optional(),
  startDate: z.string().min(1, "Başlangıç tarihi gereklidir"),
  endDate: z.string().optional(),
  vatOption: z.string().default("none"),
  customVatRate: z.coerce.number().min(0).max(100).optional(),
});

type FormValues = z.infer<typeof formSchema>;

interface ExpenseFormModalProps {
  storeId: string;
  expense?: StoreExpense | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

function vatRateToOption(vatRate: number | null | undefined): string {
  if (vatRate === null || vatRate === undefined) return "none";
  if ([0, 1, 10, 20].includes(vatRate)) return String(vatRate);
  return "custom";
}

export function ExpenseFormModal({
  storeId,
  expense,
  open,
  onOpenChange,
}: ExpenseFormModalProps) {
  const isEditing = !!expense;
  const { data: categories, isLoading: categoriesLoading } = useStoreExpenseCategories(storeId);
  const createMutation = useCreateExpense();
  const updateMutation = useUpdateExpense();
  const { formatCurrency } = useCurrency();

  const today = new Date().toISOString().split("T")[0];

  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      name: "",
      categoryId: "",
      amount: 0,
      frequency: "MONTHLY",
      description: "",
      startDate: today,
      endDate: "",
      vatOption: "none",
      customVatRate: 0,
    },
  });

  const watchedAmount = useWatch({ control: form.control, name: "amount" });
  const watchedVatOption = useWatch({ control: form.control, name: "vatOption" });
  const watchedCustomRate = useWatch({ control: form.control, name: "customVatRate" });
  const watchedFrequency = useWatch({ control: form.control, name: "frequency" });

  // Check if this is a recurring expense
  const isRecurring = watchedFrequency !== "ONE_TIME";

  const vatCalculation = useMemo(() => {
    const amount = Number(watchedAmount) || 0;
    let rate: number | null = null;

    if (watchedVatOption === "none") {
      return { rate: null, vatAmount: 0, totalWithVat: amount };
    }

    if (watchedVatOption === "custom") {
      rate = Number(watchedCustomRate) || 0;
    } else {
      rate = Number(watchedVatOption);
    }

    const vatAmount = amount * rate / 100;
    return { rate, vatAmount, totalWithVat: amount + vatAmount };
  }, [watchedAmount, watchedVatOption, watchedCustomRate]);

  // Reset form when modal opens or expense changes
  useEffect(() => {
    if (open) {
      if (expense) {
        const dateStr = expense.date ? expense.date.split("T")[0] : today;
        const endDateStr = expense.endDate ? expense.endDate.split("T")[0] : "";
        const vatOpt = vatRateToOption(expense.vatRate);
        form.reset({
          name: expense.name || "",
          categoryId: expense.expenseCategoryId || "",
          amount: expense.amount,
          frequency: expense.frequency,
          description: "",
          startDate: dateStr,
          endDate: endDateStr,
          vatOption: vatOpt,
          customVatRate: vatOpt === "custom" ? (expense.vatRate ?? 0) : 0,
        });
      } else {
        form.reset({
          name: "",
          categoryId: "",
          amount: 0,
          frequency: "MONTHLY",
          description: "",
          startDate: today,
          endDate: "",
          vatOption: "none",
          customVatRate: 0,
        });
      }
    }
  }, [open, expense, form, today]);

  const onSubmit = (values: FormValues) => {
    // Determine vatRate from form values
    let vatRate: number | null = null;
    if (values.vatOption !== "none") {
      if (values.vatOption === "custom") {
        vatRate = values.customVatRate ?? null;
      } else {
        vatRate = Number(values.vatOption);
      }
    }

    const data = {
      expenseCategoryId: values.categoryId,
      name: values.name,
      amount: values.amount,
      frequency: values.frequency as ExpenseFrequency,
      date: values.startDate ? `${values.startDate}T00:00:00` : undefined,
      endDate: values.endDate ? `${values.endDate}T23:59:59` : null,
      productId: null,
      vatRate,
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
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Gider Adı</FormLabel>
                  <FormControl>
                    <Input
                      placeholder="Örn: Ofis Kirası"
                      className="border border-gray-300 dark:border-gray-600 rounded-lg"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="categoryId"
              render={({ field }) => (
                <FormItem className="w-full">
                  <FormLabel>Kategori</FormLabel>
                  <Select
                    onValueChange={field.onChange}
                    value={field.value || ""}
                    disabled={categoriesLoading}
                  >
                    <FormControl>
                      <SelectTrigger className="w-full border border-gray-300 dark:border-gray-600 rounded-lg">
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
                    <FormLabel>Tutar (KDV Hariç)</FormLabel>
                    <FormControl>
                      <Input
                        type="number"
                        min="0"
                        step="0.01"
                        placeholder="0.00"
                        className="border border-gray-300 dark:border-gray-600 rounded-lg"
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
                  <FormItem className="w-full">
                    <FormLabel>Sıklık</FormLabel>
                    <Select onValueChange={field.onChange} value={field.value}>
                      <FormControl>
                        <SelectTrigger className="w-full border border-gray-300 dark:border-gray-600 rounded-lg">
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

            {/* KDV Bölümü */}
            <div className="space-y-3 rounded-lg border border-gray-200 dark:border-gray-700 p-3 bg-gray-50/50 dark:bg-gray-900/30">
              <div className="grid grid-cols-2 gap-4">
                <FormField
                  control={form.control}
                  name="vatOption"
                  render={({ field }) => (
                    <FormItem className="w-full">
                      <FormLabel>KDV Oranı</FormLabel>
                      <Select onValueChange={field.onChange} value={field.value}>
                        <FormControl>
                          <SelectTrigger className="w-full border border-gray-300 dark:border-gray-600 rounded-lg">
                            <SelectValue placeholder="KDV seçin" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          {VAT_OPTIONS.map((opt) => (
                            <SelectItem key={opt.value} value={opt.value}>
                              {opt.label}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                {watchedVatOption === "custom" && (
                  <FormField
                    control={form.control}
                    name="customVatRate"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel>Özel KDV Oranı (%)</FormLabel>
                        <FormControl>
                          <Input
                            type="number"
                            min="0"
                            max="100"
                            step="1"
                            placeholder="Örn: 8"
                            className="border border-gray-300 dark:border-gray-600 rounded-lg"
                            {...field}
                          />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                )}
              </div>

              {/* KDV Hesaplama Özeti */}
              {watchedVatOption !== "none" && vatCalculation.rate !== null && (
                <div className="flex items-center justify-between text-sm text-muted-foreground pt-1 border-t border-gray-200 dark:border-gray-700">
                  <div className="space-y-0.5">
                    <div>KDV Tutarı (%{vatCalculation.rate}): <span className="font-medium text-foreground">{formatCurrency(vatCalculation.vatAmount)}</span></div>
                    <div>Toplam (KDV Dahil): <span className="font-medium text-foreground">{formatCurrency(vatCalculation.totalWithVat)}</span></div>
                  </div>
                </div>
              )}
            </div>

            <div className={isRecurring ? "grid grid-cols-2 gap-4" : ""}>
              <FormField
                control={form.control}
                name="startDate"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{isRecurring ? "Başlangıç Tarihi" : "Gider Tarihi"}</FormLabel>
                    <FormControl>
                      <Input type="date" className="border border-gray-300 dark:border-gray-600 rounded-lg" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {isRecurring && (
                <FormField
                  control={form.control}
                  name="endDate"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Bitiş Tarihi (Opsiyonel)</FormLabel>
                      <FormControl>
                        <Input type="date" className="border border-gray-300 dark:border-gray-600 rounded-lg" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              )}
            </div>

            {/* Tekrarlayan gider bilgisi */}
            {isRecurring && (
              <div className="flex items-start gap-2 text-xs text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-900/20 px-3 py-2 rounded-md">
                <span className="text-base">🔄</span>
                <span>
                  Bu gider <strong>{frequencyLabels[watchedFrequency as ExpenseFrequency]}</strong> olarak tekrarlanacak.
                  {!form.getValues("endDate") && " Bitiş tarihi belirlenmediği için süresiz tekrarlanır."}
                </span>
              </div>
            )}

            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Açıklama (Opsiyonel)</FormLabel>
                  <FormControl>
                    <Textarea
                      placeholder="Gider hakkında not ekleyin..."
                      className="resize-none border border-gray-300 dark:border-gray-600 rounded-lg"
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
