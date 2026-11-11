/* ActiveTrack · panel del organizador (requiere rol ORGANIZADOR) */
'use strict';

const org = { eventoId: null };
const ROL_ORG = 'ORGANIZADOR';

async function cargarOrganizador() {
  const conSesion = sesion?.rol === ROL_ORG;
  $('#org-contenido').hidden = !conSesion;
  $('#org-sin-sesion').innerHTML = conSesion ? '' : 'Inicia sesión como <b>organizador</b> para ver este panel. '
    + '<button class="mini" id="btn-login-org">Iniciar sesión</button>';
  $('#org-sin-sesion').style.display = conSesion ? 'none' : 'block';
  if (!conSesion) {
    $('#btn-login-org').onclick = () => pedirLogin(ROL_ORG).catch(() => {});
    return;
  }
  const eventos = await api('/api/admin/eventos', { rol: ROL_ORG });
  if (!org.eventoId || !eventos.some((e) => e.id === org.eventoId)) {
    org.eventoId = (eventos.find((e) => e.estado === 'INSCRIPCIONES_ABIERTAS') || eventos[0])?.id;
  }
  $('#org-evento').innerHTML = eventos.map((e) =>
    `<option value="${e.id}" ${e.id === org.eventoId ? 'selected' : ''}>${esc(e.nombre)}</option>`).join('');
  await cargarResumenEvento();
}

async function cargarResumenEvento() {
  if (!org.eventoId) return;
  const [r, inscritos] = await Promise.all([
    api(`/api/admin/eventos/${org.eventoId}`, { rol: ROL_ORG }),
    api(`/api/admin/eventos/${org.eventoId}/inscripciones?limite=50`, { rol: ROL_ORG }),
  ]);
  $('#org-titulo').textContent = `${r.nombre} · ${formatoFecha(r.fecha)}`;
  $('#org-estado').value = r.estado;
  $('#org-categorias').innerHTML = r.categorias.map((c) => `
    <tr>
      <td>${esc(c.nombre)} <span class="km">${c.edadMin}-${c.edadMax} · ${esc(c.genero)}</span></td>
      <td class="num">${c.cupoMaximo}</td><td class="num">${c.disponibles}</td><td class="num">${c.confirmadas}</td>
      <td class="num">${c.pendientesPago}</td><td class="num">${c.enEspera}</td>
      <td><div class="fila-form sin-margen">
        <input type="number" min="0" value="${c.cupoMaximo}" data-cupo-input="${c.id}" style="width:90px">
        <button class="mini secundario" data-cupo="${c.id}">Guardar</button></div></td>
    </tr>`).join('');
  $('#org-tarifas').innerHTML = r.tarifas.map((t) =>
    `<li><b>${esc(t.nombre)}</b> · ${formatoSoles(t.precio)}<small>del ${esc(t.vigenteDesde)} al ${esc(t.vigenteHasta)}</small></li>`).join('');
  $('#org-inscripciones').innerHTML = inscritos.map((i) => `
    <tr>
      <td class="num">${i.id}</td><td class="num">${i.dorsal ?? '—'}</td><td>${esc(i.atleta)}</td><td>${esc(i.documento)}</td>
      <td>${esc(i.categoria)}</td><td>${esc(i.tarifa)} · ${formatoSoles(i.precio)}</td>
      <td><span class="estado ${i.estado === 'CONFIRMADA' ? 'FINALIZADO' : i.estado === 'ANULADA' ? 'DSQ' : i.estado === 'EN_ESPERA' ? 'OBSERVADO' : ''}">${esc(i.estado)}</span></td>
      <td>${i.estado === 'CONFIRMADA' ? `<button class="mini peligro" data-reembolso="${i.id}">Reembolsar</button>` : ''}</td>
    </tr>`).join('');
}

$('#org-evento').addEventListener('change', () => {
  org.eventoId = Number($('#org-evento').value);
  cargarResumenEvento().catch((e) => alert(e.message));
});
$('#btn-refrescar-org').addEventListener('click', () => cargarOrganizador().catch((e) => alert(e.message)));

$('#btn-org-estado').addEventListener('click', async () => {
  try {
    await api(`/api/admin/eventos/${org.eventoId}/estado`, {
      rol: ROL_ORG, method: 'PUT', body: JSON.stringify({ estado: $('#org-estado').value }),
    });
    cargarResumenEvento();
  } catch (e) { alert(e.message); }
});

$('#org-categorias').addEventListener('click', async (e) => {
  const btn = e.target.closest('[data-cupo]');
  if (!btn) return;
  const valor = Number(document.querySelector(`[data-cupo-input="${btn.dataset.cupo}"]`).value);
  try {
    await api(`/api/admin/categorias/${btn.dataset.cupo}/cupo`, {
      rol: ROL_ORG, method: 'PUT', body: JSON.stringify({ cupoMaximo: valor }),
    });
    cargarResumenEvento();
  } catch (err) { alert(err.message); }
});

$('#org-inscripciones').addEventListener('click', async (e) => {
  const btn = e.target.closest('[data-reembolso]');
  if (!btn || !confirm(`¿Reembolsar la inscripción #${btn.dataset.reembolso}? Se libera el cupo.`)) return;
  try {
    await api(`/api/admin/inscripciones/${btn.dataset.reembolso}/reembolso`, { rol: ROL_ORG, method: 'POST' });
    cargarResumenEvento();
  } catch (err) { alert(err.message); }
});

$('#form-tarifa').addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    await api(`/api/admin/eventos/${org.eventoId}/tarifas`, {
      rol: ROL_ORG, method: 'POST',
      body: JSON.stringify({
        nombre: $('#tar-nombre').value, precio: Number($('#tar-precio').value),
        vigenteDesde: $('#tar-desde').value, vigenteHasta: $('#tar-hasta').value,
      }),
    });
    e.target.reset();
    cargarResumenEvento();
  } catch (err) { alert(err.message); }
});

$('#btn-csv-inscritos').addEventListener('click', () =>
  descargar(`/api/admin/eventos/${org.eventoId}/inscritos.csv`, `inscritos-evento-${org.eventoId}.csv`, ROL_ORG)
    .catch((e) => alert(e.message)));
$('#btn-csv-resultados').addEventListener('click', () =>
  descargar(`/api/eventos/${org.eventoId}/resultados.csv`, `resultados-evento-${org.eventoId}.csv`, ROL_ORG)
    .catch((e) => alert(e.message)));

/** Convierte las líneas "a;b;c" de un textarea en arreglos de campos. */
const lineas = (sel) => $(sel).value.split('\n').map((l) => l.trim()).filter(Boolean).map((l) => l.split(';').map((x) => x.trim()));

$('#form-evento').addEventListener('submit', async (e) => {
  e.preventDefault();
  try {
    const creado = await api('/api/admin/eventos', {
      rol: ROL_ORG, method: 'POST',
      body: JSON.stringify({
        nombre: $('#ev-nombre').value,
        tipo: $('#ev-tipo').value,
        fecha: new Date($('#ev-fecha').value).toISOString(),
        ruta: $('#ev-ruta').value,
        distanciaKm: Number($('#ev-distancia').value),
        puntos: lineas('#ev-puntos').map(([nombre, km, antenaId]) => ({ nombre, km: Number(km), antenaId })),
        categorias: lineas('#ev-categorias').map(([nombre, edadMin, edadMax, genero, cupoMaximo]) =>
          ({ nombre, edadMin: Number(edadMin), edadMax: Number(edadMax), genero, cupoMaximo: Number(cupoMaximo) })),
        tarifas: lineas('#ev-tarifas').map(([nombre, precio, vigenteDesde, vigenteHasta]) =>
          ({ nombre, precio: Number(precio), vigenteDesde, vigenteHasta })),
      }),
    });
    org.eventoId = creado.id;
    alert(`Evento creado (#${creado.id}). Ya aparece en Inscripciones.`);
    cargarOrganizador();
    ins.eventoSeleccionado = null;
  } catch (err) { alert(err.message); }
});
