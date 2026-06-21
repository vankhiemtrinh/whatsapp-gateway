package de.nailsbeauty.whatsappgateway.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Externalisierte Konfiguration der WhatsApp-Cloud-API-Anbindung (Prefix {@code whatsapp}).
 *
 * @param verifyToken frei gewaehltes Geheimwort fuer die Webhook-Verifizierung, identisch zum
 *                    Wert im Meta App Dashboard
 * @param appSecret   App-Secret aus dem Meta App Dashboard fuer die HMAC-Signaturpruefung
 * @param graph       Einstellungen der Graph-API (Version und Basis-URL)
 */
@Validated
@ConfigurationProperties(prefix = "whatsapp")
public record WhatsAppProperties(
        @NotBlank String verifyToken,
        @NotBlank String appSecret,
        Graph graph
) {

    /**
     * Graph-API-Einstellungen.
     *
     * @param apiVersion Graph-API-Version, z. B. {@code v21.0}
     * @param baseUrl    Basis-URL der Graph-API, z. B. {@code https://graph.facebook.com}
     */
    public record Graph(
            @NotBlank String apiVersion,
            @NotBlank String baseUrl
    ) {
    }
}
