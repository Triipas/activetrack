package com.activetrack.inscripciones.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Contador de cupos por categoría en Redis.
 * DECR es atómico: aunque 10 000 personas intenten inscribirse a la vez, nunca se vende más del cupo
 * y no se bloquea la base de datos.
 */
@Service
public class CupoService {

    private final StringRedisTemplate redis;

    public CupoService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Inicializa el contador solo si no existe (no pisa el valor de otra instancia del servicio). */
    public void inicializar(Long categoriaId, long disponibles) {
        redis.opsForValue().setIfAbsent(clave(categoriaId), String.valueOf(disponibles));
    }

    /** Intenta tomar un cupo. Si se revierte la transacción, el cupo se devuelve solo. */
    public boolean reservar(Long categoriaId) {
        Long restantes = redis.opsForValue().decrement(clave(categoriaId));
        if (restantes == null || restantes < 0) {
            redis.opsForValue().increment(clave(categoriaId));
            return false;
        }
        Transacciones.siSeRevierte(() -> redis.opsForValue().increment(clave(categoriaId)));
        return true;
    }

    /** Devuelve un cupo (expiración, reembolso). Si se revierte la transacción, se vuelve a tomar. */
    public void liberar(Long categoriaId) {
        ajustar(categoriaId, 1);
    }

    /** Suma o resta cupos (cuando el organizador cambia el cupo máximo). */
    public void ajustar(Long categoriaId, long delta) {
        redis.opsForValue().increment(clave(categoriaId), delta);
        Transacciones.siSeRevierte(() -> redis.opsForValue().increment(clave(categoriaId), -delta));
    }

    public long disponibles(Long categoriaId) {
        String valor = redis.opsForValue().get(clave(categoriaId));
        return valor == null ? 0 : Math.max(0, Long.parseLong(valor));
    }

    static String clave(Long categoriaId) {
        return "cupos:" + categoriaId;
    }
}
