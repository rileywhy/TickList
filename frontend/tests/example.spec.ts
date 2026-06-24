import { test, expect } from "@playwright/test";

test("login page loads", async ({ page }) => {
  await page.goto("http://localhost:5173/login");

  await expect(page.locator("body")).toContainText(/login/i);
});