package de.nailsbeauty.whatsappgateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registriert den {@link AdminApiKeyFilter} ausschliesslich fuer den {@code /api/*}-Pfad.
 *
 * <p>Damit sind die Verwaltungs-Endpoints geschuetzt, waehrend {@code /webhook} (durch
 * HMAC-Signatur abgesichert) und {@code /actuator/health} oeffentlich erreichbar bleiben.
 */
@Configuration
public class SecurityConfig {

    /**
     * Registriert den Admin-API-Key-Filter fuer alle {@code /api/*}-Routen.
     *
     * @param appProperties externalisierte App-Konfiguration mit dem erwarteten API-Key
     * @param objectMapper  Jackson-Mapper fuer JSON-Fehlerantworten
     * @return Filter-Registrierung, eingeschraenkt auf {@code /api/*}
     */
    @Bean
    public FilterRegistrationBean<AdminApiKeyFilter> adminApiKeyFilter(
            AppProperties appProperties, ObjectMapper objectMapper) {
        var registration = new FilterRegistrationBean<>(
                new AdminApiKeyFilter(appProperties.adminApiKey(), objectMapper));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }
}
