package com.activetrack.notificaciones.service;

import com.activetrack.notificaciones.observer.AtletaSeguido;
import com.activetrack.notificaciones.observer.CruceDTO;
import com.activetrack.notificaciones.observer.SeguidorAtleta;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/** Mantiene un AtletaSeguido (Subject) por cada atleta que tiene al menos un seguidor. */
@Service
public class GestorSeguimientos {

    private final Map<Long, AtletaSeguido> atletas = new ConcurrentHashMap<>();

    public void seguir(Long atletaId, SeguidorAtleta seguidor) {
        atletas.computeIfAbsent(atletaId, AtletaSeguido::new).suscribir(seguidor);
    }

    public void dejarDeSeguir(Long atletaId, String seguidorId) {
        AtletaSeguido atleta = atletas.get(atletaId);
        if (atleta != null) {
            atleta.desuscribir(seguidorId);
        }
    }

    /** Quita al seguidor de todos los atletas (ej. el espectador cerró la página). */
    public void quitarSeguidor(String seguidorId) {
        atletas.values().forEach(a -> a.desuscribir(seguidorId));
    }

    public void registrarCruce(CruceDTO cruce) {
        AtletaSeguido atleta = atletas.get(cruce.atletaId());
        if (atleta != null) {
            atleta.registrarCruce(cruce);
        }
    }

    /** atletaId -> cantidad de seguidores (para la demo). */
    public Map<Long, Integer> estadisticas() {
        Map<Long, Integer> stats = new TreeMap<>();
        atletas.forEach((id, a) -> {
            if (a.cantidadSeguidores() > 0) {
                stats.put(id, a.cantidadSeguidores());
            }
        });
        return stats;
    }
}
