import { NextResponse } from "next/server";

// This route requires a store ID - redirect to the appropriate documentation
export async function GET() {
  return NextResponse.json(
    { error: "Store ID is required. Use /api/dashboard/stats/{storeId}" },
    { status: 400 },
  );
}
