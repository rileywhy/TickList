import { test, expect } from "@playwright/test";
import { makeTestUser, registerUser, loginUser } from "./helpers/users";

// M9 regression tests: the session must survive a page refresh (token persisted in
// localStorage, rehydrated via GET /current_user), and logout must actually end it.

test("session survives a page refresh", async ({ page }) => {
  const user = makeTestUser();
  await registerUser(page, user);
  await loginUser(page, user);

  await page.goto("/ticks");
  await page.reload();

  // Rehydration passes through a loading state before /current_user resolves, so
  // these assertions must survive a brief "Loading..." render (auto-waiting does).
  await expect(
    page.locator(".account-menu").getByText(`${user.firstName} ${user.lastName}`)
  ).toBeVisible();
  await expect(page).toHaveURL(/\/ticks/);
});

test("logout survives a page refresh", async ({ page }) => {
  const user = makeTestUser();
  await registerUser(page, user);
  await loginUser(page, user);

  await page.locator(".account-menu .account-button").click();
  await page.locator(".account-menu .logout-button").click();
  await page.reload();

  // The stored token must be gone too -- otherwise rehydration resurrects the session.
  await expect(page.locator(".account-menu")).not.toBeVisible();
  await page.goto("/ticks");
  await expect(page).toHaveURL(/\/login/);
});

test("expired session shows a message on the login page", async ({ page }) => {
  // A dead token in storage drives the rehydration path: the server rejects it,
  // the app settles to "expired", and the login page must say why.
  // (The mid-session 401 path through TickPage/UploadPage is not covered here.)
  await page.addInitScript(() => localStorage.setItem("user_token", "garbage"));

  await page.goto("/ticks");

  await expect(page).toHaveURL(/\/login/);
  await expect(page.getByText("Your session expired")).toBeVisible();
});
