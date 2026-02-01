import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { authApi } from "@/lib/api/auth";

describe("authApi", () => {
  beforeEach(() => {
    globalThis.fetch = vi.fn();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe("login", () => {
    it("should POST credentials to /api/auth/login", async () => {
      const credentials = { email: "test@test.com", password: "123456" };
      const responseData = { user: { id: "1", email: "test@test.com", name: "Test" } };

      (globalThis.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: vi.fn().mockResolvedValue(responseData),
      });

      const result = await authApi.login(credentials);

      expect(globalThis.fetch).toHaveBeenCalledWith("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(credentials),
        credentials: "include",
      });
      expect(result).toEqual(responseData);
    });

    it("should throw error on failed login", async () => {
      (globalThis.fetch as any).mockResolvedValueOnce({
        ok: false,
        json: vi.fn().mockResolvedValue({ message: "Invalid credentials" }),
      });

      await expect(
        authApi.login({ email: "bad@test.com", password: "wrong" })
      ).rejects.toThrow("Invalid credentials");
    });

    it("should throw default Turkish error message when no message in response", async () => {
      (globalThis.fetch as any).mockResolvedValueOnce({
        ok: false,
        json: vi.fn().mockResolvedValue({}),
      });

      await expect(
        authApi.login({ email: "bad@test.com", password: "wrong" })
      ).rejects.toThrow("Giriş başarısız");
    });
  });

  describe("register", () => {
    it("should POST registration data to /api/auth/register", async () => {
      const data = { name: "Test User", email: "new@test.com", password: "password123" };
      const responseData = { user: { id: "2", email: "new@test.com", name: "Test User" } };

      (globalThis.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: vi.fn().mockResolvedValue(responseData),
      });

      const result = await authApi.register(data);

      expect(globalThis.fetch).toHaveBeenCalledWith("/api/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
      });
      expect(result).toEqual(responseData);
    });

    it("should include referralCode when provided", async () => {
      const data = { name: "Test", email: "ref@test.com", password: "pass123", referralCode: "ABC" };

      (globalThis.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: vi.fn().mockResolvedValue({ user: {} }),
      });

      await authApi.register(data);

      expect(globalThis.fetch).toHaveBeenCalledWith(
        "/api/auth/register",
        expect.objectContaining({
          body: JSON.stringify(data),
        })
      );
    });

    it("should throw error on failed registration", async () => {
      (globalThis.fetch as any).mockResolvedValueOnce({
        ok: false,
        json: vi.fn().mockResolvedValue({ message: "Email already exists" }),
      });

      await expect(
        authApi.register({ name: "Test", email: "existing@test.com", password: "pass" })
      ).rejects.toThrow("Email already exists");
    });
  });

  describe("me", () => {
    it("should GET current user from /api/auth/me", async () => {
      const userData = { id: "1", email: "test@test.com", name: "Test User" };

      (globalThis.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: vi.fn().mockResolvedValue(userData),
      });

      const result = await authApi.me();

      expect(globalThis.fetch).toHaveBeenCalledWith("/api/auth/me", {
        credentials: "include",
        cache: "no-cache",
      });
      expect(result).toEqual(userData);
    });

    it("should throw error when not authenticated", async () => {
      (globalThis.fetch as any).mockResolvedValueOnce({
        ok: false,
      });

      await expect(authApi.me()).rejects.toThrow("Kullanıcı bilgileri alınamadı");
    });
  });

  describe("refresh", () => {
    it("should POST to /api/auth/refresh", async () => {
      const tokenData = { accessToken: "new-token" };

      (globalThis.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: vi.fn().mockResolvedValue(tokenData),
      });

      const result = await authApi.refresh();

      expect(globalThis.fetch).toHaveBeenCalledWith("/api/auth/refresh", {
        method: "POST",
        credentials: "include",
      });
      expect(result).toEqual(tokenData);
    });

    it("should throw error when refresh fails", async () => {
      (globalThis.fetch as any).mockResolvedValueOnce({
        ok: false,
      });

      await expect(authApi.refresh()).rejects.toThrow("Oturum yenilenirken hata oluştu");
    });
  });

  describe("logout", () => {
    it("should POST to /api/auth/logout", async () => {
      (globalThis.fetch as any).mockResolvedValueOnce({
        ok: true,
        json: vi.fn().mockResolvedValue({ success: true }),
      });

      const result = await authApi.logout();

      expect(globalThis.fetch).toHaveBeenCalledWith("/api/auth/logout", {
        method: "POST",
        credentials: "include",
      });
      expect(result).toEqual({ success: true });
    });

    it("should throw error when logout fails", async () => {
      (globalThis.fetch as any).mockResolvedValueOnce({
        ok: false,
      });

      await expect(authApi.logout()).rejects.toThrow("Çıkış yapılırken hata oluştu");
    });
  });
});
