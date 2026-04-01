import { expect, test } from 'playwright/test';

test.use({
  browserName: 'chromium',
  channel: 'chrome'
});

test.describe('TC-06 stale token redirect', () => {
  test('invalid tokens should redirect to login and be cleared', async ({ context, page }) => {
    await context.addInitScript(() => {
      localStorage.setItem('zongce_access_token', 'invalid');
      localStorage.setItem('zongce_refresh_token', 'invalid');
    });

    await page.goto('http://localhost:5173/dashboard');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(2500);

    await expect(page).toHaveURL(/\/login/);
    await expect(page.getByText('综测系统登录')).toBeVisible();

    const firstTokens = await page.evaluate(() => ({
      access: localStorage.getItem('zongce_access_token'),
      refresh: localStorage.getItem('zongce_refresh_token')
    }));
    expect(firstTokens.access).toBeNull();
    expect(firstTokens.refresh).toBeNull();

    await page.goto('http://localhost:5173/dashboard');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1200);

    await expect(page).toHaveURL(/\/login/);

    const secondTokens = await page.evaluate(() => ({
      access: localStorage.getItem('zongce_access_token'),
      refresh: localStorage.getItem('zongce_refresh_token')
    }));
    expect(secondTokens.access).toBeNull();
    expect(secondTokens.refresh).toBeNull();
  });
});
