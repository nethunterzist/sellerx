'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { format } from 'date-fns';
import { tr } from 'date-fns/locale';
import {
  LifeBuoy,
  Search,
  Filter,
  ChevronLeft,
  ChevronRight,
  MessageSquare,
  Clock,
  CheckCircle,
  AlertCircle,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  TicketStatusBadge,
  TicketPriorityBadge,
  TicketCategoryBadge,
} from '@/components/support';
import { useAdminTickets, useAdminTicketStats } from '@/hooks/queries/use-support';
import type { TicketStatus, TicketPriority, TicketCategory } from '@/types/support';

export default function AdminSupportPage() {
  const router = useRouter();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<TicketStatus | 'ALL'>('ALL');
  const [priorityFilter, setPriorityFilter] = useState<TicketPriority | 'ALL'>('ALL');
  const [categoryFilter, setCategoryFilter] = useState<TicketCategory | 'ALL'>('ALL');

  const filters = {
    page,
    size: 20,
    status: statusFilter !== 'ALL' ? statusFilter : undefined,
    priority: priorityFilter !== 'ALL' ? priorityFilter : undefined,
    category: categoryFilter !== 'ALL' ? categoryFilter : undefined,
  };

  const { data, isLoading } = useAdminTickets(filters);
  const { data: stats } = useAdminTicketStats();

  const handleRowClick = (ticketId: number) => {
    router.push(`/admin/support/${ticketId}`);
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
  };

  const clearFilters = () => {
    setSearch('');
    setStatusFilter('ALL');
    setPriorityFilter('ALL');
    setCategoryFilter('ALL');
    setPage(0);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold flex items-center gap-2">
          <LifeBuoy className="h-6 w-6" />
          Destek Yönetimi
        </h1>
        <p className="text-muted-foreground mt-1">
          Tüm destek taleplerini görüntüleyin ve yönetin
        </p>
      </div>

      {/* Stats Cards */}
      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-3">
                <div className="p-2 bg-blue-100 rounded-lg">
                  <MessageSquare className="h-5 w-5 text-blue-600" />
                </div>
                <div>
                  <p className="text-2xl font-bold">{stats.totalTickets}</p>
                  <p className="text-xs text-muted-foreground">Toplam Talep</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-3">
                <div className="p-2 bg-yellow-100 rounded-lg">
                  <Clock className="h-5 w-5 text-yellow-600" />
                </div>
                <div>
                  <p className="text-2xl font-bold">{stats.openTickets}</p>
                  <p className="text-xs text-muted-foreground">Açık Talep</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-3">
                <div className="p-2 bg-orange-100 rounded-lg">
                  <AlertCircle className="h-5 w-5 text-orange-600" />
                </div>
                <div>
                  <p className="text-2xl font-bold">{stats.inProgressTickets}</p>
                  <p className="text-xs text-muted-foreground">İşlemde</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-3">
                <div className="p-2 bg-green-100 rounded-lg">
                  <CheckCircle className="h-5 w-5 text-green-600" />
                </div>
                <div>
                  <p className="text-2xl font-bold">{stats.resolvedTickets}</p>
                  <p className="text-xs text-muted-foreground">Çözümlendi</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Filters */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base flex items-center gap-2">
            <Filter className="h-4 w-4" />
            Filtreler
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap gap-4">
            <form onSubmit={handleSearch} className="flex-1 min-w-[200px]">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Talep no, konu veya kullanıcı ara..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="pl-9"
                />
              </div>
            </form>

            <Select
              value={statusFilter}
              onValueChange={(v) => {
                setStatusFilter(v as TicketStatus | 'ALL');
                setPage(0);
              }}
            >
              <SelectTrigger className="w-[150px]">
                <SelectValue placeholder="Durum" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">Tüm Durumlar</SelectItem>
                <SelectItem value="OPEN">Açık</SelectItem>
                <SelectItem value="IN_PROGRESS">İşlemde</SelectItem>
                <SelectItem value="WAITING_CUSTOMER">Müşteri Bekleniyor</SelectItem>
                <SelectItem value="RESOLVED">Çözümlendi</SelectItem>
                <SelectItem value="CLOSED">Kapatıldı</SelectItem>
              </SelectContent>
            </Select>

            <Select
              value={priorityFilter}
              onValueChange={(v) => {
                setPriorityFilter(v as TicketPriority | 'ALL');
                setPage(0);
              }}
            >
              <SelectTrigger className="w-[150px]">
                <SelectValue placeholder="Öncelik" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">Tüm Öncelikler</SelectItem>
                <SelectItem value="LOW">Düşük</SelectItem>
                <SelectItem value="MEDIUM">Normal</SelectItem>
                <SelectItem value="HIGH">Yüksek</SelectItem>
                <SelectItem value="URGENT">Acil</SelectItem>
              </SelectContent>
            </Select>

            <Select
              value={categoryFilter}
              onValueChange={(v) => {
                setCategoryFilter(v as TicketCategory | 'ALL');
                setPage(0);
              }}
            >
              <SelectTrigger className="w-[150px]">
                <SelectValue placeholder="Kategori" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">Tüm Kategoriler</SelectItem>
                <SelectItem value="TECHNICAL">Teknik</SelectItem>
                <SelectItem value="BILLING">Fatura</SelectItem>
                <SelectItem value="ORDER">Sipariş</SelectItem>
                <SelectItem value="PRODUCT">Ürün</SelectItem>
                <SelectItem value="INTEGRATION">Entegrasyon</SelectItem>
                <SelectItem value="OTHER">Diğer</SelectItem>
              </SelectContent>
            </Select>

            <Button variant="outline" onClick={clearFilters}>
              Temizle
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Tickets Table */}
      <Card>
        <CardHeader>
          <CardTitle>Destek Talepleri</CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="text-center py-8 text-muted-foreground">
              Yükleniyor...
            </div>
          ) : data && data.content.length > 0 ? (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Talep No</TableHead>
                    <TableHead>Konu</TableHead>
                    <TableHead>Kullanıcı</TableHead>
                    <TableHead>Kategori</TableHead>
                    <TableHead>Öncelik</TableHead>
                    <TableHead>Durum</TableHead>
                    <TableHead>Tarih</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.content.map((ticket) => (
                    <TableRow
                      key={ticket.id}
                      className="cursor-pointer hover:bg-muted/50"
                      onClick={() => handleRowClick(ticket.id)}
                    >
                      <TableCell className="font-mono text-sm">
                        {ticket.ticketNumber}
                      </TableCell>
                      <TableCell className="max-w-[200px] truncate">
                        {ticket.subject}
                      </TableCell>
                      <TableCell>{ticket.userName}</TableCell>
                      <TableCell>
                        <TicketCategoryBadge category={ticket.category} />
                      </TableCell>
                      <TableCell>
                        <TicketPriorityBadge priority={ticket.priority} />
                      </TableCell>
                      <TableCell>
                        <TicketStatusBadge status={ticket.status} />
                      </TableCell>
                      <TableCell className="text-sm text-muted-foreground">
                        {format(new Date(ticket.createdAt), 'dd MMM yyyy', { locale: tr })}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>

              {/* Pagination */}
              {data.totalPages > 1 && (
                <div className="flex items-center justify-between mt-4">
                  <p className="text-sm text-muted-foreground">
                    Toplam {data.totalElements} talep
                  </p>
                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage((p) => Math.max(0, p - 1))}
                      disabled={data.first}
                    >
                      <ChevronLeft className="h-4 w-4" />
                    </Button>
                    <span className="text-sm">
                      {data.number + 1} / {data.totalPages}
                    </span>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage((p) => p + 1)}
                      disabled={data.last}
                    >
                      <ChevronRight className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              )}
            </>
          ) : (
            <div className="text-center py-8 text-muted-foreground">
              {search || statusFilter !== 'ALL' || priorityFilter !== 'ALL' || categoryFilter !== 'ALL'
                ? 'Filtrelere uygun talep bulunamadı'
                : 'Henüz destek talebi yok'}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
