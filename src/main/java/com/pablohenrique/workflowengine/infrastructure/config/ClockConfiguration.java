package com.pablohenrique.workflowengine.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
class ClockConfiguration {

    /**
     * Relógio em UTC com resolução de microssegundos, a mesma do {@code timestamptz} do PostgreSQL:
     * o instante devolvido na resposta de uma escrita é idêntico ao lido depois do banco.
     */
    @Bean
    Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
    }
}
