package com.activetrack.inscripciones.repository;

import com.activetrack.inscripciones.model.Ruta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RutaRepository extends JpaRepository<Ruta, Long> {

    List<Ruta> findByEventoId(Long eventoId);
}
