
import { Page } from '@playwright/test';

export async function registerUser(page:Page) {
    await page.goto("http://localhost:5173/register");
    const id = `${Date.now()}-${Math.random().toString(36).slice(2)}`;
  await page.getByPlaceholder("Email").fill(`johnsmith${id}@test.com`);
  await page.getByPlaceholder("First Name").fill("john");
  await page.getByPlaceholder("Last Name").fill("smith");
  await page.getByPlaceholder("Password").fill("password");

  await page.getByRole("button", ).click();
}