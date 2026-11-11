package com.activetrack.inscripciones.repository;

import com.activetrack.inscripciones.model.PuntoControl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PuntoControlRepository extends JpaRepository<PuntoControl, Long> {

    List<PuntoControl> findByRutaIdOrderByOrden(Long rutaId);

    boolean existsByAntenaIdIn(Collection<String> antenas);
}
