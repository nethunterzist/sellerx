import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";

const API_BASE_URL = process.env.API_BASE_URL;

// PUT change password
export async function PUT(request: NextRequest) {
  try {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("access_token")?.value;

    if (!accessToken) {
      return NextResponse.json({ error: "Yetkisiz erişim" }, { status: 401 });
    }

    const body = await request.json();

    // Validate request body
    if (!body.currentPassword || !body.newPassword) {
      return NextResponse.json(
        { error: "Mevcut şifre ve yeni şifre gereklidir" },
        { status: 400 },
      );
    }

    if (body.newPassword.length < 6) {
      return NextResponse.json(
        { error: "Yeni şifre en az 6 karakter olmalıdır" },
        { status: 400 },
      );
    }

    if (body.newPassword !== body.confirmPassword) {
      return NextResponse.json(
        { error: "Şifreler eşleşmiyor" },
        { status: 400 },
      );
    }

    const response = await fetch(`${API_BASE_URL}/users/password`, {
      method: "PUT",
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        currentPassword: body.currentPassword,
        newPassword: body.newPassword,
      }),
    });

    if (!response.ok) {
      if (response.status === 401) {
        return NextResponse.json({ error: "Yetkisiz erişim" }, { status: 401 });
      }
      if (response.status === 400) {
        return NextResponse.json(
          { error: "Mevcut şifre yanlış" },
          { status: 400 },
        );
      }
      const errorData = await response.json().catch(() => ({}));
      return NextResponse.json(
        { error: errorData.message || "Şifre değiştirilemedi" },
        { status: response.status },
      );
    }

    return NextResponse.json({
      success: true,
      message: "Şifre başarıyla değiştirildi",
    });
  } catch (error) {
    console.error("[API] /users/password PUT error:", error);
    return NextResponse.json(
      { error: "Sunucu hatası" },
      { status: 500 },
    );
  }
}
