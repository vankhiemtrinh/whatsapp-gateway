package de.nailsbeauty.whatsappgateway.dto.response;

import java.time.Instant;

/**
 * Response-DTO einer Studio-Konfiguration.
 *
 * <p>Enthaelt bewusst <strong>kein</strong> Access-Token — Secrets werden nie nach aussen gegeben.
 *
 * @param id                 technische ID
 * @param studioId           fachliche Studio-Kennung
 * @param wabaId             WhatsApp Business Account ID
 * @param phoneNumberId      Phone-Number-ID
 * @param displayPhoneNumber Anzeige-Telefonnummer
 * @param active             Aktiv-Status
 * @param backendBaseUrl     Basis-URL des Studio-Backends fuer das Forwarding
 *                           (das Forwarding-Geheimnis wird bewusst nicht ausgeliefert)
 * @param createdAt          Anlagezeitpunkt
 * @param updatedAt          letzter Aenderungszeitpunkt
 */
public record StudioConfigResponse(
        Long id,
        String studioId,
        String wabaId,
        String phoneNumberId,
        String displayPhoneNumber,
        boolean active,
        String backendBaseUrl,
        Instant createdAt,
        Instant updatedAt
) {
}
