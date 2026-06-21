package de.nailsbeauty.whatsappgateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Stellt den {@link RestClient} fuer ausgehende Aufrufe an die Meta Graph API bereit.
 */
@Configuration
public class RestClientConfig {

    /**
     * Erstellt einen wiederverwendbaren {@link RestClient} ohne feste Basis-URL.
     *
     * <p>Die vollstaendige Ziel-URL (inkl. API-Version und {@code phone_number_id}) wird pro
     * Aufruf gebaut, da sie je Studio variiert.
     *
     * @return konfigurierter {@link RestClient}
     */
    @Bean
    public RestClient whatsAppRestClient() {
        return RestClient.builder().build();
    }
}
