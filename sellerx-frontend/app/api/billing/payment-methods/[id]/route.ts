import { NextRequest, NextResponse } from 'next/server';
import { getBackendHeaders } from "@/lib/api/bff-auth";

const API_BASE_URL = process.env.API_BASE_URL;

export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  if (!API_BASE_URL) {
    return NextResponse.json({ error: "Server configuration error" }, { status: 500 });
  }

  try {
    const { id } = await params;
    const headers = await getBackendHeaders(request);

    const response = await fetch(`${API_BASE_URL}/api/billing/payment-methods/${id}`, {
      method: 'DELETE',
      headers,
    });

    if (response.status === 204) {
      return new NextResponse(null, { status: 204 });
    }

    const data = await response.json();

    if (!response.ok) {
      return NextResponse.json(data, { status: response.status });
    }

    return NextResponse.json(data);
  } catch (error) {
    console.error('Error deleting payment method:', error);
    return NextResponse.json(
      { error: 'Failed to delete payment method' },
      { status: 500 }
    );
  }
}
