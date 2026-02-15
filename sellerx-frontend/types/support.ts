// Support Ticket Types

export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'WAITING_CUSTOMER' | 'RESOLVED' | 'CLOSED';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type TicketCategory = 'TECHNICAL' | 'BILLING' | 'ORDER' | 'PRODUCT' | 'INTEGRATION' | 'OTHER';

export interface TicketMessage {
  id: number;
  senderId: number;
  senderName: string;
  senderEmail: string;
  message: string;
  isAdminReply: boolean;
  createdAt: string;
}

export interface TicketAttachment {
  id: number;
  fileName: string;
  fileType: string;
  fileSize: number;
  uploadedAt: string;
}

export interface Ticket {
  id: number;
  ticketNumber: string;
  subject: string;
  status: TicketStatus;
  priority: TicketPriority;
  category: TicketCategory;
  userId: number;
  userName: string;
  userEmail: string;
  storeId?: string;
  storeName?: string;
  assignedToId?: number;
  assignedToName?: string;
  createdAt: string;
  updatedAt: string;
  closedAt?: string;
  messageCount: number;
  messages?: TicketMessage[];
}

export interface CreateTicketRequest {
  subject: string;
  message: string;
  category: TicketCategory;
  priority?: TicketPriority;
  storeId?: string;
}

export interface AddMessageRequest {
  message: string;
}

export interface UpdateTicketStatusRequest {
  status: TicketStatus;
}

export interface AssignTicketRequest {
  adminId: number;
}

export interface TicketStats {
  totalTickets: number;
  openTickets: number;
  inProgressTickets: number;
  waitingCustomerTickets: number;
  resolvedTickets: number;
  closedTickets: number;
}

export interface TicketFilters {
  status?: TicketStatus;
  priority?: TicketPriority;
  category?: TicketCategory;
  page?: number;
  size?: number;
}

// Paginated response
export interface PagedTickets {
  content: Ticket[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
