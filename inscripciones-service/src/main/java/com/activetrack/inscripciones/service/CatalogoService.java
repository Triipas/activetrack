package com.activetrack.inscripciones.service;

import com.activetrack.inscripciones.model.Evento;
import com.activetrack.inscripciones.model.Ruta;
import com.activetrack.inscripciones.model.Tarifa;
import com.activetrack.inscripciones.repository.CategoriaRepository;
import com.activetrack.inscripciones.repository.EventoRepository;
import com.activetrack.inscripciones.repository.RutaRepository;
import com.activetrack.inscripciones.repository.TarifaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Eventos con inscripciones abiertas, con su tarifa vigente y los cupos disponibles. */
@Service
public class CatalogoService {

    public record CategoriaCatalogo(Long id, String nombre, String genero, int edadMin, int edadMax,
                                    int cupoMaximo, long disponibles) {
    }

    public record TarifaVigente(String nombre, BigDecimal precio, LocalDate vigenteHasta) {
    }

    public record EventoCatalogo(Long id, String nombre, String tipo, Instant fecha, BigDecimal distanciaKm,
                                 TarifaVigente tarifaVigente, List<CategoriaCatalogo> categorias) {
    }

    private final EventoRepository eventos;
    private final RutaRepository rutas;
    private final CategoriaRepository categorias;
    private final TarifaRepository tarifas;
    private final CupoService cupos;
    private final Clock clock;

    public CatalogoService(EventoRepository eventos, RutaRepository rutas, CategoriaRepository categorias,
                           TarifaRepository tarifas, CupoService cupos, Clock clock) {
        this.eventos = eventos;
        this.rutas = rutas;
        this.categorias = categorias;
        this.tarifas = tarifas;
        this.cupos = cupos;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<EventoCatalogo> eventosAbiertos() {
        LocalDate hoy = LocalDate.now(clock.withZone(Evento.ZONA));
        return eventos.findByEstadoOrderByFechaAsc(Evento.INSCRIPCIONES_ABIERTAS).stream().map(e -> {
            TarifaVigente tarifa = ReglasInscripcion.tarifaVigente(tarifas.findByEventoIdOrderByVigenteDesde(e.getId()), hoy)
                    .map((Tarifa t) -> new TarifaVigente(t.getNombre(), t.getPrecio(), t.getVigenteHasta()))
                    .orElse(null);
            BigDecimal distancia = rutas.findByEventoId(e.getId()).stream().findFirst()
                    .map(Ruta::getDistanciaKm).orElse(null);
            List<CategoriaCatalogo> cats = categorias.findByEventoIdOrderById(e.getId()).stream()
                    .map(c -> new CategoriaCatalogo(c.getId(), c.getNombre(), c.getGenero(), c.getEdadMin(),
                            c.getEdadMax(), c.getCupoMaximo(), cupos.disponibles(c.getId())))
                    .toList();
            return new EventoCatalogo(e.getId(), e.getNombre(), e.getTipo(), e.getFecha(), distancia, tarifa, cats);
        }).toList();
    }
}
