/**
 * HU-22 · Prueba de carga de la apertura de inscripciones (k6).
 *
 * Simula 10 000 personas intentando inscribirse en una categoría con 2 000 cupos.
 * Criterios de aceptación:
 *   - 0 errores (http_req_failed < 1 %)
 *   - p95 de la inscripción menor a 1 s
 *   - ningún cupo sobrevendido: exactamente 2 000 reservas y el resto en lista de espera
 *
 * Ejecución (con el sistema levantado por docker compose), escenario "requisito" o "estres":
 *   docker run --rm --network activetrack_default -v "%cd%/pruebas-carga:/scripts" grafana/k6:0.54.0 \
 *     run -e BASE_URL=http://inscripciones-service:8084 -e ESCENARIO=requisito /scripts/inscripcion-10k.js
 */
import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'http://localhost:8084';
const TOTAL = Number(__ENV.TOTAL || 10000);
const CUPO = Number(__ENV.CUPO || 2000);
const VUS = Number(__ENV.VUS || 500);
const ESCENARIO = __ENV.ESCENARIO || 'requisito';
const JSON_HEADERS = { 'Content-Type': 'application/json' };

const reservadas = new Counter('inscripciones_reservadas');
const enEspera = new Counter('inscripciones_en_espera');

/*
 * requisito: 10 000 personas llegan en los primeros minutos de la apertura, con un pico de 120 inscripciones
 *            por segundo (más exigente que el promedio del enunciado: 10 000 en 5 min ≈ 33 por segundo).
 * estres:    las 10 000 solicitudes lo más rápido posible con 500 usuarios simultáneos (busca el límite).
 */
const ESCENARIOS = {
  requisito: {
    executor: 'ramping-arrival-rate', startRate: 0, timeUnit: '1s', preAllocatedVUs: 200, maxVUs: 1000,
    stages: [
      { target: 120, duration: '30s' },
      { target: 120, duration: '60s' },
      { target: 0, duration: '20s' },
    ],
  },
  estres: { executor: 'shared-iterations', vus: VUS, iterations: TOTAL, maxDuration: '5m' },
};

export const options = {
  scenarios: { apertura: ESCENARIOS[ESCENARIO] },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{tipo:inscripcion}': ['p(95)<1000'],
    checks: ['rate>0.99'],
  },
};

export function setup() {
  const login = http.post(`${BASE}/api/auth/login`,
    JSON.stringify({ usuario: 'organizador', clave: 'organizador123' }), { headers: JSON_HEADERS });
  const token = login.json('token');
  const auth = { headers: { ...JSON_HEADERS, Authorization: `Bearer ${token}` } };
  const run = String(Date.now()).slice(-5);

  const evento = http.post(`${BASE}/api/admin/eventos`, JSON.stringify({
    nombre: `Prueba de carga ${run}`,
    tipo: 'CARRERA',
    fecha: '2027-08-01T12:00:00Z',
    ruta: '5K',
    distanciaKm: 5,
    puntos: [
      { nombre: 'Salida', km: 0, antenaId: `CARGA${run}-S` },
      { nombre: 'Meta', km: 5, antenaId: `CARGA${run}-M` },
    ],
    categorias: [{ nombre: 'Open', edadMin: 18, edadMax: 99, genero: 'X', cupoMaximo: CUPO }],
    tarifas: [{ nombre: 'Única', precio: 50, vigenteDesde: '2026-01-01', vigenteHasta: '2027-07-31' }],
  }), auth);
  check(evento, { 'evento de prueba creado': (r) => r.status === 201 });

  const eventoId = evento.json('id');
  const categoriaId = http.get(`${BASE}/api/admin/eventos/${eventoId}`, auth).json('categorias')[0].id;
  return { token, eventoId, categoriaId, run };
}

export default function (data) {
  const n = exec.scenario.iterationInTest;
  if (n >= TOTAL) return; // el escenario por tasa puede generar unas iteraciones de más
  const res = http.post(`${BASE}/api/inscripciones`, JSON.stringify({
    categoriaId: data.categoriaId,
    nombres: `Atleta de carga ${n}`,
    documento: `L${data.run}-${n}`,
    fechaNacimiento: '1990-01-01',
    genero: n % 2 === 0 ? 'M' : 'F',
    email: `atleta${n}@carga.test`,
  }), { headers: JSON_HEADERS, tags: { tipo: 'inscripcion' } });

  check(res, { 'inscripción aceptada (202)': (r) => r.status === 202 });
  const estado = res.status === 202 ? res.json('estado') : null;
  if (estado === 'PENDIENTE_PAGO') reservadas.add(1);
  if (estado === 'EN_ESPERA') enEspera.add(1);
}

export function teardown(data) {
  const auth = { headers: { Authorization: `Bearer ${data.token}` } };
  const categoria = http.get(`${BASE}/api/admin/eventos/${data.eventoId}`, auth).json('categorias')[0];
  console.log(`Resumen en base de datos: ${JSON.stringify(categoria)}`);
  check(categoria, {
    'sin sobreventa: reservas = cupo': (c) => c.pendientesPago + c.confirmadas === CUPO,
    'todas las solicitudes registradas': (c) => c.pendientesPago + c.confirmadas + c.enEspera === TOTAL,
    'contador de cupos en 0': (c) => c.disponibles === 0,
  });
}
