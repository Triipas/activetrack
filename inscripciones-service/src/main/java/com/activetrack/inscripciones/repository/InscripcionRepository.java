package com.activetrack.inscripciones.repository;

import com.activetrack.inscripciones.model.EstadoInscripcion;
import com.activetrack.inscripciones.model.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    @Query("""
            select count(i) > 0 from Inscripcion i, Categoria c
            where c.id = i.categoriaId and c.eventoId = :eventoId
              and i.atletaId = :atletaId and i.estado in :estados
            """)
    boolean tieneInscripcion(@Param("atletaId") Long atletaId, @Param("eventoId") Long eventoId,
                             @Param("estados") Collection<EstadoInscripcion> estados);

    long countByCategoriaIdAndEstadoIn(Long categoriaId, Collection<EstadoInscripcion> estados);

    long countByCategoriaIdAndEstadoAndCreadaEnLessThan(Long categoriaId, EstadoInscripcion estado, Instant creadaEn);

    List<Inscripcion> findByEstadoAndExpiraEnBefore(EstadoInscripcion estado, Instant ahora);

    List<Inscripcion> findByAtletaIdOrderByCreadaEnDesc(Long atletaId);

    /** Primer atleta de la lista de espera; SKIP LOCKED evita que dos procesos promuevan al mismo. */
    @Query(value = """
            SELECT * FROM inscripcion
            WHERE categoria_id = :categoriaId AND estado = 'EN_ESPERA'
            ORDER BY creada_en, id
            LIMIT 1 FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<Inscripcion> primeraEnEspera(@Param("categoriaId") Long categoriaId);

    @Query(value = "SELECT nextval('dorsal_seq')", nativeQuery = true)
    int siguienteDorsal();

    /** Conteo por categoría y estado para el resumen del organizador. */
    @Query("""
            select i.categoriaId as categoriaId, i.estado as estado, count(i) as total
            from Inscripcion i, Categoria c
            where c.id = i.categoriaId and c.eventoId = :eventoId
            group by i.categoriaId, i.estado
            """)
    List<ConteoView> contarPorCategoria(@Param("eventoId") Long eventoId);

    @Query(value = """
            SELECT i.id AS id, i.dorsal AS dorsal, i.chip_id AS chipId, a.nombres AS atleta,
                   a.documento AS documento, a.email AS email, c.nombre AS categoria, i.estado AS estado,
                   t.nombre AS tarifa, t.precio AS precio,
                   CAST(EXTRACT(EPOCH FROM i.creada_en) * 1000 AS BIGINT) AS creadaEnMs
            FROM inscripcion i
            JOIN atleta a ON a.id = i.atleta_id
            JOIN categoria c ON c.id = i.categoria_id
            JOIN tarifa t ON t.id = i.tarifa_id
            WHERE c.evento_id = :eventoId
            ORDER BY i.creada_en DESC, i.id DESC
            LIMIT :limite
            """, nativeQuery = true)
    List<InscripcionAdminView> listarPorEvento(@Param("eventoId") Long eventoId, @Param("limite") int limite);

    interface ConteoView {
        Long getCategoriaId();
        EstadoInscripcion getEstado();
        Long getTotal();
    }

    interface InscripcionAdminView {
        Long getId();
        Integer getDorsal();
        String getChipId();
        String getAtleta();
        String getDocumento();
        String getEmail();
        String getCategoria();
        String getEstado();
        String getTarifa();
        java.math.BigDecimal getPrecio();
        Long getCreadaEnMs();
    }
}
