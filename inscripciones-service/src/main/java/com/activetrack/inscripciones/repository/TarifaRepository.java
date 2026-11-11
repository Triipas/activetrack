package com.activetrack.inscripciones.repository;

import com.activetrack.inscripciones.model.Tarifa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TarifaRepository extends JpaRepository<Tarifa, Long> {

    List<Tarifa> findByEventoIdOrderByVigenteDesde(Long eventoId);
}
