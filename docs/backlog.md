# Backlog del producto (Scrum)

Generado desde `scripts/backlog.json`. Cada historia se crea como issue en GitHub Projects con `node scripts/crear-backlog-github.mjs`.

## Sprint 1 - Arquitectura

Análisis, drivers arquitectónicos, diseño (4+1, C4, ADRs) y estructura del proyecto.

| ID | Historia | Requisito | Estado |
|---|---|---|---|
| HU-01 | Diseño arquitectónico documentado | Consigna | Hecho |
| HU-02 | Estructura del proyecto por servicios | Consigna | Hecho |

## Sprint 2 - Procesamiento de tiempos

PoC: cronometraje, resultados, disputas y notificaciones en tiempo real.

| ID | Historia | Requisito | Estado |
|---|---|---|---|
| HU-03 | Recibir lecturas RFID de las antenas | RF-07 | Hecho |
| HU-04 | Calcular tiempo bruto y neto | RF-08 | Hecho |
| HU-05 | Leaderboard en tiempo real | RF-09 | Hecho |
| HU-06 | Seguir atletas y recibir notificaciones | RF-10 | Hecho |
| HU-07 | Detectar anomalías y aplicar sanciones | RF-11 | Hecho |
| HU-08 | Monitorear el estado de las antenas | RF-12 | Hecho |
| HU-09 | Latencia máxima de 2 segundos | RNF-01 | Hecho |
| HU-10 | No perder lecturas ni resultados | RNF-03 | Hecho |

## Sprint 3 - Frontend, pruebas y despliegue

Interfaz web, pruebas, contenedores y despliegue en la nube.

| ID | Historia | Requisito | Estado |
|---|---|---|---|
| HU-11 | Interfaz web del evento | RF-09, RF-10, RF-11 | Hecho |
| HU-12 | Pruebas automatizadas | Consigna | Hecho |
| HU-13 | Ejecución en contenedores | RNF-04 | Hecho |
| HU-14 | Despliegue en la nube | RNF-04 | Pendiente |

## Backlog - Inscripciones y administración

Historias diseñadas pero fuera del alcance de la PoC.

| ID | Historia | Requisito | Estado |
|---|---|---|---|
| HU-15 | Gestionar eventos, rutas y categorías | RF-01 | Pendiente |
| HU-16 | Configurar cupos y precios dinámicos | RF-02 | Pendiente |
| HU-17 | Inscribirse con validación de cupo | RF-03, RF-05 | Pendiente |
| HU-18 | Pagar la inscripción | RF-04 | Pendiente |
| HU-19 | Descargar kit digital (QR) | RF-06 | Pendiente |
| HU-20 | Reembolsos y exportación de datos | RF-13 | Pendiente |
| HU-21 | Historial de resultados del atleta | RF-14 | Pendiente |
| HU-22 | Soportar 10 000 usuarios concurrentes en inscripción | RNF-02, RNF-07 | Pendiente |
| HU-23 | Autenticación por roles | RNF-06 | Pendiente |

## Detalle

### HU-01 · Diseño arquitectónico documentado

**Historia de usuario:** Como equipo de desarrollo quiero documentar la arquitectura con el Modelo 4+1, el Modelo C4 y ADRs para justificar las decisiones antes de implementar.

**Requisito:** Consigna (RNF)

**Criterios de aceptación:**
- [x] Informe con drivers funcionales, de calidad y restricciones
- [x] Diagramas 4+1 y C4 (contexto, contenedores, componentes)
- [x] ADRs de arquitectura, broker, base de datos y tiempo real

### HU-02 · Estructura del proyecto por servicios

**Historia de usuario:** Como desarrollador quiero un repositorio multi-módulo con un módulo por servicio y contratos de eventos compartidos para trabajar en paralelo sin acoplar los servicios.

**Requisito:** Consigna (RNF)

**Criterios de aceptación:**
- [x] Módulos common-events, cronometraje, resultados y notificaciones
- [x] Misma estructura de paquetes en cada servicio
- [x] Compila con el Maven Wrapper

### HU-03 · Recibir lecturas RFID de las antenas

**Historia de usuario:** Como antena RFID quiero enviar cada detección de chip al sistema para que el cruce del atleta quede registrado.

**Requisito:** RF-07 (RF)

**Criterios de aceptación:**
- [x] POST /api/lecturas responde 202 sin esperar cálculos
- [x] Las lecturas repetidas del mismo chip en el mismo punto se descartan
- [x] Chip o antena desconocidos se rechazan con 422
- [x] Se publica el evento AtletaCruzoPuntoControl

### HU-04 · Calcular tiempo bruto y neto

**Historia de usuario:** Como atleta quiero que mi tiempo se calcule desde la largada oficial (bruto) y desde que pisé la salida (neto) para conocer mi marca real.

**Requisito:** RF-08 (RF)

**Criterios de aceptación:**
- [x] Bruto = cruce - largada oficial
- [x] Neto = cruce - lectura de salida (o bruto si no hubo lectura)
- [x] Las penalidades de tiempo se suman a ambos

### HU-05 · Leaderboard en tiempo real

**Historia de usuario:** Como espectador quiero ver la tabla de posiciones general y por categoría actualizarse sola para seguir la carrera en vivo.

**Requisito:** RF-09 (RF)

**Criterios de aceptación:**
- [x] Ordena por punto más avanzado y luego por menor tiempo neto
- [x] Filtro por categoría
- [x] Los descalificados aparecen al final
- [x] Se actualiza sin recargar la página

### HU-06 · Seguir atletas y recibir notificaciones

**Historia de usuario:** Como espectador quiero seguir a uno o más atletas para recibir un aviso cada vez que crucen un punto de control.

**Requisito:** RF-10 (RF)

**Criterios de aceptación:**
- [x] Suscripción por WebSocket (patrón Observer)
- [x] Canal push mediante token de dispositivo
- [x] Quien deja de seguir ya no recibe avisos

### HU-07 · Detectar anomalías y aplicar sanciones

**Historia de usuario:** Como juez de ruta quiero ver los atletas que omitieron un punto de control y decidir una sanción para garantizar resultados justos.

**Requisito:** RF-11 (RF)

**Criterios de aceptación:**
- [x] Un atleta con punto omitido queda en estado OBSERVADO
- [x] El juez puede aplicar DSQ, penalidad de tiempo o sin sanción
- [x] El leaderboard y los seguidores se actualizan tras la decisión

### HU-08 · Monitorear el estado de las antenas

**Historia de usuario:** Como cronometrador quiero ver cuántas lecturas envió cada antena y cuándo fue su última señal para detectar fallas de conectividad.

**Requisito:** RF-12 (RF)

**Criterios de aceptación:**
- [x] Lista de antenas con punto de control y km
- [x] Total de lecturas y tiempo desde la última señal

### HU-09 · Latencia máxima de 2 segundos

**Historia de usuario:** Como espectador quiero recibir el tiempo del atleta en menos de 2 segundos desde que pisa la alfombra para vivir la carrera en tiempo real.

**Requisito:** RNF-01 (RNF)

**Criterios de aceptación:**
- [x] Procesamiento asíncrono por eventos (RabbitMQ)
- [x] Leaderboard en Redis
- [x] La interfaz muestra la latencia medida de cada actualización

### HU-10 · No perder lecturas ni resultados

**Historia de usuario:** Como organizador quiero que ninguna lectura se pierda aunque un servicio falle para confiar en los resultados oficiales.

**Requisito:** RNF-03 (RNF)

**Criterios de aceptación:**
- [x] Colas durables
- [x] Reintentos y cola de mensajes fallidos (DLQ)
- [x] Lecturas persistidas en PostgreSQL antes de publicar

### HU-11 · Interfaz web del evento

**Historia de usuario:** Como usuario quiero una interfaz web responsive con leaderboard, seguimiento, panel del juez y simulador para usar el sistema desde cualquier dispositivo.

**Requisito:** RF-09, RF-10, RF-11 (RF)

**Criterios de aceptación:**
- [x] HTML, CSS y JavaScript sin frameworks
- [x] Conexión WebSocket con reconexión automática
- [x] Usable en celular

### HU-12 · Pruebas automatizadas

**Historia de usuario:** Como equipo quiero pruebas unitarias de las reglas de negocio y del patrón Observer para detectar errores antes de entregar.

**Requisito:** Consigna (RNF)

**Criterios de aceptación:**
- [x] Pruebas de cálculo de tiempos, secuencia, leaderboard y estados
- [x] Pruebas del Observer (suscripción, baja, fallas de canal)
- [x] ./mvnw test en verde

### HU-13 · Ejecución en contenedores

**Historia de usuario:** Como equipo quiero levantar todo el sistema con un solo comando para probarlo y desplegarlo igual en cualquier entorno.

**Requisito:** RNF-04 (RNF)

**Criterios de aceptación:**
- [x] docker compose up --build levanta servicios, broker, bases de datos y frontend
- [x] Variables de entorno para la configuración

### HU-14 · Despliegue en la nube

**Historia de usuario:** Como organizador quiero el sistema desplegado en la nube para usarlo durante el evento real.

**Requisito:** RNF-04 (RNF)

**Criterios de aceptación:**
- [ ] Servicios en AWS ECS detrás de un balanceador
- [ ] Amazon MQ, RDS PostgreSQL y ElastiCache Redis
- [ ] URL pública del frontend

### HU-15 · Gestionar eventos, rutas y categorías

**Historia de usuario:** Como organizador quiero crear eventos con rutas, categorías y puntos de control para configurar cada competencia.

**Requisito:** RF-01 (RF)

**Criterios de aceptación:**
- [ ] CRUD de eventos, rutas, categorías y puntos de control
- [ ] Asignar una antena a cada punto de control

### HU-16 · Configurar cupos y precios dinámicos

**Historia de usuario:** Como organizador quiero definir cupos por categoría y tarifas por vigencia (Early Bird) para maximizar las inscripciones.

**Requisito:** RF-02 (RF)

**Criterios de aceptación:**
- [ ] Cupo máximo por categoría
- [ ] Tarifa vigente según la fecha de inscripción

### HU-17 · Inscribirse con validación de cupo

**Historia de usuario:** Como atleta quiero inscribirme y saber al instante si tengo cupo o quedo en lista de espera para asegurar mi participación.

**Requisito:** RF-03, RF-05 (RF)

**Criterios de aceptación:**
- [ ] Reserva atómica del cupo en Redis
- [ ] Reserva de 10 minutos para pagar
- [ ] Lista de espera cuando no hay cupo

### HU-18 · Pagar la inscripción

**Historia de usuario:** Como atleta quiero pagar con una pasarela externa para confirmar mi inscripción.

**Requisito:** RF-04 (RF)

**Criterios de aceptación:**
- [ ] Checkout externo en modo sandbox
- [ ] Webhook de pago confirma la inscripción
- [ ] Evento InscripcionConfirmada

### HU-19 · Descargar kit digital (QR)

**Historia de usuario:** Como atleta quiero descargar mi kit digital con código QR para recoger mi dorsal y chip.

**Requisito:** RF-06 (RF)

**Criterios de aceptación:**
- [ ] Código QR único por inscripción
- [ ] Disponible al confirmarse el pago

### HU-20 · Reembolsos y exportación de datos

**Historia de usuario:** Como organizador quiero emitir reembolsos y exportar los datos del evento para la gestión administrativa.

**Requisito:** RF-13 (RF)

**Criterios de aceptación:**
- [ ] Reembolso vía pasarela
- [ ] Exportación CSV de inscritos y resultados

### HU-21 · Historial de resultados del atleta

**Historia de usuario:** Como atleta quiero consultar mis resultados históricos y métricas para ver mi evolución.

**Requisito:** RF-14 (RF)

**Criterios de aceptación:**
- [ ] Lista de eventos con tiempos y posiciones
- [ ] Ritmo promedio por kilómetro

### HU-22 · Soportar 10 000 usuarios concurrentes en inscripción

**Historia de usuario:** Como organizador quiero que la apertura de inscripciones soporte 10 000 usuarios en 5 minutos sin caídas ni sobreventa para no perder ventas.

**Requisito:** RNF-02, RNF-07 (RNF)

**Criterios de aceptación:**
- [ ] Prueba de carga con k6 o JMeter
- [ ] 0 errores 5xx y respuesta ≤ 1 s
- [ ] Ningún cupo sobrevendido

### HU-23 · Autenticación por roles

**Historia de usuario:** Como organizador quiero que solo los jueces puedan aplicar sanciones y solo los organizadores configurar eventos para proteger la integridad del sistema.

**Requisito:** RNF-06 (RNF)

**Criterios de aceptación:**
- [ ] JWT con roles ATLETA, ORGANIZADOR, JUEZ
- [ ] Endpoints del juez protegidos
