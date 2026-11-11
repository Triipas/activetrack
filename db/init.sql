-- =====================================================================
-- ActiveTrack - Esquema y datos semilla (PostgreSQL)
-- Se ejecuta automáticamente al crear el contenedor de PostgreSQL.
-- =====================================================================

CREATE TABLE evento (
    id      BIGSERIAL PRIMARY KEY,
    nombre  VARCHAR(150) NOT NULL,
    tipo    VARCHAR(20)  NOT NULL,           -- MARATON, TRIATLON, CICLISMO
    fecha   TIMESTAMPTZ  NOT NULL,           -- hora oficial de largada
    estado  VARCHAR(20)  NOT NULL            -- PROGRAMADO, EN_CURSO, FINALIZADO
);

CREATE TABLE ruta (
    id            BIGSERIAL PRIMARY KEY,
    evento_id     BIGINT NOT NULL REFERENCES evento(id),
    nombre        VARCHAR(100) NOT NULL,
    distancia_km  NUMERIC(6,3) NOT NULL
);

CREATE TABLE punto_control (
    id         BIGSERIAL PRIMARY KEY,
    ruta_id    BIGINT NOT NULL REFERENCES ruta(id),
    nombre     VARCHAR(50) NOT NULL,
    km         NUMERIC(6,3) NOT NULL,
    orden      INT NOT NULL,
    es_meta    BOOLEAN NOT NULL DEFAULT FALSE,
    antena_id  VARCHAR(30) NOT NULL UNIQUE,
    UNIQUE (ruta_id, orden)
);

CREATE TABLE categoria (
    id           BIGSERIAL PRIMARY KEY,
    evento_id    BIGINT NOT NULL REFERENCES evento(id),
    nombre       VARCHAR(80) NOT NULL,
    edad_min     INT NOT NULL,
    edad_max     INT NOT NULL,
    genero       CHAR(1) NOT NULL,
    cupo_maximo  INT NOT NULL
);

CREATE TABLE tarifa (
    id             BIGSERIAL PRIMARY KEY,
    evento_id      BIGINT NOT NULL REFERENCES evento(id),
    nombre         VARCHAR(50) NOT NULL,
    precio         NUMERIC(10,2) NOT NULL,
    vigente_desde  DATE NOT NULL,
    vigente_hasta  DATE NOT NULL
);

CREATE TABLE atleta (
    id                BIGSERIAL PRIMARY KEY,
    nombres           VARCHAR(150) NOT NULL,
    documento         VARCHAR(20) NOT NULL UNIQUE,
    fecha_nacimiento  DATE NOT NULL,
    genero            CHAR(1) NOT NULL,
    email             VARCHAR(120) NOT NULL
);

CREATE TABLE inscripcion (
    id            BIGSERIAL PRIMARY KEY,
    atleta_id     BIGINT NOT NULL REFERENCES atleta(id),
    categoria_id  BIGINT NOT NULL REFERENCES categoria(id),
    tarifa_id     BIGINT NOT NULL REFERENCES tarifa(id),
    dorsal        INT NOT NULL,
    chip_id       VARCHAR(30) NOT NULL UNIQUE,
    estado        VARCHAR(20) NOT NULL,      -- PENDIENTE_PAGO, CONFIRMADA, EXPIRADA, EN_ESPERA
    codigo_qr     VARCHAR(255),
    expira_en     TIMESTAMPTZ
);

CREATE TABLE pago (
    id                   BIGSERIAL PRIMARY KEY,
    inscripcion_id       BIGINT NOT NULL REFERENCES inscripcion(id),
    monto                NUMERIC(10,2) NOT NULL,
    referencia_pasarela  VARCHAR(80),
    estado               VARCHAR(20) NOT NULL,
    fecha                TIMESTAMPTZ NOT NULL
);

CREATE TABLE lectura_rfid (
    id                BIGSERIAL PRIMARY KEY,
    inscripcion_id    BIGINT NOT NULL REFERENCES inscripcion(id),
    punto_control_id  BIGINT NOT NULL REFERENCES punto_control(id),
    antena_id         VARCHAR(30) NOT NULL,
    marca_tiempo      TIMESTAMPTZ NOT NULL,  -- hora en que el atleta pisó la alfombra
    recibido_en       TIMESTAMPTZ NOT NULL,  -- hora en que el sistema recibió la señal
    UNIQUE (inscripcion_id, punto_control_id) -- la antena lee varias veces el mismo chip
);

CREATE TABLE resultado (
    id                  BIGSERIAL PRIMARY KEY,
    inscripcion_id      BIGINT NOT NULL UNIQUE REFERENCES inscripcion(id),
    ultimo_punto_id     BIGINT REFERENCES punto_control(id),
    ultimo_punto_orden  INT,
    ultima_marca        TIMESTAMPTZ,
    tiempo_bruto_ms     BIGINT,
    tiempo_neto_ms      BIGINT,
    posicion_general    INT,
    posicion_categoria  INT,
    estado              VARCHAR(20) NOT NULL  -- EN_CARRERA, OBSERVADO, FINALIZADO, DSQ
);

CREATE TABLE penalizacion (
    id            BIGSERIAL PRIMARY KEY,
    resultado_id  BIGINT NOT NULL REFERENCES resultado(id),
    tipo          VARCHAR(20) NOT NULL,       -- TIEMPO, DSQ, SIN_SANCION
    segundos      INT NOT NULL DEFAULT 0,
    motivo        VARCHAR(255) NOT NULL,
    juez          VARCHAR(100) NOT NULL,
    fecha         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_lectura_inscripcion ON lectura_rfid (inscripcion_id);
CREATE INDEX idx_inscripcion_categoria ON inscripcion (categoria_id);

-- =====================================================================
-- Datos semilla: un evento con 12 atletas inscritos (datos ficticios)
-- =====================================================================

INSERT INTO evento (nombre, tipo, fecha, estado)
VALUES ('Maratón Internacional Arequipa 2026', 'MARATON', '2026-11-15 06:00:00-05', 'EN_CURSO');

INSERT INTO ruta (evento_id, nombre, distancia_km) VALUES (1, 'Maratón 42K', 42.195);

INSERT INTO punto_control (ruta_id, nombre, km, orden, es_meta, antena_id) VALUES
    (1, 'Salida',      0.000, 0, FALSE, 'ANT-SALIDA'),
    (1, 'Control 10K', 10.000, 1, FALSE, 'ANT-10K'),
    (1, 'Control 21K', 21.097, 2, FALSE, 'ANT-21K'),
    (1, 'Meta',        42.195, 3, TRUE,  'ANT-META');

INSERT INTO categoria (evento_id, nombre, edad_min, edad_max, genero, cupo_maximo) VALUES
    (1, 'Varones 18-39', 18, 39, 'M', 2000),
    (1, 'Damas 18-39',   18, 39, 'F', 2000),
    (1, 'Varones 40+',   40, 99, 'M', 1000),
    (1, 'Damas 40+',     40, 99, 'F', 1000);

INSERT INTO tarifa (evento_id, nombre, precio, vigente_desde, vigente_hasta) VALUES
    (1, 'Early Bird', 120.00, '2026-06-01', '2026-08-31'),
    (1, 'Regular',    150.00, '2026-09-01', '2026-11-10');

INSERT INTO atleta (nombres, documento, fecha_nacimiento, genero, email) VALUES
    ('Carlos Mamani Quispe',    '70000001', '1995-03-12', 'M', 'carlos.mamani@example.com'),
    ('Luis Huamán Torres',      '70000002', '1992-07-25', 'M', 'luis.huaman@example.com'),
    ('Diego Paredes Salas',     '70000003', '1998-11-02', 'M', 'diego.paredes@example.com'),
    ('Ana Condori Flores',      '70000004', '1996-01-30', 'F', 'ana.condori@example.com'),
    ('María Rivera Chávez',     '70000005', '1993-05-18', 'F', 'maria.rivera@example.com'),
    ('Lucía Ramos Pinto',       '70000006', '2000-09-09', 'F', 'lucia.ramos@example.com'),
    ('Jorge Vargas Medina',     '70000007', '1980-04-21', 'M', 'jorge.vargas@example.com'),
    ('Ricardo Apaza Cruz',      '70000008', '1976-12-14', 'M', 'ricardo.apaza@example.com'),
    ('Pedro Ticona Vilca',      '70000009', '1983-08-03', 'M', 'pedro.ticona@example.com'),
    ('Rosa Gutiérrez Luna',     '70000010', '1979-02-27', 'F', 'rosa.gutierrez@example.com'),
    ('Elena Zapata Rojas',      '70000011', '1984-10-11', 'F', 'elena.zapata@example.com'),
    ('Carmen Delgado Ponce',    '70000012', '1975-06-06', 'F', 'carmen.delgado@example.com');

INSERT INTO inscripcion (atleta_id, categoria_id, tarifa_id, dorsal, chip_id, estado, codigo_qr) VALUES
    (1,  1, 1, 101, 'CHIP-101', 'CONFIRMADA', 'QR-101'),
    (2,  1, 1, 102, 'CHIP-102', 'CONFIRMADA', 'QR-102'),
    (3,  1, 2, 103, 'CHIP-103', 'CONFIRMADA', 'QR-103'),
    (4,  2, 1, 104, 'CHIP-104', 'CONFIRMADA', 'QR-104'),
    (5,  2, 2, 105, 'CHIP-105', 'CONFIRMADA', 'QR-105'),
    (6,  2, 1, 106, 'CHIP-106', 'CONFIRMADA', 'QR-106'),
    (7,  3, 1, 107, 'CHIP-107', 'CONFIRMADA', 'QR-107'),
    (8,  3, 2, 108, 'CHIP-108', 'CONFIRMADA', 'QR-108'),
    (9,  3, 1, 109, 'CHIP-109', 'CONFIRMADA', 'QR-109'),
    (10, 4, 2, 110, 'CHIP-110', 'CONFIRMADA', 'QR-110'),
    (11, 4, 1, 111, 'CHIP-111', 'CONFIRMADA', 'QR-111'),
    (12, 4, 2, 112, 'CHIP-112', 'CONFIRMADA', 'QR-112');

INSERT INTO pago (inscripcion_id, monto, referencia_pasarela, estado, fecha)
SELECT i.id, t.precio, 'SANDBOX-' || i.dorsal, 'APROBADO', '2026-08-15 10:00:00-05'
FROM inscripcion i JOIN tarifa t ON t.id = i.tarifa_id;
