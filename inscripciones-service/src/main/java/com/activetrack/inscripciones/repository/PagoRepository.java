package com.activetrack.inscripciones.repository;

import com.activetrack.inscripciones.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByReferenciaPasarela(String referencia);

    Optional<Pago> findFirstByInscripcionIdAndEstado(Long inscripcionId, Pago.Estado estado);
}
