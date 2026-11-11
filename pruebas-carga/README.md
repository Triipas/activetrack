# Prueba de carga · apertura de inscripciones (HU-22)

Script: [`inscripcion-10k.js`](inscripcion-10k.js) (k6). Crea un evento con una categoría de **2 000 cupos** y lanza
**10 000 solicitudes de inscripción** de atletas distintos. Al final verifica en la base de datos que no haya sobreventa.

## Cómo ejecutarla

Con el sistema levantado (`docker compose up -d`):

```bash
docker run --rm --network activetrack_default -v "$(pwd)/pruebas-carga:/scripts" grafana/k6:0.54.0 run -e BASE_URL=http://inscripciones-service:8084 -e ESCENARIO=requisito /scripts/inscripcion-10k.js
```

`ESCENARIO=requisito` reproduce el enunciado; `ESCENARIO=estres` busca el límite del sistema.

## Resultados (10/11/2026, una sola instancia de cada servicio en una laptop con Docker Desktop)

| Escenario | Carga | Errores | p95 inscripción | Reservas / en espera | Sobreventa |
|---|---|---|---|---|---|
| **Requisito** | 10 000 solicitudes en ~2 min, pico de 120/s (el enunciado equivale a ~33/s) | 0 % | **10 ms** | 2 000 / 8 000 | No |
| **Estrés** | 10 000 solicitudes en 17 s, 500 usuarios simultáneos (580/s) | 0 % | **1,49 s** | 2 000 / 8 000 | No |

Resúmenes completos de k6: [`resultado-requisito.json`](resultado-requisito.json) y [`resultado-estres.json`](resultado-estres.json).

## Conclusión

- **El requisito se cumple con holgura**: 10 000 usuarios en la apertura se atienden sin errores y con un p95 de 10 ms.
- **La reserva atómica en Redis evita la sobreventa** incluso con 500 solicitudes simultáneas: siempre quedan
  exactamente 2 000 reservas, aunque se procesen 580 por segundo.
- En el escenario de estrés el p95 sube a 1,49 s porque una sola instancia comparte la CPU de la laptop con la base de datos,
  Redis y RabbitMQ. Como el servicio no guarda estado (el cupo vive en Redis), se puede escalar horizontalmente
  agregando instancias detrás del balanceador.
