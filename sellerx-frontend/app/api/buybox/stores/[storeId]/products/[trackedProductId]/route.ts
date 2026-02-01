import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";

const API_BASE_URL = process.env.API_BASE_URL;

export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ storeId: string; trackedProductId: string }> }
) {
  try {
    const { storeId, trackedProductId } = await params;
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("access_token")?.value;

    if (!accessToken) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const response = await fetch(
      `${API_BASE_URL}/buybox/stores/${storeId}/products/${trackedProductId}`,
      {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
      }
    );

    if (!response.ok) {
      if (response.status === 401) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
      }
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.message || `HTTP ${response.status}`);
    }

    return NextResponse.json({ success: true });
  } catch (error: any) {
    console.error("Remove buybox product error:", error.message);
    return NextResponse.json(
      { message: error.message || "Ürün takipten çıkarılamadı" },
      { status: 500 }
    );
  }
}
