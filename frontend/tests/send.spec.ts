import { test, expect, type Page } from '@playwright/test';
import { makeTestUser, registerUser, loginUser } from './helpers/users';

function getSendRow(page: Page, climbName: string) {
  return page.locator('.send-row').filter({ hasText: climbName });
}

test.beforeEach(async ({ page }) => {
  const user = makeTestUser();

  await registerUser(page, user);
  await loginUser(page, user);

  await page.getByRole('link', { name: 'Sends' }).click();
  await expect(page.locator('form.create-send-form')).toBeVisible();
});

test('logs a send', async ({ page }) => {
  const climbName = `Climb ${Date.now()}`;

  const form = page.locator('form.create-send-form');

  await form.getByPlaceholder('Climb name').fill(climbName);
  await form.getByPlaceholder('Area').fill('Little Cottonwood Canyon');
  await form.getByPlaceholder('Grade').fill('V4');
  await form.locator('select[name="gradeSystem"]').selectOption('V_SCALE');
  await form.locator('select[name="sourceApp"]').selectOption('MANUAL');
  await form.getByPlaceholder('Climb ID').fill(`manual-${Date.now()}`);
  await form.getByPlaceholder('Notes').fill('Playwright logged this send');

  await Promise.all([
    page.waitForResponse(
      response =>
        response.url().includes('/sends') &&
        response.request().method() === 'POST'
    ),
    page.waitForResponse(
      response =>
        response.url().includes('/sends') &&
        response.request().method() === 'GET'
    ),
    form.getByRole('button', { name: 'Log Send' }).click(),
  ]);

  const card = getSendRow(page, climbName);

  await expect(card).toBeVisible({ timeout: 10000 });
  await expect(card).toContainText('Little Cottonwood Canyon');
  await expect(card).toContainText('V4');
  await expect(card).toContainText('Manual');

  await card.getByRole('button', { name: 'Show Details' }).click();
  await expect(page.getByRole('dialog')).toContainText('Playwright logged this send');
});

test('filters sends by search, source, and grade system', async ({ page }) => {
  const firstClimb = `Slab ${Date.now()}`;
  const secondClimb = `Roof ${Date.now() + 1}`;

  const form = page.locator('form.create-send-form');

  await form.getByPlaceholder('Climb name').fill(firstClimb);
  await form.getByPlaceholder('Area').fill('Joe\'s Valley');
  await form.getByPlaceholder('Grade').fill('V2');
  await form.locator('select[name="gradeSystem"]').selectOption('V_SCALE');
  await form.locator('select[name="sourceApp"]').selectOption('MANUAL');
  await form.getByPlaceholder('Notes').fill('Warmup slab');

  await Promise.all([
    page.waitForResponse(
      response =>
        response.url().includes('/sends') &&
        response.request().method() === 'POST'
    ),
    page.waitForResponse(
      response =>
        response.url().includes('/sends') &&
        response.request().method() === 'GET'
    ),
    form.getByRole('button', { name: 'Log Send' }).click(),
  ]);

  await form.getByPlaceholder('Climb name').fill(secondClimb);
  await form.getByPlaceholder('Area').fill('Rocklands');
  await form.getByPlaceholder('Grade').fill('7A');
  await form.locator('select[name="gradeSystem"]').selectOption('FONT');
  await form.locator('select[name="sourceApp"]').selectOption('KAYA');
  await form.getByPlaceholder('Notes').fill('Powerful roof');

  await Promise.all([
    page.waitForResponse(
      response =>
        response.url().includes('/sends') &&
        response.request().method() === 'POST'
    ),
    page.waitForResponse(
      response =>
        response.url().includes('/sends') &&
        response.request().method() === 'GET'
    ),
    form.getByRole('button', { name: 'Log Send' }).click(),
  ]);

  const filters = page.locator('.filters');

  await filters.getByPlaceholder('Search').fill('roof');
  await expect(getSendRow(page, secondClimb)).toBeVisible();
  await expect(getSendRow(page, firstClimb)).toHaveCount(0);

  await filters.getByPlaceholder('Search').fill('');
  await filters.locator('select').nth(0).selectOption('KAYA');
  await expect(getSendRow(page, secondClimb)).toBeVisible();
  await expect(getSendRow(page, firstClimb)).toHaveCount(0);

  await filters.locator('select').nth(0).selectOption('ALL');
  await filters.locator('select').nth(1).selectOption('FONT');
  await expect(getSendRow(page, secondClimb)).toBeVisible();
  await expect(getSendRow(page, firstClimb)).toHaveCount(0);
});

test('edits a send', async ({ page }) => {
  const originalClimb = `Edit Me ${Date.now()}`;
  const updatedClimb = `${originalClimb} Updated`;

  const form = page.locator('form.create-send-form');

  await form.getByPlaceholder('Climb name').fill(originalClimb);
  await form.getByPlaceholder('Area').fill('Smith Rock');
  await form.getByPlaceholder('Grade').fill('5.10a');
  await form.locator('select[name="gradeSystem"]').selectOption('YDS');
  await form.locator('select[name="sourceApp"]').selectOption('MANUAL');
  await form.getByPlaceholder('Notes').fill('This send will be updated');

  await Promise.all([
    page.waitForResponse(
      response =>
        response.url().includes('/sends') &&
        response.request().method() === 'POST'
    ),
    page.waitForResponse(
      response =>
        response.url().includes('/sends') &&
        response.request().method() === 'GET'
    ),
    form.getByRole('button', { name: 'Log Send' }).click(),
  ]);

  const card = getSendRow(page, originalClimb);

  await expect(card).toBeVisible({ timeout: 10000 });
  await card.getByRole('button', { name: 'Edit send' }).click();

  const editForm = card.locator('form');

  await editForm.getByPlaceholder('Climb name').fill(updatedClimb);
  await editForm.getByPlaceholder('Area').fill('Ten Sleep');
  await editForm.getByPlaceholder('Grade').fill('5.11b');
  await editForm.locator('select[name="gradeSystem"]').selectOption('YDS');
  await editForm.locator('select[name="sourceApp"]').selectOption('MOUNTAIN_PROJECT');
  await editForm.getByPlaceholder('Notes').fill('This send was updated by Playwright');

  await Promise.all([
    page.waitForResponse(
      response =>
        response.url().includes('/sends/') &&
        response.request().method() === 'PUT'
    ),
    page.waitForResponse(
      response =>
        response.url().includes('/sends') &&
        response.request().method() === 'GET'
    ),
    editForm.getByRole('button', { name: 'Save' }).click(),
  ]);

  const updatedCard = getSendRow(page, updatedClimb);

  await expect(updatedCard).toBeVisible({ timeout: 10000 });
  await expect(updatedCard).toContainText('Ten Sleep');
  await expect(updatedCard).toContainText('5.11b');
  await expect(updatedCard).toContainText('Mountain Project');
});

test('deletes a send', async ({ page }) => {
  const climbName = `Delete Me ${Date.now()}`;

  const form = page.locator('form.create-send-form');

  await form.getByPlaceholder('Climb name').fill(climbName);
  await form.getByPlaceholder('Area').fill('Bishop');
  await form.getByPlaceholder('Grade').fill('V3');
  await form.locator('select[name="gradeSystem"]').selectOption('V_SCALE');
  await form.locator('select[name="sourceApp"]').selectOption('MANUAL');
  await form.getByPlaceholder('Notes').fill('This send should be removed');

  await Promise.all([
    page.waitForResponse(
      response =>
        response.url().includes('/sends') &&
        response.request().method() === 'POST'
    ),
    page.waitForResponse(
      response =>
        response.url().includes('/sends') &&
        response.request().method() === 'GET'
    ),
    form.getByRole('button', { name: 'Log Send' }).click(),
  ]);

  const card = getSendRow(page, climbName);

  await expect(card).toBeVisible({ timeout: 10000 });

  await Promise.all([
    page.waitForResponse(
      response =>
        response.url().includes('/sends/') &&
        response.request().method() === 'DELETE'
    ),
    page.waitForResponse(
      response =>
        response.url().includes('/sends') &&
        response.request().method() === 'GET'
    ),
    card.getByRole('button', { name: 'Delete send' }).click(),
  ]);

  await expect(getSendRow(page, climbName)).toHaveCount(0);
});
