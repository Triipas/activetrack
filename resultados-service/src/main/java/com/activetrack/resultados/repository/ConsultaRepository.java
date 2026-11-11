package com.activetrack.resultados.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Consultas de solo lectura sobre los datos del evento (inscripciones, puntos de control, lecturas).
 * Se usa JDBC directo porque son lecturas puntuales que no necesitan entidades JPA.
 */
@Repository
public class ConsultaRepository {

    public record InscripcionInfo(Long inscripcionId, Long atletaId, String nombre, int dorsal,
                                  Long categoriaId, String categoria, Long eventoId, Instant horaSalidaOficial) {
    }

    public record PuntoInfo(Long id, Long rutaId, String nombre, double km, int orden, boolean esMeta) {
    }

    public record EventoResumen(Long id, String nombre, String tipo, Instant fecha) {
    }

    public record CategoriaResumen(Long id, String nombre) {
    }

    public record AtletaResumen(Long inscripcionId, Long atletaId, int dorsal, String nombre,
                                String categoria, String chipId) {
    }

    private static final String SQL_INSCRIPCION = """
            SELECT i.id, a.id AS atleta_id, a.nombres, i.dorsal, c.id AS categoria_id, c.nombre AS categoria,
                   e.id AS evento_id, e.fecha
            FROM inscripcion i
            JOIN atleta a ON a.id = i.atleta_id
            JOIN categoria c ON c.id = i.categoria_id
            JOIN evento e ON e.id = c.evento_id
            WHERE i.id = ?
            """;

    private static final String FILTRO_EVENTO = """
            SELECT i.id FROM inscripcion i JOIN categoria c ON c.id = i.categoria_id WHERE c.evento_id = ?
            """;

    private final JdbcTemplate jdbc;

    public ConsultaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public InscripcionInfo inscripcion(Long inscripcionId) {
        try {
            return jdbc.queryForObject(SQL_INSCRIPCION, (rs, n) -> new InscripcionInfo(
                    rs.getLong("id"), rs.getLong("atleta_id"), rs.getString("nombres"), rs.getInt("dorsal"),
                    rs.getLong("categoria_id"), rs.getString("categoria"), rs.getLong("evento_id"),
                    instante(rs, "fecha")), inscripcionId);
        } catch (EmptyResultDataAccessException e) {
            throw new NoSuchElementException("Inscripción no encontrada: " + inscripcionId);
        }
    }

    public PuntoInfo punto(Long puntoId) {
        try {
            return jdbc.queryForObject(
                    "SELECT id, ruta_id, nombre, km, orden, es_meta FROM punto_control WHERE id = ?",
                    (rs, n) -> new PuntoInfo(rs.getLong("id"), rs.getLong("ruta_id"), rs.getString("nombre"),
                            rs.getDouble("km"), rs.getInt("orden"), rs.getBoolean("es_meta")),
                    puntoId);
        } catch (EmptyResultDataAccessException e) {
            throw new NoSuchElementException("Punto de control no encontrado: " + puntoId);
        }
    }

    /** Orden y nombre de los puntos de una ruta, ordenados. */
    public Map<Integer, String> puntosDeRuta(Long rutaId) {
        Map<Integer, String> puntos = new LinkedHashMap<>();
        jdbc.query("SELECT orden, nombre FROM punto_control WHERE ruta_id = ? ORDER BY orden",
                rs -> {
                    puntos.put(rs.getInt("orden"), rs.getString("nombre"));
                }, rutaId);
        return puntos;
    }

    /** Órdenes de los puntos de control que el atleta ya cruzó. */
    public List<Integer> ordenesRecorridos(Long inscripcionId) {
        return jdbc.queryForList("""
                SELECT p.orden FROM lectura_rfid l JOIN punto_control p ON p.id = l.punto_control_id
                WHERE l.inscripcion_id = ?
                """, Integer.class, inscripcionId);
    }

    /** Momento en que el chip pisó la alfombra de salida (base del tiempo neto). */
    public Optional<Instant> marcaSalida(Long inscripcionId) {
        List<Instant> marcas = jdbc.query("""
                SELECT l.marca_tiempo FROM lectura_rfid l JOIN punto_control p ON p.id = l.punto_control_id
                WHERE l.inscripcion_id = ? AND p.orden = 0
                """, (rs, n) -> instante(rs, "marca_tiempo"), inscripcionId);
        return marcas.stream().findFirst();
    }

    public Optional<EventoResumen> evento(Long eventoId) {
        return jdbc.query("SELECT id, nombre, tipo, fecha FROM evento WHERE id = ?",
                (rs, n) -> new EventoResumen(rs.getLong("id"), rs.getString("nombre"), rs.getString("tipo"),
                        instante(rs, "fecha")), eventoId).stream().findFirst();
    }

    public List<CategoriaResumen> categorias(Long eventoId) {
        return jdbc.query("SELECT id, nombre FROM categoria WHERE evento_id = ? ORDER BY id",
                (rs, n) -> new CategoriaResumen(rs.getLong("id"), rs.getString("nombre")), eventoId);
    }

    public List<AtletaResumen> atletas(Long eventoId) {
        return jdbc.query("""
                SELECT i.id, a.id AS atleta_id, i.dorsal, a.nombres, c.nombre AS categoria, i.chip_id
                FROM inscripcion i
                JOIN atleta a ON a.id = i.atleta_id
                JOIN categoria c ON c.id = i.categoria_id
                WHERE c.evento_id = ? AND i.estado = 'CONFIRMADA'
                ORDER BY i.dorsal
                """, (rs, n) -> new AtletaResumen(rs.getLong("id"), rs.getLong("atleta_id"), rs.getInt("dorsal"),
                rs.getString("nombres"), rs.getString("categoria"), rs.getString("chip_id")), eventoId);
    }

    public List<Long> inscripcionesObservadas(Long eventoId) {
        return jdbc.queryForList("""
                SELECT r.inscripcion_id FROM resultado r
                JOIN inscripcion i ON i.id = r.inscripcion_id
                JOIN categoria c ON c.id = i.categoria_id
                WHERE c.evento_id = ? AND r.estado = 'OBSERVADO'
                ORDER BY i.dorsal
                """, Long.class, eventoId);
    }

    public List<Long> inscripcionesDeEvento(Long eventoId) {
        return jdbc.queryForList(FILTRO_EVENTO, Long.class, eventoId);
    }

    public List<Long> categoriasDeEvento(Long eventoId) {
        return jdbc.queryForList("SELECT id FROM categoria WHERE evento_id = ?", Long.class, eventoId);
    }

    /** Fila de resultado con posiciones calculadas (general y por categoría). */
    public record FilaResultado(Long eventoId, String evento, Instant fecha, String atleta, String documento,
                                String categoria, Integer dorsal, String estado, String ultimoPunto, Double km,
                                Long tiempoNetoMs, Long tiempoBrutoMs, Long posicionGeneral, Long posicionCategoria) {
    }

    /**
     * Ranking: primero quien llegó más lejos y luego el menor tiempo neto. Los descalificados no tienen posición.
     */
    private static final String SQL_RESULTADOS = """
            WITH ranking AS (
                SELECT r.inscripcion_id,
                       RANK() OVER (PARTITION BY c.evento_id ORDER BY r.ultimo_punto_orden DESC, r.tiempo_neto_ms) AS pos_general,
                       RANK() OVER (PARTITION BY i.categoria_id ORDER BY r.ultimo_punto_orden DESC, r.tiempo_neto_ms) AS pos_categoria
                FROM resultado r
                JOIN inscripcion i ON i.id = r.inscripcion_id
                JOIN categoria c ON c.id = i.categoria_id
                WHERE r.estado <> 'DSQ'
            )
            SELECT e.id AS evento_id, e.nombre AS evento, e.fecha, a.nombres, a.documento, c.nombre AS categoria,
                   i.dorsal, r.estado, p.nombre AS ultimo_punto, p.km, r.tiempo_neto_ms, r.tiempo_bruto_ms,
                   rk.pos_general, rk.pos_categoria
            FROM resultado r
            JOIN inscripcion i ON i.id = r.inscripcion_id
            JOIN atleta a ON a.id = i.atleta_id
            JOIN categoria c ON c.id = i.categoria_id
            JOIN evento e ON e.id = c.evento_id
            LEFT JOIN punto_control p ON p.id = r.ultimo_punto_id
            LEFT JOIN ranking rk ON rk.inscripcion_id = r.inscripcion_id
            """;

    public List<FilaResultado> resultadosDeAtleta(String documento) {
        return jdbc.query(SQL_RESULTADOS + " WHERE a.documento = ? ORDER BY e.fecha DESC",
                (rs, n) -> filaResultado(rs), documento);
    }

    public List<FilaResultado> resultadosDeEvento(Long eventoId) {
        return jdbc.query(SQL_RESULTADOS + " WHERE e.id = ? ORDER BY rk.pos_general NULLS LAST, i.dorsal",
                (rs, n) -> filaResultado(rs), eventoId);
    }

    public Optional<String> nombreAtleta(String documento) {
        return jdbc.queryForList("SELECT nombres FROM atleta WHERE documento = ?", String.class, documento)
                .stream().findFirst();
    }

    private static FilaResultado filaResultado(ResultSet rs) throws SQLException {
        return new FilaResultado(rs.getLong("evento_id"), rs.getString("evento"), instante(rs, "fecha"),
                rs.getString("nombres"), rs.getString("documento"), rs.getString("categoria"),
                (Integer) rs.getObject("dorsal"), rs.getString("estado"), rs.getString("ultimo_punto"),
                rs.getObject("km") == null ? null : rs.getDouble("km"),
                (Long) rs.getObject("tiempo_neto_ms"), (Long) rs.getObject("tiempo_bruto_ms"),
                (Long) rs.getObject("pos_general"), (Long) rs.getObject("pos_categoria"));
    }

    /** Solo para la demo: borra resultados y penalizaciones del evento. */
    public void borrarResultados(Long eventoId) {
        jdbc.update("DELETE FROM penalizacion WHERE resultado_id IN "
                + "(SELECT id FROM resultado WHERE inscripcion_id IN (" + FILTRO_EVENTO + "))", eventoId);
        jdbc.update("DELETE FROM resultado WHERE inscripcion_id IN (" + FILTRO_EVENTO + ")", eventoId);
    }

    private static Instant instante(ResultSet rs, String columna) throws SQLException {
        OffsetDateTime valor = rs.getObject(columna, OffsetDateTime.class);
        return valor == null ? null : valor.toInstant();
    }
}
