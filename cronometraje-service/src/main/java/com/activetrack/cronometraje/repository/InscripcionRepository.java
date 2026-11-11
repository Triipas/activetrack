package com.activetrack.cronometraje.repository;

import com.activetrack.cronometraje.model.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    Optional<Inscripcion> findByChipId(String chipId);
}
