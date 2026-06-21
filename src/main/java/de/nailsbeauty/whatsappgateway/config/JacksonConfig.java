package de.nailsbeauty.whatsappgateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Stellt einen Jackson-2-{@link ObjectMapper} als Spring-Bean bereit.
 *
 * <p>Spring Boot 4 migriert die Auto-Konfiguration auf Jackson 3
 * ({@code tools.jackson.databind.ObjectMapper}). Jackson 2 ist nur noch transitiv im
 * Classpath, wird aber von dieser Anwendung weiterhin direkt verwendet
 * ({@code com.fasterxml.jackson.databind.ObjectMapper}). Da Boot fuer den Jackson-2-Typ
 * keinen Bean mehr registriert, wird er hier explizit bereitgestellt.
 */
@Configuration
public class JacksonConfig {

    /**
     * Erzeugt einen Standard-{@link ObjectMapper} (Jackson 2) fuer die manuelle
     * JSON-Verarbeitung in Filtern und Services.
     *
     * @return ein einsatzbereiter Jackson-2-{@link ObjectMapper}
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
