'use client';

import { useState } from 'react';
import { Plus, LifeBuoy } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { TicketList, TicketFormModal } from '@/components/support';
import { useMyTickets } from '@/hooks/queries/use-support';
import {
  FilterBarSkeleton,
  ListItemSkeleton,
  PaginationSkeleton,
} from '@/components/ui/skeleton-blocks';

function SupportPageSkeleton() {
  return (
    <div className="space-y-6">
      <FilterBarSkeleton showSearch={true} buttonCount={1} />
      <Card>
        <CardContent className="p-6">
          <ListItemSkeleton count={8} />
        </CardContent>
      </Card>
      <PaginationSkeleton />
    </div>
  );
}

export default function SupportPage() {
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [page, setPage] = useState(0);
  const { data, isLoading, error } = useMyTickets(page, 10);

  if (isLoading) return <SupportPageSkeleton />;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold flex items-center gap-2">
            <LifeBuoy className="h-6 w-6" />
            Destek Talepleri
          </h1>
          <p className="text-muted-foreground mt-1">
            Sorularınız ve sorunlarınız için destek talebi oluşturun
          </p>
        </div>
        <Button onClick={() => setShowCreateModal(true)}>
          <Plus className="h-4 w-4 mr-2" />
          Yeni Talep
        </Button>
      </div>

      {/* Ticket List */}
      <Card>
        <CardHeader>
          <CardTitle>Taleplerim</CardTitle>
        </CardHeader>
        <CardContent>
          {error ? (
            <div className="text-center py-8 text-destructive">
              Talepler yüklenirken hata oluştu
            </div>
          ) : data ? (
            <>
              <TicketList tickets={data.content} />

              {/* Pagination */}
              {data.totalPages > 1 && (
                <div className="flex justify-center gap-2 mt-4">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={data.first}
                  >
                    Önceki
                  </Button>
                  <span className="flex items-center px-4 text-sm">
                    Sayfa {data.number + 1} / {data.totalPages}
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setPage((p) => p + 1)}
                    disabled={data.last}
                  >
                    Sonraki
                  </Button>
                </div>
              )}
            </>
          ) : null}
        </CardContent>
      </Card>

      {/* Create Ticket Modal */}
      <TicketFormModal
        open={showCreateModal}
        onOpenChange={setShowCreateModal}
      />
    </div>
  );
}
