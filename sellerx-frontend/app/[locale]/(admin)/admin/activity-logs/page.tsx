"use client";

import { useState } from "react";
import { useAdminActivityLogs } from "@/hooks/queries/use-admin-activity";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import {
  ChevronLeft,
  ChevronRight,
  Search,
  Activity,
  Filter,
  CheckCircle2,
  XCircle,
} from "lucide-react";

export default function AdminActivityLogsPage() {
  const [page, setPage] = useState(0);
  const [emailFilter, setEmailFilter] = useState("");
  const [actionFilter, setActionFilter] = useState("");
  const [appliedEmail, setAppliedEmail] = useState("");
  const [appliedAction, setAppliedAction] = useState("");

  const { data, isLoading } = useAdminActivityLogs({
    email: appliedEmail || undefined,
    action: appliedAction || undefined,
    page,
  });

  const logs = data?.content || [];
  const totalPages = data?.totalPages || 1;
  const totalElements = data?.totalElements || 0;

  const handleApplyFilters = () => {
    setAppliedEmail(emailFilter);
    setAppliedAction(actionFilter);
    setPage(0);
  };

  const handleClearFilters = () => {
    setEmailFilter("");
    setActionFilter("");
    setAppliedEmail("");
    setAppliedAction("");
    setPage(0);
  };

  const getActionBadge = (action: string) => {
    switch (action?.toLowerCase()) {
      case "login":
        return (
          <Badge className="bg-green-900/30 text-green-400 border-green-700/50 hover:bg-green-900/30">
            LOGIN
          </Badge>
        );
      case "failed_login":
        return (
          <Badge className="bg-red-900/30 text-red-400 border-red-700/50 hover:bg-red-900/30">
            FAILED LOGIN
          </Badge>
        );
      case "logout":
        return (
          <Badge className="bg-yellow-900/30 text-yellow-400 border-yellow-700/50 hover:bg-yellow-900/30">
            LOGOUT
          </Badge>
        );
      case "register":
        return (
          <Badge className="bg-blue-900/30 text-blue-400 border-blue-700/50 hover:bg-blue-900/30">
            REGISTER
          </Badge>
        );
      case "password_change":
        return (
          <Badge className="bg-purple-900/30 text-purple-400 border-purple-700/50 hover:bg-purple-900/30">
            PASSWORD CHANGE
          </Badge>
        );
      default:
        return (
          <Badge className="bg-slate-800 text-slate-300 border-slate-700/50 hover:bg-slate-800">
            {action?.toUpperCase() || "UNKNOWN"}
          </Badge>
        );
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">Aktivite Loglari</h1>
          <p className="text-sm text-slate-400 mt-1">
            Kullanici giris/cikis ve sistem aktivitelerini izleyin
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Activity className="h-5 w-5 text-blue-500" />
          <span className="text-lg font-semibold text-white">
            {totalElements}
          </span>
        </div>
      </div>

      {/* Filters */}
      <Card className="bg-slate-900 border-slate-700/50">
        <CardContent className="pt-6">
          <div className="flex flex-col sm:flex-row gap-3">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
              <Input
                placeholder="Email ile filtrele..."
                value={emailFilter}
                onChange={(e) => setEmailFilter(e.target.value)}
                className="pl-10 bg-slate-800 border-slate-700 text-white placeholder:text-slate-500"
                onKeyDown={(e) => {
                  if (e.key === "Enter") handleApplyFilters();
                }}
              />
            </div>
            <Select value={actionFilter} onValueChange={setActionFilter}>
              <SelectTrigger className="w-full sm:w-[200px] bg-slate-800 border-slate-700 text-white">
                <SelectValue placeholder="Aksiyon tipi" />
              </SelectTrigger>
              <SelectContent className="bg-slate-800 border-slate-700">
                <SelectItem value="all">Tumu</SelectItem>
                <SelectItem value="login">Login</SelectItem>
                <SelectItem value="failed_login">Failed Login</SelectItem>
                <SelectItem value="logout">Logout</SelectItem>
                <SelectItem value="register">Register</SelectItem>
                <SelectItem value="password_change">Password Change</SelectItem>
              </SelectContent>
            </Select>
            <Button
              onClick={handleApplyFilters}
              className="bg-blue-600 hover:bg-blue-700 text-white"
            >
              <Filter className="h-4 w-4 mr-2" />
              Uygula
            </Button>
            {(appliedEmail || appliedAction) && (
              <Button
                onClick={handleClearFilters}
                variant="outline"
                className="border-slate-700 text-slate-300 hover:bg-slate-800"
              >
                Temizle
              </Button>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Activity Logs Table */}
      <Card className="bg-slate-900 border-slate-700/50">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg text-white">
            <Activity className="h-5 w-5" />
            Aktivite Kayitlari
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-4">
              {[...Array(8)].map((_, i) => (
                <Skeleton key={i} className="h-14 w-full bg-slate-800" />
              ))}
            </div>
          ) : logs.length === 0 ? (
            <div className="text-center py-12 text-slate-400">
              <Activity className="h-12 w-12 mx-auto mb-4 opacity-50" />
              <p>Aktivite kaydı bulunamadı</p>
            </div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow className="border-slate-700/50 hover:bg-transparent">
                    <TableHead className="text-slate-400">Email</TableHead>
                    <TableHead className="text-slate-400">Aksiyon</TableHead>
                    <TableHead className="text-slate-400">IP Adresi</TableHead>
                    <TableHead className="text-slate-400">Cihaz</TableHead>
                    <TableHead className="text-slate-400">Tarayici</TableHead>
                    <TableHead className="text-slate-400">Durum</TableHead>
                    <TableHead className="text-slate-400">Tarih</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {logs.map((log: any) => (
                    <TableRow
                      key={log.id}
                      className="border-slate-700/50 hover:bg-slate-800/50"
                    >
                      <TableCell className="text-white font-medium">
                        {log.email || log.userEmail || "-"}
                      </TableCell>
                      <TableCell>{getActionBadge(log.action)}</TableCell>
                      <TableCell className="text-slate-300 font-mono text-sm">
                        {log.ipAddress || "-"}
                      </TableCell>
                      <TableCell className="text-slate-300 text-sm">
                        {log.device || "-"}
                      </TableCell>
                      <TableCell className="text-slate-300 text-sm">
                        {log.browser || "-"}
                      </TableCell>
                      <TableCell>
                        {log.success ? (
                          <CheckCircle2 className="h-5 w-5 text-green-500" />
                        ) : (
                          <XCircle className="h-5 w-5 text-red-500" />
                        )}
                      </TableCell>
                      <TableCell className="text-slate-400 text-sm whitespace-nowrap">
                        {log.createdAt
                          ? new Date(log.createdAt).toLocaleString("tr-TR")
                          : "-"}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>

              {/* Pagination */}
              <div className="flex items-center justify-between mt-4">
                <p className="text-sm text-slate-400">
                  Sayfa {page + 1} / {totalPages}
                </p>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setPage(Math.max(0, page - 1))}
                    disabled={page === 0}
                    className="border-slate-700 text-slate-300 hover:bg-slate-800"
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                    disabled={page >= totalPages - 1}
                    className="border-slate-700 text-slate-300 hover:bg-slate-800"
                  >
                    <ChevronRight className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
