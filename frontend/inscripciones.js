/* ActiveTrack · inscripciones del atleta (catálogo, reserva de cupo, pago, kit QR e historial) */
'use strict';

const ins = { eventos: [], eventoSeleccionado: null, cuentaRegresiva: null };

const formatoSoles = (n) => `S/ ${Number(n).toFixed(2)}`;
const formatoFecha = (iso) => new Date(iso).toLocaleString('es-PE', { dateStyle: 'medium', timeStyle: 'short' });
const formatoRitmo = (s) => (s == null ? '—' : `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')} min/km`);

async function cargarInscripciones() {
  ins.eventos = await api('/api/catalogo/eventos');
  pintarCatalogo();
  cargarCorreos();
  if (!ins.eventoSeleccionado && ins.eventos.length) seleccionarEvento(ins.eventos[0].id);
}

function pintarCatalogo() {
  $('#catalogo-vacio').style.display = ins.eventos.length ? 'none' : 'block';
  $('#catalogo').innerHTML = ins.eventos.map((e) => `
    <button class="evento-card ${ins.eventoSeleccionado === e.id ? 'seleccionado' : ''}" data-evento="${e.id}">
      <h3>${esc(e.nombre)}</h3>
      <span class="meta-evento">${formatoFecha(e.fecha)} · ${e.distanciaKm ?? '—'} km · ${esc(e.tipo)}</span>
      <span class="precio">${e.tarifaVigente ? formatoSoles(e.tarifaVigente.precio) : 'Sin tarifa vigente'}</span>
      ${e.tarifaVigente ? `<span class="meta-evento">${esc(e.tarifaVigente.nombre)} hasta el ${esc(e.tarifaVigente.vigenteHasta)}</span>` : ''}
      <div class="cupos">${e.categorias.map((c) => `
        <span class="cupo ${c.disponibles === 0 ? 'agotado' : ''}">${esc(c.nombre)}: ${c.disponibles === 0 ? 'lista de espera' : c.disponibles + ' cupos'}</span>`).join('')}
      </div>
    </button>`).join('');
}

$('#catalogo').addEventListener('click', (e) => {
  const tarjeta = e.target.closest('[data-evento]');
  if (tarjeta) seleccionarEvento(Number(tarjeta.dataset.evento));
});

function seleccionarEvento(id) {
  ins.eventoSeleccionado = id;
  const evento = ins.eventos.find((e) => e.id === id);
  pintarCatalogo();
  $('#titulo-form-inscripcion').textContent = `Inscripción · ${evento.nombre}`;
  $('#ins-categoria').innerHTML = evento.categorias.map((c) => `
    <option value="${c.id}">${esc(c.nombre)} (${c.edadMin}-${c.edadMax} años${c.disponibles === 0 ? ', lista de espera' : ''})</option>`).join('');
  $('#btn-inscribir').disabled = !evento.tarifaVigente;
}

$('#form-inscripcion').addEventListener('submit', async (e) => {
  e.preventDefault();
  const boton = $('#btn-inscribir');
  boton.disabled = true;
  try {
    const r = await api('/api/inscripciones', {
      method: 'POST',
      body: JSON.stringify({
        categoriaId: Number($('#ins-categoria').value),
        nombres: $('#ins-nombres').value,
        documento: $('#ins-documento').value,
        fechaNacimiento: $('#ins-nacimiento').value,
        genero: $('#ins-genero').value,
        email: $('#ins-email').value,
      }),
    });
    mostrarResultadoInscripcion(r, $('#ins-documento').value);
    $('#mis-documento').value = $('#ins-documento').value;
    cargarInscripciones();
    setTimeout(cargarCorreos, 500);
  } catch (err) {
    $('#resultado-inscripcion').innerHTML = `<div class="aviso error">${esc(err.message)}</div>`;
  } finally {
    boton.disabled = false;
  }
});

function mostrarResultadoInscripcion(r, documento) {
  clearInterval(ins.cuentaRegresiva);
  const caja = $('#resultado-inscripcion');
  if (r.estado === 'EN_ESPERA') {
    caja.innerHTML = `<div class="aviso espera"><b>Lista de espera · puesto ${r.posicionEspera}</b>
      <span>${esc(r.mensaje)}. Te avisaremos por correo si se libera un cupo.</span></div>`;
    return;
  }
  caja.innerHTML = `<div class="aviso ok">
      <b>Cupo reservado · inscripción #${r.inscripcionId}</b>
      <span>${esc(r.tarifa)}: ${formatoSoles(r.monto)} · tiempo para pagar: <span class="cuenta" id="cuenta">—</span></span>
      <div class="botones sin-margen"><button id="btn-pagar">Pagar ahora</button></div>
    </div>`;
  const fin = Date.parse(r.expiraEn);
  const tick = () => {
    const resta = Math.max(0, fin - Date.now());
    const el = $('#cuenta');
    if (!el) return clearInterval(ins.cuentaRegresiva);
    el.textContent = `${Math.floor(resta / 60000)}:${String(Math.floor((resta % 60000) / 1000)).padStart(2, '0')}`;
    if (resta === 0) clearInterval(ins.cuentaRegresiva);
  };
  tick();
  ins.cuentaRegresiva = setInterval(tick, 1000);
  $('#btn-pagar').onclick = () => pagar(r.inscripcionId, documento);
}

async function pagar(inscripcionId, documento) {
  try {
    const cobro = await api(`/api/inscripciones/${inscripcionId}/pago?documento=${encodeURIComponent(documento)}`, { method: 'POST' });
    window.open(cobro.urlCheckout, '_blank', 'noopener');
  } catch (err) {
    alert(err.message);
  }
}

/* ---------- mis inscripciones e historial ---------- */
$('#form-mis-inscripciones').addEventListener('submit', (e) => {
  e.preventDefault();
  cargarMisInscripciones($('#mis-documento').value.trim());
});

async function cargarMisInscripciones(documento) {
  const lista = await api(`/api/inscripciones?documento=${encodeURIComponent(documento)}`);
  $('#mis-inscripciones').innerHTML = lista.length ? lista.map((i) => `
    <li class="${i.estado === 'CONFIRMADA' ? 'meta' : i.estado === 'EN_ESPERA' ? 'OBSERVADO' : ''}">
      <b>${esc(i.evento)}</b> · ${esc(i.categoria)}
      <small>#${i.id} · ${esc(etiquetaInscripcion(i))}${i.dorsal ? ` · dorsal ${i.dorsal}` : ''}</small>
      <div class="botones sin-margen">
        ${i.estado === 'PENDIENTE_PAGO' ? `<button class="mini" data-pagar="${i.id}">Pagar ${formatoSoles(i.monto)}</button>` : ''}
        ${i.estado === 'CONFIRMADA' ? `<button class="mini secundario" data-kit="${i.id}">Ver kit QR</button>` : ''}
      </div>
    </li>`).join('') : '<li>No hay inscripciones con ese documento.</li>';
  $('#mis-inscripciones').dataset.documento = documento;
  cargarHistorial(documento);
}

function etiquetaInscripcion(i) {
  switch (i.estado) {
    case 'PENDIENTE_PAGO': return `pendiente de pago hasta ${new Date(i.expiraEn).toLocaleTimeString('es-PE')}`;
    case 'EN_ESPERA': return `lista de espera, puesto ${i.posicionEspera}`;
    case 'CONFIRMADA': return 'confirmada';
    case 'EXPIRADA': return 'expirada (no se pagó a tiempo)';
    case 'ANULADA': return 'anulada y reembolsada';
    default: return i.estado;
  }
}

$('#mis-inscripciones').addEventListener('click', (e) => {
  const documento = $('#mis-inscripciones').dataset.documento;
  const pagarBtn = e.target.closest('[data-pagar]');
  if (pagarBtn) pagar(pagarBtn.dataset.pagar, documento);
  const kitBtn = e.target.closest('[data-kit]');
  if (kitBtn) mostrarKit(kitBtn.dataset.kit, documento);
});

async function mostrarKit(id, documento) {
  const d = await api(`/api/inscripciones/${id}?documento=${encodeURIComponent(documento)}`);
  const img = document.createElement('img');
  img.alt = `Código QR del dorsal ${d.dorsal}`;
  img.src = `/api/inscripciones/${id}/qr.png?documento=${encodeURIComponent(documento)}`;
  $('#kit-qr').replaceChildren(img);
  $('#kit-detalle').innerHTML = `<b>${esc(d.atleta)}</b><br>${esc(d.evento)} · ${esc(d.categoria)}<br>
    Dorsal <b>${d.dorsal}</b> · Chip ${esc(d.chipId)} · Código ${esc(d.codigoQr)}`;
  $('#dialogo-kit').showModal();
}
$('#btn-cerrar-kit').addEventListener('click', () => $('#dialogo-kit').close());

async function cargarHistorial(documento) {
  const caja = $('#historial');
  try {
    const h = await api(`/api/resultados/atletas/${encodeURIComponent(documento)}/historial`);
    if (!h.resultados.length) {
      caja.innerHTML = '<p class="ayuda">Aún no hay resultados de carreras.</p>';
      return;
    }
    caja.innerHTML = `<h2>Historial de ${esc(h.atleta)}</h2>
      <p class="ayuda">${h.finalizados} de ${h.eventos} carreras finalizadas · mejor ritmo ${formatoRitmo(h.mejorRitmoSegundosPorKm)}</p>
      <div class="tabla-contenedor"><table class="tabla">
        <thead><tr><th>Evento</th><th>Categoría</th><th class="num">Tiempo neto</th><th class="num">Ritmo</th>
          <th class="num">Pos. gral.</th><th class="num">Pos. cat.</th><th>Estado</th></tr></thead>
        <tbody>${h.resultados.map((r) => `<tr>
          <td>${esc(r.evento)}</td><td>${esc(r.categoria)}</td>
          <td class="num">${formatoTiempo(r.tiempoNetoMs)}</td><td class="num">${formatoRitmo(r.ritmoSegundosPorKm)}</td>
          <td class="num">${r.posicionGeneral ?? '—'}</td><td class="num">${r.posicionCategoria ?? '—'}</td>
          <td><span class="estado ${r.estado}">${etiquetaEstado(r.estado)}</span></td></tr>`).join('')}
        </tbody></table></div>`;
  } catch {
    caja.innerHTML = '';
  }
}

async function cargarCorreos() {
  const correos = await api('/api/correos').catch(() => []);
  $('#correos').innerHTML = correos.length
    ? correos.slice(0, 15).map((c) => `<li><b>${esc(c.asunto)}</b> → ${esc(c.destinatario)}<small>${esc(c.cuerpo)}</small></li>`).join('')
    : '<li>Sin correos enviados todavía.</li>';
}

/* Al volver de la pasarela, refresca el estado. */
window.addEventListener('focus', () => {
  if (panelActivo() !== 'inscripciones') return;
  const documento = $('#mis-inscripciones').dataset.documento || $('#ins-documento').value;
  if (documento) cargarMisInscripciones(documento).catch(() => {});
  cargarCorreos();
});
