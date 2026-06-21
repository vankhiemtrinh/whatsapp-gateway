package de.nailsbeauty.whatsappgateway.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Externalisierte Anwendungs-Konfiguration (Prefix {@code app}).
 *
 * @param adminApiKey API-Key zum Schutz der {@code /api/**}-Verwaltungs-Endpoints
 * @param encryption  Einstellungen zur Verschluesselung sensibler Felder at-rest
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String adminApiKey,
        Encryption encryption
) {

    /**
     * Verschluesselungs-Einstellungen.
     *
     * @param key Base64-kodierter 32-Byte-Schluessel fuer AES-256-GCM
     */
    public record Encryption(
            @NotBlank String key
    ) {
    }
}
