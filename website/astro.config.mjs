import {defineConfig} from 'astro/config';
import starlight from '@astrojs/starlight';
import navigation from './src/generated-navigation.json' with {type: 'json'};

const base = (process.env.SITE_BASE ?? '/retail-pos').replace(/\/$/, '');
const site = process.env.SITE_URL ?? 'https://albertpastrana.github.io';

export default defineConfig({
  site,
  base: base || '/',
  integrations: [starlight({
    title: 'Retail POS',
    logo: {src: './src/assets/logo.svg'},
    social: [{icon: 'github', label: 'GitHub', href: 'https://github.com/albertpastrana/retail-pos'}],
    sidebar: navigation.map(({language, entries}) => ({
      label: language === 'en' ? 'Operator guide · English' : 'Guia de l’operador · Català',
      items: [
        {label: language === 'en' ? 'Overview' : 'Índex', slug: `docs/${language}`},
        ...entries.map(({label, slug}) => ({label, slug: `docs/${slug}`})),
      ],
    })),
    customCss: ['./src/styles/site.css'],
    components: {PageTitle: './src/components/PageTitle.astro'},
    head: [{tag: 'link', attrs: {rel: 'icon', href: `${base}/favicon.svg`}}],
  })],
});
