
import { expect, Page } from '@playwright/test';

export type TestUser = {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
};

export async function registerUser(page: Page, user: TestUser) {
  await page.goto('/register');

  await page.getByPlaceholder('First name').fill(user.firstName);
  await page.getByPlaceholder('Last name').fill(user.lastName);
  await page.getByPlaceholder('Email').fill(user.email);
  await page.getByPlaceholder('Password').fill(user.password);

  await Promise.all([
    page.waitForResponse(
      response =>
        response.url().includes('/register') &&
        response.request().method() === 'POST' &&
        response.ok()
    ),
    page.getByRole('button', { name: 'Register' }).click(),
  ]);
}

export function makeTestUser() {
  const id = `${Date.now()}-${Math.random().toString(36).slice(2)}`;

  return {
    firstName: 'John',
    lastName: 'Smith',
    email: `john.smith.${id}@test.local`,
    password: 'password',
  };
}

export async function loginUser(page: Page, user: TestUser) {
  await page.goto('/login');
  await page.getByPlaceholder('Email').fill(user.email);
  await page.getByPlaceholder('Password').fill(user.password);

  await Promise.all([
    page.waitForResponse(
      response =>
        response.url().includes('/login') &&
        response.request().method() === 'POST'
    ),
    page.getByRole('button', { name: 'Log In' }).click(),
  ]);

  await expect(
    page.locator('.account-menu').getByText(`${user.firstName} ${user.lastName}`)
  ).toBeVisible();
}
