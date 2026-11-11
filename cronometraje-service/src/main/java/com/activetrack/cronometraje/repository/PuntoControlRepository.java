package com.activetrack.cronometraje.repository;

import com.activetrack.cronometraje.model.PuntoControl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PuntoControlRepository extends JpaRepository<PuntoControl, Long> {

    Optional<PuntoControl> findByAntenaId(String antenaId);
}
