import { test, expect, type Page } from '@playwright/test';
import { makeTestUser, registerUser, loginUser } from './helpers/users';

function getTickRow(page: Page, climbName: string) {
  return page.locator('.tick-row').filter({ hasText: climbName });
}

test.beforeEach(async ({ page }) => {
  const user = makeTestUser();

  await registerUser(page, user);
  await loginUser(page, user);

  await page.getByRole('link', { name: 'TickList' }).click();
  await expect(page.locator('form.create-tick-form')).toBeVisible();
});

test('logs a tick', async ({ page }) => {
  const climbName = `Climb ${Date.now()}`;

  const form = page.locator('form.create-tick-form');

  await form.getByPlaceholder('Climb name').fill(climbName);
  await form.getByPlaceholder('Location').fill('Little Cottonwood Canyon');
  await form.getByPlaceholder('Grade').fill('V4');
  await form.locator('select[name="gradeSystem"]').selectOption('V_SCALE');
  await form.locator('select[name="sourceApp"]').selectOption('MANUAL');
  await form.getByPlaceholder('Climb ID').fill(`manual-${Date.now()}`);
  await form.getByPlaceholder('Notes').fill('Playwright logged this tick');

  await Promise.all([
    page.waitForResponse(
      response =>
        response.url().includes('/ticks') &&
        response.request().method() === 'POST'
    ),
    page.waitForResponse(
      response =>
        response.url().includes('/ticks') &&
        response.request().method() === 'GET'
    ),
    form.getByRole('button', { name: 'Log Tick' }).click(),
  ]);

  const card = getTickRow(page, climbName);

  await expect(card).toBeVisible({ timeout: 10000 });
  await expect(card).toContainText('Little Cottonwood Canyon');
  await expect(card).toContainText('V4');
  await expect(card).toContainText('Manual');

  await card.getByRole('button', { name: 'Show Details' }).click();
  await expect(page.getByRole('dialog')).toContainText('Playwright logged this tick');
});

test('filters ticks by search, source, and grade system', async ({ page }) => {
  const firstClimb = `Slab ${Date.now()}`;
  const secondClimb = `Roof ${Date.now() + 1}`;

  const form = page.locator('form.create-tick-form');

  await form.getByPlaceholder('Climb name').fill(firstClimb);
  await form.getByPlaceholder('Location').fill('Joe\'s Valley');
  await form.getByPlaceholder('Grade').fill('V2');
  await form.locator('select[name="gradeSystem"]').selectOption('V_SCALE');
  await form.locator('select[name="sourceApp"]').selectOption('MANUAL');
  await form.getByPlaceholder('Notes').fill('Warmup slab');

  await Promise.all([
    page.waitForResponse(
      response =>
        response.url().includes('/ticks') &&
        response.request().method() === 'POST'
    ),
    page.waitForResponse(
      response =>
        response.url().includes('/ticks') &&
        response.request().method() === 'GET'
    ),
    form.getByRole('button', { name: 'Log Tick' }).click(),
  ]);

  await form.getByPlaceholder('Climb name').fill(secondClimb);
  await form.getByPlaceholder('Location').fill('Rocklands');
  await form.getByPlaceholder('Grade').fill('7A');
  await form.locator('select[name="gradeSystem"]').selectOption('FONT');
  await form.locator('select[name="sourceApp"]').selectOption('KAYA');
  await form.getByPlaceholder('Notes').fill('Powerful roof');

  await Promise.all([
    page.waitForResponse(
      response =>
        response.url().includes('/ticks') &&
        response.request().method() === 'POST'
    ),
    page.waitForResponse(
      response =>
        response.url().includes('/ticks') &&
        response.request().method() === 'GET'
    ),
    form.getByRole('button', { name: 'Log Tick' }).click(),
  ]);

  const filters = page.locator('.filters');

  await filters.getByPlaceholder('Search').fill('roof');
  await expect(getTickRow(page, secondClimb)).toBeVisible();
  await expect(getTickRow(page, firstClimb)).toHaveCount(0);

  await filters.getByPlaceholder('Search').fill('');
  await filters.locator('select').nth(0).selectOption('KAYA');
  await expect(getTickRow(page, secondClimb)).toBeVisible();
  await expect(getTickRow(page, firstClimb)).toHaveCount(0);

  await filters.locator('select').nth(0).selectOption('ALL');
  await filters.locator('select').nth(1).selectOption('FONT');
  await expect(getTickRow(page, secondClimb)).toBeVisible();
  await expect(getTickRow(page, firstClimb)).toHaveCount(0);
});

test('edits a tick', async ({ page }) => {
  const originalClimb = `Edit Me ${Date.now()}`;
  const updatedClimb = `${originalClimb} Updated`;

  const form = page.locator('form.create-tick-form');

  await form.getByPlaceholder('Climb name').fill(originalClimb);
  await form.getByPlaceholder('Location').fill('Smith Rock');
  await form.getByPlaceholder('Grade').fill('5.10a');
  await form.locator('select[name="gradeSystem"]').selectOption('YDS');
  await form.locator('select[name="sourceApp"]').selectOption('MANUAL');
  await form.getByPlaceholder('Notes').fill('This tick will be updated');

  await Promise.all([
    page.waitForResponse(
      response =>
        response.url().includes('/ticks') &&
        response.request().method() === 'POST'
    ),
    page.waitForResponse(
      response =>
        response.url().includes('/ticks') &&
        response.request().method() === 'GET'
    ),
    form.getByRole('button', { name: 'Log Tick' }).click(),
  ]);

  const card = getTickRow(page, originalClimb);

  await expect(card).toBeVisible({ timeout: 10000 });
  await card.getByRole('button', { name: 'Edit tick' }).click();

  const editForm = card.locator('form');

  await editForm.getByPlaceholder('Climb name').fill(updatedClimb);
  await editForm.getByPlaceholder('Location').fill('Ten Sleep');
  await editForm.getByPlaceholder('Grade').fill('5.11b');
  await editForm.locator('select[name="gradeSystem"]').selectOption('YDS');
  await editForm.locator('select[name="sourceApp"]').selectOption('MOUNTAIN_PROJECT');
  await editForm.getByPlaceholder('Notes').fill('This tick was updated by Playwright');

  await Promise.all([
    page.waitForResponse(
      response =>
        response.url().includes('/ticks/') &&
        response.request().method() === 'PUT'
    ),
    page.waitForResponse(
      response =>
        response.url().includes('/ticks') &&
        response.request().method() === 'GET'
    ),
    editForm.getByRole('button', { name: 'Save' }).click(),
  ]);

  const updatedCard = getTickRow(page, updatedClimb);

  await expect(updatedCard).toBeVisible({ timeout: 10000 });
  await expect(updatedCard).toContainText('Ten Sleep');
  await expect(updatedCard).toContainText('5.11b');
  await expect(updatedCard).toContainText('Mountain Project');
});

test('deletes a tick', async ({ page }) => {
  const climbName = `Delete Me ${Date.now()}`;

  const form = page.locator('form.create-tick-form');

  await form.getByPlaceholder('Climb name').fill(climbName);
  await form.getByPlaceholder('Location').fill('Bishop');
  await form.getByPlaceholder('Grade').fill('V3');
  await form.locator('select[name="gradeSystem"]').selectOption('V_SCALE');
  await form.locator('select[name="sourceApp"]').selectOption('MANUAL');
  await form.getByPlaceholder('Notes').fill('This tick should be removed');

  await Promise.all([
    page.waitForResponse(
      response =>
        response.url().includes('/ticks') &&
        response.request().method() === 'POST'
    ),
    page.waitForResponse(
      response =>
        response.url().includes('/ticks') &&
        response.request().method() === 'GET'
    ),
    form.getByRole('button', { name: 'Log Tick' }).click(),
  ]);

  const card = getTickRow(page, climbName);

  await expect(card).toBeVisible({ timeout: 10000 });

  await Promise.all([
    page.waitForResponse(
      response =>
        response.url().includes('/ticks/') &&
        response.request().method() === 'DELETE'
    ),
    page.waitForResponse(
      response =>
        response.url().includes('/ticks') &&
        response.request().method() === 'GET'
    ),
    card.getByRole('button', { name: 'Delete tick' }).click(),
  ]);

  await expect(getTickRow(page, climbName)).toHaveCount(0);
});
