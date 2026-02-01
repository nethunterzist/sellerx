'use client';

import { CreditCard, MoreVertical, Star, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import type { PaymentMethod } from '@/types/billing';

interface PaymentMethodCardProps {
  paymentMethod: PaymentMethod;
  onSetDefault?: (id: string) => void;
  onDelete?: (id: string) => void;
  isLoading?: boolean;
}

function getCardBrandIcon(brand: string): string {
  const icons: Record<string, string> = {
    VISA: '💳',
    MASTER_CARD: '💳',
    MASTERCARD: '💳',
    AMEX: '💳',
    TROY: '💳',
  };
  return icons[brand?.toUpperCase()] || '💳';
}

function getCardBrandName(brand: string): string {
  const names: Record<string, string> = {
    VISA: 'Visa',
    MASTER_CARD: 'Mastercard',
    MASTERCARD: 'Mastercard',
    AMEX: 'American Express',
    TROY: 'Troy',
  };
  return names[brand?.toUpperCase()] || brand || 'Kart';
}

export function PaymentMethodCard({
  paymentMethod,
  onSetDefault,
  onDelete,
  isLoading = false,
}: PaymentMethodCardProps) {
  const isExpired = () => {
    const now = new Date();
    const expDate = new Date(paymentMethod.cardExpYear, paymentMethod.cardExpMonth - 1);
    return expDate < now;
  };

  return (
    <Card className={`${paymentMethod.isDefault ? 'border-primary' : ''}`}>
      <CardContent className="flex items-center justify-between p-4">
        <div className="flex items-center gap-4">
          <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-muted text-2xl">
            {getCardBrandIcon(paymentMethod.cardBrand)}
          </div>

          <div>
            <div className="flex items-center gap-2">
              <p className="font-medium">
                {getCardBrandName(paymentMethod.cardBrand)} •••• {paymentMethod.cardLastFour}
              </p>
              {paymentMethod.isDefault && (
                <Badge variant="secondary" className="text-xs">
                  <Star className="mr-1 h-3 w-3" />
                  Varsayılan
                </Badge>
              )}
              {isExpired() && (
                <Badge variant="destructive" className="text-xs">
                  Süresi Dolmuş
                </Badge>
              )}
            </div>

            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <span>
                {paymentMethod.cardExpMonth.toString().padStart(2, '0')}/{paymentMethod.cardExpYear}
              </span>
              {paymentMethod.cardBankName && (
                <>
                  <span>•</span>
                  <span>{paymentMethod.cardBankName}</span>
                </>
              )}
            </div>

            {paymentMethod.cardAlias && (
              <p className="text-xs text-muted-foreground">{paymentMethod.cardAlias}</p>
            )}
          </div>
        </div>

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon" disabled={isLoading}>
              <MoreVertical className="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            {!paymentMethod.isDefault && onSetDefault && (
              <DropdownMenuItem onClick={() => onSetDefault(paymentMethod.id)}>
                <Star className="mr-2 h-4 w-4" />
                Varsayılan Yap
              </DropdownMenuItem>
            )}
            {onDelete && (
              <DropdownMenuItem
                className="text-destructive"
                onClick={() => onDelete(paymentMethod.id)}
              >
                <Trash2 className="mr-2 h-4 w-4" />
                Sil
              </DropdownMenuItem>
            )}
          </DropdownMenuContent>
        </DropdownMenu>
      </CardContent>
    </Card>
  );
}
