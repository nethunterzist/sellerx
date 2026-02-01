"use client";

import React, { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { AlertCircle, Loader2 } from "lucide-react";
import { useClaimIssueReasons, useRejectClaim } from "@/hooks/queries/use-returns";
import type { TrendyolClaim } from "@/types/returns";

interface RejectClaimModalProps {
  claim: TrendyolClaim;
  storeId: string;
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export function RejectClaimModal({
  claim,
  storeId,
  open,
  onClose,
  onSuccess,
}: RejectClaimModalProps) {
  const [selectedReasonId, setSelectedReasonId] = useState<string>("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState<string | null>(null);

  const { data: issueReasons, isLoading: reasonsLoading } = useClaimIssueReasons();
  const rejectMutation = useRejectClaim();

  const selectedReason = issueReasons?.find(
    (r) => r.id.toString() === selectedReasonId
  );

  const handleSubmit = () => {
    setError(null);

    if (!selectedReasonId) {
      setError("Lutfen bir red nedeni secin.");
      return;
    }

    const reasonId = parseInt(selectedReasonId, 10);
    const claimItemIds = claim.items.map((item) => item.claimItemId);

    rejectMutation.mutate(
      {
        storeId,
        claimId: claim.claimId,
        reasonId,
        claimItemIds,
        description: description.trim() || undefined,
      },
      {
        onSuccess: () => {
          setSelectedReasonId("");
          setDescription("");
          onSuccess();
        },
        onError: (err) => {
          setError(err.message || "Iade red islemi basarisiz oldu.");
        },
      }
    );
  };

  const handleClose = () => {
    setSelectedReasonId("");
    setDescription("");
    setError(null);
    onClose();
  };

  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && handleClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>Iadeyi Reddet</DialogTitle>
          <DialogDescription>
            Iade talebini reddetmek icin bir neden secin ve aciklama yazin.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          {/* Claim Info */}
          <div className="p-3 bg-muted rounded-lg">
            <p className="text-sm font-medium">
              Siparis: {claim.orderNumber || claim.claimId}
            </p>
            <p className="text-xs text-muted-foreground mt-1">
              {claim.totalItemCount} adet urun
            </p>
          </div>

          {/* Reason Selection */}
          <div className="space-y-2">
            <Label htmlFor="reason">Red Nedeni *</Label>
            <Select
              value={selectedReasonId}
              onValueChange={setSelectedReasonId}
              disabled={reasonsLoading}
            >
              <SelectTrigger id="reason">
                <SelectValue placeholder="Bir neden secin..." />
              </SelectTrigger>
              <SelectContent>
                {reasonsLoading ? (
                  <SelectItem value="loading" disabled>
                    Yukluyor...
                  </SelectItem>
                ) : (
                  issueReasons?.map((reason) => (
                    <SelectItem key={reason.id} value={reason.id.toString()}>
                      {reason.name}
                      {reason.requiresFile && " (Dosya Gerekli)"}
                    </SelectItem>
                  ))
                )}
              </SelectContent>
            </Select>
            {selectedReason?.requiresFile && (
              <p className="text-xs text-amber-600 dark:text-amber-400">
                Bu neden icin dosya eki gereklidir. Lutfen Trendyol satici
                panelinden dosya yukleyin.
              </p>
            )}
          </div>

          {/* Description */}
          <div className="space-y-2">
            <Label htmlFor="description">Aciklama (Opsiyonel)</Label>
            <Textarea
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Red sebebiyle ilgili detay yazabilirsiniz..."
              maxLength={500}
              rows={3}
            />
            <p className="text-xs text-muted-foreground text-right">
              {description.length}/500
            </p>
          </div>

          {/* Error Message */}
          {error && (
            <div className="flex items-center gap-2 p-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg">
              <AlertCircle className="h-4 w-4 text-red-600 dark:text-red-400" />
              <p className="text-sm text-red-600 dark:text-red-400">{error}</p>
            </div>
          )}

          {/* API Error */}
          {rejectMutation.isError && (
            <div className="flex items-center gap-2 p-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg">
              <AlertCircle className="h-4 w-4 text-red-600 dark:text-red-400" />
              <p className="text-sm text-red-600 dark:text-red-400">
                {rejectMutation.error.message}
              </p>
            </div>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={handleClose}>
            Iptal
          </Button>
          <Button
            variant="destructive"
            onClick={handleSubmit}
            disabled={!selectedReasonId || rejectMutation.isPending}
          >
            {rejectMutation.isPending ? (
              <>
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                Reddediliyor...
              </>
            ) : (
              "Iadeyi Reddet"
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
