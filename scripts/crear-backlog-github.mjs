#!/usr/bin/env node
/**
 * Crea en GitHub el tablero Scrum del proyecto a partir de scripts/backlog.json:
 * etiquetas, un milestone por sprint, un issue por historia de usuario y un GitHub Project.
 * Las historias ya implementadas se cierran (el Project las muestra en "Done").
 *
 * Requisitos: GitHub CLI autenticado con el scope "project" (gh auth refresh -s project).
 * Uso (desde la raíz del repositorio, después de subirlo a GitHub):
 *   node scripts/crear-backlog-github.mjs
 *   node scripts/crear-backlog-github.mjs --md   (solo regenera docs/backlog.md, sin tocar GitHub)
 */
import { execFileSync } from 'node:child_process';
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const raiz = join(dirname(fileURLToPath(import.meta.url)), '..');
const backlog = JSON.parse(readFileSync(join(raiz, 'scripts', 'backlog.json'), 'utf8'));

const cuerpo = (h) => [
  `**Historia de usuario:** ${h.historia}`,
  '',
  `**Requisito:** ${h.requisito} (${h.tipo})`,
  '',
  '**Criterios de aceptación:**',
  ...h.criterios.map((c) => `- [${h.estado === 'hecho' ? 'x' : ' '}] ${c}`),
].join('\n');

function generarMarkdown() {
  const lineas = ['# Backlog del producto (Scrum)', '',
    'Generado desde `scripts/backlog.json`. Cada historia se crea como issue en GitHub Projects con `node scripts/crear-backlog-github.mjs`.', ''];
  backlog.sprints.forEach((s, i) => {
    lineas.push(`## ${s.titulo}`, '', s.descripcion, '', '| ID | Historia | Requisito | Estado |', '|---|---|---|---|');
    backlog.historias.filter((h) => h.sprint === i).forEach((h) => {
      lineas.push(`| ${h.id} | ${h.titulo} | ${h.requisito} | ${h.estado === 'hecho' ? 'Hecho' : 'Pendiente'} |`);
    });
    lineas.push('');
  });
  lineas.push('## Detalle', '');
  backlog.historias.forEach((h) => lineas.push(`### ${h.id} · ${h.titulo}`, '', cuerpo(h), ''));
  writeFileSync(join(raiz, 'docs', 'backlog.md'), lineas.join('\n'), 'utf8');
  console.log('docs/backlog.md generado');
}

if (process.argv.includes('--md')) {
  generarMarkdown();
  process.exit(0);
}

const gh = (...args) => execFileSync('gh', args, { encoding: 'utf8' }).trim();
const repo = gh('repo', 'view', '--json', 'nameWithOwner', '-q', '.nameWithOwner');
const owner = repo.split('/')[0];
console.log(`Repositorio: ${repo}`);

// 1. Etiquetas
const etiquetas = [
  ['historia-usuario', '1f4e79', 'Historia de usuario'],
  ['RF', 'c8102e', 'Requisito funcional'],
  ['RNF', '6f42c1', 'Requisito no funcional'],
];
for (const [nombre, color, desc] of etiquetas) {
  gh('label', 'create', nombre, '--repo', repo, '--color', color, '--description', desc, '--force');
}

// 2. Milestones (sprints)
const existentes = JSON.parse(gh('api', `repos/${repo}/milestones?state=all&per_page=100`));
for (const s of backlog.sprints) {
  if (!existentes.some((m) => m.title === s.titulo)) {
    gh('api', `repos/${repo}/milestones`, '-f', `title=${s.titulo}`, '-f', `description=${s.descripcion}`);
  }
}

// 3. Project (tablero)
const titulo = 'ActiveTrack - Scrum';
const proyectos = JSON.parse(gh('project', 'list', '--owner', owner, '--format', 'json')).projects;
let proyecto = proyectos.find((p) => p.title === titulo);
if (!proyecto) {
  proyecto = JSON.parse(gh('project', 'create', '--owner', owner, '--title', titulo, '--format', 'json'));
}
gh('project', 'link', String(proyecto.number), '--owner', owner, '--repo', repo);

// 4. Issues
const issues = JSON.parse(gh('issue', 'list', '--repo', repo, '--state', 'all', '--limit', '200', '--json', 'title'));
for (const h of backlog.historias) {
  const tituloIssue = `${h.id} · ${h.titulo}`;
  if (issues.some((i) => i.title === tituloIssue)) {
    console.log(`= ${tituloIssue} (ya existe)`);
    continue;
  }
  const url = gh('issue', 'create', '--repo', repo, '--title', tituloIssue, '--body', cuerpo(h),
    '--label', 'historia-usuario', '--label', h.tipo, '--milestone', backlog.sprints[h.sprint].titulo);
  gh('project', 'item-add', String(proyecto.number), '--owner', owner, '--url', url);
  if (h.estado === 'hecho') {
    gh('issue', 'close', url, '--reason', 'completed', '--comment', 'Implementado en la PoC (ver commits del sprint).');
  }
  console.log(`+ ${tituloIssue}`);
}

generarMarkdown();
console.log(`Listo. Tablero: https://github.com/users/${owner}/projects/${proyecto.number}`);
