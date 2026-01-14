import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";

const API_BASE_URL = process.env.API_BASE_URL;

// GET user preferences
export async function GET() {
  try {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("access_token")?.value;

    if (!accessToken) {
      return NextResponse.json({ error: "Yetkisiz erişim" }, { status: 401 });
    }

    const response = await fetch(`${API_BASE_URL}/users/preferences`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
    });

    if (!response.ok) {
      if (response.status === 401) {
        return NextResponse.json({ error: "Yetkisiz erişim" }, { status: 401 });
      }
      // If preferences don't exist yet, return defaults
      if (response.status === 404) {
        return NextResponse.json({
          language: "tr",
          theme: "light",
          currency: "TRY",
          notifications: {
            email: true,
            push: true,
            orderUpdates: true,
            stockAlerts: true,
            weeklyReport: false,
          },
        });
      }
      throw new Error(`HTTP ${response.status}`);
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error("[API] /users/preferences GET error:", error);
    // Return defaults on error
    return NextResponse.json({
      language: "tr",
      theme: "light",
      currency: "TRY",
      notifications: {
        email: true,
        push: true,
        orderUpdates: true,
        stockAlerts: true,
        weeklyReport: false,
      },
    });
  }
}

// PUT update user preferences
export async function PUT(request: NextRequest) {
  try {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("access_token")?.value;

    if (!accessToken) {
      return NextResponse.json({ error: "Yetkisiz erişim" }, { status: 401 });
    }

    const body = await request.json();

    const response = await fetch(`${API_BASE_URL}/users/preferences`, {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      if (response.status === 401) {
        return NextResponse.json({ error: "Yetkisiz erişim" }, { status: 401 });
      }
      const errorData = await response.json().catch(() => ({}));
      return NextResponse.json(
        { error: errorData.message || "Tercihler kaydedilemedi" },
        { status: response.status },
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error("[API] /users/preferences PUT error:", error);
    return NextResponse.json(
      { error: "Sunucu hatası" },
      { status: 500 },
    );
  }
}
