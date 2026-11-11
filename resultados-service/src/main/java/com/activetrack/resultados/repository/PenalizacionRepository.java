package com.activetrack.resultados.repository;

import com.activetrack.resultados.model.Penalizacion;
import com.activetrack.resultados.model.TipoPenalizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PenalizacionRepository extends JpaRepository<Penalizacion, Long> {

    @Query("select coalesce(sum(p.segundos), 0) from Penalizacion p where p.resultadoId = :resultadoId and p.tipo = :tipo")
    long sumarSegundos(@Param("resultadoId") Long resultadoId, @Param("tipo") TipoPenalizacion tipo);
}
