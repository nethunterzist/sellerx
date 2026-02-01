import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, userEvent } from "../../helpers/test-utils";
import { RegisterForm } from "@/components/auth/register-form";

// Mock the useRegister hook
const mockRegister = vi.fn();
vi.mock("@/hooks/queries/use-auth", () => ({
  useRegister: () => ({
    mutate: mockRegister,
    isPending: false,
  }),
}));

describe("RegisterForm", () => {
  beforeEach(() => {
    mockRegister.mockClear();
  });

  it("should render all form fields", () => {
    render(<RegisterForm />);

    expect(screen.getByLabelText("Ad Soyad")).toBeInTheDocument();
    expect(screen.getByLabelText("E-posta")).toBeInTheDocument();
    expect(screen.getByLabelText("Şifre")).toBeInTheDocument();
    expect(screen.getByLabelText("Şifre Tekrar")).toBeInTheDocument();
  });

  it("should render the submit button", () => {
    render(<RegisterForm />);

    expect(
      screen.getByRole("button", { name: /Hesap Oluştur/i })
    ).toBeInTheDocument();
  });

  it("should render the sign-in link", () => {
    render(<RegisterForm />);

    expect(screen.getByText("Giriş yap")).toBeInTheDocument();
  });

  it("should show error when submitting with empty fields", async () => {
    const user = userEvent.setup();
    render(<RegisterForm />);

    const submitButton = screen.getByRole("button", { name: /Hesap Oluştur/i });
    await user.click(submitButton);

    expect(screen.getByText("Lütfen tüm alanları doldurun")).toBeInTheDocument();
  });

  it("should show error when passwords do not match", async () => {
    const user = userEvent.setup();
    render(<RegisterForm />);

    await user.type(screen.getByLabelText("Ad Soyad"), "Test User");
    await user.type(screen.getByLabelText("E-posta"), "test@test.com");
    await user.type(screen.getByLabelText("Şifre"), "password123");
    await user.type(screen.getByLabelText("Şifre Tekrar"), "different");

    // Check terms checkbox
    const checkbox = screen.getByRole("checkbox");
    await user.click(checkbox);

    const submitButton = screen.getByRole("button", { name: /Hesap Oluştur/i });
    await user.click(submitButton);

    expect(screen.getByText("Şifreler eşleşmiyor")).toBeInTheDocument();
  });

  it("should show error when password is too short", async () => {
    const user = userEvent.setup();
    render(<RegisterForm />);

    await user.type(screen.getByLabelText("Ad Soyad"), "Test User");
    await user.type(screen.getByLabelText("E-posta"), "test@test.com");
    await user.type(screen.getByLabelText("Şifre"), "12345");
    await user.type(screen.getByLabelText("Şifre Tekrar"), "12345");

    const checkbox = screen.getByRole("checkbox");
    await user.click(checkbox);

    const submitButton = screen.getByRole("button", { name: /Hesap Oluştur/i });
    await user.click(submitButton);

    expect(
      screen.getByText("Şifre en az 6 karakter olmalıdır")
    ).toBeInTheDocument();
  });

  it("should show error when terms are not accepted", async () => {
    const user = userEvent.setup();
    render(<RegisterForm />);

    await user.type(screen.getByLabelText("Ad Soyad"), "Test User");
    await user.type(screen.getByLabelText("E-posta"), "test@test.com");
    await user.type(screen.getByLabelText("Şifre"), "password123");
    await user.type(screen.getByLabelText("Şifre Tekrar"), "password123");

    // Do NOT check the terms checkbox
    const submitButton = screen.getByRole("button", { name: /Hesap Oluştur/i });
    await user.click(submitButton);

    expect(
      screen.getByText("Lütfen kullanım koşullarını kabul edin")
    ).toBeInTheDocument();
  });

  it("should call register mutation with valid data", async () => {
    const user = userEvent.setup();
    render(<RegisterForm />);

    await user.type(screen.getByLabelText("Ad Soyad"), "Test User");
    await user.type(screen.getByLabelText("E-posta"), "test@test.com");
    await user.type(screen.getByLabelText("Şifre"), "password123");
    await user.type(screen.getByLabelText("Şifre Tekrar"), "password123");

    const checkbox = screen.getByRole("checkbox");
    await user.click(checkbox);

    const submitButton = screen.getByRole("button", { name: /Hesap Oluştur/i });
    await user.click(submitButton);

    expect(mockRegister).toHaveBeenCalledWith(
      { name: "Test User", email: "test@test.com", password: "password123" },
      expect.objectContaining({
        onSuccess: expect.any(Function),
        onError: expect.any(Function),
      })
    );
  });

  it("should toggle password visibility", async () => {
    const user = userEvent.setup();
    render(<RegisterForm />);

    const passwordInput = screen.getByLabelText("Şifre");
    expect(passwordInput).toHaveAttribute("type", "password");

    // Find the toggle button (within the password field's container)
    const toggleButtons = screen.getAllByRole("button").filter(
      (btn) => !btn.textContent?.includes("Hesap")
    );
    // The first toggle button toggles password visibility
    await user.click(toggleButtons[0]);

    expect(passwordInput).toHaveAttribute("type", "text");
  });
});
