import { NextRequest, NextResponse } from 'next/server';
import { getBackendHeaders } from "@/lib/api/bff-auth";

const API_BASE_URL = process.env.API_BASE_URL;
const isDev = process.env.NODE_ENV === 'development';

// GET /api/admin/activity-logs - Get activity logs (admin)
export async function GET(request: NextRequest) {
  try {
    const headers = await getBackendHeaders(request);

    if (!headers.Authorization) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const searchParams = request.nextUrl.searchParams;
    const params = new URLSearchParams();

    const email = searchParams.get('email');
    const action = searchParams.get('action');
    const page = searchParams.get('page') || '0';
    const size = searchParams.get('size') || '20';

    if (email) params.set('email', email);
    if (action) params.set('action', action);
    params.set('page', page);
    params.set('size', size);

    const response = await fetch(
      `${API_BASE_URL}/api/admin/activity-logs?${params.toString()}`,
      {
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
      throw new Error(`HTTP ${response.status}`);
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    if (isDev) console.error('[API] /admin/activity-logs error:', error);
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }
}
