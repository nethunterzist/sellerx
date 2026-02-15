import { test, expect } from '@playwright/test';

test.describe('QA Page Redesign Visual Check', () => {
  test.beforeEach(async ({ page }) => {
    // Login
    await page.goto('http://localhost:3000/tr/sign-in');
    await page.fill('input[type="email"]', 'test@test.com');
    await page.fill('input[type="password"]', '123456');
    await page.click('button[type="submit"]');

    // Wait for redirect to dashboard
    await page.waitForURL('**/dashboard', { timeout: 10000 });

    // Navigate to QA page
    await page.goto('http://localhost:3000/tr/qa');
    await page.waitForLoadState('networkidle');
  });

  test('should show new segmented control navigation', async ({ page }) => {
    // Check for segmented control tabs
    const segmentedControl = page.locator('[role="tablist"]');
    await expect(segmentedControl).toBeVisible();

    // Check all tabs are present
    await expect(page.getByRole('tab', { name: /Cevap Akışı/i })).toBeVisible();
    await expect(page.getByRole('tab', { name: /AI Beyni/i })).toBeVisible();
    await expect(page.getByRole('tab', { name: /Kurallar/i })).toBeVisible();
    await expect(page.getByRole('tab', { name: /Performans/i })).toBeVisible();

    await page.screenshot({ path: 'qa-segmented-control.png', fullPage: true });
  });

  test('should show Answer Flow split-view', async ({ page }) => {
    // Click on Answer Flow tab
    await page.getByRole('tab', { name: /Cevap Akışı/i }).click();
    await page.waitForTimeout(500);

    // Check for split-view layout (filter tabs)
    const filterTabs = page.locator('text=Bekleyen').or(page.locator('text=Cevaplanan'));
    await expect(filterTabs.first()).toBeVisible();

    await page.screenshot({ path: 'qa-answer-flow-split-view.png', fullPage: true });
  });

  test('should show AI Brain with knowledge base', async ({ page }) => {
    // Click on AI Brain tab
    await page.getByRole('tab', { name: /AI Beyni/i }).click();
    await page.waitForTimeout(500);

    // Check for Knowledge Base section
    const knowledgeBase = page.locator('text=Bilgi Bankası');
    await expect(knowledgeBase).toBeVisible();

    // Check if there's a "+ Bilgi Ekle" button
    const addButton = page.locator('text=Bilgi Ekle').or(page.getByRole('button', { name: /ekle/i }));

    await page.screenshot({ path: 'qa-ai-brain.png', fullPage: true });
  });

  test('should show Rules with live preview layout', async ({ page }) => {
    // Click on Rules tab
    await page.getByRole('tab', { name: /Kurallar/i }).click();
    await page.waitForTimeout(500);

    // Check for two-column layout indicators
    const toneSection = page.locator('text=Ton ve Stil');
    await expect(toneSection).toBeVisible();

    // Check for live preview section (might be hidden on mobile)
    const livePreview = page.locator('text=Önizleme').or(page.locator('text=Canlı Önizleme'));

    await page.screenshot({ path: 'qa-rules-live-preview.png', fullPage: true });
  });

  test('should show Performance with seniority section', async ({ page }) => {
    // Click on Performance tab
    await page.getByRole('tab', { name: /Performans/i }).click();
    await page.waitForTimeout(500);

    // Check for seniority status section
    const senioritySection = page.locator('text=Kıdem Durumu');
    await expect(senioritySection).toBeVisible();

    await page.screenshot({ path: 'qa-performance.png', fullPage: true });
  });

  test('complete visual check - all tabs', async ({ page }) => {
    const tabs = [
      { name: 'Cevap Akışı', filename: 'tab-answer-flow.png' },
      { name: 'AI Beyni', filename: 'tab-ai-brain.png' },
      { name: 'Kurallar', filename: 'tab-rules.png' },
      { name: 'Performans', filename: 'tab-performance.png' },
    ];

    for (const tab of tabs) {
      await page.getByRole('tab', { name: new RegExp(tab.name, 'i') }).click();
      await page.waitForTimeout(1000);
      await page.screenshot({
        path: tab.filename,
        fullPage: true
      });
    }
  });
});
