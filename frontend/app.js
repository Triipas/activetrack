/* ActiveTrack · frontend de la PoC (HTML + CSS + JavaScript sin frameworks) */
'use strict';

const EVENTO_ID = 1;
const estado = {
  evento: null,
  categorias: [],
  atletas: [],
  antenas: [],
  seguidos: new Set(cargarSeguidos()),
  ws: null,
  timers: [],
  cambiados: new Set(),
};

/* ------------------------------------------------------------------ utilidades */
const $ = (sel) => document.querySelector(sel);

function esc(texto) {
  return String(texto ?? '').replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
  }[c]));
}

function formatoTiempo(ms) {
  if (ms == null) return '—';
  const total = Math.round(ms / 1000);
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;
  return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

function debounce(fn, ms) {
  let t;
  return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), ms); };
}

async function api(url, opciones = {}) {
  const resp = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...opciones,
  });
  const texto = await resp.text();
  const datos = texto ? JSON.parse(texto) : null;
  if (!resp.ok) throw new Error(datos?.error || `HTTP ${resp.status}`);
  return datos;
}

function cargarSeguidos() {
  try { return JSON.parse(localStorage.getItem('at-seguidos') || '[]'); } catch { return []; }
}
function guardarSeguidos() {
  try { localStorage.setItem('at-seguidos', JSON.stringify([...estado.seguidos])); } catch { /* sin almacenamiento */ }
}

/* ------------------------------------------------------------------ pestañas */
document.querySelectorAll('.pestanas button').forEach((btn) => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.pestanas button').forEach((b) => b.classList.toggle('activa', b === btn));
    document.querySelectorAll('.panel').forEach((p) => p.classList.toggle('activo', p.id === btn.dataset.panel));
    if (btn.dataset.panel === 'juez') cargarJuez();
    if (btn.dataset.panel === 'seguir') cargarHistorialPush();
  });
});
const panelActivo = () => document.querySelector('.panel.activo')?.id;

/* ------------------------------------------------------------------ WebSocket */
function conectarWebSocket() {
  const protocolo = location.protocol === 'https:' ? 'wss' : 'ws';
  const ws = new WebSocket(`${protocolo}://${location.host}/ws/seguimiento`);
  estado.ws = ws;

  ws.onopen = () => {
    marcarConexion(true);
    ws.send(JSON.stringify({ accion: 'leaderboard' }));
    estado.seguidos.forEach((id) => ws.send(JSON.stringify({ accion: 'seguir', atletaId: id })));
  };
  ws.onclose = () => {
    marcarConexion(false);
    setTimeout(conectarWebSocket, 2000);
  };
  ws.onmessage = (e) => {
    const msg = JSON.parse(e.data);
    if (msg.tipo === 'LEADERBOARD') {
      mostrarLatencia(msg.recibidoEn);
      estado.cambiados.add(msg.inscripcionId);
      refrescarLeaderboard();
      if (panelActivo() === 'juez') refrescarJuez();
      if (panelActivo() === 'seguir') refrescarPush();
    } else if (msg.tipo === 'CRUCE') {
      agregarAlFeed(msg.cruce);
    }
  };
}

function marcarConexion(conectado) {
  const el = $('#estado-ws');
  el.classList.toggle('conectado', conectado);
  el.classList.toggle('desconectado', !conectado);
  el.querySelector('.texto').textContent = conectado ? 'Tiempo real conectado' : 'Reconectando…';
}

function enviarWs(mensaje) {
  if (estado.ws?.readyState === WebSocket.OPEN) estado.ws.send(JSON.stringify(mensaje));
}

function mostrarLatencia(recibidoEn) {
  if (!recibidoEn) return;
  const ms = Date.now() - Date.parse(recibidoEn);
  const el = $('#latencia');
  el.querySelector('.valor').textContent = `${ms} ms`;
  el.classList.toggle('ok', ms <= 2000);
  el.classList.toggle('lenta', ms > 2000);
}

/* ------------------------------------------------------------------ leaderboard */
async function cargarLeaderboard() {
  const categoria = $('#filtro-categoria').value;
  const url = `/api/eventos/${EVENTO_ID}/leaderboard${categoria ? `?categoriaId=${categoria}` : ''}`;
  const filas = await api(url);
  const cuerpo = $('#tabla-leaderboard');
  cuerpo.innerHTML = filas.map((f) => `
    <tr class="${estado.cambiados.has(f.inscripcionId) ? 'flash' : ''}">
      <td class="num pos ${f.posicion && f.posicion <= 3 ? 'pos-' + f.posicion : ''}">${f.posicion ?? '—'}</td>
      <td class="num">${f.dorsal}</td>
      <td>${esc(f.nombre)}</td>
      <td>${esc(f.categoria)}</td>
      <td>${esc(f.puntoControl)} <span class="km">${f.km.toFixed(1)} km</span></td>
      <td class="num">${formatoTiempo(f.tiempoNetoMs)}</td>
      <td class="num">${formatoTiempo(f.tiempoBrutoMs)}</td>
      <td><span class="estado ${f.estado}">${etiquetaEstado(f.estado)}</span></td>
    </tr>`).join('');
  estado.cambiados.clear();
  $('#leaderboard-vacio').style.display = filas.length ? 'none' : 'block';
}
const refrescarLeaderboard = debounce(() => cargarLeaderboard().catch(console.error), 120);

function etiquetaEstado(e) {
  return { EN_CARRERA: 'En carrera', FINALIZADO: 'Finalizado', OBSERVADO: 'Observado', DSQ: 'DSQ' }[e] || e;
}

$('#filtro-categoria').addEventListener('change', () => cargarLeaderboard().catch(console.error));

/* ------------------------------------------------------------------ seguir atletas (Observer) */
function pintarAtletas() {
  $('#lista-atletas').innerHTML = estado.atletas.map((a) => {
    const sigue = estado.seguidos.has(a.atletaId);
    return `<li>
      <div><span class="dorsal">${a.dorsal}</span>${esc(a.nombre)}<small>${esc(a.categoria)}</small></div>
      <button class="mini ${sigue ? 'siguiendo' : 'secundario'}" data-atleta="${a.atletaId}">
        ${sigue ? 'Siguiendo' : 'Seguir'}</button>
    </li>`;
  }).join('');
  $('#push-atleta').innerHTML = estado.atletas
    .map((a) => `<option value="${a.atletaId}">${a.dorsal} · ${esc(a.nombre)}</option>`).join('');
}

$('#lista-atletas').addEventListener('click', (e) => {
  const btn = e.target.closest('button[data-atleta]');
  if (!btn) return;
  const id = Number(btn.dataset.atleta);
  if (estado.seguidos.has(id)) {
    estado.seguidos.delete(id);
    enviarWs({ accion: 'dejar', atletaId: id });
  } else {
    estado.seguidos.add(id);
    enviarWs({ accion: 'seguir', atletaId: id });
  }
  guardarSeguidos();
  pintarAtletas();
});

function agregarAlFeed(c) {
  const latencia = c.recibidoEn ? Date.now() - Date.parse(c.recibidoEn) : null;
  const accion = c.estado === 'DSQ' ? 'fue descalificado'
    : c.esMeta ? 'llegó a la meta' : `pasó por ${c.puntoControl}`;
  const li = document.createElement('li');
  li.className = `${c.esMeta ? 'meta' : ''} ${c.estado}`;
  li.innerHTML = `<b>${esc(c.nombre)} (${c.dorsal})</b> ${esc(accion)}
    · ${formatoTiempo(c.tiempoNetoMs)}${c.posicionGeneral ? ` · Puesto ${c.posicionGeneral}` : ''}
    <small>${new Date().toLocaleTimeString()} · ${esc(etiquetaEstado(c.estado))}
    ${latencia != null ? ` · latencia ${latencia} ms` : ''}</small>`;
  $('#feed').prepend(li);
  $('#feed-vacio').style.display = 'none';

  if ('Notification' in window && Notification.permission === 'granted') {
    new Notification(`${c.nombre} (${c.dorsal})`, { body: `${accion} · ${formatoTiempo(c.tiempoNetoMs)}` });
  }
}

$('#btn-permiso').addEventListener('click', async () => {
  if (!('Notification' in window)) return alert('Este navegador no soporta notificaciones.');
  const permiso = await Notification.requestPermission();
  $('#btn-permiso').textContent = permiso === 'granted' ? 'Notificaciones activadas' : 'Permiso denegado';
});

$('#form-push').addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    await api('/api/seguimientos/push', {
      method: 'POST',
      body: JSON.stringify({ atletaId: Number($('#push-atleta').value), tokenDispositivo: $('#push-token').value }),
    });
    cargarHistorialPush();
  } catch (err) {
    alert(err.message);
  }
});

async function cargarHistorialPush() {
  const push = await api('/api/seguimientos/push').catch(() => []);
  $('#historial-push').innerHTML = push.length
    ? push.map((p) => `<li><b>${esc(p.titulo)}</b> → ${esc(p.token)}<small>${esc(p.cuerpo)}</small></li>`).join('')
    : '<li>Sin notificaciones push enviadas todavía.</li>';
}
const refrescarPush = debounce(cargarHistorialPush, 300);

/* ------------------------------------------------------------------ juez */
async function cargarJuez() {
  const [anomalias, antenas] = await Promise.all([
    api(`/api/eventos/${EVENTO_ID}/anomalias`),
    api(`/api/antenas?eventoId=${EVENTO_ID}`),
  ]);
  $('#tabla-anomalias').innerHTML = anomalias.map((a) => `
    <tr>
      <td class="num">${a.dorsal}</td>
      <td>${esc(a.nombre)}<br><span class="km">${esc(a.categoria)}</span></td>
      <td>${esc(a.ultimoPunto)}</td>
      <td>${a.puntosFaltantes.map(esc).join(', ')}</td>
      <td class="botones">
        <button class="mini secundario" data-insc="${a.inscripcionId}" data-tipo="SIN_SANCION">Sin sanción</button>
        <button class="mini secundario" data-insc="${a.inscripcionId}" data-tipo="TIEMPO" data-seg="300">+5 min</button>
        <button class="mini peligro" data-insc="${a.inscripcionId}" data-tipo="DSQ">DSQ</button>
      </td>
    </tr>`).join('');
  $('#anomalias-vacio').style.display = anomalias.length ? 'none' : 'block';

  $('#tabla-antenas').innerHTML = antenas.map((a) => `
    <tr>
      <td>${esc(a.antenaId)}</td>
      <td>${esc(a.puntoControl)}</td>
      <td class="num">${Number(a.km).toFixed(1)}</td>
      <td class="num">${a.totalLecturas}</td>
      <td>${a.ultimaSenalMs ? `hace ${Math.max(0, Math.round((Date.now() - a.ultimaSenalMs) / 1000))} s` : 'Sin señal'}</td>
    </tr>`).join('');
}
const refrescarJuez = debounce(() => cargarJuez().catch(console.error), 300);

$('#btn-refrescar-juez').addEventListener('click', () => cargarJuez().catch((e) => alert(e.message)));

$('#tabla-anomalias').addEventListener('click', async (e) => {
  const btn = e.target.closest('button[data-insc]');
  if (!btn) return;
  const tipo = btn.dataset.tipo;
  const motivos = {
    SIN_SANCION: 'Revisado: falla de lectura de la antena',
    TIEMPO: 'Penalidad por punto de control no registrado',
    DSQ: 'Recorrido incompleto: omitió punto de control',
  };
  try {
    await api(`/api/resultados/${btn.dataset.insc}/penalizaciones`, {
      method: 'POST',
      body: JSON.stringify({ tipo, segundos: Number(btn.dataset.seg || 0), motivo: motivos[tipo], juez: 'Juez de ruta' }),
    });
    cargarJuez();
  } catch (err) {
    alert(err.message);
  }
});

/* ------------------------------------------------------------------ simulador RFID */
function logSim(texto, error = false) {
  const li = document.createElement('li');
  if (error) li.className = 'error';
  li.textContent = `${new Date().toLocaleTimeString()} · ${texto}`;
  const log = $('#log-sim');
  log.prepend(li);
  while (log.children.length > 80) log.lastChild.remove();
}

async function enviarLectura(chipId, antenaId, marcaTiempo) {
  try {
    const r = await api('/api/lecturas', {
      method: 'POST',
      body: JSON.stringify({ chipId, antenaId, marcaTiempo }),
    });
    logSim(`${chipId} @ ${antenaId} → ${r.estado}`);
  } catch (err) {
    logSim(`${chipId} @ ${antenaId} → ${err.message}`, true);
  }
}

function generarCarrera() {
  const largada = Date.parse(estado.evento.fecha);
  const anomalia = $('#sim-anomalia').checked;
  const duplicados = $('#sim-duplicados').checked;
  const cruces = [];
  for (const atleta of estado.atletas) {
    const ritmoSegKm = 180 + Math.random() * 150;   // entre 3:00 y 5:30 min/km
    const demoraSalida = Math.random() * 40;        // segundos hasta pisar la alfombra de salida
    for (const antena of estado.antenas) {
      if (anomalia && atleta.dorsal === 107 && antena.orden === 1) continue;
      const seg = demoraSalida + ritmoSegKm * Number(antena.km) * (0.97 + Math.random() * 0.06);
      const cruce = { chip: atleta.chipId, antena: antena.antenaId, seg, marca: new Date(largada + seg * 1000).toISOString() };
      cruces.push(cruce);
      if (duplicados && Math.random() < 0.25) cruces.push({ ...cruce, seg: seg + 0.4 });
    }
  }
  return cruces.sort((a, b) => a.seg - b.seg);
}

$('#btn-simular').addEventListener('click', () => {
  const factor = Number($('#sim-velocidad').value);
  const cruces = generarCarrera();
  const ultimo = cruces[cruces.length - 1].seg;
  let enviados = 0;
  $('#btn-simular').disabled = true;
  $('#btn-detener').disabled = false;
  $('#sim-estado').textContent = `Carrera en curso: ${cruces.length} lecturas programadas.`;

  estado.timers = cruces.map((c) => setTimeout(async () => {
    await enviarLectura(c.chip, c.antena, c.marca);
    enviados++;
    $('#barra-progreso').style.width = `${Math.round((enviados / cruces.length) * 100)}%`;
    if (enviados === cruces.length) terminarSimulacion('Carrera terminada.');
  }, (c.seg / factor) * 1000));
  logSim(`Simulación iniciada (x${factor}, duración ≈ ${Math.round(ultimo / factor)} s)`);
});

function terminarSimulacion(mensaje) {
  estado.timers.forEach(clearTimeout);
  estado.timers = [];
  $('#btn-simular').disabled = false;
  $('#btn-detener').disabled = true;
  $('#sim-estado').textContent = mensaje;
}

$('#btn-detener').addEventListener('click', () => terminarSimulacion('Simulación detenida.'));

$('#btn-reiniciar').addEventListener('click', async () => {
  if (!confirm('¿Borrar todas las lecturas, resultados y penalizaciones del evento?')) return;
  terminarSimulacion('Demo reiniciada.');
  await api(`/api/lecturas?eventoId=${EVENTO_ID}`, { method: 'DELETE' });
  await api(`/api/eventos/${EVENTO_ID}/resultados`, { method: 'DELETE' });
  $('#barra-progreso').style.width = '0';
  $('#feed').innerHTML = '';
  $('#feed-vacio').style.display = 'block';
  logSim('Demo reiniciada');
  cargarLeaderboard();
});

$('#form-manual').addEventListener('submit', (e) => {
  e.preventDefault();
  enviarLectura($('#manual-chip').value, $('#manual-antena').value, null);
});

/* ------------------------------------------------------------------ inicio */
async function iniciar() {
  try {
    const [evento, categorias, atletas, antenas] = await Promise.all([
      api(`/api/eventos/${EVENTO_ID}`),
      api(`/api/eventos/${EVENTO_ID}/categorias`),
      api(`/api/eventos/${EVENTO_ID}/atletas`),
      api(`/api/antenas?eventoId=${EVENTO_ID}`),
    ]);
    Object.assign(estado, { evento, categorias, atletas, antenas });
    $('#nombre-evento').textContent = evento.nombre;
    $('#filtro-categoria').insertAdjacentHTML('beforeend',
      categorias.map((c) => `<option value="${c.id}">${esc(c.nombre)}</option>`).join(''));
    $('#manual-chip').innerHTML = atletas
      .map((a) => `<option value="${esc(a.chipId)}">${esc(a.chipId)} · ${esc(a.nombre)}</option>`).join('');
    $('#manual-antena').innerHTML = antenas
      .map((a) => `<option value="${esc(a.antenaId)}">${esc(a.antenaId)} · ${esc(a.puntoControl)}</option>`).join('');
    pintarAtletas();
    await cargarLeaderboard();
  } catch (err) {
    $('#nombre-evento').textContent = `No se pudo conectar con los servicios (${err.message})`;
  }
  conectarWebSocket();
}

iniciar();
