import {readFile, stat, readdir} from 'node:fs/promises';
import {join, resolve} from 'node:path';
import assert from 'node:assert/strict';
import navigation from '../src/generated-navigation.json' with {type: 'json'};

const dist = resolve('dist');
const base = (process.env.SITE_BASE ?? '/retail-pos').replace(/\/$/, '');
const pages = ['index.html', 'docs/index.html', ...navigation.flatMap(({language, entries}) => [
  `docs/${language}/index.html`, ...entries.map(({slug}) => `docs/${slug}/index.html`),
])];
for (const file of pages) {
  const html = await readFile(join(dist, file), 'utf8');
  if (file.startsWith('docs/')) assert.match(html, /Print this page|Imprimeix aquesta pàgina/);
  for (const [, href] of html.matchAll(/\bhref="([^"]+)"/g)) {
    if (href.startsWith('#') || /^(?:https?:|mailto:|data:)/.test(href)) continue;
    const path = new URL(href, `https://example.org${base}/${file.replace(/index\.html$/, '')}`).pathname;
    assert.ok(path.startsWith(base + '/'), `Link outside base: ${file}: ${href}`);
    const target = join(dist, decodeURIComponent(path.slice(base.length + 1)));
    const disk = path.endsWith('/') ? join(target, 'index.html') : target;
    try { await stat(disk); } catch { throw new Error(`Broken link: ${file}: ${href}`); }
  }
}
for (const {language} of navigation) {
  const pdf = await readFile(join(dist, 'downloads', `documentation-${language}.pdf`));
  assert.ok(pdf.subarray(0, 4).toString() === '%PDF' && pdf.length > 2000);
}
assert.ok((await readdir(join(dist, 'pagefind'))).length > 1, 'Missing production search index');
console.log(`Checked ${pages.length} pages, links, search assets, and both PDFs.`);
