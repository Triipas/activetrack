package com.activetrack.resultados.service;

import com.activetrack.resultados.model.EstadoResultado;
import com.activetrack.resultados.model.Resultado;
import com.activetrack.resultados.repository.ConsultaRepository.InscripcionInfo;
import com.activetrack.resultados.repository.ConsultaRepository.PuntoInfo;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Leaderboard en Redis (Sorted Set): se lee en milisegundos sin tocar PostgreSQL.
 * Orden: primero quien llegó más lejos y, a igual punto, quien tiene menor tiempo neto.
 */
@Service
public class LeaderboardService {

    public record Posiciones(Long general, Long categoria) {
    }

    public record LeaderboardFila(Long posicion, Long inscripcionId, Long atletaId, int dorsal, String nombre,
                                  String categoria, String puntoControl, double km, int ordenPunto,
                                  long tiempoNetoMs, long tiempoBrutoMs, String estado) {
    }

    static final int MAX_ORDEN = 1000;
    static final double PUNTAJE_DSQ = 9e15;

    private final StringRedisTemplate redis;

    public LeaderboardService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Menor puntaje = mejor posición. Los descalificados van al final. */
    static double puntaje(int ordenPunto, long netoMs, EstadoResultado estado) {
        if (estado == EstadoResultado.DSQ) {
            return PUNTAJE_DSQ;
        }
        return (double) (MAX_ORDEN - ordenPunto) * 1e11 + netoMs;
    }

    public Posiciones actualizar(InscripcionInfo info, Resultado resultado, PuntoInfo punto) {
        String miembro = info.inscripcionId().toString();
        double puntaje = puntaje(punto.orden(), resultado.getTiempoNetoMs(), resultado.getEstado());
        String claveGeneral = claveGeneral(info.eventoId());
        String claveCategoria = claveCategoria(info.eventoId(), info.categoriaId());

        redis.opsForZSet().add(claveGeneral, miembro, puntaje);
        redis.opsForZSet().add(claveCategoria, miembro, puntaje);

        Map<String, String> ficha = new HashMap<>();
        ficha.put("atletaId", info.atletaId().toString());
        ficha.put("nombre", info.nombre());
        ficha.put("dorsal", String.valueOf(info.dorsal()));
        ficha.put("categoria", info.categoria());
        ficha.put("punto", punto.nombre());
        ficha.put("km", String.valueOf(punto.km()));
        ficha.put("orden", String.valueOf(punto.orden()));
        ficha.put("netoMs", String.valueOf(resultado.getTiempoNetoMs()));
        ficha.put("brutoMs", String.valueOf(resultado.getTiempoBrutoMs()));
        ficha.put("estado", resultado.getEstado().name());
        redis.opsForHash().putAll(claveFicha(info.inscripcionId()), ficha);

        if (resultado.getEstado() == EstadoResultado.DSQ) {
            return new Posiciones(null, null);
        }
        return new Posiciones(posicion(claveGeneral, miembro), posicion(claveCategoria, miembro));
    }

    public List<LeaderboardFila> consultar(Long eventoId, Long categoriaId, int limite) {
        String clave = categoriaId == null ? claveGeneral(eventoId) : claveCategoria(eventoId, categoriaId);
        Set<String> miembros = redis.opsForZSet().range(clave, 0, limite - 1L);
        List<LeaderboardFila> filas = new ArrayList<>();
        if (miembros == null) {
            return filas;
        }
        long posicion = 0;
        for (String miembro : miembros) {
            Map<Object, Object> f = redis.opsForHash().entries(claveFicha(Long.valueOf(miembro)));
            if (f.isEmpty()) {
                continue;
            }
            String estado = (String) f.get("estado");
            Long pos = EstadoResultado.DSQ.name().equals(estado) ? null : ++posicion;
            filas.add(new LeaderboardFila(pos, Long.valueOf(miembro), Long.valueOf((String) f.get("atletaId")),
                    Integer.parseInt((String) f.get("dorsal")), (String) f.get("nombre"), (String) f.get("categoria"),
                    (String) f.get("punto"), Double.parseDouble((String) f.get("km")),
                    Integer.parseInt((String) f.get("orden")), Long.parseLong((String) f.get("netoMs")),
                    Long.parseLong((String) f.get("brutoMs")), estado));
        }
        return filas;
    }

    /** Solo para la demo. */
    public void borrarEvento(Long eventoId, Collection<Long> categorias, Collection<Long> inscripciones) {
        List<String> claves = new ArrayList<>();
        claves.add(claveGeneral(eventoId));
        categorias.forEach(c -> claves.add(claveCategoria(eventoId, c)));
        inscripciones.forEach(i -> claves.add(claveFicha(i)));
        redis.delete(claves);
    }

    private Long posicion(String clave, String miembro) {
        Long rank = redis.opsForZSet().rank(clave, miembro);
        return rank == null ? null : rank + 1;
    }

    static String claveGeneral(Long eventoId) {
        return "leaderboard:" + eventoId;
    }

    static String claveCategoria(Long eventoId, Long categoriaId) {
        return "leaderboard:" + eventoId + ":cat:" + categoriaId;
    }

    static String claveFicha(Long inscripcionId) {
        return "resultado:" + inscripcionId;
    }
}
