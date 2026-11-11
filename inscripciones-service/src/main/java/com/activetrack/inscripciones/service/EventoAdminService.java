package com.activetrack.inscripciones.service;

import com.activetrack.inscripciones.model.Categoria;
import com.activetrack.inscripciones.model.EstadoInscripcion;
import com.activetrack.inscripciones.model.Evento;
import com.activetrack.inscripciones.model.PuntoControl;
import com.activetrack.inscripciones.model.Ruta;
import com.activetrack.inscripciones.model.Tarifa;
import com.activetrack.inscripciones.repository.CategoriaRepository;
import com.activetrack.inscripciones.repository.EventoRepository;
import com.activetrack.inscripciones.repository.InscripcionRepository;
import com.activetrack.inscripciones.repository.InscripcionRepository.ConteoView;
import com.activetrack.inscripciones.repository.InscripcionRepository.InscripcionAdminView;
import com.activetrack.inscripciones.repository.PuntoControlRepository;
import com.activetrack.inscripciones.repository.RutaRepository;
import com.activetrack.inscripciones.repository.TarifaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Configuración de eventos por parte del organizador: rutas, categorías, cupos y tarifas. */
@Service
public class EventoAdminService {

    public record NuevoEvento(String nombre, String tipo, Instant fecha, String ruta, BigDecimal distanciaKm,
                              List<NuevoPunto> puntos, List<NuevaCategoria> categorias, List<NuevaTarifa> tarifas) {
    }

    public record NuevoPunto(String nombre, BigDecimal km, String antenaId) {
    }

    public record NuevaCategoria(String nombre, int edadMin, int edadMax, String genero, int cupoMaximo) {
    }

    public record NuevaTarifa(String nombre, BigDecimal precio, LocalDate vigenteDesde, LocalDate vigenteHasta) {
    }

    public record ResumenCategoria(Long id, String nombre, String genero, int edadMin, int edadMax, int cupoMaximo,
                                   long disponibles, long confirmadas, long pendientesPago, long enEspera) {
    }

    public record ResumenEvento(Long id, String nombre, String tipo, Instant fecha, String estado,
                                List<ResumenCategoria> categorias, List<Tarifa> tarifas) {
    }

    private final EventoRepository eventos;
    private final RutaRepository rutas;
    private final PuntoControlRepository puntos;
    private final CategoriaRepository categorias;
    private final TarifaRepository tarifas;
    private final InscripcionRepository inscripciones;
    private final CupoService cupos;
    private final ListaEsperaService listaEspera;

    public EventoAdminService(EventoRepository eventos, RutaRepository rutas, PuntoControlRepository puntos,
                              CategoriaRepository categorias, TarifaRepository tarifas,
                              InscripcionRepository inscripciones, CupoService cupos, ListaEsperaService listaEspera) {
        this.eventos = eventos;
        this.rutas = rutas;
        this.puntos = puntos;
        this.categorias = categorias;
        this.tarifas = tarifas;
        this.inscripciones = inscripciones;
        this.cupos = cupos;
        this.listaEspera = listaEspera;
    }

    @Transactional
    public ResumenEvento crear(NuevoEvento n) {
        List<NuevoPunto> ordenados = n.puntos().stream().sorted(Comparator.comparing(NuevoPunto::km)).toList();
        if (ordenados.size() < 2 || ordenados.get(0).km().signum() != 0) {
            throw new ReglaNegocioException("La ruta necesita al menos la salida (km 0) y la meta");
        }
        if (ordenados.get(ordenados.size() - 1).km().compareTo(n.distanciaKm()) != 0) {
            throw new ReglaNegocioException("La meta debe estar en el km " + n.distanciaKm());
        }
        List<String> antenas = ordenados.stream().map(NuevoPunto::antenaId).toList();
        if (antenas.stream().distinct().count() != antenas.size() || puntos.existsByAntenaIdIn(antenas)) {
            throw new ReglaNegocioException("Cada punto de control necesita una antena distinta y no usada en otro evento");
        }

        Evento evento = eventos.save(new Evento(n.nombre(), n.tipo().toUpperCase(), n.fecha()));
        Ruta ruta = rutas.save(new Ruta(evento.getId(), n.ruta(), n.distanciaKm()));
        for (int i = 0; i < ordenados.size(); i++) {
            NuevoPunto p = ordenados.get(i);
            puntos.save(new PuntoControl(ruta.getId(), p.nombre(), p.km(), i, i == ordenados.size() - 1, p.antenaId()));
        }
        for (NuevaCategoria c : n.categorias()) {
            if (c.edadMax() < c.edadMin()) {
                throw new ReglaNegocioException("Rango de edades inválido en " + c.nombre());
            }
            Categoria categoria = categorias.save(new Categoria(evento.getId(), c.nombre(), c.edadMin(), c.edadMax(),
                    c.genero().toUpperCase(), c.cupoMaximo()));
            Transacciones.alConfirmar(() -> cupos.inicializar(categoria.getId(), categoria.getCupoMaximo()));
        }
        List<Tarifa> guardadas = new java.util.ArrayList<>();
        for (NuevaTarifa t : n.tarifas()) {
            Tarifa tarifa = nuevaTarifa(evento.getId(), t);
            ReglasInscripcion.validarTarifaNueva(guardadas, tarifa);
            guardadas.add(tarifas.save(tarifa));
        }
        return new ResumenEvento(evento.getId(), evento.getNombre(), evento.getTipo(), evento.getFecha(),
                evento.getEstado(), List.of(), guardadas);
    }

    @Transactional(readOnly = true)
    public List<Evento> listar() {
        return eventos.findAllByOrderByFechaDesc();
    }

    @Transactional(readOnly = true)
    public ResumenEvento resumen(Long eventoId) {
        Evento evento = evento(eventoId);
        Map<Long, Map<EstadoInscripcion, Long>> conteo = inscripciones.contarPorCategoria(eventoId).stream()
                .collect(Collectors.groupingBy(ConteoView::getCategoriaId,
                        Collectors.toMap(ConteoView::getEstado, ConteoView::getTotal)));
        List<ResumenCategoria> resumen = categorias.findByEventoIdOrderById(eventoId).stream().map(c -> {
            Map<EstadoInscripcion, Long> porEstado = conteo.getOrDefault(c.getId(), Map.of());
            return new ResumenCategoria(c.getId(), c.getNombre(), c.getGenero(), c.getEdadMin(), c.getEdadMax(),
                    c.getCupoMaximo(), cupos.disponibles(c.getId()),
                    porEstado.getOrDefault(EstadoInscripcion.CONFIRMADA, 0L),
                    porEstado.getOrDefault(EstadoInscripcion.PENDIENTE_PAGO, 0L),
                    porEstado.getOrDefault(EstadoInscripcion.EN_ESPERA, 0L));
        }).toList();
        return new ResumenEvento(evento.getId(), evento.getNombre(), evento.getTipo(), evento.getFecha(),
                evento.getEstado(), resumen, tarifas.findByEventoIdOrderByVigenteDesde(eventoId));
    }

    @Transactional
    public void cambiarEstado(Long eventoId, String estado) {
        try {
            evento(eventoId).cambiarEstado(estado);
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException(e.getMessage());
        }
    }

    /** Cambia el cupo máximo; si aumenta, los primeros de la lista de espera reciben cupo. */
    @Transactional
    public void cambiarCupo(Long categoriaId, int nuevoCupo) {
        Categoria categoria = categorias.findById(categoriaId)
                .orElseThrow(() -> new NoEncontradoException("Categoría no encontrada"));
        long ocupados = inscripciones.countByCategoriaIdAndEstadoIn(categoriaId, EstadoInscripcion.OCUPAN_CUPO);
        if (nuevoCupo < ocupados) {
            throw new ReglaNegocioException("Ya hay " + ocupados + " cupos ocupados; el cupo no puede ser menor");
        }
        int delta = nuevoCupo - categoria.getCupoMaximo();
        categoria.cambiarCupo(nuevoCupo);
        cupos.ajustar(categoriaId, delta);
        if (delta > 0) {
            listaEspera.promover(categoriaId);
        }
    }

    @Transactional
    public Tarifa agregarTarifa(Long eventoId, NuevaTarifa t) {
        evento(eventoId);
        Tarifa tarifa = nuevaTarifa(eventoId, t);
        ReglasInscripcion.validarTarifaNueva(tarifas.findByEventoIdOrderByVigenteDesde(eventoId), tarifa);
        return tarifas.save(tarifa);
    }

    @Transactional(readOnly = true)
    public List<InscripcionAdminView> inscripciones(Long eventoId, int limite) {
        return inscripciones.listarPorEvento(eventoId, limite);
    }

    /** Exportación de inscritos en CSV (separador ';' para que Excel en español lo abra bien). */
    @Transactional(readOnly = true)
    public String exportarCsv(Long eventoId) {
        StringBuilder csv = new StringBuilder("id;dorsal;chip;atleta;documento;email;categoria;estado;tarifa;precio\n");
        for (InscripcionAdminView i : inscripciones.listarPorEvento(eventoId, Integer.MAX_VALUE)) {
            csv.append(String.join(";", String.valueOf(i.getId()), valor(i.getDorsal()), valor(i.getChipId()),
                    limpiar(i.getAtleta()), i.getDocumento(), limpiar(i.getEmail()), limpiar(i.getCategoria()),
                    i.getEstado(), limpiar(i.getTarifa()), i.getPrecio().toPlainString())).append('\n');
        }
        return csv.toString();
    }

    private Tarifa nuevaTarifa(Long eventoId, NuevaTarifa t) {
        try {
            return new Tarifa(eventoId, t.nombre(), t.precio(), t.vigenteDesde(), t.vigenteHasta());
        } catch (IllegalArgumentException e) {
            throw new ReglaNegocioException(e.getMessage());
        }
    }

    private Evento evento(Long eventoId) {
        return eventos.findById(eventoId).orElseThrow(() -> new NoEncontradoException("Evento no encontrado"));
    }

    private static String valor(Object o) {
        return o == null ? "" : o.toString();
    }

    private static String limpiar(String texto) {
        return texto == null ? "" : texto.replace(';', ',').replace('\n', ' ');
    }
}
