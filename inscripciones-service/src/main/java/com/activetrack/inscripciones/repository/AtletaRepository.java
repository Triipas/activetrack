package com.activetrack.inscripciones.repository;

import com.activetrack.inscripciones.model.Atleta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AtletaRepository extends JpaRepository<Atleta, Long> {

    Optional<Atleta> findByDocumento(String documento);
}
