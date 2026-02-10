"use client";

import { useState } from "react";
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
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Settings, Plus, Trash2, Loader2, AlertTriangle, Pencil, Check, X } from "lucide-react";
import {
  useStoreExpenseCategories,
  useCreateExpenseCategory,
  useUpdateExpenseCategory,
  useDeleteExpenseCategory,
} from "@/hooks/queries/use-expenses";
import type { ExpenseCategory } from "@/types/expense";
import { toast } from "sonner";

const formSchema = z.object({
  name: z
    .string()
    .min(1, "Kategori adı gereklidir")
    .max(255, "Kategori adı en fazla 255 karakter olabilir"),
});

type FormValues = z.infer<typeof formSchema>;

interface ExpenseCategoryModalProps {
  storeId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function ExpenseCategoryModal({
  storeId,
  open,
  onOpenChange,
}: ExpenseCategoryModalProps) {
  const [deleteTarget, setDeleteTarget] = useState<ExpenseCategory | null>(null);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editingName, setEditingName] = useState("");

  const { data: categories, isLoading } = useStoreExpenseCategories(storeId);
  const createMutation = useCreateExpenseCategory();
  const updateMutation = useUpdateExpenseCategory();
  const deleteMutation = useDeleteExpenseCategory();

  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      name: "",
    },
  });

  const onSubmit = (values: FormValues) => {
    createMutation.mutate(
      { storeId, name: values.name.trim() },
      {
        onSuccess: () => {
          toast.success("Kategori eklendi");
          form.reset();
        },
        onError: (error) => {
          if (error.message.includes("zaten mevcut")) {
            toast.error("Bu isimde bir kategori zaten mevcut");
          } else {
            toast.error("Kategori eklenirken bir hata oluştu");
          }
        },
      }
    );
  };

  const handleEditClick = (category: ExpenseCategory) => {
    setEditingId(category.id);
    setEditingName(category.name);
  };

  const handleEditCancel = () => {
    setEditingId(null);
    setEditingName("");
  };

  const handleEditSave = (categoryId: string) => {
    const trimmedName = editingName.trim();
    if (!trimmedName) {
      toast.error("Kategori adı boş olamaz");
      return;
    }

    updateMutation.mutate(
      { storeId, categoryId, name: trimmedName },
      {
        onSuccess: () => {
          toast.success("Kategori güncellendi");
          setEditingId(null);
          setEditingName("");
        },
        onError: (error) => {
          if (error.message.includes("zaten mevcut")) {
            toast.error("Bu isimde bir kategori zaten mevcut");
          } else {
            toast.error("Kategori güncellenirken bir hata oluştu");
          }
        },
      }
    );
  };

  const handleDeleteClick = (category: ExpenseCategory) => {
    setDeleteTarget(category);
  };

  const handleDeleteConfirm = () => {
    if (!deleteTarget) return;

    deleteMutation.mutate(
      { storeId, categoryId: deleteTarget.id },
      {
        onSuccess: () => {
          toast.success("Kategori silindi");
          setDeleteTarget(null);
        },
        onError: (error) => {
          if (error.message.includes("silinemez")) {
            toast.error(error.message);
          } else {
            toast.error("Kategori silinirken bir hata oluştu");
          }
          setDeleteTarget(null);
        },
      }
    );
  };

  const isDeleting = deleteMutation.isPending;
  const isUpdating = updateMutation.isPending;

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="sm:max-w-[450px]">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Settings className="h-5 w-5" />
              Gider Kategorileri
            </DialogTitle>
            <DialogDescription>
              Gider kategorilerinizi yönetin. Yeni kategori ekleyin, düzenleyin veya kullanılmayan kategorileri silin.
            </DialogDescription>
          </DialogHeader>

          {/* Add new category form */}
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="flex gap-2">
              <FormField
                control={form.control}
                name="name"
                render={({ field }) => (
                  <FormItem className="flex-1">
                    <FormControl>
                      <Input
                        placeholder="Yeni kategori adı"
                        className="border border-gray-300 dark:border-gray-600 rounded-lg"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <Button
                type="submit"
                disabled={createMutation.isPending}
                className="gap-1"
              >
                {createMutation.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Plus className="h-4 w-4" />
                )}
                Ekle
              </Button>
            </form>
          </Form>

          {/* Category list */}
          <div className="mt-4 space-y-2 max-h-[350px] overflow-y-auto">
            {isLoading ? (
              <div className="flex items-center justify-center py-8">
                <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
              </div>
            ) : categories && categories.length > 0 ? (
              categories.map((category) => (
                <div
                  key={category.id}
                  className="flex items-center justify-between p-3 rounded-lg border border-gray-200 dark:border-gray-700 bg-gray-50/50 dark:bg-gray-900/30"
                >
                  <div className="flex items-center gap-3 flex-1 min-w-0">
                    {editingId === category.id ? (
                      <Input
                        value={editingName}
                        onChange={(e) => setEditingName(e.target.value)}
                        className="h-8 border border-gray-300 dark:border-gray-600 rounded"
                        autoFocus
                        onKeyDown={(e) => {
                          if (e.key === "Enter") {
                            e.preventDefault();
                            handleEditSave(category.id);
                          } else if (e.key === "Escape") {
                            handleEditCancel();
                          }
                        }}
                      />
                    ) : (
                      <>
                        <span className="font-medium truncate">{category.name}</span>
                        {category.expenseCount !== undefined && category.expenseCount > 0 && (
                          <span className="text-xs text-muted-foreground px-2 py-0.5 bg-gray-200 dark:bg-gray-700 rounded-full shrink-0">
                            {category.expenseCount} gider
                          </span>
                        )}
                      </>
                    )}
                  </div>
                  <div className="flex items-center gap-1 shrink-0 ml-2">
                    {editingId === category.id ? (
                      <>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8 text-green-600 hover:text-green-700 hover:bg-green-50"
                          onClick={() => handleEditSave(category.id)}
                          disabled={isUpdating}
                        >
                          {isUpdating ? (
                            <Loader2 className="h-4 w-4 animate-spin" />
                          ) : (
                            <Check className="h-4 w-4" />
                          )}
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8 text-muted-foreground hover:text-foreground"
                          onClick={handleEditCancel}
                          disabled={isUpdating}
                        >
                          <X className="h-4 w-4" />
                        </Button>
                      </>
                    ) : (
                      <>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8 text-muted-foreground hover:text-foreground"
                          onClick={() => handleEditClick(category)}
                          disabled={isDeleting || isUpdating}
                        >
                          <Pencil className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-8 w-8 text-muted-foreground hover:text-destructive"
                          onClick={() => handleDeleteClick(category)}
                          disabled={isDeleting || isUpdating}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </>
                    )}
                  </div>
                </div>
              ))
            ) : (
              <div className="text-center py-8 text-muted-foreground">
                Henüz kategori yok
              </div>
            )}
          </div>
        </DialogContent>
      </Dialog>

      {/* Delete confirmation dialog */}
      <AlertDialog open={!!deleteTarget} onOpenChange={() => setDeleteTarget(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle className="flex items-center gap-2">
              {deleteTarget?.expenseCount && deleteTarget.expenseCount > 0 ? (
                <>
                  <AlertTriangle className="h-5 w-5 text-destructive" />
                  Bu kategori silinemez
                </>
              ) : (
                "Kategoriyi Sil"
              )}
            </AlertDialogTitle>
            <AlertDialogDescription>
              {deleteTarget?.expenseCount && deleteTarget.expenseCount > 0 ? (
                <>
                  <strong>{deleteTarget.name}</strong> kategorisine ait{" "}
                  <strong>{deleteTarget.expenseCount}</strong> adet gider kaydı bulunmaktadır.
                  <br /><br />
                  Önce bu giderleri farklı bir kategoriye taşıyın veya silin.
                </>
              ) : (
                <>
                  <strong>{deleteTarget?.name}</strong> kategorisini silmek istediğinize emin misiniz?
                  Bu işlem geri alınamaz.
                </>
              )}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>İptal</AlertDialogCancel>
            {(!deleteTarget?.expenseCount || deleteTarget.expenseCount === 0) && (
              <AlertDialogAction
                onClick={handleDeleteConfirm}
                disabled={isDeleting}
                className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              >
                {isDeleting ? (
                  <Loader2 className="h-4 w-4 animate-spin mr-2" />
                ) : null}
                Sil
              </AlertDialogAction>
            )}
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
