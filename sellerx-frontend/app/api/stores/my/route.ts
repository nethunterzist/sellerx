import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import { logger } from "@/lib/logger";

const API_BASE_URL = process.env.API_BASE_URL;

export async function GET() {
  try {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("access_token")?.value;

    if (!accessToken) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const response = await fetch(`${API_BASE_URL}/stores/my`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
        "Content-Type": "application/json",
      },
    });

    if (!response.ok) {
      if (response.status === 401) {
        return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
      }
      throw new Error(`HTTP ${response.status}`);
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    logger.error("GET /stores/my error", { endpoint: "/stores/my", error });
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 },
    );
  }
}
