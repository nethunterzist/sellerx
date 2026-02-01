export interface Supplier {
  id: number;
  storeId: string;
  name: string;
  contactPerson?: string;
  email?: string;
  phone?: string;
  address?: string;
  country?: string;
  currency: string;
  paymentTermsDays?: number;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateSupplierRequest {
  name: string;
  contactPerson?: string;
  email?: string;
  phone?: string;
  address?: string;
  country?: string;
  currency?: string;
  paymentTermsDays?: number;
  notes?: string;
}

export interface UpdateSupplierRequest {
  name?: string;
  contactPerson?: string;
  email?: string;
  phone?: string;
  address?: string;
  country?: string;
  currency?: string;
  paymentTermsDays?: number;
  notes?: string;
}
