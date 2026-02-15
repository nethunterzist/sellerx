/**
 * SellerX login helper for verification tests.
 */
import type { Page } from "@playwright/test";

const TEST_USER = {
  email: "test@test.com",
  password: "123456",
};

export async function loginToSellerX(page: Page): Promise<void> {
  await page.goto("/tr/sign-in");
  await page.waitForLoadState("domcontentloaded");

  await page.getByLabel("E-posta").fill(TEST_USER.email);
  await page.getByLabel("Şifre").fill(TEST_USER.password);
  await page.getByRole("button", { name: "Giriş Yap" }).click();

  await page.waitForURL(/\/dashboard/, { timeout: 30000 });
}

export function getTestUserEmail(): string {
  return TEST_USER.email;
}
