package com.activetrack.cronometraje.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class RelojConfig {

    /** Reloj inyectable: en las pruebas se reemplaza por uno fijo. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
