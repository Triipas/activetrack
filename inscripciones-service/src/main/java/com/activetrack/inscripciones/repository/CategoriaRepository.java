package com.activetrack.inscripciones.repository;

import com.activetrack.inscripciones.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByEventoIdOrderById(Long eventoId);
}
