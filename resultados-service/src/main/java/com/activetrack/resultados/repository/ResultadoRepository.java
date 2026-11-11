package com.activetrack.resultados.repository;

import com.activetrack.resultados.model.Resultado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResultadoRepository extends JpaRepository<Resultado, Long> {

    Optional<Resultado> findByInscripcionId(Long inscripcionId);
}
