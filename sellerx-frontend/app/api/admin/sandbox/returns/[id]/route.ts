import { NextRequest, NextResponse } from 'next/server';
import { getBackendHeaders } from "@/lib/api/bff-auth";

const API_BASE_URL = process.env.API_BASE_URL;
const isDev = process.env.NODE_ENV === 'development';

// DELETE /api/admin/sandbox/returns/[id] - Sandbox iadesini sil
export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const headers = await getBackendHeaders(request);

    if (!headers.Authorization) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const { id } = await params;

    const response = await fetch(
      `${API_BASE_URL}/api/admin/sandbox/returns/${id}`,
      {
        method: 'DELETE',
        headers,
      }
    );

    if (!response.ok) {
      if (response.status === 401) {
        return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
      }
      if (response.status === 403) {
        return NextResponse.json({ error: 'Forbidden' }, { status: 403 });
      }
      if (response.status === 404) {
        return NextResponse.json({ error: 'Return not found' }, { status: 404 });
      }
      throw new Error(`HTTP ${response.status}`);
    }

    return new NextResponse(null, { status: 204 });
  } catch (error) {
    if (isDev) console.error('[API] /admin/sandbox/returns/[id] DELETE error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}
