import {readFile, mkdir} from 'node:fs/promises';
import {resolve, join} from 'node:path';
import MarkdownIt from 'markdown-it';
import {chromium} from 'playwright';
import navigation from '../src/generated-navigation.json' with {type: 'json'};

const md = new MarkdownIt({html: false, linkify: true});
const escape = md.utils.escapeHtml;
const output = resolve('dist/downloads');
await mkdir(output, {recursive: true});
const browser = await chromium.launch({headless: true, ...(process.env.CHROMIUM_PATH ? {executablePath: process.env.CHROMIUM_PATH} : {})});
try {
  for (const {language, entries} of navigation) {
    const index = await readFile(resolve('..', 'docs/user', language, 'index.md'), 'utf8');
    const guideTitle = index.match(/^# (.+)$/m)[1];
    const chapters = new Map(entries.map(({slug}, i) => [slug.slice(language.length + 1) + '.md', i + 1]));
    const linksToChapters = (text) => text.replace(/\]\(([^)#]+\.md)(?:#[^)]+)?\)/g, (match, target) => {
      const chapter = chapters.get(target) ?? chapters.get(`use-cases/${target}`);
      if (!chapter) throw new Error(`Unresolved PDF link: ${target}`);
      return `](#chapter-${chapter})`;
    });
    const toc = entries.map(({label}, i) => `<li><a href="#chapter-${i + 1}">${escape(label)}</a></li>`).join('');
    const sections = [`<section><h1>${escape(guideTitle)}</h1>${md.render(linksToChapters(index.replace(/^# .+\r?\n/, '')))}</section>`];
    for (const [i, {slug}] of entries.entries()) {
      const text = await readFile(resolve('..', 'docs/user', slug + '.md'), 'utf8');
      const title = text.match(/^# (.+)$/m)[1];
      const contents = linksToChapters(text.replace(/^# .+\r?\n/, ''));
      sections.push(`<section id="chapter-${i + 1}"><h1>${escape(title)}</h1>${md.render(contents)}</section>`);
    }
    const page = await browser.newPage();
    await page.setContent(`<!doctype html><html lang="${language}"><meta charset="utf-8"><title>${escape(guideTitle)}</title>
      <style>body{font:11pt/1.55 system-ui,sans-serif;color:#172532} h1{font-size:20pt} h2{font-size:14pt} a{color:#14538a}
      .cover{break-after:page;padding-top:30mm} .toc{break-after:page} .toc li{margin:.35rem 0}
      section{break-before:page} h1,h2,h3{break-after:avoid} pre,table,img,blockquote,li{break-inside:avoid}
      pre{white-space:pre-wrap;background:#f4f6f8;padding:.7rem}table{border-collapse:collapse}td,th{border:1px solid #888;padding:.3rem}
      @page{size:A4;margin:18mm 17mm}</style>
      <div class="cover"><h1>Retail POS</h1><h2>${escape(guideTitle)}</h2></div>
      <nav class="toc"><h1>${language === 'ca' ? 'Contingut' : 'Contents'}</h1><ol>${toc}</ol></nav>
      ${sections.join('')}</html>`, {waitUntil: 'load'});
    await page.pdf({path: join(output, `documentation-${language}.pdf`), format: 'A4', printBackground: true,
      displayHeaderFooter: true, headerTemplate: '<span></span>',
      footerTemplate: '<div style="width:100%;text-align:center;font:9px sans-serif;color:#555"><span class="pageNumber"></span></div>',
      margin: {top: '18mm', bottom: '18mm', left: '17mm', right: '17mm'}});
    await page.close();
  }
} finally {
  await browser.close();
}
