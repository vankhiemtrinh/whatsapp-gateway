package de.nailsbeauty.whatsappgateway.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Stellt die {@link RestClient}-Beans fuer ausgehende Aufrufe bereit — an die Meta
 * Graph API und an die Studio-Backends (Forwarding).
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

    /**
     * Erstellt den {@link RestClient} fuer das Weiterleiten eingehender Events an die
     * Studio-Backends. Mit knappen Timeouts, damit ein langsames/nicht erreichbares
     * Backend einen Webhook-Worker-Thread nicht dauerhaft blockiert.
     *
     * @return konfigurierter {@link RestClient} mit Connect-/Read-Timeout
     */
    @Bean
    public RestClient backendRestClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().requestFactory(factory).build();
    }
}
