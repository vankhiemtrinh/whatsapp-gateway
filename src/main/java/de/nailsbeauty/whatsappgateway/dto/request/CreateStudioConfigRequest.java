package de.nailsbeauty.whatsappgateway.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request-DTO zum Anlegen einer Studio-Konfiguration.
 *
 * @param studioId           fachliche, eindeutige Studio-Kennung
 * @param wabaId             WhatsApp Business Account ID
 * @param phoneNumberId      Phone-Number-ID (eindeutig, Routing-Schluessel)
 * @param displayPhoneNumber Anzeige-Telefonnummer (optional)
 * @param accessToken        Access-Token des Studios (wird verschluesselt gespeichert)
 * @param active             ob die Konfiguration aktiv ist
 * @param backendBaseUrl     Basis-URL des Studio-Backends fuer das Forwarding
 *                           eingehender Nachrichten (optional, ohne Trailing-Slash)
 * @param forwardSecret      geteiltes Geheimnis fuer den Header {@code X-Gateway-Secret}
 *                           (optional, wird verschluesselt gespeichert)
 */
public record CreateStudioConfigRequest(
        @NotBlank String studioId,
        @NotBlank String wabaId,
        @NotBlank String phoneNumberId,
        String displayPhoneNumber,
        @NotBlank String accessToken,
        boolean active,
        String backendBaseUrl,
        String forwardSecret
) {
}
