package com.activetrack.cronometraje.repository;

import com.activetrack.cronometraje.model.LecturaRfid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface LecturaRfidRepository extends JpaRepository<LecturaRfid, Long> {

    boolean existsByInscripcionIdAndPuntoControlId(Long inscripcionId, Long puntoControlId);

    /** Estado de cada antena del evento: total de lecturas y última señal recibida. */
    @Query(value = """
            SELECT p.antena_id AS antenaId, p.nombre AS puntoControl, p.km AS km, p.orden AS orden,
                   COUNT(l.id) AS totalLecturas,
                   CAST(EXTRACT(EPOCH FROM MAX(l.recibido_en)) * 1000 AS BIGINT) AS ultimaSenalMs
            FROM punto_control p
            JOIN ruta r ON r.id = p.ruta_id
            LEFT JOIN lectura_rfid l ON l.punto_control_id = p.id
            WHERE r.evento_id = :eventoId
            GROUP BY p.id, p.antena_id, p.nombre, p.km, p.orden
            ORDER BY p.orden
            """, nativeQuery = true)
    List<EstadoAntenaView> estadoAntenas(@Param("eventoId") Long eventoId);

    /** Solo para la demo: borra las lecturas del evento. */
    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM lectura_rfid l USING punto_control p, ruta r
            WHERE l.punto_control_id = p.id AND p.ruta_id = r.id AND r.evento_id = :eventoId
            """, nativeQuery = true)
    int borrarPorEvento(@Param("eventoId") Long eventoId);
}
