import { chromium } from 'playwright';

const APP_BASE = process.env.APP_BASE || 'http://localhost:5173';

async function main() {
  const browser = await chromium.launch({
    channel: 'chrome',
    headless: true
  });

  const context = await browser.newContext();
  await context.addInitScript(() => {
    localStorage.setItem('zongce_access_token', 'invalid');
    localStorage.setItem('zongce_refresh_token', 'invalid');
  });

  const page = await context.newPage();

  await page.goto(`${APP_BASE}/dashboard`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(2500);

  const firstUrl = page.url();
  const loginTitleCount = await page.getByText('综测系统登录').count();
  const firstTokens = await page.evaluate(() => ({
    access: localStorage.getItem('zongce_access_token'),
    refresh: localStorage.getItem('zongce_refresh_token')
  }));

  await page.goto(`${APP_BASE}/dashboard`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1200);

  const secondUrl = page.url();
  const secondTokens = await page.evaluate(() => ({
    access: localStorage.getItem('zongce_access_token'),
    refresh: localStorage.getItem('zongce_refresh_token')
  }));

  const passed = firstUrl.includes('/login')
    && secondUrl.includes('/login')
    && loginTitleCount > 0
    && !firstTokens.access
    && !firstTokens.refresh
    && !secondTokens.access
    && !secondTokens.refresh;

  console.log(JSON.stringify({
    passed,
    firstUrl,
    secondUrl,
    loginTitleCount,
    firstTokens,
    secondTokens
  }, null, 2));

  await browser.close();
  process.exit(passed ? 0 : 1);
}

main().catch((error) => {
  console.error(error);
  process.exit(2);
});
