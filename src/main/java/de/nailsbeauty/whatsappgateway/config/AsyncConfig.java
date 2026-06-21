package de.nailsbeauty.whatsappgateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Konfiguriert den Thread-Pool fuer die asynchrone Webhook-Verarbeitung.
 *
 * <p>Der Webhook-Controller antwortet sofort mit {@code 200}; die eigentliche Verarbeitung
 * laeuft entkoppelt in diesem Executor, damit Meta keine Retries (und damit Duplikate)
 * ausloest.
 */
@Configuration
public class AsyncConfig {

    /**
     * Erstellt den Executor fuer {@code @Async}-Webhook-Verarbeitung.
     *
     * @return konfigurierter {@link ThreadPoolTaskExecutor}
     */
    @Bean("webhookExecutor")
    public Executor webhookExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("webhook-");
        executor.initialize();
        return executor;
    }
}
