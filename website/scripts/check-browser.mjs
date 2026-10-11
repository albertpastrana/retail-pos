import {createServer} from 'node:http';
import {readFile, stat} from 'node:fs/promises';
import {join, resolve, extname} from 'node:path';
import assert from 'node:assert/strict';
import {chromium} from 'playwright';

const base = (process.env.SITE_BASE ?? '/retail-pos').replace(/\/$/, '');
const dist = resolve('dist');
const types = {'.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css', '.json': 'application/json',
  '.svg': 'image/svg+xml', '.pdf': 'application/pdf', '.woff2': 'font/woff2', '.wasm': 'application/wasm'};
const server = createServer(async (req, res) => {
  const pathname = decodeURIComponent(new URL(req.url, 'http://localhost').pathname);
  if (!pathname.startsWith(base + '/')) {res.writeHead(404).end(); return;}
  const target = resolve(dist, pathname.slice(base.length + 1));
  if (!target.startsWith(dist + '/') && target !== dist) {res.writeHead(403).end(); return;}
  try {
    const file = (await stat(target)).isDirectory() ? join(target, 'index.html') : target;
    res.setHeader('content-type', types[extname(file)] ?? 'application/octet-stream');
    res.end(await readFile(file));
  } catch {res.writeHead(404).end();}
});
await new Promise((done) => server.listen(0, '127.0.0.1', done));
const origin = `http://127.0.0.1:${server.address().port}${base}`;
let browser;
try {
  browser = await chromium.launch({headless: true, ...(process.env.CHROMIUM_PATH ? {executablePath: process.env.CHROMIUM_PATH} : {})});
  const page = await browser.newPage({viewport: {width: 1280, height: 800}});
  await page.goto(origin + '/');
  assert.match(await page.title(), /Retail POS/);
  await page.getByRole('link', {name: 'Get started'}).click();
  assert.ok(page.url().endsWith('/docs/'));
  await page.goto(origin + '/docs/en/use-cases/normal-sale/');
  assert.ok(await page.getByRole('button', {name: 'Print this page'}).isVisible());
  const theme = page.getByLabel('Select theme').first();
  await theme.selectOption('light');
  assert.equal(await page.locator('html').getAttribute('data-theme'), 'light');
  await theme.selectOption('dark');
  assert.equal(await page.locator('html').getAttribute('data-theme'), 'dark');
  await page.emulateMedia({media: 'print'});
  assert.equal(await page.locator('.doc-actions').evaluate((el) => getComputedStyle(el).display), 'none');
  assert.ok(await page.getByRole('heading', {level: 1}).first().isVisible());
  await page.emulateMedia({media: 'screen'});
  const pagefind = await page.evaluate(async (prefix) => {
    const search = await import(`${prefix}/pagefind/pagefind.js`);
    const results = await search.search('barcode');
    return results.results.length;
  }, base);
  assert.ok(pagefind > 0, 'Production Pagefind search did not return results');
  await page.setViewportSize({width: 390, height: 844});
  assert.ok(await page.getByRole('button', {name: 'Print this page'}).isVisible());
  await page.goto(origin + '/docs/ca/use-cases/venda-normal/');
  assert.ok(await page.getByRole('button', {name: 'Imprimeix aquesta pàgina'}).isVisible());
  assert.ok(await page.getByRole('link', {name: 'Descarrega la guia en PDF'}).isVisible());
  console.log('Browser checked landing, EN/CA pages, print view, themes, search, and narrow layout.');
} finally {
  await browser?.close();
  server.close();
}
