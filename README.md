# ActiveTrack · Prueba de Concepto

Plataforma de cronometraje de eventos deportivos construida con una **Arquitectura Orientada a Eventos (EDA)**
y el patrón de diseño **Observer**. Esta PoC implementa el módulo de **procesamiento de tiempos**: desde la
lectura de la antena RFID hasta la notificación al espectador, con una latencia objetivo de **2 segundos como máximo**.

![Arquitectura](docs/arquitectura.png)

## Arquitectura

| Contenedor | Tecnología | Rol en la EDA | Puerto |
|---|---|---|---|
| `cronometraje-service` | Java 17, Spring Boot 3 | **Productor**: recibe lecturas RFID y publica `AtletaCruzoPuntoControl` | 8081 |
| `resultados-service` | Java 17, Spring Boot 3 | **Consumidor/productor**: calcula tiempos, actualiza el leaderboard (Redis) y publica `ResultadoActualizado` | 8082 |
| `notificaciones-service` | Java 17, Spring Boot 3, WebSocket | **Consumidor**: avisa a los espectadores (patrón Observer) | 8083 |
| `frontend` | HTML, CSS, JavaScript (Nginx) | Leaderboard en vivo, seguimiento, panel del juez y simulador RFID | 8080 |
| `rabbitmq` | RabbitMQ 3.13 | Broker de eventos (colas durables + DLQ) | 5672 / 15672 |
| `postgres` | PostgreSQL 16 | Datos transaccionales | 5432 |
| `redis` | Redis 7 | Leaderboard (Sorted Set) | 6379 |

Flujo de un cruce:

```
Antena RFID ──POST /api/lecturas──▶ Cronometraje ──AtletaCruzoPuntoControl──▶ RabbitMQ
                    ◀── 202 Accepted (no espera cálculos)                          │
                                                                                     ▼
Espectador ◀──WebSocket── Notificaciones ◀──ResultadoActualizado── RabbitMQ ◀── Resultados ──▶ Redis
              (Observer)                                                         (tiempos, disputas)
```

### Patrón Observer (`notificaciones-service`, paquete `observer`)

| Rol GoF | Clase |
|---|---|
| Subject | `AtletaSeguido` |
| Observer | `SeguidorAtleta` (interfaz) |
| ConcreteObserver | `WebSocketSeguidor`, `PushSeguidor` |
| Client | `ResultadoEventListener` |

## Estructura del repositorio

```
activetrack/
├── common-events/            Contratos de eventos compartidos (records Java)
├── cronometraje-service/     controller · service · model · repository · messaging · config
├── resultados-service/       controller · service · model · repository · messaging · config
├── notificaciones-service/   controller · service · observer · messaging · config
├── frontend/                 index.html · styles.css · app.js · nginx.conf
├── db/init.sql               Esquema y datos semilla (1 evento, 12 atletas ficticios)
├── docs/                     Diagramas de arquitectura (PlantUML + PNG)
├── Dockerfile                Imagen de los servicios (se elige el módulo con MODULE)
└── docker-compose.yml
```

## Cómo ejecutarlo

Requisito: **Docker Desktop**.

```bash
docker compose up --build
```

Luego abrir **http://localhost:8080**:

1. Pestaña **Seguir atletas**: seguir a uno o más atletas (por ejemplo, el dorsal 107).
2. Pestaña **Simulador RFID**: *Iniciar carrera*. El simulador reemplaza al hardware y envía las lecturas.
3. Pestaña **En vivo**: el leaderboard se reordena solo y muestra la latencia de cada actualización.
4. Pestaña **Juez**: aparece la anomalía del dorsal 107 (omitió el control 10K); aplicar *Sin sanción*, *+5 min* o *DSQ*.

Consola de RabbitMQ: http://localhost:15672 (usuario y clave `guest`).

## Pruebas

Pruebas unitarias (JUnit 5 + Mockito), no necesitan Docker:

```bash
./mvnw test
```

En Windows: `mvnw.cmd test`.

## API principal

| Método | Ruta | Servicio | Descripción |
|---|---|---|---|
| POST | `/api/lecturas` | Cronometraje | Lectura RFID `{chipId, antenaId, marcaTiempo?}` → 202 |
| GET | `/api/antenas?eventoId=1` | Cronometraje | Estado y última señal de cada antena |
| GET | `/api/eventos/{id}/leaderboard?categoriaId=` | Resultados | Leaderboard desde Redis |
| GET | `/api/eventos/{id}/anomalias` | Resultados | Atletas con puntos de control omitidos |
| POST | `/api/resultados/{inscripcionId}/penalizaciones` | Resultados | Decisión del juez `{tipo: TIEMPO\|DSQ\|SIN_SANCION, segundos, motivo, juez}` |
| WS | `/ws/seguimiento` | Notificaciones | `{"accion":"seguir","atletaId":7}` · `{"accion":"leaderboard"}` |
| POST | `/api/seguimientos/push` | Notificaciones | Suscribe un token push a un atleta |

## Despliegue en la nube (AWS)

Cada servicio es una imagen Docker independiente, por lo que se despliega igual en AWS ECS (Fargate) detrás de un
Application Load Balancer, con Amazon MQ (RabbitMQ), Amazon RDS (PostgreSQL) y ElastiCache (Redis). Solo cambian
las variables de entorno `DB_URL`, `DB_USER`, `DB_PASSWORD`, `RABBIT_HOST`, `RABBIT_USER`, `RABBIT_PASSWORD` y `REDIS_HOST`.
