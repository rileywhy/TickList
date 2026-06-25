import { test, expect } from "@playwright/test";
import { makeTestUser, registerUser } from "./helpers/users";
test("login", async ({ page }) => {
  const user = makeTestUser();
  await registerUser(page, user);
  await page.goto("http://localhost:5173/login");

  await page.getByPlaceholder("Email").fill(user.email);

  await page.getByPlaceholder("Password").fill(user.password);

  await page.getByRole("button", ).click();

  await page.pause();

  await expect(
  page.locator('.account-menu').getByText(`${user.firstName} ${user.lastName}`)
).toBeVisible();


});