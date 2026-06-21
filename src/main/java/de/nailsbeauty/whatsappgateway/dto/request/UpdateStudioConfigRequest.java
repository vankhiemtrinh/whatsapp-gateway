package de.nailsbeauty.whatsappgateway.dto.request;

/**
 * Request-DTO zum Aktualisieren einer Studio-Konfiguration.
 *
 * <p>Alle Felder sind optional ({@code null} = unveraendert lassen). Die {@code studioId} und
 * {@code phoneNumberId} bleiben als fachliche Schluessel bewusst aussen vor.
 *
 * @param wabaId             neue WABA-ID oder {@code null}
 * @param displayPhoneNumber neue Anzeige-Telefonnummer oder {@code null}
 * @param accessToken        neues Access-Token oder {@code null} (wird verschluesselt gespeichert)
 * @param active             neuer Aktiv-Status oder {@code null}
 * @param backendBaseUrl     neue Backend-Basis-URL fuer das Forwarding oder {@code null}
 * @param forwardSecret      neues Forwarding-Geheimnis oder {@code null} (wird verschluesselt gespeichert)
 */
public record UpdateStudioConfigRequest(
        String wabaId,
        String displayPhoneNumber,
        String accessToken,
        Boolean active,
        String backendBaseUrl,
        String forwardSecret
) {
}
