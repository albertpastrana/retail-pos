import {readFile, copyFile, mkdir, rm, writeFile} from 'node:fs/promises';
import {join, dirname, resolve} from 'node:path';

const source = resolve('..', 'docs/user');
const destination = resolve('src/content/docs');
const languages = ['en', 'ca'];
const navigation = [];

await rm(destination, {recursive: true, force: true});
await mkdir(destination, {recursive: true});
const mark = resolve('..', 'design-system/assets/Logo/retail-pos-app-icon.svg');
await copyFile(mark, resolve('src/assets/logo.svg'));
await copyFile(mark, resolve('public/favicon.svg'));

for (const language of languages) {
  const home = await readFile(join(source, language, 'index.md'), 'utf8');
  const entries = [...home.matchAll(/^\d+\. \[([^\]]+)\]\((use-cases\/[^)]+\.md)\)/gm)]
    .map(([, label, file]) => ({label, slug: `${language}/${file.slice(0, -3)}`}));
  if (!entries.length) throw new Error(`No navigation found for ${language}`);
  navigation.push({language, entries});
  for (const file of ['index.md', ...entries.map(({slug}) => slug.slice(language.length + 1) + '.md')]) {
    const input = join(source, language, file);
    const text = await readFile(input, 'utf8');
    const title = text.match(/^# (.+)$/m)?.[1];
    if (!title) throw new Error(`Missing title in ${input}`);
    // Keep links relative so the same guide works in both the repository and the site.
    const content = text.replace(/^# .+\r?\n/, '').replace(/\]\(([^)#]+)\.md(#[^)]+)?\)/g,
      (_, target, anchor = '') => `](${file === 'index.md' ? '' : '../'}${target}/${anchor})`);
    const output = join(destination, 'docs', language, file);
    await mkdir(dirname(output), {recursive: true});
    await writeFile(output, `---\ntitle: ${JSON.stringify(title)}\n---\n\n${content}`);
  }
}
await writeFile(join(destination, 'docs/index.md'), '---\ntitle: Retail POS documentation\n---\n\nChoose a guide:\n\n- [English operator guide](en/)\n- [Guia de l’operador en català](ca/)\n');
await writeFile(resolve('src/generated-navigation.json'), JSON.stringify(navigation, null, 2));
