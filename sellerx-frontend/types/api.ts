// Common API types for pagination and responses

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // current page (0-indexed)
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface ApiError {
  message: string;
  status: number;
  timestamp?: string;
  path?: string;
}

export interface SyncResponse {
  success: boolean;
  message: string;
  totalFetched: number;
  totalSaved: number;
  totalUpdated: number;
  totalSkipped: number;
}
