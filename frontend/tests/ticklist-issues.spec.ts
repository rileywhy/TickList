import { test, expect, type Page, type Route } from '@playwright/test';
import { makeTestUser, registerUser, loginUser } from './helpers/users';

/**
 * These tests document KNOWN, currently-unfixed defects in the tick list
 * (rendered by src/pages/TickPage.tsx). Each one calls `test.fail()`, which tells
 * Playwright the test body is EXPECTED to fail against the current code:
 *
 *   - While the bug exists, the test "fails as expected" and the suite stays green.
 *   - Once the bug is fixed, the test starts passing, and Playwright reports it as
 *     an unexpected pass ("expected to fail"). That is the signal to delete the
 *     `test.fail()` line and keep the test as a normal regression guard.
 *
 * They rely on `page.route()` to mock the `GET /ticks` response so each failure
 * mode is deterministic and does not depend on real backend data. Auth still goes
 * through the real backend, matching the rest of the suite.
 */

type MockTick = Record<string, unknown>;

function makeMockTick(overrides: MockTick = {}): MockTick {
  return {
    id: 1,
    climbName: 'Sunset Arete',
    climbId: 'mp-123',
    location: 'Bishop',
    discipline: 'SPORT',
    grade: '5.11a',
    gradeSystem: 'YDS',
    sourceApp: 'MOUNTAIN_PROJECT',
    tickType: 'SEND',
    externalId: 'ext-1',
    sourceUrl: 'https://example.com/route/1',
    tickDate: '2026-01-01',
    style: 'Powerful',
    ropeStyle: 'REDPOINT',
    attempts: 3,
    notes: 'Great climb',
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides,
  };
}

/** Mock only GET /ticks; let every other method (POST/PUT/DELETE) hit the backend. */
async function mockTickList(page: Page, handler: (route: Route) => Promise<void> | void) {
  await page.route('**/ticks', async (route) => {
    if (route.request().method() !== 'GET') {
      return route.fallback();
    }
    await handler(route);
  });
}

async function openTickList(page: Page) {
  await page.getByRole('link', { name: 'TickList' }).click();
}

function getTickRow(page: Page, climbName: string) {
  return page.locator('.tick-row').filter({ hasText: climbName });
}

test.beforeEach(async ({ page }) => {
  const user = makeTestUser();

  await registerUser(page, user);
  await loginUser(page, user);
});

// Issue: loadTicks() only branches on HTTP status codes. A rejected fetch
// (network failure / server down / offline) is never caught, so the user is
// shown a silently blank list with no indication anything went wrong.
test('shows an error when the tick list fails to load', async ({ page }) => {
  await mockTickList(page, (route) => route.abort('failed'));
  await openTickList(page);
  await expect(page.getByRole('alert')).toBeVisible({ timeout: 2000 });
});

// Issue: setTicks(data) trusts the response is an array. A non-array payload
// makes the next render throw on ticks.filter(...), and with no error boundary
// the whole page (including the create form) unmounts.
test('does not crash when the backend returns a non-array payload', async ({ page }) => {
  await mockTickList(page, (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: '{}' })
  );
  await openTickList(page);
  await expect(page.locator('form.create-tick-form')).toBeVisible({ timeout: 2000 });
});

// Issue: when the list is empty there is no empty state. The area below the
// filters renders nothing, which is indistinguishable from a broken/failed load.
test('shows an empty state when there are no ticks', async ({ page }) => {
  test.fail();

  await mockTickList(page, (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: '[]' })
  );

  await openTickList(page);
  await expect(page.getByText(/no ticks/i)).toBeVisible({ timeout: 2000 });
});

// Issue: loadTicks is async (and deferred via setTimeout) but there is no loading
// state. On first paint the list is empty with no spinner/skeleton.
test('shows a loading indicator while ticks are loading', async ({ page }) => {
  test.fail();

  await mockTickList(page, async (route) => {
    await new Promise((resolve) => setTimeout(resolve, 3000));
    await route.fulfill({ status: 200, contentType: 'application/json', body: '[]' });
  });

  await openTickList(page);
  await expect(page.getByText(/loading/i)).toBeVisible({ timeout: 2000 });
});

// Issue: the search predicate matches the RAW enum codes (e.g. "CLEAN_TR"), not
// the formatted labels the user actually sees ("Clean Tr"). Searching the visible
// text therefore filters out matching rows.
test('search matches the labels the user actually sees', async ({ page }) => {
  test.fail();

  await mockTickList(page, (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify([makeMockTick({ tickType: 'CLEAN_TR' })]),
    })
  );

  await openTickList(page);
  await expect(getTickRow(page, 'Sunset Arete')).toBeVisible();
  // The card displays tickType "CLEAN_TR" as "Clean Tr"; searching that visible
  // text should keep the row, but the raw-code match ("clean_tr") drops it.
  await page.locator('.filters').getByPlaceholder('Search').fill('Clean Tr');

  await expect(getTickRow(page, 'Sunset Arete')).toBeVisible({ timeout: 2000 });
});
