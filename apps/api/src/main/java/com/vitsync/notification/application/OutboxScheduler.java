package com.vitsync.notification.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Lanza el {@link OutboxWorker} cada pocos segundos.
 *
 * <p>Separado del worker para poder apagarlo con {@code vitsync.outbox.worker.enabled=false}
 * (los tests): así ningún hilo en segundo plano envía mensajes mientras un test comprueba el
 * estado de la tabla.
 *
 * <p>{@code fixedDelay} (y no {@code fixedRate}): la siguiente ejecución empieza X segundos
 * después de que termine la anterior, así nunca se solapan dos lotes en la misma instancia.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "vitsync.outbox.worker.enabled", havingValue = "true", matchIfMissing = true)
class OutboxScheduler {

    private final OutboxWorker worker;

    OutboxScheduler(OutboxWorker worker) {
        this.worker = worker;
    }

    @Scheduled(fixedDelayString = "${vitsync.outbox.worker.interval:5s}")
    void run() {
        worker.processBatch();
    }
}
