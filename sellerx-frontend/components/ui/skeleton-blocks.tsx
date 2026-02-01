import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

export function StatCardSkeleton() {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <Skeleton className="h-4 w-24" />
        <Skeleton className="h-4 w-4 rounded" />
      </CardHeader>
      <CardContent>
        <Skeleton className="h-7 w-28" />
        <Skeleton className="h-3 w-20 mt-2" />
      </CardContent>
    </Card>
  );
}

export function TableSkeleton({
  columns = 5,
  rows = 5,
  showImage = false,
}: {
  columns?: number;
  rows?: number;
  showImage?: boolean;
}) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          {Array.from({ length: columns }).map((_, i) => (
            <TableHead key={i}>
              <Skeleton className="h-4 w-20" />
            </TableHead>
          ))}
        </TableRow>
      </TableHeader>
      <TableBody>
        {Array.from({ length: rows }).map((_, r) => (
          <TableRow key={r}>
            {Array.from({ length: columns }).map((_, c) => (
              <TableCell key={c}>
                {showImage && c === 0 ? (
                  <div className="flex items-center gap-3">
                    <Skeleton className="h-10 w-10 rounded" />
                    <Skeleton className="h-4 w-28" />
                  </div>
                ) : (
                  <Skeleton className="h-4 w-16" />
                )}
              </TableCell>
            ))}
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

export function ChartSkeleton({ height = "h-64" }: { height?: string }) {
  return (
    <Card>
      <CardHeader>
        <Skeleton className="h-5 w-32" />
      </CardHeader>
      <CardContent>
        <Skeleton className={`${height} w-full rounded-lg`} />
      </CardContent>
    </Card>
  );
}

export function FilterBarSkeleton({
  showSearch = true,
  buttonCount = 1,
}: {
  showSearch?: boolean;
  buttonCount?: number;
}) {
  return (
    <div className="flex items-center gap-3 flex-wrap">
      {showSearch && <Skeleton className="h-9 w-64 rounded-md" />}
      <Skeleton className="h-9 w-36 rounded-md" />
      {Array.from({ length: buttonCount }).map((_, i) => (
        <Skeleton key={i} className="h-9 w-24 rounded-md" />
      ))}
    </div>
  );
}

export function PaginationSkeleton() {
  return (
    <div className="flex items-center justify-between pt-4">
      <Skeleton className="h-4 w-32" />
      <div className="flex items-center gap-2">
        <Skeleton className="h-9 w-9 rounded-md" />
        <Skeleton className="h-4 w-20" />
        <Skeleton className="h-9 w-9 rounded-md" />
      </div>
    </div>
  );
}

export function TabsSkeleton({ tabCount = 3 }: { tabCount?: number }) {
  return (
    <div className="flex items-center gap-1 border-b pb-2">
      {Array.from({ length: tabCount }).map((_, i) => (
        <Skeleton key={i} className="h-9 w-28 rounded-md" />
      ))}
    </div>
  );
}

export function ListItemSkeleton({ count = 5 }: { count?: number }) {
  return (
    <div className="space-y-3">
      {Array.from({ length: count }).map((_, i) => (
        <div key={i} className="flex items-center gap-3 p-3">
          <Skeleton className="h-10 w-10 rounded-full" />
          <div className="flex-1 space-y-2">
            <Skeleton className="h-4 w-48" />
            <Skeleton className="h-3 w-64" />
          </div>
          <Skeleton className="h-3 w-16" />
        </div>
      ))}
    </div>
  );
}
